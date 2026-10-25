package jp.painlog.app;

import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int INK = 0xff253d38, TEAL = 0xff226e63, PAPER = 0xfff6f7f2;
    private EntryStore store;
    private LinearLayout root, form, history;
    private ScrollView formScroll, historyScroll;
    private Spinner pain, fatigue, discomfort, abdominalPain, headache;
    private final Spinner[] symptoms = new Spinner[Entry.SYMPTOMS.length];
    private EditText location, notes;
    private Button dateButton, saveButton;
    private final Calendar selected = Calendar.getInstance();
    private long editingId;
    private int tab;
    private boolean exporting;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.JAPAN);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new EntryStore(this);
        root = column(); root.setPadding(dp(20), dp(12), dp(20), 0); root.setBackgroundColor(PAPER);
        setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(dp(20) + bars.left, dp(12) + bars.top, dp(20) + bars.right, bars.bottom);
                return insets;
            });
        }
        root.addView(label("PAIN LOG  /  自分のペースで", 12, TEAL));
        TextView title = label("からだの記録", 28, INK); title.setTypeface(null, Typeface.BOLD); root.addView(title);
        root.addView(label("今日の体調を、少しずつ残しましょう。", 14, INK));
        LinearLayout tabs = new LinearLayout(this);
        Button recordTab = button("記録する", () -> showTab(0));
        Button historyTab = button("履歴・書き出し", () -> showTab(1));
        tabs.addView(recordTab, new LinearLayout.LayoutParams(0, dp(56), 1));
        tabs.addView(historyTab, new LinearLayout.LayoutParams(0, dp(56), 1)); root.addView(tabs);
        formScroll = new ScrollView(this); form = column(); formScroll.setFillViewport(true); formScroll.addView(form);
        historyScroll = new ScrollView(this); history = column(); historyScroll.addView(history);
        root.addView(formScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(historyScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        buildForm();
        if (state != null) {
            editingId = state.getLong("editingId"); selected.setTimeInMillis(state.getLong("time"));
            abdominalPain.setSelection(state.getInt("abdominalPain")); headache.setSelection(state.getInt("headache"));
            pain.setSelection(state.getInt("pain")); fatigue.setSelection(state.getInt("fatigue")); discomfort.setSelection(state.getInt("discomfort"));
            for (int i = 0; i < symptoms.length; i++) symptoms[i].setSelection(state.getInt("symptom" + i));
            location.setText(state.getString("location", "")); notes.setText(state.getString("notes", ""));
            exporting = state.getBoolean("exporting"); tab = state.getInt("tab");
        }
        updateDate(); showTab(tab);
    }
    private void buildForm() {
        LinearLayout when = card(form, "記録日時");
        dateButton = button("", this::pickDate); when.addView(dateButton);
        LinearLayout scales = card(form, "症状の強さ");
        scales.addView(label("1＝最も軽い · 10＝最も強い（すべて必須）", 13, INK));
        pain = rating(scales, "身体の痛み（腹痛・頭痛を除く）"); fatigue = rating(scales, "怠さ"); discomfort = rating(scales, "症状に対する不快感");
        LinearLayout separate = card(form, "腹痛・頭痛");
        separate.addView(label("身体の痛みとは別に記録します。なし／未記録も選べます。", 13, INK));
        abdominalPain = separateRating(separate, "腹痛", false);
        headache = separateRating(separate, "頭痛", true);
        LinearLayout flags = card(form, "気になる身体症状");
        flags.addView(label("わからない・答えない項目は「未記録」のままで。", 13, INK));
        for (int i = 0; i < symptoms.length; i++) {
            flags.addView(label(Entry.SYMPTOMS[i], 16, INK));
            symptoms[i] = spinner(new String[]{"未記録", "なし", "あり"}); flags.addView(symptoms[i]);
        }
        LinearLayout memo = card(form, "メモ");
        memo.addView(label("痛む部位（任意）", 15, INK)); location = input("例：肩、背中、全身", false); memo.addView(location);
        memo.addView(label("その他自由記述（任意）", 15, INK));
        notes = input("症状の変化、服薬、睡眠、きっかけなど", true); memo.addView(notes);
        saveButton = button("この内容を保存", this::save); form.addView(saveButton);
        form.addView(button("入力をリセット／編集をやめる", () -> new AlertDialog.Builder(this).setMessage("入力内容を消して、新しい記録に戻りますか？").setNegativeButton("戻る", null).setPositiveButton("リセット", (d,w) -> reset()).show()));
        form.addView(label("記録は端末内に保存されます。診断を行うアプリではありません。", 12, INK));
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private TextView label(String text, int size, int color) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); v.setPadding(0, dp(7), 0, dp(7)); return v;
    }
    private LinearLayout card(LinearLayout parent, String title) {
        LinearLayout v = column(); v.setPadding(dp(16), dp(10), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(18)); v.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(10), 0, dp(6)); parent.addView(v, p);
        TextView heading = label(title, 19, TEAL); heading.setTypeface(null, Typeface.BOLD); v.addView(heading); return v;
    }
    private Button button(String text, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setTextColor(TEAL); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setOnClickListener(v -> action.run()); return b;
    }
    private Spinner spinner(String[] options) {
        Spinner s = new Spinner(this); ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a); s.setMinimumHeight(dp(48)); return s;
    }
    private Spinner rating(LinearLayout parent, String name) {
        parent.addView(label(name, 16, INK)); String[] values = new String[11]; values[0] = "選択してください";
        for (int i = 1; i <= 10; i++) values[i] = i + " / 10";
        Spinner s = spinner(values); s.setContentDescription(name + "、1から10"); parent.addView(s); return s;
    }
    private Spinner separateRating(LinearLayout parent, String name, boolean legacy) {
        parent.addView(label(name, 16, INK));
        String[] options = new String[legacy ? 13 : 12];
        options[0] = "未記録"; options[1] = "なし";
        for (int i = 1; i <= 10; i++) options[i + 1] = i + " / 10";
        if (legacy) options[12] = "あり（強さ未記録）";
        Spinner s = spinner(options); s.setContentDescription(name + "の強さ"); parent.addView(s); return s;
    }
    private int painValue(Spinner s) { return s.getSelectedItemPosition() == 12 ? -2 : s.getSelectedItemPosition() - 1; }
    private int painPosition(int value) { return value == -2 ? 12 : value + 1; }
    private EditText input(String hint, boolean multiline) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(16); e.setTextColor(INK);
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (multiline ? android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        e.setMinLines(multiline ? 3 : 1); e.setGravity(android.view.Gravity.TOP); return e;
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void updateDate() { dateButton.setText(getString(jp.painlog.app.R.string.change_record_date, dateFormat.format(selected.getTime()))); saveButton.setText(editingId == 0 ? "この内容を保存" : "変更を保存"); }
    private void pickDate() {
        new DatePickerDialog(this, (view,y,m,d) -> {
            new TimePickerDialog(this, (v,h,min) -> { selected.set(y,m,d); selected.set(Calendar.HOUR_OF_DAY,h); selected.set(Calendar.MINUTE,min); selected.set(Calendar.SECOND,0); updateDate(); }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true).show();
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
    }
    private Entry current() {
        Entry e = new Entry(); e.id = editingId; e.time = selected.getTimeInMillis();
        e.abdominalPain = painValue(abdominalPain); e.headache = painValue(headache);
        e.pain = pain.getSelectedItemPosition(); e.fatigue = fatigue.getSelectedItemPosition(); e.discomfort = discomfort.getSelectedItemPosition();
        for (int i = 0; i < symptoms.length; i++) e.symptoms[i] = symptoms[i].getSelectedItemPosition() - 1;
        e.location = location.getText().toString(); e.notes = notes.getText().toString(); return e;
    }
    private void save() {
        Entry e = current();
        if (e.pain == 0 || e.fatigue == 0 || e.discomfort == 0) { toast("身体の痛み・怠さ・不快感をすべて選択してください"); return; }
        try { store.save(e); reset(); showTab(1); toast("保存しました"); }
        catch (RuntimeException ex) { toast("保存できませんでした。空き容量などを確認してください。"); }
    }
    private void reset() {
        abdominalPain.setSelection(0); headache.setSelection(0);
        editingId = 0; selected.setTimeInMillis(System.currentTimeMillis()); pain.setSelection(0); fatigue.setSelection(0); discomfort.setSelection(0);
        for (Spinner s : symptoms) s.setSelection(0); location.setText(""); notes.setText(""); updateDate(); formScroll.smoothScrollTo(0,0);
    }
    private void edit(Entry e) {
        Entry draft = current();
        if (draft.abdominalPain != -1 || draft.headache != -1 || draft.id != 0 || draft.pain != 0 || draft.fatigue != 0 || draft.discomfort != 0 || !draft.location.isEmpty() || !draft.notes.isEmpty() || Arrays.stream(draft.symptoms).anyMatch(s -> s != -1)) {
            new AlertDialog.Builder(this).setMessage("入力中の内容を破棄して、この記録を編集しますか？").setNegativeButton("戻る", null).setPositiveButton("編集", (d,w) -> load(e)).show();
        } else load(e);
    }
    private void load(Entry e) {
        abdominalPain.setSelection(painPosition(e.abdominalPain)); headache.setSelection(painPosition(e.headache));
        editingId = e.id; selected.setTimeInMillis(e.time); pain.setSelection(e.pain); fatigue.setSelection(e.fatigue); discomfort.setSelection(e.discomfort);
        for (int i = 0; i < symptoms.length; i++) symptoms[i].setSelection(e.symptoms[i] + 1);
        location.setText(e.location); notes.setText(e.notes); updateDate(); showTab(0); formScroll.smoothScrollTo(0,0);
    }
    private void showTab(int which) {
        tab = which; formScroll.setVisibility(which == 0 ? View.VISIBLE : View.GONE); historyScroll.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        if (which == 1) renderHistory();
    }
    private void renderHistory() {
        history.removeAllViews();
        try {
            List<Entry> entries = store.all();
            history.addView(label("これまでの記録  ·  " + entries.size() + "件", 20, INK));
            Button export = button(exporting ? "書き出し中…" : "すべての記録をCSVに書き出す", this::export);
            export.setEnabled(!entries.isEmpty() && !exporting); history.addView(export);
            history.addView(label("Excel・Googleスプレッドシートで開けます。書き出し先は自分で選べます。アプリの削除で端末内の記録も消えるため、大切な記録は書き出してください。", 13, INK));
            if (entries.isEmpty()) history.addView(label("まだ記録がありません。\n「記録する」から、今日の体調を残してみましょう。", 16, INK));
            for (Entry e : entries) {
                LinearLayout c = card(history, dateFormat.format(new Date(e.time)));
                c.addView(label("身体の痛み " + e.pain + "  /  怠さ " + e.fatigue + "  /  不快感 " + e.discomfort, 17, INK));
                c.addView(label("腹痛：" + CsvExporter.painText(e.abdominalPain) + "  /  頭痛：" + CsvExporter.painText(e.headache), 16, INK));
                StringBuilder flags = new StringBuilder();
                for (int i = 0; i < e.symptoms.length; i++) flags.append(Entry.SYMPTOMS[i]).append("：").append(e.symptoms[i] == 1 ? "あり" : e.symptoms[i] == 0 ? "なし" : "未記録").append('\n');
                c.addView(label(flags.toString().trim(), 14, INK));
                if (!e.location.isEmpty()) c.addView(label("部位：" + e.location, 15, INK));
                if (!e.notes.isEmpty()) c.addView(label(e.notes, 15, INK));
                c.addView(button("編集", () -> edit(e)));
                c.addView(button("削除", () -> new AlertDialog.Builder(this).setTitle("この記録を削除しますか？").setMessage("削除すると元に戻せません。").setNegativeButton("戻る", null).setPositiveButton("削除", (d,w) -> {
                    try { store.delete(e.id); if (editingId == e.id) reset(); renderHistory(); toast("削除しました"); }
                    catch (RuntimeException ex) { toast("削除できませんでした"); }
                }).show()));
            }
        } catch (RuntimeException e) { history.addView(label("記録を読み込めませんでした。アプリのデータを消さずに再起動してください。", 16, INK)); }
    }
    private void export() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "pain-log-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".csv");
        try { startActivityForResult(intent, 100); } catch (android.content.ActivityNotFoundException ex) { toast("保存先を選ぶアプリがありません"); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (request != 100 || result != RESULT_OK || data == null || data.getData() == null) return;
        android.net.Uri uri = data.getData(); exporting = true; if (tab == 1) renderHistory();
        new Thread(() -> {
            String message;
            try {
                List<Entry> snapshot = store.all();
                try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
                    if (stream == null) throw new IOException("No stream");
                    try (Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) { CsvExporter.write(writer, snapshot, ZoneId.systemDefault()); }
                }
                message = snapshot.size() + "件を書き出しました";
            } catch (IOException | RuntimeException ex) { message = "書き出せませんでした。保存先と空き容量を確認し、再試行してください。"; }
            String finalMessage = message;
            runOnUiThread(() -> { exporting = false; if (!isDestroyed()) { toast(finalMessage); if (tab == 1) renderHistory(); } });
        }).start();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putLong("editingId", editingId); state.putLong("time", selected.getTimeInMillis());
        state.putInt("abdominalPain", abdominalPain.getSelectedItemPosition()); state.putInt("headache", headache.getSelectedItemPosition());
        state.putInt("pain", pain.getSelectedItemPosition()); state.putInt("fatigue", fatigue.getSelectedItemPosition()); state.putInt("discomfort", discomfort.getSelectedItemPosition());
        for (int i = 0; i < symptoms.length; i++) state.putInt("symptom" + i, symptoms[i].getSelectedItemPosition());
        state.putString("location", location.getText().toString()); state.putString("notes", notes.getText().toString()); state.putInt("tab", tab);
        // A worker belongs to the previous Activity; allow another export after recreation.
        state.putBoolean("exporting", false);
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
