package com.smartopt.core;

/**
 * Khi 1 người chơi vừa vào server, gửi ngay view-distance đầy đủ có thể gây "spike"
 * TPS (server phải gửi hàng loạt chunk cùng lúc). JoinRampCalculator tính toán
 * view/simulation distance nên tăng dần theo thời gian kể từ lúc join, tới khi đạt
 * mức mục tiêu, giúp trải đều tải ra thay vì dồn vào đúng lúc người chơi vào.
 *
 * Thuần Java, không phụ thuộc Bukkit -> test độc lập được.
 */
public class JoinRampCalculator {

    private final int startDistance;
    private final int targetDistance;
    private final long rampDurationMillis;

    public JoinRampCalculator(int startDistance, int targetDistance, long rampDurationMillis) {
        this.startDistance = Math.min(startDistance, targetDistance);
        this.targetDistance = targetDistance;
        this.rampDurationMillis = Math.max(1, rampDurationMillis);
    }

    /**
     * Trả về khoảng cách nên áp dụng tại thời điểm "elapsedMillis" kể từ lúc join.
     * Tăng tuyến tính (linear) từ startDistance -> targetDistance, làm tròn xuống
     * theo bước 1 (mỗi bước giữ nguyên trong 1 khoảng thời gian bằng nhau).
     */
    public int distanceAt(long elapsedMillis) {
        if (elapsedMillis <= 0) return startDistance;
        if (elapsedMillis >= rampDurationMillis) return targetDistance;
        if (targetDistance <= startDistance) return targetDistance;

        double progress = (double) elapsedMillis / (double) rampDurationMillis;
        int range = targetDistance - startDistance;
        int value = startDistance + (int) Math.floor(progress * range);
        return Math.min(targetDistance, Math.max(startDistance, value));
    }

    public boolean isComplete(long elapsedMillis) {
        return elapsedMillis >= rampDurationMillis;
    }

    public int getTargetDistance() {
        return targetDistance;
    }
}
