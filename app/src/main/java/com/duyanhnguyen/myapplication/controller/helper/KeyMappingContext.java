package com.duyanhnguyen.myapplication.controller.helper;

import com.duyanhnguyen.myapplication.R;

import java.util.HashMap;
import java.util.Map;

public class KeyMappingContext {

    private static final Map<Integer, String> portraitKeyMap = new HashMap<>();

    public static final String[] FUNCTION_TOKENS = {
            "asin(", "acos(", "atan(", "cbrt(",   // 5 chars
            "sin(", "cos(", "tan(", "log(", "abs(", // 4 chars
            "^(",                                   // 2 chars
            "√(",                                    // 2 chars (sqrt symbol)
            "ln("                                   // 3 chars
    };

    static {
        portraitKeyMap.put(R.id.btn_0, "0");
        portraitKeyMap.put(R.id.btn_1, "1");
        portraitKeyMap.put(R.id.btn_2, "2");
        portraitKeyMap.put(R.id.btn_3, "3");
        portraitKeyMap.put(R.id.btn_4, "4");
        portraitKeyMap.put(R.id.btn_5, "5");
        portraitKeyMap.put(R.id.btn_6, "6");
        portraitKeyMap.put(R.id.btn_7, "7");
        portraitKeyMap.put(R.id.btn_8, "8");
        portraitKeyMap.put(R.id.btn_9, "9");

        portraitKeyMap.put(R.id.btn_dot, ".");
        portraitKeyMap.put(R.id.btn_plus, "+");
        portraitKeyMap.put(R.id.btn_minus, "-");
        portraitKeyMap.put(R.id.btn_multiply, "×");
        portraitKeyMap.put(R.id.btn_divide, "÷");

        portraitKeyMap.put(R.id.btn_open_paren, "(");
        portraitKeyMap.put(R.id.btn_close_paren, ")");
        portraitKeyMap.put(R.id.btn_percent, "%");
    }

    public static boolean hasPortraitKey(int id) {
        return portraitKeyMap.containsKey(id);
    }

    public static String getPortraitKey(int id) {
        return portraitKeyMap.get(id);
    }

    public static String getLandscapeKeyValue(int id) {
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

    public static String getKeyValue(int id) {
        if (hasPortraitKey(id)) return getPortraitKey(id);
        return getLandscapeKeyValue(id);
    }

    public static boolean isBinaryOperator(String v) {
        return v.equals("+") || v.equals("-") || v.equals("×") || v.equals("÷") || v.equals("^") || v.equals("^(");
    }

}
