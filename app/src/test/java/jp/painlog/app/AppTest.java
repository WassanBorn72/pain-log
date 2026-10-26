package jp.painlog.app;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AppTest {
    @Before public void clean() { RuntimeEnvironment.getApplication().deleteDatabase("pain-log.db"); }
    @Test public void recordsPersistUpdateAndDelete() {
        EntryStore db = new EntryStore(RuntimeEnvironment.getApplication());
        Entry e = new Entry(); e.time = 123; e.pain = 2; e.fatigue = 8; e.discomfort = 10;
        e.symptoms[0] = 1; e.symptoms[1] = 0; e.locationMask = (1 << 3) | (1 << 5); e.location = "左側"; e.notes = "服薬後\n少し改善";
        db.save(e); assertTrue(e.id > 0); db.close();
        db = new EntryStore(RuntimeEnvironment.getApplication());
        Entry restored = db.all().get(0);
        assertEquals(e.locationMask, restored.locationMask); assertEquals("肩、背中", restored.selectedLocations());
        assertEquals(e.notes, restored.notes); assertEquals(e.location, restored.location);
        assertArrayEquals(e.symptoms, restored.symptoms); assertEquals(10, restored.discomfort);
        restored.locationMask = 1 << 6; restored.pain = 5; db.save(restored); assertEquals(1, db.all().size()); assertEquals(5, db.all().get(0).pain); assertEquals("腰", db.all().get(0).selectedLocations());
        db.delete(e.id); assertTrue(db.all().isEmpty()); db.close();
    }
    @Test public void upgradesLegacyHeadacheWithoutInventingSeverity() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        android.database.sqlite.SQLiteDatabase old = context.openOrCreateDatabase("pain-log.db", 0, null);
        old.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY, time INTEGER, pain INTEGER, fatigue INTEGER, discomfort INTEGER, symptoms TEXT, location TEXT, notes TEXT)");
        for (int i = -1; i <= 1; i++) old.execSQL("INSERT INTO entries VALUES (?, 123, 2, 3, 4, ?, '肩', '旧記録')", new Object[]{i + 2, "1," + i + ",0,-1,1,0,-1,1"});
        old.setVersion(1); old.close();
        EntryStore db = new EntryStore(context);
        List<Entry> records = db.all(); assertEquals(3, records.size());
        for (Entry e : records) {
            assertEquals(0, e.locationMask); assertEquals("肩", e.location);
            assertEquals(-1, e.abdominalPain);
            assertEquals(e.id == 3 ? -2 : e.id == 2 ? 0 : -1, e.headache);
            assertArrayEquals(new int[]{1,0,-1,1,0,-1,1}, e.symptoms);
            assertEquals("旧記録", e.notes);
        }
        db.close();
    }
    @Test public void upgradesVersionTwoPreservingFreeText() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        android.database.sqlite.SQLiteDatabase old = context.openOrCreateDatabase("pain-log.db", 0, null);
        old.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY, time INTEGER, pain INTEGER, fatigue INTEGER, discomfort INTEGER, abdominal_pain INTEGER, headache INTEGER, symptoms TEXT, location TEXT, notes TEXT)");
        old.execSQL("INSERT INTO entries VALUES (1,123,2,3,4,5,6,'1,0,-1,1,0,-1,1','左肩、右手の指先','旧記録')");
        old.setVersion(2); old.close();
        EntryStore db = new EntryStore(context); Entry e = db.all().get(0);
        assertEquals(0, e.locationMask); assertEquals("左肩、右手の指先", e.location); assertEquals(6, e.headache);
        e.locationMask = (1 << 3) | (1 << 10); db.save(e); db.close();
        db = new EntryStore(context); e = db.all().get(0);
        assertEquals("肩、手・指", e.selectedLocations()); assertEquals("左肩、右手の指先", e.location); db.close();
    }
    @Test public void invalidScoreCannotBeSaved() {
        EntryStore db = new EntryStore(RuntimeEnvironment.getApplication());
        Entry e = new Entry(); e.time = 1; e.pain = 0; e.fatigue = 1; e.discomfort = 1;
        assertThrows(android.database.sqlite.SQLiteConstraintException.class, () -> db.save(e)); db.close();
    }
    @Test public void formSurvivesRecreationAndSupportsEditDelete() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get();
            findButton(a, "この内容を保存").performClick();
            assertTrue(new EntryStore(a).all().isEmpty());
            assertEquals(14, views(a, CheckBox.class).size());
            List<Spinner> spinners = views(a, Spinner.class);
            spinners.get(0).setSelection(3); spinners.get(1).setSelection(7); spinners.get(2).setSelection(9); spinners.get(3).setSelection(5); spinners.get(4).setSelection(8); spinners.get(5).setSelection(2);
            views(a, CheckBox.class).get(2).setChecked(true); views(a, CheckBox.class).get(4).setChecked(true);
            List<EditText> inputs = views(a, EditText.class); inputs.get(0).setText("背中"); inputs.get(1).setText("夜に強い");
            controller.recreate(); a = controller.get();
            assertTrue(views(a, CheckBox.class).get(2).isChecked()); assertTrue(views(a, CheckBox.class).get(4).isChecked());
            assertEquals("夜に強い", views(a, EditText.class).get(1).getText().toString());
            assertEquals(3, views(a, Spinner.class).get(0).getSelectedItemPosition());
            assertEquals(5, views(a, Spinner.class).get(3).getSelectedItemPosition());
            findButton(a, "この内容を保存").performClick();
            EntryStore db = new EntryStore(a); assertEquals(1, db.all().size());
            assertEquals("肩、背中", db.all().get(0).selectedLocations());
            assertFalse(views(a, CheckBox.class).get(2).isChecked());
            assertEquals(4, db.all().get(0).abdominalPain); assertEquals(7, db.all().get(0).headache);
            assertEquals(1, db.all().get(0).symptoms[0]); assertEquals(7, db.all().get(0).fatigue);
            findButton(a, "すべての記録をCSVに書き出す").performClick();
            android.content.Intent export = Shadows.shadowOf(a).getNextStartedActivityForResult().intent;
            assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT, export.getAction());
            assertEquals("text/csv", export.getType());
            assertTrue(export.hasCategory(android.content.Intent.CATEGORY_OPENABLE));
            findButton(a, "編集").performClick();
            assertTrue(views(a, CheckBox.class).get(2).isChecked());
            views(a, CheckBox.class).get(2).setChecked(false); views(a, CheckBox.class).get(5).setChecked(true);
            views(a, Spinner.class).get(0).setSelection(5);
            findButton(a, "変更を保存").performClick(); assertEquals(1, db.all().size()); assertEquals(5, db.all().get(0).pain);
            assertEquals("背中、腰", db.all().get(0).selectedLocations());
            findButton(a, "削除").performClick();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog(); dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(db.all().isEmpty()); db.close();
        }
    }
    private static Button findButton(MainActivity a, String text) {
        for (Button b : views(a, Button.class)) if (b.getText().toString().equals(text)) return b;
        throw new AssertionError("Missing button: " + text);
    }
    private static <T> List<T> views(MainActivity a, Class<T> type) {
        List<T> result = new ArrayList<>(); collect(a.findViewById(android.R.id.content), type, result); return result;
    }
    private static <T> void collect(View v, Class<T> type, List<T> result) {
        if (type.isInstance(v)) result.add(type.cast(v));
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup)v).getChildCount(); i++) collect(((ViewGroup)v).getChildAt(i), type, result);
    }
}
