package com.duyanhnguyen.myapplication.controller.helper;

import com.duyanhnguyen.myapplication.R;

public class KeyMappingContext {

    public static final String[] FUNCTION_TOKENS = {
            "asin(", "acos(", "atan(", "cbrt(",
            "sin(", "cos(", "tan(", "log(", "abs(",
            "^(",
            "√(",
            "ln("
    };

    public static String getPortraitValue(int id) {
        if (id == R.id.btn_0) return "0";
        if (id == R.id.btn_1) return "1";
        if (id == R.id.btn_2) return "2";
        if (id == R.id.btn_3) return "3";
        if (id == R.id.btn_4) return "4";
        if (id == R.id.btn_5) return "5";
        if (id == R.id.btn_6) return "6";
        if (id == R.id.btn_7) return "7";
        if (id == R.id.btn_8) return "8";
        if (id == R.id.btn_9) return "9";
        if (id == R.id.btn_dot) return ".";
        if (id == R.id.btn_plus) return "+";
        if (id == R.id.btn_minus) return "-";
        if (id == R.id.btn_multiply) return "×";
        if (id == R.id.btn_divide) return "÷";
        if (id == R.id.btn_open_paren) return "(";
        if (id == R.id.btn_close_paren) return ")";
        if (id == R.id.btn_percent) return "%";
        return null;
    }

    public static String getLandscapeValue(int id) {
        if (id == R.id.btn_sin) return "sin(";
        if (id == R.id.btn_cos) return "cos(";
        if (id == R.id.btn_tan) return "tan(";
        if (id == R.id.btn_log) return "log(";
        if (id == R.id.btn_ln) return "ln(";
        if (id == R.id.btn_sqrt) return "√(";
        if (id == R.id.btn_power) return "^(";
        if (id == R.id.btn_factorial) return "!";
        if (id == R.id.btn_pi) return "π";
        if (id == R.id.btn_e) return "e";
        if (id == R.id.btn_abs) return "abs(";
        if (id == R.id.btn_cbrt) return "cbrt(";
        if (id == R.id.btn_asin) return "asin(";
        if (id == R.id.btn_acos) return "acos(";
        if (id == R.id.btn_atan) return "atan(";
        return null;
    }

    public static String getValue(int id) {
        String value = getPortraitValue(id);
        if (value != null) return value;
        return getLandscapeValue(id);
    }

    public static boolean isBinaryOperator(String v) {
        return v.equals("+")
                || v.equals("-")
                || v.equals("×")
                || v.equals("÷")
                || v.equals("^")
                || v.equals("^(");
    }

}
