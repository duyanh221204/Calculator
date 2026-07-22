package com.duyanhnguyen.myapplication;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.duyanhnguyen.myapplication.controller.MainActivityController;

public class MainActivity extends AppCompatActivity {

    private MainActivityController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new MainActivityController(this);
        controller.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (controller != null) {
            controller.onSaveInstanceState(outState);
        }
    }

    public void onButtonClick(View v) {
        if (controller != null) {
            controller.onButtonClick(v);
        }
    }

}
