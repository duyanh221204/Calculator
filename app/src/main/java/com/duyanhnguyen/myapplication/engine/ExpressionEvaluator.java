package com.duyanhnguyen.myapplication.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Core math engine of the calculator.
 *
 * Pipeline:
 *   infix expression (String)
 *      -> tokenize()          produces a list of tokens
 *      -> infixToPostfix()    Shunting-Yard algorithm (operator Stack = the "stack")
 *      -> evaluatePostfix()   evaluates the RPN token list (value Stack = the "stack")
 */
public class ExpressionEvaluator {

    private ExpressionEvaluator() { }

    /** Thrown for any problem that only shows up once we actually try to evaluate. */
    public static class EvalException extends RuntimeException {
        public EvalException(String message) { super(message); }
    }

    private static final String[] FUNCTIONS = {
            "asin", "acos", "atan", "sin", "cos", "tan", "log", "ln", "sqrt"
    };

    // ---------------------------------------------------------------------
    // Public entry points
    // ---------------------------------------------------------------------

    public static double evaluate(String rawExpression, boolean degreeMode) {
        List<String> tokens = tokenize(rawExpression);
        List<String> postfix = infixToPostfix(tokens);
        return evaluatePostfix(postfix, degreeMode);
    }

    // ---------------------------------------------------------------------
    // 1. Tokenizer
    // ---------------------------------------------------------------------

    public static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int n = expr.length();

        while (i < n) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                StringBuilder num = new StringBuilder();
                while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    num.append(expr.charAt(i));
                    i++;
                }
                tokens.add(num.toString());
                continue;
            }

            if (Character.isLetter(c)) {
                StringBuilder word = new StringBuilder();
                while (i < n && Character.isLetter(expr.charAt(i))) {
                    word.append(expr.charAt(i));
                    i++;
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
                    tokens.add(String.valueOf(c)); // + - * / ^ % ( ) ! e
            }
            i++;
        }
        return markUnaryMinus(tokens);
    }

    /** Rewrites every *unary* '-' into the internal token "~" so it can get its own precedence. */
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

    // ---------------------------------------------------------------------
    // Token classification helpers
    // ---------------------------------------------------------------------

    private static int precedence(String op) {
        switch (op) {
            case "+": case "-": return 1;
            case "*": case "/": return 2;
            case "~": return 3;          // unary minus
            case "^": return 4;          // right-associative
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
        for (String f : FUNCTIONS) if (f.equals(t)) return true;
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

    // ---------------------------------------------------------------------
    // 2. Shunting-Yard: infix -> postfix (Stack<String> used as the operator stack)
    // ---------------------------------------------------------------------

    public static List<String> infixToPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> opStack = new Stack<>();

        for (String token : tokens) {
            if (isNumber(token) || isConstant(token)) {
                output.add(token);
            } else if (isFunction(token)) {
                opStack.push(token);
            } else if (token.equals("!") || token.equals("%")) {
                // Postfix unary operators: they apply to whatever value is already
                // sitting on top of the evaluation stack, so emit right away.
                // % = divide by 100 (percentage), ! = factorial
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
                    throw new EvalException("Thừa dấu đóng ngoặc ')'");
                }
                opStack.pop(); // discard the matching "("
                if (!opStack.isEmpty() && isFunction(opStack.peek())) {
                    output.add(opStack.pop());
                }
            } else {
                throw new EvalException("Ký tự không xác định: " + token);
            }
        }

        while (!opStack.isEmpty()) {
            String op = opStack.pop();
            if (op.equals("(")) throw new EvalException("Thiếu dấu đóng ngoặc ')'");
            output.add(op);
        }
        return output;
    }

    // ---------------------------------------------------------------------
    // 3. Postfix evaluator (Stack<Double> used as the value stack)
    // ---------------------------------------------------------------------

    public static double evaluatePostfix(List<String> postfix, boolean degreeMode) {
        Stack<Double> stack = new Stack<>();

        for (String token : postfix) {
            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (token.equals("π")) {
                stack.push(Math.PI);
            } else if (token.equals("e")) {
                stack.push(Math.E);
            } else if (token.equals("~")) {
                stack.push(-pop(stack));
            } else if (token.equals("!")) {
                stack.push(factorial(pop(stack)));
            } else if (token.equals("%")) {
                // Percentage: divide by 100 (e.g. 5% = 0.05)
                stack.push(pop(stack) / 100.0);
            } else if (isFunction(token)) {
                double v = pop(stack);
                stack.push(applyFunction(token, v, degreeMode));
            } else if (isOperator(token)) {
                double b = pop(stack);
                double a = pop(stack);
                stack.push(applyOperator(token, a, b));
            } else {
                throw new EvalException("Không thể tính: " + token);
            }
        }

        if (stack.size() != 1) throw new EvalException("Biểu thức không hợp lệ");
        double result = stack.pop();
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new EvalException("Kết quả không xác định");
        }
        return result;
    }

    private static double pop(Stack<Double> stack) {
        if (stack.isEmpty()) throw new EvalException("Biểu thức không hợp lệ");
        return stack.pop();
    }

    private static double applyOperator(String op, double a, double b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new EvalException("Lỗi: không thể chia cho 0");
                return a / b;
            case "^":
                return Math.pow(a, b);
            default:
                throw new EvalException("Toán tử không hợp lệ: " + op);
        }
    }

    private static double applyFunction(String fn, double v, boolean degreeMode) {
        switch (fn) {
            case "sin": return Math.sin(degreeMode ? Math.toRadians(v) : v);
            case "cos": return Math.cos(degreeMode ? Math.toRadians(v) : v);
            case "tan": return Math.tan(degreeMode ? Math.toRadians(v) : v);
            case "asin": {
                double r = Math.asin(v);
                return degreeMode ? Math.toDegrees(r) : r;
            }
            case "acos": {
                double r = Math.acos(v);
                return degreeMode ? Math.toDegrees(r) : r;
            }
            case "atan": {
                double r = Math.atan(v);
                return degreeMode ? Math.toDegrees(r) : r;
            }
            case "log":
                if (v <= 0) throw new EvalException("Lỗi: log của số phải > 0");
                return Math.log10(v);
            case "ln":
                if (v <= 0) throw new EvalException("Lỗi: ln của số phải > 0");
                return Math.log(v);
            case "sqrt":
                if (v < 0) throw new EvalException("Lỗi: căn bậc hai của số âm");
                return Math.sqrt(v);
            default:
                throw new EvalException("Hàm không xác định: " + fn);
        }
    }

    private static double factorial(double v) {
        if (v < 0 || v != Math.floor(v) || v > 170) {
            throw new EvalException("Lỗi: giai thừa chỉ áp dụng cho số nguyên 0-170");
        }
        double result = 1;
        for (int i = 2; i <= (int) v; i++) result *= i;
        return result;
    }

}
