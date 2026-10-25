package jp.painlog.app;

public class Entry {
    public long id;
    public long time;
    public int pain, fatigue, discomfort;
    // -1 = 未記録, 0 = なし, 1..10 = 強さ。頭痛の -2 は旧版の「あり・強さ未記録」。
    public int abdominalPain = -1, headache = -1;
    // -1 = 未記録, 0 = なし, 1 = あり
    public int[] symptoms = {-1, -1, -1, -1, -1, -1, -1};
    // Bit positions are persistent IDs; append new regions without reordering existing ones.
    public static final String[] BODY_PARTS = {
        "全身", "頭・顔", "首", "肩", "胸", "背中", "腰", "腹部", "お尻・股関節",
        "腕・肘", "手・指", "太もも", "膝", "すね・ふくらはぎ", "足首・足"
    };
    public int locationMask;
    public String selectedLocations() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < BODY_PARTS.length; i++) {
            if ((locationMask & (1 << i)) != 0) {
                if (result.length() > 0) result.append("、");
                result.append(BODY_PARTS[i]);
            }
        }
        return result.toString();
    }
    public String location = "", notes = "";
    public static final String[] SYMPTOMS = {
        "浮腫（むくみ）", "じんましん", "口の渇き", "のどの渇き", "皮膚の乾燥", "光過敏", "睡眠で休めなかった"
    };
}
