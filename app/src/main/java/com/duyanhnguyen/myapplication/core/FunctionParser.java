package com.duyanhnguyen.myapplication.core;

import java.util.ArrayList;
import java.util.List;

public class FunctionParser {

    public static class ParseException extends RuntimeException {
        public ParseException(String msg) { super(msg); }
    }

    public interface Expr {
        double eval(double x);
    }

    public static Expr parse(String input) {
        List<Token> tokens = tokenize(input);
        Parser p = new Parser(tokens);
        Expr e = p.parseExpression();
        if (!p.isAtEnd()) {
            throw new ParseException("Dư ký tự không hợp lệ trong biểu thức");
        }
        return e;
    }

    private enum TokType { NUMBER, IDENT, OP, LPAREN, RPAREN, COMMA }

    private static class Token {
        TokType type;
        String text;
        double num;
        Token(TokType type, String text) { this.type = type; this.text = text; }
    }

    private static List<Token> tokenize(String s) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }

            if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                Token t = new Token(TokType.NUMBER, s.substring(start, i));
                t.num = Double.parseDouble(t.text);
                tokens.add(t);
                continue;
            }
            if (Character.isLetter(c)) {
                int start = i;
                while (i < n && Character.isLetter(s.charAt(i))) i++;
                tokens.add(new Token(TokType.IDENT, s.substring(start, i)));
                continue;
            }
            if (c == '(') { tokens.add(new Token(TokType.LPAREN, "(")); i++; continue; }
            if (c == ')') { tokens.add(new Token(TokType.RPAREN, ")")); i++; continue; }
            if (c == ',') { tokens.add(new Token(TokType.COMMA, ",")); i++; continue; }
            if ("+-*/^".indexOf(c) >= 0) {
                tokens.add(new Token(TokType.OP, String.valueOf(c)));
                i++;
                continue;
            }

            if (c == '×') { tokens.add(new Token(TokType.OP, "*")); i++; continue; }
            if (c == '÷') { tokens.add(new Token(TokType.OP, "/")); i++; continue; }

            throw new ParseException("Ký tự không hợp lệ: '" + c + "'");
        }
        return tokens;
    }

    private static final java.util.Set<String> FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "ln", "log", "abs", "exp"));

    private static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(List<Token> tokens) { this.tokens = tokens; }

        boolean isAtEnd() { return pos >= tokens.size(); }
        private Token peek() { return isAtEnd() ? null : tokens.get(pos); }
        private Token advance() { return tokens.get(pos++); }

        Expr parseExpression() {
            Expr left = parseTerm();
            while (!isAtEnd() && peek().type == TokType.OP
                    && (peek().text.equals("+") || peek().text.equals("-"))) {
                String op = advance().text;
                Expr right = parseTerm();
                left = combine(left, right, op);
            }
            return left;
        }

        private Expr parseTerm() {
            Expr left = parsePower();
            while (!isAtEnd() && peek().type == TokType.OP
                    && (peek().text.equals("*") || peek().text.equals("/"))) {
                String op = advance().text;
                Expr right = parsePower();
                left = combine(left, right, op);
            }
            return left;
        }

        private Expr parsePower() {
            Expr base = parseUnary();
            if (!isAtEnd() && peek().type == TokType.OP && peek().text.equals("^")) {
                advance();
                Expr exponent = parsePower();
                return x -> Math.pow(base.eval(x), exponent.eval(x));
            }
            return base;
        }

        private Expr parseUnary() {
            if (!isAtEnd() && peek().type == TokType.OP && peek().text.equals("-")) {
                advance();
                Expr operand = parseUnary();
                return x -> -operand.eval(x);
            }
            return parsePrimary();
        }

        private Expr parsePrimary() {
            if (isAtEnd()) throw new ParseException("Biểu thức thiếu toán hạng");
            Token t = peek();

            if (t.type == TokType.NUMBER) {
                advance();
                double v = t.num;
                return x -> v;
            }
            if (t.type == TokType.LPAREN) {
                advance();
                Expr inner = parseExpression();
                expect(TokType.RPAREN, ")");
                return inner;
            }
            if (t.type == TokType.IDENT) {
                String name = t.text;
                advance();
                if (name.equals("x")) return x -> x;
                if (name.equals("pi")) { double v = Math.PI; return x -> v; }
                if (name.equals("e")) { double v = Math.E; return x -> v; }

                if (FUNCTIONS.contains(name)) {
                    expect(TokType.LPAREN, "(");
                    Expr arg = parseExpression();
                    expect(TokType.RPAREN, ")");
                    return wrapFunction(name, arg);
                }
                throw new ParseException("Không nhận diện được: '" + name + "'");
            }
            throw new ParseException("Không hợp lệ tại token: '" + t.text + "'");
        }

        private void expect(TokType type, String desc) {
            if (isAtEnd() || peek().type != type) {
                throw new ParseException("Thiếu '" + desc + "'");
            }
            advance();
        }

        private Expr combine(Expr left, Expr right, String op) {
            switch (op) {
                case "+": return x -> left.eval(x) + right.eval(x);
                case "-": return x -> left.eval(x) - right.eval(x);
                case "*": return x -> left.eval(x) * right.eval(x);
                case "/": return x -> left.eval(x) / right.eval(x);
                default: throw new ParseException("Toán tử không hợp lệ: " + op);
            }
        }

        private Expr wrapFunction(String name, Expr arg) {
            switch (name) {
                case "sin": return x -> Math.sin(arg.eval(x));
                case "cos": return x -> Math.cos(arg.eval(x));
                case "tan": return x -> Math.tan(arg.eval(x));
                case "asin": return x -> Math.asin(arg.eval(x));
                case "acos": return x -> Math.acos(arg.eval(x));
                case "atan": return x -> Math.atan(arg.eval(x));
                case "sqrt": return x -> Math.sqrt(arg.eval(x));
                case "ln": return x -> Math.log(arg.eval(x));
                case "log": return x -> Math.log10(arg.eval(x));
                case "abs": return x -> Math.abs(arg.eval(x));
                case "exp": return x -> Math.exp(arg.eval(x));
                default: throw new ParseException("Hàm không hỗ trợ: " + name);
            }
        }
    }
}
