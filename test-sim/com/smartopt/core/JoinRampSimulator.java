package com.smartopt.core;

public class JoinRampSimulator {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("===== Mô Phỏng JoinRampCalculator =====\n");
        scenario1_TangDanTheoThoiGian();
        scenario2_DungLaiOMucTargetSauKhiHetThoiGianRamp();
        scenario3_KhongVuotQuaTarget();

        System.out.println("\n===== Kết Qủa: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }

    private static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }

    private static void scenario1_TangDanTheoThoiGian() {
        System.out.println("Kịch bản 1: Người chơi vừa join, tăng dần view-distance từ 3 lên 10 trong 10 giây");
        JoinRampCalculator ramp = new JoinRampCalculator(3, 10, 10_000);
        int at0 = ramp.distanceAt(0);
        int at5s = ramp.distanceAt(5_000);
        int at10s = ramp.distanceAt(10_000);
        System.out.println("  -> t=0s:" + at0 + " t=5s:" + at5s + " t=10s:" + at10s);
        check("Lúc mới join phải bằng giá trị bắt đầu (3)", at0 == 3);
        check("Giữa chừng phải lớn hơn lúc bắt đầu và nhỏ hơn target", at5s > at0 && at5s < 10);
        check("Sau đủ thời gian phải đạt target (10)", at10s == 10);
        System.out.println();
    }

    private static void scenario2_DungLaiOMucTargetSauKhiHetThoiGianRamp() {
        System.out.println("Kịch bản 2: Sau khi hết thời gian ramp, dù chờ đợi thêm cũng không vượt target");
        JoinRampCalculator ramp = new JoinRampCalculator(4, 8, 5_000);
        int atLongAfter = ramp.distanceAt(999_999);
        check("Không được vượt quá target dù chờ đợi rất lâu", atLongAfter == 8);
        check("isComplete() phải trả về true sau khi hết thời gian ramp", ramp.isComplete(999_999));
        System.out.println();
    }

    private static void scenario3_KhongVuotQuaTarget() {
        System.out.println("Kịch bản 3: Trường hợp start >= target (server đang ở mức thấp sẵn) -> không tăng");
        JoinRampCalculator ramp = new JoinRampCalculator(10, 6, 5_000);
        int at0 = ramp.distanceAt(0);
        int atMid = ramp.distanceAt(2_500);
        int atEnd = ramp.distanceAt(5_000);
        System.out.println("  -> t=0:" + at0 + " t=mid:" + atMid + " t=end:" + atEnd);
        check("Khi start > target, luôn trả về target ngay (không cần ramp ngược)", at0 == 6 && atMid == 6 && atEnd == 6);
        System.out.println();
    }
}
