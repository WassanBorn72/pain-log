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
        e.symptoms[0] = 1; e.symptoms[1] = 0; e.location = "肩"; e.notes = "服薬後\n少し改善";
        db.save(e); assertTrue(e.id > 0); db.close();
        db = new EntryStore(RuntimeEnvironment.getApplication());
        Entry restored = db.all().get(0);
        assertEquals(e.notes, restored.notes); assertEquals(e.location, restored.location);
        assertArrayEquals(e.symptoms, restored.symptoms); assertEquals(10, restored.discomfort);
        restored.pain = 5; db.save(restored); assertEquals(1, db.all().size()); assertEquals(5, db.all().get(0).pain);
        db.delete(e.id); assertTrue(db.all().isEmpty()); db.close();
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
            List<Spinner> spinners = views(a, Spinner.class);
            spinners.get(0).setSelection(3); spinners.get(1).setSelection(7); spinners.get(2).setSelection(9); spinners.get(3).setSelection(2);
            List<EditText> inputs = views(a, EditText.class); inputs.get(0).setText("背中"); inputs.get(1).setText("夜に強い");
            controller.recreate(); a = controller.get();
            assertEquals("夜に強い", views(a, EditText.class).get(1).getText().toString());
            assertEquals(3, views(a, Spinner.class).get(0).getSelectedItemPosition());
            findButton(a, "この内容を保存").performClick();
            EntryStore db = new EntryStore(a); assertEquals(1, db.all().size());
            assertEquals(1, db.all().get(0).symptoms[0]); assertEquals(7, db.all().get(0).fatigue);
            findButton(a, "すべての記録をCSVに書き出す").performClick();
            android.content.Intent export = Shadows.shadowOf(a).getNextStartedActivityForResult().intent;
            assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT, export.getAction());
            assertEquals("text/csv", export.getType());
            assertTrue(export.hasCategory(android.content.Intent.CATEGORY_OPENABLE));
            findButton(a, "編集").performClick(); views(a, Spinner.class).get(0).setSelection(5);
            findButton(a, "変更を保存").performClick(); assertEquals(1, db.all().size()); assertEquals(5, db.all().get(0).pain);
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
