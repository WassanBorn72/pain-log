package jp.painlog.app;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() {}
    public static String cell(String text) {
        // Prevent spreadsheet formulas, including leading whitespace/control characters.
        int start = 0;
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
        String probe = text.substring(start);
        if (!probe.isEmpty() && "=+-@".indexOf(probe.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    public static void write(Writer out, List<Entry> entries, ZoneId zone) throws IOException {
        out.write('\uFEFF'); // UTF-8 BOM for Excel
        out.write("記録ID,記録日時,痛み(1-10),怠さ(1-10),不快感(1-10)");
        for (String symptom : Entry.SYMPTOMS) out.write("," + cell(symptom));
        out.write(",痛む部位,その他自由記述\r\n");
        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX").withZone(zone);
        for (Entry e : entries) {
            out.write(e.id + "," + cell(format.format(Instant.ofEpochMilli(e.time))) + "," + e.pain + "," + e.fatigue + "," + e.discomfort);
            for (int state : e.symptoms) out.write("," + cell(state == 1 ? "あり" : state == 0 ? "なし" : "未記録"));
            out.write("," + cell(e.location) + "," + cell(e.notes) + "\r\n");
        }
    }
}
