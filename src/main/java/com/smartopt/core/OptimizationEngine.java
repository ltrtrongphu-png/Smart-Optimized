package com.smartopt.core;

/**
 * OptimizationEngine là "bộ não" quyết định mức tối ưu dựa trên TPS trung bình.
 * Class này được viết THUẦN JAVA, không import bất kỳ class nào của Bukkit/Paper,
 * để có thể unit-test độc lập (mô phỏng) mà không cần chạy server thật.
 *
 * Cơ chế chống "nhấp nháy" (flapping):
 *  - Khi TPS giảm -> hạ cấp NGAY (ưu tiên cứu server trước).
 *  - Khi TPS hồi phục -> phải giữ ổn định ở mức tốt trong N lần đo liên tiếp
 *    (RECOVERY_STREAK_REQUIRED) rồi mới nâng cấp trở lại từng bậc một.
 */
public class OptimizationEngine {

    public enum Level {
        NORMAL,     // TPS >= 19.3  -> không can thiệp, để server chạy full
        MILD,       // 17.0 - 19.3  -> giảm nhẹ view/sim distance, bắt đầu gộp item rơi
        MODERATE,   // 14.0 - 17.0  -> giảm mob spawn, gộp item mạnh hơn, giảm hopper nhẹ
        SEVERE      // < 14.0       -> tối ưu tối đa, giảm mọi thứ có thể giảm an toàn
    }

    public static final class Settings {
        public final Level level;
        public final int viewDistance;
        public final int simulationDistance;
        public final double mobSpawnMultiplier;   // % số mob tối đa so với base
        public final double itemMergeRadius;      // 0 = tắt gộp item
        public final double hopperThrottleChance; // 0-1, xác suất bỏ qua 1 lần chuyển hopper

        Settings(Level level, int viewDistance, int simulationDistance,
                 double mobSpawnMultiplier, double itemMergeRadius, double hopperThrottleChance) {
            this.level = level;
            this.viewDistance = viewDistance;
            this.simulationDistance = simulationDistance;
            this.mobSpawnMultiplier = mobSpawnMultiplier;
            this.itemMergeRadius = itemMergeRadius;
            this.hopperThrottleChance = hopperThrottleChance;
        }

        @Override
        public String toString() {
            return String.format(
                "[%s] view=%d sim=%d mobSpawn=%.0f%% itemMergeR=%.1f hopperSkip=%.0f%%",
                level, viewDistance, simulationDistance, mobSpawnMultiplier * 100,
                itemMergeRadius, hopperThrottleChance * 100);
        }
    }

    // Ngưỡng TPS (có thể chỉnh trong config.yml, đây là giá trị mặc định)
    private double mildThreshold = 19.3;
    private double moderateThreshold = 17.0;
    private double severeThreshold = 14.0;

    // Base = trạng thái server khi khỏe mạnh (đọc từ config, ví dụ server.properties view-distance)
    private int baseViewDistance = 10;
    private int baseSimDistance = 8;

    private static final int RECOVERY_STREAK_REQUIRED = 3;

    private Level currentLevel = Level.NORMAL;
    private int goodStreak = 0;

    public OptimizationEngine() {}

    public OptimizationEngine(int baseViewDistance, int baseSimDistance) {
        this.baseViewDistance = baseViewDistance;
        this.baseSimDistance = baseSimDistance;
    }

    public void setThresholds(double mild, double moderate, double severe) {
        this.mildThreshold = mild;
        this.moderateThreshold = moderate;
        this.severeThreshold = severe;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    /** Xác định mức "đáng ra phải ở" dựa thuần vào giá trị TPS hiện tại, không xét lịch sử. */
    private Level levelForTps(double avgTps) {
        if (avgTps < severeThreshold) return Level.SEVERE;
        if (avgTps < moderateThreshold) return Level.MODERATE;
        if (avgTps < mildThreshold) return Level.MILD;
        return Level.NORMAL;
    }

    /**
     * Nạp 1 mẫu TPS mới (trung bình trượt, ví dụ TPS 1 phút gần nhất của Paper).
     * Trả về Settings áp dụng cho tick này.
     */
    public Settings onTpsSample(double avgTps) {
        Level target = levelForTps(avgTps);

        if (rank(target) > rank(currentLevel)) {
            // TPS đang tệ hơn mức hiện tại -> hạ cấp ngay, không chờ đợi
            currentLevel = target;
            goodStreak = 0;
        } else if (rank(target) < rank(currentLevel)) {
            // TPS đang tốt hơn mức hiện tại -> cần ổn định trước khi phục hồi
            goodStreak++;
            if (goodStreak >= RECOVERY_STREAK_REQUIRED) {
                // chỉ nâng từng bậc một để tránh dao động qua lại
                currentLevel = stepTowards(currentLevel, target);
                goodStreak = 0;
            }
        } else {
            goodStreak = 0;
        }

        return buildSettings(currentLevel);
    }

    private int rank(Level l) {
        switch (l) {
            case NORMAL: return 0;
            case MILD: return 1;
            case MODERATE: return 2;
            case SEVERE: return 3;
            default: return 0;
        }
    }

    private Level stepTowards(Level current, Level target) {
        int cur = rank(current);
        int tgt = rank(target);
        int next = cur - 1 < tgt ? tgt : cur - 1;
        return fromRank(next);
    }

    private Level fromRank(int r) {
        switch (r) {
            case 0: return Level.NORMAL;
            case 1: return Level.MILD;
            case 2: return Level.MODERATE;
            default: return Level.SEVERE;
        }
    }

    private Settings buildSettings(Level level) {
        switch (level) {
            case MILD:
                return new Settings(level,
                        Math.max(6, baseViewDistance - 1),
                        Math.max(5, baseSimDistance - 1),
                        0.85, 1.5, 0.0);
            case MODERATE:
                return new Settings(level,
                        Math.max(5, baseViewDistance - 2),
                        Math.max(4, baseSimDistance - 2),
                        0.60, 2.5, 0.15);
            case SEVERE:
                return new Settings(level,
                        Math.max(4, baseViewDistance - 3),
                        Math.max(3, baseSimDistance - 3),
                        0.35, 3.5, 0.35);
            default:
                return new Settings(level, baseViewDistance, baseSimDistance, 1.0, 0.0, 0.0);
        }
    }
}
