package com.duyanhnguyen.myapplication.core;

public class ExpressionPreprocessor {

    private ExpressionPreprocessor() { }

    public static String normalize(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return "";
        }

        String processed = ocrText.replaceAll("\\s+", "");

        if (processed.endsWith("=")) {
            processed = processed.substring(0, processed.length() - 1);
        }

        processed = processed.replace('x', '*').replace('X', '*');

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < processed.length(); i++) {
            char curr = processed.charAt(i);
            sb.append(curr);

            if (i < processed.length() - 1) {
                char next = processed.charAt(i + 1);

                boolean currIsDigit = Character.isDigit(curr);
                boolean nextIsDigit = Character.isDigit(next);
                boolean currIsClosingParen = (curr == ')');
                boolean nextIsOpeningParen = (next == '(');
                boolean currIsConstant = (curr == 'π' || curr == 'e');
                boolean nextIsConstant = (next == 'π' || next == 'e');
                boolean nextIsFunction = (Character.isLetter(next) && next != 'π' && next != 'e');

                boolean shouldAddMultiply = false;

                if (currIsDigit && (nextIsOpeningParen || nextIsConstant || nextIsFunction)) {
                    shouldAddMultiply = true;
                } else if (currIsClosingParen && (nextIsDigit || nextIsOpeningParen || nextIsConstant || nextIsFunction)) {
                    shouldAddMultiply = true;
                } else if (currIsConstant && (nextIsDigit || nextIsOpeningParen || nextIsFunction)) {
                    shouldAddMultiply = true;
                }

                if (shouldAddMultiply) {
                    sb.append("*");
                }
            }
        }

        return sb.toString();
    }
}
