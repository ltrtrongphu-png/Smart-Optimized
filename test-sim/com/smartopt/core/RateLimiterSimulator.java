package com.smartopt.core;

public class RateLimiterSimulator {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("===== Mô Phỏng RateLimiter (Anti Redstone-Clock) =====\n");
        scenario1_ClockNhanh();
        scenario2_HanhDongBinhThuong();
        scenario3_ResetSauCuaSo();
        scenario4_NhieuKeyDocLap();

        System.out.println("\n===== Kết Quả: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }

    private static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }

    /** Redstone clock nhanh (bật tắt 40 lần/giây tại cùng 1 vị trí) -> phải bị chặn sau khi vượt ngưỡng. */
    private static void scenario1_ClockNhanh() {
        System.out.println("Kịch bản 1: Redstone clock nhanh tại 1 block (ngưỡng 10 lần / 1000ms)");
        RateLimiter limiter = new RateLimiter(1000);
        String key = "world;100;64;200";
        boolean blockedAtSomePoint = false;
        int blockedCount = 0;
        long start = 0;
        for (int i = 0; i < 40; i++) {
            long now = start + (i * 25L); // 40 lần trong 1000ms
            boolean over = limiter.isOverLimit(key, 10, now);
            if (over) { blockedAtSomePoint = true; blockedCount++; }
        }
        System.out.println("  -> Bị chặn " + blockedCount + "/40 lần");
        check("Phải chặn được ít nhất 1 lần trong cửa sổ 1000ms", blockedAtSomePoint);
        check("Phải chặn phần lớn (trên 20 lần) do clock quá nhanh", blockedCount > 20);
        System.out.println();
    }

    /** Hành động bình thường (redstone door, ít lần/giây) -> không bao giờ bị chặn. */
    private static void scenario2_HanhDongBinhThuong() {
        System.out.println("Kịch bản 2: Redstone hoạt động bình thường (1 lần / 2 giây)");
        RateLimiter limiter = new RateLimiter(1000);
        String key = "world;50;70;50";
        boolean everBlocked = false;
        for (int i = 0; i < 10; i++) {
            long now = i * 2000L;
            if (limiter.isOverLimit(key, 10, now)) everBlocked = true;
        }
        check("Không được chặn hành động bình thường", !everBlocked);
        System.out.println();
    }

    /** Sau khi hết cửa sổ thời gian, bộ đếm phải reset chứ không cộng dồn mãi mãi. */
    private static void scenario3_ResetSauCuaSo() {
        System.out.println("Kịch bản 3: Reset đếm sau khi hết cửa sổ thời gian");
        RateLimiter limiter = new RateLimiter(1000);
        String key = "world;1;1;1";
        for (int i = 0; i < 15; i++) limiter.isOverLimit(key, 10, i * 10L); // spam trong 150ms đầu
        boolean blockedLater = limiter.isOverLimit(key, 10, 5000L); // 5 giây sau, cửa sổ mới, 1 lần duy nhất
        check("Sau khi sang cửa sổ mới, 1 lần đơn lẻ không bị chặn", !blockedLater);
        System.out.println();
    }

    /** Các block khác nhau (key khác nhau) phải độc lập, không ảnh hưởng lẫn nhau. */
    private static void scenario4_NhieuKeyDocLap() {
        System.out.println("Kịch bản 4: Nhiều block khác nhau phải độc lập với nhau");
        RateLimiter limiter = new RateLimiter(1000);
        for (int i = 0; i < 30; i++) limiter.isOverLimit("blockA", 10, i * 10L); // blockA bị spam
        boolean blockBBlocked = limiter.isOverLimit("blockB", 10, 100L); // blockB mới dùng 1 lần
        check("Block khác không bị ảnh hưởng bởi block bị spam", !blockBBlocked);
        System.out.println();
    }
}
