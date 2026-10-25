package jp.painlog.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class EntryStore extends SQLiteOpenHelper {
    EntryStore(Context context) { super(context, "pain-log.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER NOT NULL, pain INTEGER NOT NULL CHECK(pain BETWEEN 1 AND 10), fatigue INTEGER NOT NULL CHECK(fatigue BETWEEN 1 AND 10), discomfort INTEGER NOT NULL CHECK(discomfort BETWEEN 1 AND 10), symptoms TEXT NOT NULL, location TEXT NOT NULL, notes TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Migration required");
    }
    void save(Entry e) {
        ContentValues values = new ContentValues();
        values.put("time", e.time); values.put("pain", e.pain);
        values.put("fatigue", e.fatigue); values.put("discomfort", e.discomfort);
        StringBuilder states = new StringBuilder();
        for (int state : e.symptoms) { if (states.length() > 0) states.append(','); states.append(state); }
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
                e.discomfort = c.getInt(c.getColumnIndexOrThrow("discomfort"));
                String[] states = c.getString(c.getColumnIndexOrThrow("symptoms")).split(",");
                for (int i = 0; i < e.symptoms.length; i++) e.symptoms[i] = Integer.parseInt(states[i]);
                e.location = c.getString(c.getColumnIndexOrThrow("location")); e.notes = c.getString(c.getColumnIndexOrThrow("notes"));
                result.add(e);
            }
        }
        return result;
    }
}
