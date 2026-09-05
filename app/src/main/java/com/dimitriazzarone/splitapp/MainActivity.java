package com.dimitriazzarone.splitapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(244, 247, 250));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(20), dp(20), dp(20));
        header.setBackground(makeRounded(Color.rgb(30, 43, 59), dp(22)));

        TextView title = new TextView(this);
        title.setText("Split App");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        header.addView(title, lpMatchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Le tue coppie di app, pronte in un tocco");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(210, 220, 230));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(6), 0, 0);
        header.addView(subtitle, lpMatchWrap());

        LinearLayout.LayoutParams headerLp = lpMatchWrap();
        headerLp.setMargins(0, 0, 0, dp(16));
        root.addView(header, headerLp);

        TextView createTitle = new TextView(this);
        createTitle.setText("Nuova coppia");
        createTitle.setTextSize(21);
        createTitle.setTypeface(Typeface.DEFAULT_BOLD);
        createTitle.setTextColor(Color.rgb(35, 45, 58));
        createTitle.setPadding(dp(4), 0, 0, dp(8));
        root.addView(createTitle, lpMatchWrap());

        LinearLayout chooser = new LinearLayout(this);
        chooser.setOrientation(LinearLayout.VERTICAL);
        chooser.setPadding(dp(14), dp(14), dp(14), dp(14));
        chooser.setBackground(makeRounded(Color.WHITE, dp(20)));

        app1Button = makeChoice("＋  Scegli App 1");
        app2Button = makeChoice("＋  Scegli App 2");

        app1Button.setOnClickListener(v -> pickApp(1));
        app2Button.setOnClickListener(v -> pickApp(2));

        chooser.addView(app1Button);

        TextView swap = new TextView(this);
        swap.setText("⇅");
        swap.setTextSize(28);
        swap.setTextColor(Color.rgb(78, 99, 125));
        swap.setGravity(Gravity.CENTER);
        swap.setPadding(0, dp(6), 0, dp(6));
        swap.setOnClickListener(v -> swapSelection());
        chooser.addView(swap, lpMatchWrap());

        chooser.addView(app2Button);

        LinearLayout.LayoutParams chooserLp = lpMatchWrap();
        chooserLp.setMargins(0, 0, 0, dp(12));
        root.addView(chooser, chooserLp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);

        Button save = makeActionButton("SALVA");
        save.setOnClickListener(v -> saveCurrentPair());

        Button launch = makeActionButton("AVVIA ORA");
        launch.setOnClickListener(v -> launchCurrentPair());

        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        actionLp.setMargins(dp(4), dp(4), dp(4), dp(4));

        actions.addView(save, actionLp);
        actions.addView(launch, actionLp);
        root.addView(actions, lpMatchWrap());

        Button accessibility = new Button(this);
        accessibility.setText("Modalità Accessibilità");
        accessibility.setAllCaps(false);
        accessibility.setTextSize(15);
        accessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        LinearLayout.LayoutParams accLp = lpMatchWrap();
        accLp.setMargins(0, dp(8), 0, dp(4));
        root.addView(accessibility, accLp);

        TextView help = new TextView(this);
        help.setText(
                "Se una delle due app esce dallo schermo diviso, abilita Split App in Accessibilità. " +
                "La v0.3 usa una sequenza più lenta e stabile."
        );
        help.setTextSize(13);
        help.setTextColor(Color.rgb(90, 100, 112));
        help.setPadding(dp(6), dp(2), dp(6), dp(14));
        root.addView(help, lpMatchWrap());

        TextView savedTitle = new TextView(this);
        savedTitle.setText("Coppie salvate");
        savedTitle.setTextSize(21);
        savedTitle.setTypeface(Typeface.DEFAULT_BOLD);
        savedTitle.setTextColor(Color.rgb(35, 45, 58));
        savedTitle.setPadding(dp(4), dp(8), 0, dp(8));
        root.addView(savedTitle, lpMatchWrap());

        pairsContainer = new LinearLayout(this);
        pairsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(pairsContainer, lpMatchWrap());

        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView makeChoice(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18);
        view.setTextColor(Color.rgb(35, 45, 58));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackground(makeRounded(Color.rgb(239, 244, 249), dp(16)));

        LinearLayout.LayoutParams lp = lpMatchWrap();
        lp.setMargins(0, dp(5), 0, dp(5));
        view.setLayoutParams(lp);

        return view;
    }

    private Button makeActionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        return button;
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

            if (packageName.equals(getPackageName())) continue;

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
                        app1Button.setText("App 1  •  " + entry.name);
                    } else {
                        selected2 = entry;
                        app2Button.setText("App 2  •  " + entry.name);
                    }
                })
                .show();
    }

    private void swapSelection() {
        AppEntry temp = selected1;
        selected1 = selected2;
        selected2 = temp;

        app1Button.setText(
                selected1 == null ? "＋  Scegli App 1" : "App 1  •  " + selected1.name
        );

        app2Button.setText(
                selected2 == null ? "＋  Scegli App 2" : "App 2  •  " + selected2.name
        );
    }

    private void saveCurrentPair() {
        if (!hasValidSelection()) return;

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        for (int i = 0; i < MAX_PAIRS; i++) {
            String p1 = prefs.getString("p1_" + i, null);
            String p2 = prefs.getString("p2_" + i, null);

            if (selected1.packageName.equals(p1) && selected2.packageName.equals(p2)) {
                Toast.makeText(this, "Questa coppia è già salvata.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

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

            // Più tempo alla prima app per stabilizzarsi sul Lenovo.
            pairsContainer.postDelayed(() -> {

                boolean usedAccessibility = false;

                if (SplitAccessibilityService.isAvailable()) {
                    usedAccessibility = SplitAccessibilityService.toggleSplitScreen();
                }

                final boolean accessibilityOk = usedAccessibility;

                // Niente MULTIPLE_TASK nella v0.3: può creare task separati instabili.
                pairsContainer.postDelayed(() -> {
                    try {
                        second.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                        );

                        startActivity(second);

                        if (accessibilityOk) {
                            Toast.makeText(
                                    this,
                                    "Split avviato con Accessibilità.",
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
                }, accessibilityOk ? 900 : 450);

            }, 1200);

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

            if (p1 == null || p2 == null) continue;

            found = true;

            String n1 = prefs.getString("n1_" + i, p1);
            String n2 = prefs.getString("n2_" + i, p2);

            final int index = i;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(12), dp(10), dp(12));
            card.setBackground(makeRounded(Color.WHITE, dp(18)));

            LinearLayout.LayoutParams cardLp = lpMatchWrap();
            cardLp.setMargins(0, dp(6), 0, dp(6));
            card.setLayoutParams(cardLp);

            LinearLayout icons = new LinearLayout(this);
            icons.setOrientation(LinearLayout.HORIZONTAL);
            icons.setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon1 = makeIcon(p1);
            ImageView icon2 = makeIcon(p2);

            icons.addView(icon1);
            icons.addView(icon2);

            card.addView(icons);

            LinearLayout textBox = new LinearLayout(this);
            textBox.setOrientation(LinearLayout.VERTICAL);
            textBox.setPadding(dp(12), 0, dp(8), 0);

            TextView label = new TextView(this);
            label.setText(n1 + "  +  " + n2);
            label.setTextSize(17);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(Color.rgb(40, 50, 62));

            TextView hint = new TextView(this);
            hint.setText("Tocca per avviare");
            hint.setTextSize(13);
            hint.setTextColor(Color.rgb(110, 120, 130));

            textBox.addView(label);
            textBox.addView(hint);

            LinearLayout.LayoutParams textLp =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                    );

            card.addView(textBox, textLp);

            TextView delete = new TextView(this);
            delete.setText("✕");
            delete.setTextSize(20);
            delete.setGravity(Gravity.CENTER);
            delete.setPadding(dp(14), dp(10), dp(14), dp(10));

            delete.setOnClickListener(v -> {
                prefs.edit()
                        .remove("p1_" + index)
                        .remove("n1_" + index)
                        .remove("p2_" + index)
                        .remove("n2_" + index)
                        .apply();

                refreshSavedPairs();
            });

            card.addView(delete);

            card.setOnClickListener(v -> launchPair(p1, p2));

            pairsContainer.addView(card);
        }

        if (!found) {
            TextView empty = new TextView(this);
            empty.setText("Nessuna coppia salvata.\nCreane una qui sopra.");
            empty.setTextSize(16);
            empty.setTextColor(Color.rgb(100, 110, 120));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(22), 0, dp(22));
            pairsContainer.addView(empty);
        }
    }

    private ImageView makeIcon(String packageName) {
        ImageView image = new ImageView(this);

        int size = dp(42);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(size, size);

        lp.setMargins(0, 0, dp(6), 0);
        image.setLayoutParams(lp);

        try {
            Drawable icon = getPackageManager().getApplicationIcon(packageName);
            image.setImageDrawable(icon);
        } catch (Exception ignored) {
            image.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        return image;
    }

    private LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private GradientDrawable makeRounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
