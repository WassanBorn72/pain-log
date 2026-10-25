package jp.painlog.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.StringWriter;
import java.time.ZoneId;
import java.util.List;

public class CsvExporterTest {
    @Test public void escapesQuotesCommasAndNewlines() {
        assertEquals("\"肩,\"\"痛い\"\"\n翌日\"", CsvExporter.cell("肩,\"痛い\"\n翌日"));
    }
    @Test public void protectsSpreadsheetFormulas() {
        for (String s : new String[]{"=1+1", "+SUM(A1)", "-1+2", "@SUM(A1)", " \t=1", "\r\n=1"})
            assertTrue(CsvExporter.cell(s).startsWith("\"'"));
        assertEquals("\"通常の記録\"", CsvExporter.cell("通常の記録"));
    }
    @Test public void exportsJapaneseStatesBomAndTimezone() throws Exception {
        Entry e = new Entry(); e.id = 7; e.time = 0; e.pain = 2; e.fatigue = 4; e.discomfort = 10;
        e.symptoms[0] = 1; e.symptoms[1] = 0; e.notes = "痛み,強い\n\"夜\"";
        StringWriter out = new StringWriter(); CsvExporter.write(out, List.of(e), ZoneId.of("Asia/Tokyo"));
        String csv = out.toString();
        assertTrue(csv.startsWith("\uFEFF記録ID"));
        assertTrue(csv.contains("1970-01-01T09:00:00+09:00"));
        assertTrue(csv.contains(",2,4,10,\"あり\",\"なし\",\"未記録\""));
        assertTrue(csv.endsWith("\"痛み,強い\n\"\"夜\"\"\"\r\n"));
    }
    @Test public void emptyExportHasHeader() throws Exception {
        StringWriter out = new StringWriter(); CsvExporter.write(out, List.of(), ZoneId.of("UTC"));
        assertEquals(1, out.toString().split("\r\n").length);
        assertTrue(out.toString().contains("光過敏"));
    }
}
