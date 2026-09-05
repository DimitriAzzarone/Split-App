package com.dimitriazzarone.splitapp;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class SplitAccessibilityService extends AccessibilityService {

    private static SplitAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static boolean toggleSplitScreen() {
        if (instance == null) return false;
        return instance.performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
    }
}
