package com.duyanhnguyen.myapplication.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ExpressionConverter {

    private ExpressionConverter() { }

    private static final String[] FUNCTIONS = {
            "asin", "acos", "atan", "sin", "cos", "tan", "log", "ln", "sqrt", "abs", "cbrt"
    };

    public static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int n = expr.length();

        while (i < n) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                ++i;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                StringBuilder num = new StringBuilder();
                while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    num.append(expr.charAt(i));
                    ++i;
                }
                tokens.add(num.toString());
                continue;
            }

            if (Character.isLetter(c)) {
                StringBuilder word = new StringBuilder();
                while (i < n && Character.isLetter(expr.charAt(i))) {
                    word.append(expr.charAt(i));
                    ++i;
                }
                tokens.add(word.toString());
                continue;
            }

            switch (c) {
                case '√':
                    tokens.add("sqrt");
                    break;
                case 'π':
                    tokens.add("π");
                    break;
                case '×':
                    tokens.add("*");
                    break;
                case '÷':
                    tokens.add("/");
                    break;
                case '−':
                    tokens.add("-");
                    break;
                default:
                    tokens.add(String.valueOf(c));
            }
            ++i;
        }
        return markUnaryMinus(tokens);
    }

    private static List<String> markUnaryMinus(List<String> tokens) {
        List<String> result = new ArrayList<>();
        String prev = null;
        for (String t : tokens) {
            if (t.equals("-") && (prev == null || prev.equals("(") || isOperator(prev) || isFunction(prev))) {
                result.add("~");
            } else {
                result.add(t);
            }
            prev = t;
        }
        return result;
    }

    private static int precedence(String op) {
        switch (op) {
            case "+": case "-": return 1;
            case "*": case "/": return 2;
            case "~": return 3;
            case "^": return 4;
            default: return 0;
        }
    }

    private static boolean isRightAssociative(String op) {
        return op.equals("^") || op.equals("~");
    }

    static boolean isOperator(String t) {
        switch (t) {
            case "+": case "-": case "*": case "/": case "^": case "~":
                return true;
            default:
                return false;
        }
    }

    static boolean isFunction(String t) {
        if (t == null) return false;
        for (String f : FUNCTIONS) {
            if (f.equals(t)) return true;
        }
        return false;
    }

    private static boolean isConstant(String t) {
        return t.equals("π") || t.equals("e");
    }

    static boolean isNumber(String t) {
        if (t == null || t.isEmpty()) return false;
        char c0 = t.charAt(0);
        return Character.isDigit(c0) || (c0 == '.' && t.length() > 1);
    }

    public static List<String> infixToPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> opStack = new Stack<>();

        for (String token : tokens) {
            if (isNumber(token) || isConstant(token)) {
                output.add(token);
            } else if (isFunction(token)) {
                opStack.push(token);
            } else if (token.equals("!") || token.equals("%")) {
                output.add(token);
            } else if (isOperator(token)) {
                while (!opStack.isEmpty() && isOperator(opStack.peek()) &&
                        (precedence(opStack.peek()) > precedence(token) ||
                                (precedence(opStack.peek()) == precedence(token) && !isRightAssociative(token)))) {
                    output.add(opStack.pop());
                }
                opStack.push(token);
            } else if (token.equals("(")) {
                opStack.push(token);
            } else if (token.equals(")")) {
                while (!opStack.isEmpty() && !opStack.peek().equals("(")) {
                    output.add(opStack.pop());
                }
                if (opStack.isEmpty()) {
                    throw new ExpressionEvaluator.EvalException("Thừa dấu đóng ngoặc ')'");
                }
                opStack.pop();
                if (!opStack.isEmpty() && isFunction(opStack.peek())) {
                    output.add(opStack.pop());
                }
            } else {
                throw new ExpressionEvaluator.EvalException("Ký tự không xác định: " + token);
            }
        }

        while (!opStack.isEmpty()) {
            if (opStack.peek().equals("(")) {
                throw new ExpressionEvaluator.EvalException("Thừa dấu mở ngoặc '('");
            }
            output.add(opStack.pop());
        }

        return output;
    }

}
