package jp.painlog.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class EntryStore extends SQLiteOpenHelper {
    EntryStore(Context context) { super(context, "pain-log.db", null, 3); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER NOT NULL, pain INTEGER NOT NULL CHECK(pain BETWEEN 1 AND 10), fatigue INTEGER NOT NULL CHECK(fatigue BETWEEN 1 AND 10), discomfort INTEGER NOT NULL CHECK(discomfort BETWEEN 1 AND 10), abdominal_pain INTEGER NOT NULL DEFAULT -1 CHECK(abdominal_pain BETWEEN -1 AND 10), headache INTEGER NOT NULL DEFAULT -1 CHECK(headache BETWEEN -2 AND 10), symptoms TEXT NOT NULL, location_mask INTEGER NOT NULL DEFAULT 0 CHECK(location_mask >= 0), location TEXT NOT NULL, notes TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1 || newVersion != 3) throw new IllegalStateException("Migration required");
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN abdominal_pain INTEGER NOT NULL DEFAULT -1 CHECK(abdominal_pain BETWEEN -1 AND 10)");
            db.execSQL("ALTER TABLE entries ADD COLUMN headache INTEGER NOT NULL DEFAULT -1 CHECK(headache BETWEEN -2 AND 10)");
            try (Cursor c = db.query("entries", new String[]{"id", "symptoms"}, null, null, null, null, null)) {
                while (c.moveToNext()) {
                    String[] old = c.getString(1).split(",");
                    if (old.length != 8) throw new IllegalStateException("Unexpected legacy symptoms");
                    ContentValues values = new ContentValues();
                    int presence = Integer.parseInt(old[1]);
                    values.put("headache", presence == 1 ? -2 : presence);
                    StringBuilder remaining = new StringBuilder(old[0]);
                    for (int i = 2; i < old.length; i++) remaining.append(',').append(old[i]);
                    values.put("symptoms", remaining.toString());
                    db.update("entries", values, "id=?", new String[]{Long.toString(c.getLong(0))});
                }
            }
        }
        if (oldVersion < 3) db.execSQL("ALTER TABLE entries ADD COLUMN location_mask INTEGER NOT NULL DEFAULT 0 CHECK(location_mask >= 0)");
    }
    void save(Entry e) {
        ContentValues values = new ContentValues();
        values.put("time", e.time); values.put("pain", e.pain);
        values.put("abdominal_pain", e.abdominalPain); values.put("headache", e.headache);
        values.put("fatigue", e.fatigue); values.put("discomfort", e.discomfort);
        StringBuilder states = new StringBuilder();
        for (int state : e.symptoms) { if (states.length() > 0) states.append(','); states.append(state); }
        values.put("location_mask", e.locationMask);
        values.put("symptoms", states.toString()); values.put("location", e.location); values.put("notes", e.notes);
        if (e.id == 0) e.id = getWritableDatabase().insertOrThrow("entries", null, values);
        else if (getWritableDatabase().update("entries", values, "id=?", new String[]{Long.toString(e.id)}) != 1)
            throw new IllegalStateException("Record missing");
    }
    void delete(long id) { getWritableDatabase().delete("entries", "id=?", new String[]{Long.toString(id)}); }
    List<Entry> all() {
        List<Entry> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("entries", null, null, null, null, null, "time DESC, id DESC")) {
            while (c.moveToNext()) {
                Entry e = new Entry();
                e.id = c.getLong(c.getColumnIndexOrThrow("id")); e.time = c.getLong(c.getColumnIndexOrThrow("time"));
                e.pain = c.getInt(c.getColumnIndexOrThrow("pain")); e.fatigue = c.getInt(c.getColumnIndexOrThrow("fatigue"));
                e.abdominalPain = c.getInt(c.getColumnIndexOrThrow("abdominal_pain"));
                e.headache = c.getInt(c.getColumnIndexOrThrow("headache"));
                e.discomfort = c.getInt(c.getColumnIndexOrThrow("discomfort"));
                String[] states = c.getString(c.getColumnIndexOrThrow("symptoms")).split(",");
                for (int i = 0; i < e.symptoms.length; i++) e.symptoms[i] = Integer.parseInt(states[i]);
                e.locationMask = c.getInt(c.getColumnIndexOrThrow("location_mask"));
                e.location = c.getString(c.getColumnIndexOrThrow("location")); e.notes = c.getString(c.getColumnIndexOrThrow("notes"));
                result.add(e);
            }
        }
        return result;
    }
}
