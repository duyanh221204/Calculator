package com.duyanhnguyen.myapplication.core;

import java.util.Stack;

/**
 * Stand-alone stack-based checker for matching parentheses.
 * Detects both a missing ')' (unclosed '(' left on the stack at the end)
 * and an extra/unmatched ')' (a ')' arrives with nothing to pop).
 */
public class BracketValidator {

    private BracketValidator() {
    }

    public static class Result {
        public final boolean valid;
        public final String message;

        private Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        static Result ok() {
            return new Result(true, null);
        }
        static Result fail(String msg) {
            return new Result(false, msg);
        }
    }

    public static Result check(String expression) {
        Stack<Character> stack = new Stack<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                if (stack.isEmpty()) {
                    return Result.fail("Thừa dấu đóng ngoặc ')'");
                }
                stack.pop();
            }
        }

        if (!stack.isEmpty()) {
            return Result.fail("Thiếu " + stack.size() + " dấu đóng ngoặc ')'");
        }
        return Result.ok();
    }

}
