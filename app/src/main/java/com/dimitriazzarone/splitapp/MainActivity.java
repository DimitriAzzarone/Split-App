package com.dimitriazzarone.splitapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS = "split_pairs";
    private static final int MAX_PAIRS = 12;

    private final List<AppEntry> apps = new ArrayList<>();

    private LinearLayout pairsContainer;
    private TextView app1Button;
    private TextView app2Button;

    private AppEntry selected1;
    private AppEntry selected2;

    static class AppEntry {
        final String name;
        final String packageName;

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

        loadApps();
        buildUi();
        refreshSavedPairs();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 36);
        root.setBackgroundColor(Color.rgb(248, 247, 250));

        TextView title = new TextView(this);
        title.setText("Split App");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(40, 35, 55));
        root.addView(title, lpMatchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Crea una coppia di app e avviala in schermo diviso.");
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 24);
        root.addView(subtitle, lpMatchWrap());

        LinearLayout chooser = new LinearLayout(this);
        chooser.setOrientation(LinearLayout.VERTICAL);
        chooser.setPadding(16, 16, 16, 16);
        chooser.setBackground(makeRounded(Color.rgb(233, 227, 248), 22));

        app1Button = makeChoice("App 1   +");
        app2Button = makeChoice("App 2   +");

        app1Button.setOnClickListener(v -> pickApp(1));
        app2Button.setOnClickListener(v -> pickApp(2));

        chooser.addView(app1Button, lpMatchWrap());

        TextView swap = new TextView(this);
        swap.setText("⇅");
        swap.setTextSize(26);
        swap.setGravity(Gravity.CENTER);
        swap.setPadding(0, 8, 0, 8);
        swap.setOnClickListener(v -> swapSelection());
        chooser.addView(swap, lpMatchWrap());

        chooser.addView(app2Button, lpMatchWrap());

        root.addView(chooser, lpMatchWrap());

        Button save = new Button(this);
        save.setText("SALVA COPPIA");
        save.setOnClickListener(v -> saveCurrentPair());
        root.addView(save, lpMatchWrap());

        Button launch = new Button(this);
        launch.setText("AVVIA ORA");
        launch.setOnClickListener(v -> launchCurrentPair());
        root.addView(launch, lpMatchWrap());

        Button accessibility = new Button(this);
        accessibility.setText("ACCESSIBILITÀ (OPZIONALE)");
        accessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        root.addView(accessibility, lpMatchWrap());

        TextView help = new TextView(this);
        help.setText(
                "Se il Lenovo non divide stabilmente lo schermo, abilita Split App " +
                "nelle impostazioni Accessibilità e riprova."
        );
        help.setTextSize(14);
        help.setPadding(6, 6, 6, 18);
        root.addView(help, lpMatchWrap());

        TextView savedTitle = new TextView(this);
        savedTitle.setText("Coppie salvate");
        savedTitle.setTextSize(22);
        savedTitle.setPadding(0, 14, 0, 10);
        root.addView(savedTitle, lpMatchWrap());

        pairsContainer = new LinearLayout(this);
        pairsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(pairsContainer, lpMatchWrap());

        scroll.addView(root);
        setContentView(scroll);
    }

    private LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private TextView makeChoice(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18);
        view.setTextColor(Color.rgb(45, 35, 70));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(22, 24, 22, 24);
        view.setBackground(makeRounded(Color.rgb(246, 241, 255), 16));

        LinearLayout.LayoutParams lp = lpMatchWrap();
        lp.setMargins(0, 8, 0, 8);
        view.setLayoutParams(lp);
        return view;
    }

    private GradientDrawable makeRounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private void loadApps() {
        apps.clear();

        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> results =
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);

        for (ResolveInfo info : results) {
            String packageName = info.activityInfo.packageName;

            if (packageName.equals(getPackageName())) {
                continue;
            }

            CharSequence label = info.loadLabel(pm);

            apps.add(
                    new AppEntry(
                            label == null ? packageName : label.toString(),
                            packageName
                    )
            );
        }

        Collections.sort(
                apps,
                Comparator.comparing(a -> a.name.toLowerCase())
        );
    }

    private void pickApp(int slot) {
        if (apps.isEmpty()) {
            Toast.makeText(this, "Nessuna app disponibile.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] names = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            names[i] = apps.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle(slot == 1 ? "Scegli App 1" : "Scegli App 2")
                .setItems(names, (dialog, which) -> {
                    AppEntry entry = apps.get(which);

                    if (slot == 1) {
                        selected1 = entry;
                        app1Button.setText("App 1   " + entry.name);
                    } else {
                        selected2 = entry;
                        app2Button.setText("App 2   " + entry.name);
                    }
                })
                .show();
    }

    private void swapSelection() {
        AppEntry temp = selected1;
        selected1 = selected2;
        selected2 = temp;

        app1Button.setText(
                selected1 == null ? "App 1   +" : "App 1   " + selected1.name
        );

        app2Button.setText(
                selected2 == null ? "App 2   +" : "App 2   " + selected2.name
        );
    }

    private void saveCurrentPair() {
        if (!hasValidSelection()) return;

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        int free = -1;
        for (int i = 0; i < MAX_PAIRS; i++) {
            if (!prefs.contains("p1_" + i)) {
                free = i;
                break;
            }
        }

        if (free == -1) {
            Toast.makeText(
                    this,
                    "Hai raggiunto il limite di coppie salvate.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        prefs.edit()
                .putString("p1_" + free, selected1.packageName)
                .putString("n1_" + free, selected1.name)
                .putString("p2_" + free, selected2.packageName)
                .putString("n2_" + free, selected2.name)
                .apply();

        refreshSavedPairs();

        Toast.makeText(this, "Coppia salvata.", Toast.LENGTH_SHORT).show();
    }

    private boolean hasValidSelection() {
        if (selected1 == null || selected2 == null) {
            Toast.makeText(
                    this,
                    "Scegli prima App 1 e App 2.",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        if (selected1.packageName.equals(selected2.packageName)) {
            Toast.makeText(
                    this,
                    "Scegli due app diverse.",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        return true;
    }

    private void launchCurrentPair() {
        if (!hasValidSelection()) return;

        launchPair(selected1.packageName, selected2.packageName);
    }

    private void launchPair(String firstPackage, String secondPackage) {
        PackageManager pm = getPackageManager();

        Intent first = pm.getLaunchIntentForPackage(firstPackage);
        Intent second = pm.getLaunchIntentForPackage(secondPackage);

        if (first == null || second == null) {
            Toast.makeText(
                    this,
                    "Una delle due app non può essere avviata.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        try {
            first.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(first);

            app1Button.postDelayed(() -> {
                boolean accessibilityUsed = false;

                if (SplitAccessibilityService.isAvailable()) {
                    accessibilityUsed = SplitAccessibilityService.toggleSplitScreen();
                }

                final boolean used = accessibilityUsed;

                app1Button.postDelayed(() -> {
                    try {
                        second.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        );

                        startActivity(second);

                        if (used) {
                            Toast.makeText(
                                    this,
                                    "Avvio con modalità Accessibilità.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(
                                this,
                                "Seconda app non avviata: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }, used ? 500 : 150);

            }, 700);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Errore di avvio: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void refreshSavedPairs() {
        if (pairsContainer == null) return;

        pairsContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        boolean found = false;

        for (int i = 0; i < MAX_PAIRS; i++) {
            String p1 = prefs.getString("p1_" + i, null);
            String p2 = prefs.getString("p2_" + i, null);

            if (p1 == null || p2 == null) {
                continue;
            }

            found = true;

            String n1 = prefs.getString("n1_" + i, p1);
            String n2 = prefs.getString("n2_" + i, p2);

            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 14, 10, 14);
            row.setBackground(makeRounded(Color.WHITE, 18));

            LinearLayout.LayoutParams rowLp = lpMatchWrap();
            rowLp.setMargins(0, 7, 0, 7);
            row.setLayoutParams(rowLp);

            TextView label = new TextView(this);
            label.setText(n1 + "  +  " + n2);
            label.setTextSize(17);
            label.setPadding(10, 6, 10, 6);

            LinearLayout.LayoutParams labelLp =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                    );

            row.addView(label, labelLp);

            Button start = new Button(this);
            start.setText("▶");
            start.setOnClickListener(v -> launchPair(p1, p2));
            row.addView(start);

            Button delete = new Button(this);
            delete.setText("✕");
            delete.setOnClickListener(v -> {
                prefs.edit()
                        .remove("p1_" + index)
                        .remove("n1_" + index)
                        .remove("p2_" + index)
                        .remove("n2_" + index)
                        .apply();

                refreshSavedPairs();
            });
            row.addView(delete);

            pairsContainer.addView(row);
        }

        if (!found) {
            TextView empty = new TextView(this);
            empty.setText("Nessuna coppia salvata.");
            empty.setTextSize(16);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 22, 0, 22);
            pairsContainer.addView(empty);
        }
    }
}
