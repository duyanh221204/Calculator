package com.duyanhnguyen.myapplication.core;

import java.util.List;
import java.util.Stack;

public class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    public static class EvalException extends RuntimeException {
        public EvalException(String message) {
            super(message);
        }
    }

    public static double evaluate(String rawExpression, boolean degreeMode) {
        List<String> tokens = ExpressionConverter.tokenize(rawExpression);
        List<String> postfix = ExpressionConverter.infixToPostfix(tokens);
        return evaluatePostfix(postfix, degreeMode);
    }

    public static double evaluatePostfix(List<String> postfix, boolean degreeMode) {
        Stack<Double> stack = new Stack<>();

        for (String token : postfix) {
            if (ExpressionConverter.isNumber(token)) {
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
                stack.push(pop(stack) / 100.0);
            } else if (ExpressionConverter.isFunction(token)) {
                double v = pop(stack);
                stack.push(applyFunction(token, v, degreeMode));
            } else if (ExpressionConverter.isOperator(token)) {
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
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "*":
                return a * b;
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
            case "abs":
                return Math.abs(v);
            case "cbrt":
                return Math.cbrt(v);
            default:
                throw new EvalException("Hàm không xác định: " + fn);
        }
    }

    private static double factorial(double v) {
        if (v < 0 || v != Math.floor(v) || v > 170) {
            throw new EvalException("Lỗi: giai thừa chỉ áp dụng cho số nguyên 0-170");
        }
        double result = 1;
        for (int i = 2; i <= (int) v; ++i) result *= i;
        return result;
    }

}
