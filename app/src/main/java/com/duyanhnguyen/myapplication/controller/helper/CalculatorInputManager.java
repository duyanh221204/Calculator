package com.duyanhnguyen.myapplication.controller.helper;

public class CalculatorInputManager {

    private CalculatorInputManager() { }

    public static int getLeadingZeroToRemove(String currentText, int cursor, String value) {
        if (cursor == 0) return 0;
        char prev = currentText.charAt(cursor - 1);
        if (prev != '0') return 0;

        boolean isIsolatedZero = false;
        if (cursor == 1) {
            isIsolatedZero = true;
        } else {
            char prevPrev = currentText.charAt(cursor - 2);
            if (!Character.isDigit(prevPrev) && prevPrev != '.') {
                isIsolatedZero = true;
            }
        }

        if (isIsolatedZero) {
            boolean isDigitOrConst = Character.isDigit(value.charAt(0)) || value.equals("π") || value.equals("e");
            boolean isFuncOrParen = value.endsWith("(") && !value.equals("^(");
            if (isDigitOrConst || isFuncOrParen) {
                return 1;
            }
        }
        return 0;
    }

    public static boolean shouldAddImplicitMultiply(String value, String currentText, int cursor) {
        if (cursor == 0) return false;

        char prev = currentText.charAt(cursor - 1);
        boolean prevIsDigitOrDot = Character.isDigit(prev) || prev == '.';
        boolean prevIsRightOperand = prev == ')' || prev == '!' || prev == 'π' || prev == 'e';

        boolean valueIsDigitOrDot = Character.isDigit(value.charAt(0)) || value.equals(".");
        boolean valueIsFunctionOrParen = (value.endsWith("(") && !value.equals("^(")) || value.equals("π") || value.equals("e");

        if (prevIsRightOperand && (valueIsDigitOrDot || valueIsFunctionOrParen)) {
            return true;
        }
        if (prevIsDigitOrDot && valueIsFunctionOrParen) {
            return true;
        }
        return false;
    }

    public static boolean hasInvalidLeadingZero(String text) {
        for (int i = 0; i < text.length() - 1; ++i) {
            if (text.charAt(i) == '0' && Character.isDigit(text.charAt(i + 1))) {
                if (i == 0) return true;
                char before = text.charAt(i - 1);
                if (!Character.isDigit(before) && before != '.') return true;
            }
        }
        return false;
    }

    public static boolean currentNumberHasDecimal(String s, int cursorPos) {
        for (int i = cursorPos - 1; i >= 0; --i) {
            char c = s.charAt(i);
            if (c == '.') return true;
            if (!Character.isDigit(c)) break;
        }
        for (int i = cursorPos; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '.') return true;
            if (!Character.isDigit(c)) break;
        }
        return false;
    }

    public static int getFunctionDeleteLength(String text, int cursor) {
        for (String fn : KeyMappingContext.FUNCTION_TOKENS) {
            if (cursor >= fn.length()
                    && text.substring(cursor - fn.length(), cursor).equals(fn)) {
                return fn.length();
            }
        }
        return 1;
    }

}
