package com.duyanhnguyen.myapplication.controller;

import android.util.Log;

import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;

public class MathInkManager {

    private static final String TAG = "MathInkManager";
    private DigitalInkRecognizer recognizer;

    public interface InkCallback {
        void onResult(String recognizedText);
        void onError(String error);
        void onModelDownloaded();
    }

    public MathInkManager() {
        DigitalInkRecognitionModelIdentifier modelIdentifier;
        try {

            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en");
            if (modelIdentifier == null) {
                return;
            }
            DigitalInkRecognitionModel model = DigitalInkRecognitionModel.builder(modelIdentifier).build();

            recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model).build());

        } catch (Exception e) {
            Log.e(TAG, "Error initializing MathInkManager", e);
        }
    }

    public void downloadModelIfNeeded(InkCallback callback) {
        if (recognizer == null) {
            callback.onError("Recognizer not initialized");
            return;
        }

        DigitalInkRecognitionModelIdentifier modelIdentifier;
        try {
            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en");
            if (modelIdentifier == null) return;

            DigitalInkRecognitionModel model = DigitalInkRecognitionModel.builder(modelIdentifier).build();
            RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();

            remoteModelManager.isModelDownloaded(model).addOnSuccessListener(isDownloaded -> {
                if (isDownloaded) {
                    callback.onModelDownloaded();
                } else {
                    DownloadConditions conditions = new DownloadConditions.Builder().build();
                    remoteModelManager.download(model, conditions)
                            .addOnSuccessListener(aVoid -> callback.onModelDownloaded())
                            .addOnFailureListener(e -> callback.onError("Failed to download model: " + e.getMessage()));
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void recognize(Ink ink, InkCallback callback) {
        if (recognizer == null) {
            callback.onError("Recognizer not initialized");
            return;
        }

        if (ink.getStrokes().isEmpty()) {
            callback.onResult("");
            return;
        }

        recognizer.recognize(ink)
                .addOnSuccessListener(result -> {
                    if (result.getCandidates().size() > 0) {
                        String bestCandidate = result.getCandidates().get(0).getText();
                        callback.onResult(bestCandidate);
                    } else {
                        callback.onResult("");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Recognition failed", e);
                    callback.onError("Recognition failed: " + e.getMessage());
                });
    }

    public void close() {
        if (recognizer != null) {
            recognizer.close();
        }
    }
}
