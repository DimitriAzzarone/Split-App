package com.dimitriazzarone.splitapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private Spinner spinner1;
    private Spinner spinner2;

    private final List<AppEntry> apps = new ArrayList<>();

    static class AppEntry {
        String name;
        String packageName;

        AppEntry(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Split App");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        description.setText("\nScegli due applicazioni da aprire insieme.\n");
        description.setTextSize(18);

        spinner1 = new Spinner(this);
        spinner2 = new Spinner(this);

        Button openButton = new Button(this);
        openButton.setText("APRI IN 2");

        root.addView(title);
        root.addView(description);
        root.addView(spinner1);
        root.addView(spinner2);
        root.addView(openButton);

        setContentView(root);

        loadApps();

        openButton.setOnClickListener(v -> openSelectedApps());
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved =
                pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL);

        for (ResolveInfo info : resolved) {

            String packageName = info.activityInfo.packageName;

            if (packageName.equals(getPackageName())) {
                continue;
            }

            CharSequence label = info.loadLabel(pm);

            apps.add(new AppEntry(
                    label != null ? label.toString() : packageName,
                    packageName
            ));
        }

        Collections.sort(apps, Comparator.comparing(a -> a.name.toLowerCase()));

        ArrayAdapter<AppEntry> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        apps
                );

        spinner1.setAdapter(adapter);
        spinner2.setAdapter(adapter);

        if (apps.size() > 1) {
            spinner2.setSelection(1);
        }
    }

    private void openSelectedApps() {

        if (apps.size() < 2) {
            Toast.makeText(
                    this,
                    "Non ci sono abbastanza app disponibili.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        AppEntry first = (AppEntry) spinner1.getSelectedItem();
        AppEntry second = (AppEntry) spinner2.getSelectedItem();

        if (first.packageName.equals(second.packageName)) {
            Toast.makeText(
                    this,
                    "Scegli due app diverse.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        PackageManager pm = getPackageManager();

        Intent firstIntent =
                pm.getLaunchIntentForPackage(first.packageName);

        Intent secondIntent =
                pm.getLaunchIntentForPackage(second.packageName);

        if (firstIntent == null || secondIntent == null) {
            Toast.makeText(
                    this,
                    "Impossibile aprire una delle app.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        try {

            firstIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            secondIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            );

            startActivity(firstIntent);

            rootPostDelayed(secondIntent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Errore: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void rootPostDelayed(Intent secondIntent) {
        spinner1.postDelayed(
                () -> {
                    try {
                        startActivity(secondIntent);
                    } catch (Exception e) {
                        Toast.makeText(
                                this,
                                "Seconda app non avviata.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                },
                700
        );
    }
}
