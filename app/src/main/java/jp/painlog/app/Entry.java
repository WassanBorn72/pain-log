package jp.painlog.app;

public class Entry {
    public long id;
    public long time;
    public int pain, fatigue, discomfort;
    // -1 = 未記録, 0 = なし, 1..10 = 強さ。頭痛の -2 は旧版の「あり・強さ未記録」。
    public int abdominalPain = -1, headache = -1;
    // -1 = 未記録, 0 = なし, 1 = あり
    public int[] symptoms = {-1, -1, -1, -1, -1, -1, -1};
    public String location = "", notes = "";
    public static final String[] SYMPTOMS = {
        "浮腫（むくみ）", "じんましん", "口の渇き", "のどの渇き", "皮膚の乾燥", "光過敏", "睡眠で休めなかった"
    };
}
