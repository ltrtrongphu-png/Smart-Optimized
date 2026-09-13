package com.smartopt.core;

/**
 * Cấu hình plugin - đọc 1 lần khi start/reload, truyền vào các module.
 * Thuần dữ liệu, không phụ thuộc Bukkit để dễ test.
 */
public class OptConfig {

    public boolean enabled = true;

    // Ngưỡng TPS quyết định mức tối ưu
    public double mildThreshold = 19.3;
    public double moderateThreshold = 17.0;
    public double severeThreshold = 14.0;

    // Chu kỳ kiểm tra TPS (giây)
    public int checkIntervalSeconds = 5;

    // -1 nghĩa là tự lấy view-distance/simulation-distance hiện tại của từng world làm base
    public int baseViewDistance = -1;
    public int baseSimulationDistance = -1;

    // Bật/tắt từng module tối ưu riêng lẻ
    public boolean moduleViewDistance = true;
    public boolean moduleMobSpawnLimit = true;
    public boolean moduleItemMerge = true;
    public boolean moduleHopperThrottle = true;

    // Item merge: bán kính gộp cơ bản (nhân thêm theo Settings.itemMergeRadius)
    public double itemMergeCheckIntervalSeconds = 3;
    // Bỏ qua item có tên riêng / PDC custom (đồ RPG) để không gộp nhầm đồ đặc biệt
    public boolean itemMergeSkipNamedOrEnchanted = true;

    // Thông báo cho op khi đổi mức tối ưu
    public boolean notifyAdminsOnLevelChange = true;
    public boolean logToConsoleOnLevelChange = true;

    // ---- Redstone Limiter (chống máy lag redstone / clock quá nhanh) ----
    public boolean moduleRedstoneLimiter = true;
    public int redstoneMaxEventsPerWindow = 12;   // tối đa bao nhiêu lần đổi tín hiệu / cửa sổ
    public long redstoneWindowMillis = 1000;      // độ dài cửa sổ (ms)

    // ---- Mob Cap Enforcer (giới hạn số mob vanilla không bảo vệ mỗi chunk) ----
    // MẶC ĐỊNH TẮT vì đây là can thiệp mạnh nhất (xóa bớt mob dư thừa) - admin tự bật nếu cần.
    public boolean moduleMobCap = false;
    public int mobCapPerChunk = 12;
    public double mobCapCheckIntervalSeconds = 20;
    // Chỉ kích hoạt dọn dẹp khi đang ở mức tối ưu này trở lên (MODERATE hoặc SEVERE)
    public boolean mobCapOnlyWhenLagging = true;

    // ---- Join Ramp (giảm spike TPS khi người chơi vừa vào server) ----
    public boolean moduleJoinRamp = true;
    public int joinRampStartDistance = 4;
    public long joinRampDurationSeconds = 12;

    // ---- XP Orb Merge (gộp các quả cầu kinh nghiệm gần nhau, giảm entity ở farm mob) ----
    public boolean moduleXpOrbMerge = true;

    // ---- Falling Block Limiter (giới hạn số khối đang rơi / chunk: cát, sỏi, TNT dup...) ----
    public boolean moduleFallingBlockLimiter = false;
    public int fallingBlockCapPerChunk = 40;
    public double fallingBlockCheckIntervalSeconds = 10;
    public boolean fallingBlockOnlyWhenLagging = true;

    // ---- Armor Stand Limiter (giới hạn số armor stand / chunk, ưu tiên giữ cái có tên) ----
    public boolean moduleArmorStandLimiter = false;
    public int armorStandCapPerChunk = 16;
    public double armorStandCheckIntervalSeconds = 15;
    public boolean armorStandOnlyWhenLagging = true;

    // ---- MythicMobs compat ----
    public boolean mythicMobsCompatAutoDetect = true;

    // ---- Lưu lịch sử TPS ra file CSV (sống sót qua restart) ----
    public boolean historyFileEnabled = true;
    public String historyFileName = "history.csv";
    public int historyFileMaxLines = 20000;

    // ---- Báo cáo Top Chunks (chỉ đọc, không xóa gì) ----
    public int topChunksDefaultLimit = 5;

    // ---- World exclusions: các world này sẽ KHÔNG bị plugin can thiệp gì cả ----
    public java.util.List<String> worldExclusions = new java.util.ArrayList<>();

    // ---- Discord Webhook ----
    public boolean discordWebhookEnabled = false;
    public String discordWebhookUrl = "";
    // Chỉ gửi webhook khi đổi sang các mức này (mặc định: chỉ báo khi vào SEVERE và khi PHỤC HỒI về NORMAL)
    public boolean discordNotifyOnSevere = true;
    public boolean discordNotifyOnRecoveryToNormal = true;

    // ---- PlaceholderAPI ----
    public boolean placeholderApiEnabled = true;

    // ---- bStats ----
    public boolean bstatsEnabled = true;
}
