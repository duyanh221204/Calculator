package com.duyanhnguyen.myapplication.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.duyanhnguyen.myapplication.R;
import com.duyanhnguyen.myapplication.ui.activity.GraphActivity;

public class MainUiShellController {

    private static final String PREFS_THEME = "theme_prefs";
    private static final String KEY_IS_DARK = "is_dark_mode";
    private static final String KEY_IS_DEG = "is_deg_mode";

    private final AppCompatActivity activity;

    public MainUiShellController(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
    }

    public void onViewCreated() {
        updateThemeIcon();
        updateDegRadLabel();
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onButtonClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_theme) {
            toggleTheme();
            updateThemeIcon();
        } else if (id == R.id.btn_rotate) {
            rotateScreen();
        } else if (id == R.id.btn_deg_rad) {
            toggleDegRad();
            updateDegRadLabel();
        } else if (id == R.id.btn_graph) {
            openGraphActivity();
        } else if (id == R.id.btn_math_note) {
            openMathNoteActivity();
        }
    }

    public boolean isDegMode() {
        return activity.getSharedPreferences(PREFS_THEME, AppCompatActivity.MODE_PRIVATE)
                .getBoolean(KEY_IS_DEG, true);
    }

    private void toggleDegRad() {
        boolean newIsDeg = !isDegMode();
        activity.getSharedPreferences(PREFS_THEME, AppCompatActivity.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_DEG, newIsDeg)
                .apply();
    }

    private void updateDegRadLabel() {
        Button btn = activity.findViewById(R.id.btn_deg_rad);
        if (btn != null) {
            btn.setText(isDegMode() ? "DEG" : "RAD");
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_THEME, AppCompatActivity.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_IS_DARK, true);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void toggleTheme() {
        boolean currentlyDark = isNightMode();
        boolean newIsDark = !currentlyDark;

        activity.getSharedPreferences(PREFS_THEME, AppCompatActivity.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_DARK, newIsDark)
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                newIsDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private boolean isNightMode() {
        int nightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateThemeIcon() {
        ImageButton themeBtn = activity.findViewById(R.id.btn_theme);
        if (themeBtn != null) {
            themeBtn.setImageResource(
                    isNightMode() ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        }
    }

    private void rotateScreen() {
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private int getCurrentEffectiveOrientation() {
        int requested = activity.getRequestedOrientation();
        if (requested != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            return requested;
        }
        int configOrientation = activity.getResources().getConfiguration().orientation;
        if (configOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        } else if (configOrientation == Configuration.ORIENTATION_PORTRAIT) {
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        }
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    private void openGraphActivity() {
        Intent intent = new Intent(activity, GraphActivity.class);
        intent.putExtra("EXTRA_ORIENTATION", getCurrentEffectiveOrientation());
        activity.startActivity(intent);
    }

    private void openMathNoteActivity() {
        Intent intent = new Intent(activity, com.duyanhnguyen.myapplication.ui.activity.MathNoteActivity.class);
        intent.putExtra("EXTRA_ORIENTATION", getCurrentEffectiveOrientation());
        activity.startActivity(intent);
    }
}
