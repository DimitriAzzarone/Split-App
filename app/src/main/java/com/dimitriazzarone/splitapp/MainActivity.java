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
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS = "split_pairs";
    private static final int MAX_SCAN_PAIRS = 1000;
    private static final int REQ_EXPORT_PAIRS = 7001;
    private static final int REQ_IMPORT_PAIRS = 7002;

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
        Button exportPairs = new Button(this);
        exportPairs.setText("ESPORTA COPPIE IN FILE");
        exportPairs.setOnClickListener(v -> exportPairs());
        root.addView(exportPairs, lpMatchWrap());

        Button importPairs = new Button(this);
        importPairs.setText("IMPORTA COPPIE DA FILE");
        importPairs.setOnClickListener(v -> importPairs());
        root.addView(importPairs, lpMatchWrap());

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

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 12, 24, 12);

        EditText search = new EditText(this);
        search.setHint("Digita il nome dell'app...");
        box.addView(search, lpMatchWrap());

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.addView(results);
        box.addView(resultScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 700));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(slot == 1 ? "Scegli App 1" : "Scegli App 2")
                .setView(box)
                .setNegativeButton("ANNULLA", null)
                .create();

        Runnable refresh = () -> {
            String q = search.getText().toString().trim().toLowerCase();
            results.removeAllViews();
            for (AppEntry entry : apps) {
                if (!q.isEmpty() && !entry.name.toLowerCase().contains(q)) continue;
                TextView row = new TextView(this);
                row.setText(entry.name);
                row.setTextSize(18);
                row.setPadding(18, 20, 18, 20);
                row.setOnClickListener(v -> {
                    if (slot == 1) {
                        selected1 = entry;
                        app1Button.setText("App 1   " + entry.name);
                    } else {
                        selected2 = entry;
                        app2Button.setText("App 2   " + entry.name);
                    }
                    dialog.dismiss();
                });
                results.addView(row, lpMatchWrap());
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { refresh.run(); }
            public void afterTextChanged(Editable s) {}
        });

        dialog.setOnShowListener(d -> refresh.run());
        dialog.show();
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
        int free = 0;
        while (prefs.contains("p1_" + free)) free++;

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

    private JSONArray buildPairsJson() throws Exception {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        JSONArray arr = new JSONArray();

        for (int i = 0; i < MAX_SCAN_PAIRS; i++) {
            String p1 = prefs.getString("p1_" + i, null);
            String p2 = prefs.getString("p2_" + i, null);
            if (p1 == null || p2 == null) continue;

            JSONObject o = new JSONObject();
            o.put("p1", p1);
            o.put("n1", prefs.getString("n1_" + i, p1));
            o.put("p2", p2);
            o.put("n2", prefs.getString("n2_" + i, p2));
            arr.put(o);
        }

        return arr;
    }

    private void exportPairs() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "split_app_backup.json");
        startActivityForResult(intent, REQ_EXPORT_PAIRS);
    }

    private void importPairs() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQ_IMPORT_PAIRS);
    }

    private void writePairsToUri(Uri uri) {
        try {
            JSONArray arr = buildPairsJson();

            try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new Exception("Impossibile aprire il file");
                out.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            Toast.makeText(
                    this,
                    "Backup salvato. Coppie esportate: " + arr.length(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Errore backup: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void readPairsFromUri(Uri uri) {
        try {
            StringBuilder sb = new StringBuilder();

            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new Exception("Impossibile aprire il file");

                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    sb.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
                }
            }

            JSONArray arr = new JSONArray(sb.toString());
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();

            for (int i = 0; i < MAX_SCAN_PAIRS; i++) {
                ed.remove("p1_" + i)
                  .remove("n1_" + i)
                  .remove("p2_" + i)
                  .remove("n2_" + i);
            }

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                String p1 = o.getString("p1");
                String p2 = o.getString("p2");

                ed.putString("p1_" + i, p1);
                ed.putString("n1_" + i, o.optString("n1", p1));
                ed.putString("p2_" + i, p2);
                ed.putString("n2_" + i, o.optString("n2", p2));
            }

            ed.apply();
            refreshSavedPairs();

            Toast.makeText(
                    this,
                    "Coppie ripristinate: " + arr.length(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Errore ripristino: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQ_EXPORT_PAIRS) {
            writePairsToUri(uri);
        } else if (requestCode == REQ_IMPORT_PAIRS) {
            readPairsFromUri(uri);
        }
    }

    private void refreshSavedPairs() {
        if (pairsContainer == null) return;

        pairsContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        boolean found = false;

        for (int i = 0; i < MAX_SCAN_PAIRS; i++) {
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
