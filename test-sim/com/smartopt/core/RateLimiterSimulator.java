package com.smartopt.core;

public class RateLimiterSimulator {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("===== MO PHONG RateLimiter (Anti Redstone-Clock) =====\n");
        scenario1_ClockNhanh();
        scenario2_HanhDongBinhThuong();
        scenario3_ResetSauCuaSo();
        scenario4_NhieuKeyDocLap();

        System.out.println("\n===== KET QUA: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }

    private static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }

    /** Redstone clock nhanh (bat tat 40 lan/giay tai cung 1 vi tri) -> phai bi chan sau khi vuot nguong. */
    private static void scenario1_ClockNhanh() {
        System.out.println("Kich ban 1: Redstone clock nhanh tai 1 block (nguong 10 lan / 1000ms)");
        RateLimiter limiter = new RateLimiter(1000);
        String key = "world;100;64;200";
        boolean blockedAtSomePoint = false;
        int blockedCount = 0;
        long start = 0;
        for (int i = 0; i < 40; i++) {
            long now = start + (i * 25L); // 40 lan trong 1000ms
            boolean over = limiter.isOverLimit(key, 10, now);
            if (over) { blockedAtSomePoint = true; blockedCount++; }
        }
        System.out.println("  -> Bi chan " + blockedCount + "/40 lan");
        check("Phai chan duoc it nhat 1 lan trong cua so 1000ms", blockedAtSomePoint);
        check("Phai chan phan lon (tren 20 lan) do clock qua nhanh", blockedCount > 20);
        System.out.println();
    }

    /** Hành động bình thường (redstone door, ít lần/giây) -> không bao giờ bị chặn. */
    private static void scenario2_HanhDongBinhThuong() {
        System.out.println("Kich ban 2: Redstone hoat dong binh thuong (1 lan / 2 giay)");
        RateLimiter limiter = new RateLimiter(1000);
        String key = "world;50;70;50";
        boolean everBlocked = false;
        for (int i = 0; i < 10; i++) {
            long now = i * 2000L;
            if (limiter.isOverLimit(key, 10, now)) everBlocked = true;
        }
        check("Khong duoc chan hanh dong binh thuong", !everBlocked);
        System.out.println();
    }

    /** Sau khi hết cửa sổ thời gian, bộ đếm phải reset chứ không cộng dồn mãi mãi. */
    private static void scenario3_ResetSauCuaSo() {
        System.out.println("Kich ban 3: Reset dem sau khi het cua so thoi gian");
        RateLimiter limiter = new RateLimiter(1000);
        String key = "world;1;1;1";
        for (int i = 0; i < 15; i++) limiter.isOverLimit(key, 10, i * 10L); // spam trong 150ms dau
        boolean blockedLater = limiter.isOverLimit(key, 10, 5000L); // 5 giay sau, cua so moi, 1 lan duy nhat
        check("Sau khi sang cua so moi, 1 lan don le khong bi chan", !blockedLater);
        System.out.println();
    }

    /** Các block khác nhau (key khác nhau) phải độc lập, không ảnh hưởng lẫn nhau. */
    private static void scenario4_NhieuKeyDocLap() {
        System.out.println("Kich ban 4: Nhieu block khac nhau phai doc lap voi nhau");
        RateLimiter limiter = new RateLimiter(1000);
        for (int i = 0; i < 30; i++) limiter.isOverLimit("blockA", 10, i * 10L); // blockA bi spam
        boolean blockBBlocked = limiter.isOverLimit("blockB", 10, 100L); // blockB moi dung 1 lan
        check("Block khac khong bi anh huong boi block bi spam", !blockBBlocked);
        System.out.println();
    }
}
