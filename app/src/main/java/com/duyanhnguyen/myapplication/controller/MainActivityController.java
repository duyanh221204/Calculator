package com.duyanhnguyen.myapplication.controller;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivityController {

    private final MainUiShellController shellController;
    private final MainCalculatorController calculatorController;

    public MainActivityController(AppCompatActivity activity) {
        this.shellController = new MainUiShellController(activity);
        this.calculatorController = new MainCalculatorController(activity);
    }

    public void onCreate(Bundle savedInstanceState) {
        shellController.onCreate(savedInstanceState);
        calculatorController.onCreate(savedInstanceState);
        shellController.onViewCreated();
    }

    public void onSaveInstanceState(Bundle outState) {
        shellController.onSaveInstanceState(outState);
        calculatorController.onSaveInstanceState(outState);
    }

    public void onButtonClick(View v) {
        shellController.onButtonClick(v);
        calculatorController.onButtonClick(v);
    }

}
