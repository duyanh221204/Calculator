package com.duyanhnguyen.myapplication.engine;

import java.util.List;

/**
 * Validates an infix expression BEFORE we ever try to evaluate it:
 *   1. Bracket matching        - delegated to BracketValidator (stack based)
 *   2. Token-sequence rules    - no double operators, no dangling operator,
 *                                functions must be followed by '(', no empty '()'.
 *
 * Only when {@link #validate} returns a valid Result does MainActivity attempt
 * ExpressionEvaluator.evaluate(...) and save the calculation to history.
 */
public class ExpressionValidator {

    private ExpressionValidator() { }

    public static class Result {
        public final boolean valid;
        public final String message;

        private Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        static Result ok() { return new Result(true, null); }
        static Result fail(String msg) { return new Result(false, msg); }
    }

    public static Result validate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return Result.fail("Biểu thức trống");
        }

        BracketValidator.Result br = BracketValidator.check(expression);
        if (!br.valid) return Result.fail(br.message);

        List<String> tokens;
        try {
            tokens = ExpressionEvaluator.tokenize(expression);
        } catch (Exception e) {
            return Result.fail("Không thể phân tích biểu thức");
        }
        if (tokens.isEmpty()) return Result.fail("Biểu thức trống");

        String prev = null;
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            String next = (i + 1 < tokens.size()) ? tokens.get(i + 1) : null;

            // binary operator with nothing valid before it (unary '~' is exempt)
            if (isBinaryOp(t) && (prev == null || isBinaryOp(prev) || "(".equals(prev))) {
                return Result.fail("Thiếu số hạng trước toán tử '" + displaySymbol(t) + "'");
            }
            // binary operator with nothing valid after it
            if (isBinaryOp(t) && (next == null || isBinaryOp(next) || ")".equals(next))) {
                return Result.fail("Thiếu số hạng sau toán tử '" + displaySymbol(t) + "'");
            }
            // empty parentheses "()"
            if ("(".equals(t) && ")".equals(next)) {
                return Result.fail("Dấu ngoặc rỗng '()'");
            }
            // a function name must always be followed by '('
            if (ExpressionEvaluator.isFunction(t) && !"(".equals(next)) {
                return Result.fail("Hàm '" + t + "' phải có dấu '(' theo sau");
            }
            prev = t;
        }

        String last = tokens.get(tokens.size() - 1);
        if (isBinaryOp(last) || "~".equals(last) || ExpressionEvaluator.isFunction(last)) {
            return Result.fail("Biểu thức chưa hoàn chỉnh");
        }

        return Result.ok();
    }

    private static boolean isBinaryOp(String t) {
        if (t == null) return false;
        switch (t) {
            case "+": case "-": case "*": case "/": case "^":
                return true;
            default:
                return false;
        }
    }

    private static String displaySymbol(String internal) {
        switch (internal) {
            case "*": return "×";
            case "/": return "÷";
            default: return internal;
        }
    }

}
