package com.smartopt.core;

public class JoinRampSimulator {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("===== MO PHONG JoinRampCalculator =====\n");
        scenario1_TangDanTheoThoiGian();
        scenario2_DungLaiOMucTargetSauKhiHetThoiGianRamp();
        scenario3_KhongVuotQuaTarget();

        System.out.println("\n===== KET QUA: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }

    private static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }

    private static void scenario1_TangDanTheoThoiGian() {
        System.out.println("Kich ban 1: Nguoi choi vua join, tang dan view-distance tu 3 len 10 trong 10 giay");
        JoinRampCalculator ramp = new JoinRampCalculator(3, 10, 10_000);
        int at0 = ramp.distanceAt(0);
        int at5s = ramp.distanceAt(5_000);
        int at10s = ramp.distanceAt(10_000);
        System.out.println("  -> t=0s:" + at0 + " t=5s:" + at5s + " t=10s:" + at10s);
        check("Luc moi join phai bang gia tri bat dau (3)", at0 == 3);
        check("Giua chung phai lon hon luc bat dau va nho hon target", at5s > at0 && at5s < 10);
        check("Sau du thoi gian phai dat target (10)", at10s == 10);
        System.out.println();
    }

    private static void scenario2_DungLaiOMucTargetSauKhiHetThoiGianRamp() {
        System.out.println("Kich ban 2: Sau khi het thoi gian ramp, du cho doi them cung khong vuot target");
        JoinRampCalculator ramp = new JoinRampCalculator(4, 8, 5_000);
        int atLongAfter = ramp.distanceAt(999_999);
        check("Khong duoc vuot qua target du cho doi rat lau", atLongAfter == 8);
        check("isComplete() phai tra ve true sau khi het thoi gian ramp", ramp.isComplete(999_999));
        System.out.println();
    }

    private static void scenario3_KhongVuotQuaTarget() {
        System.out.println("Kich ban 3: Truong hop start >= target (server dang o muc thap san) -> khong tang");
        JoinRampCalculator ramp = new JoinRampCalculator(10, 6, 5_000);
        int at0 = ramp.distanceAt(0);
        int atMid = ramp.distanceAt(2_500);
        int atEnd = ramp.distanceAt(5_000);
        System.out.println("  -> t=0:" + at0 + " t=mid:" + atMid + " t=end:" + atEnd);
        check("Khi start > target, luon tra ve target ngay (khong can ramp nguoc)", at0 == 6 && atMid == 6 && atEnd == 6);
        System.out.println();
    }
}
