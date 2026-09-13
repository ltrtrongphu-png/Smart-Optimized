package com.smartopt.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Mô phỏng (simulation) hành vi của OptimizationEngine qua nhiều kịch bản TPS
 * để kiểm tra logic TRƯỚC KHI đóng gói thành plugin thật.
 * Chạy: java com.smartopt.core.EngineSimulator
 */
public class EngineSimulator {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("===== Mô Phỏng SmartOptimizer - OptimizationEngine =====\n");

        scenario1_ServerKhoeManh();
        scenario2_LagDotNgot();
        scenario3_LagKeoDaiRoiHoiPhuc();
        scenario4_ChongNhapNhay();
        scenario5_LagCucDoan();

        System.out.println("\n===== Kết Quả: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            pass++;
            System.out.println("  [OK] " + label);
        } else {
            fail++;
            System.out.println("  [FAIL] " + label);
        }
    }

    /** Server chạy TPS 20 ổn định -> engine phải luôn ở NORMAL, không can thiệp gì. */
    private static void scenario1_ServerKhoeManh() {
        System.out.println("Kịch bản 1: Server ổn định TPS ~20");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        OptimizationEngine.Settings last = null;
        for (int i = 0; i < 10; i++) {
            last = engine.onTpsSample(20.0);
        }
        System.out.println("  -> " + last);
        check("Level phải là NORMAL", last.level == OptimizationEngine.Level.NORMAL);
        check("View distance không bị giảm", last.viewDistance == 10);
        System.out.println();
    }

    /** TPS rơi đột ngột xuống 12 (SEVERE) -> engine phải hạ cấp NGAY trong tick đầu tiên, không chờ đợi. */
    private static void scenario2_LagDotNgot() {
        System.out.println("Kịch bản 2: TPS rớt đột ngột từ 20 xuống 12 (lag spike)");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        engine.onTpsSample(20.0);
        OptimizationEngine.Settings reaction = engine.onTpsSample(12.0);
        System.out.println("  -> phản ứng ngay lập tức: " + reaction);
        check("Phải hạ cấp SEVERE ngay không delay", reaction.level == OptimizationEngine.Level.SEVERE);
        check("Mob spawn multiplier phải giảm mạnh (<=0.35)", reaction.mobSpawnMultiplier <= 0.35);
        System.out.println();
    }

    /** TPS tụt kéo dài rồi hồi phục dần -> engine phải hạ xuống rồi phục hồi từng bậc, không nhảy thẳng về NORMAL. */
    private static void scenario3_LagKeoDaiRoiHoiPhuc() {
        System.out.println("Kịch bản 3: Lag kéo dài (Moderate) rồi phục hồi dần về NORMAL");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        List<OptimizationEngine.Level> history = new ArrayList<>();

        double[] tpsSeries = {
            20, 20, 15.5, 15.5, 15.5, 15.5,      // rơi xuống MODERATE và giữ nguyên
            20, 20, 20,                           // 3 lần liên tiếp tốt -> đủ điều kiện phục hồi 1 bậc
            20, 20, 20,                           // 3 lần nữa -> phục hồi tiếp 1 bậc (hết về NORMAL)
        };
        for (double tps : tpsSeries) {
            history.add(engine.onTpsSample(tps).level);
        }
        System.out.println("  -> Lịch sử level: " + history);

        check("Phải rơi xuống Moderate khi TPS=15.5", history.get(2) == OptimizationEngine.Level.MODERATE);
        check("Không được nhảy thẳng về NORMAL ngay sau 1 lần TPS tốt",
                history.get(6) != OptimizationEngine.Level.NORMAL);
        check("Cuối cùng phải phục hồi về NORMAL sau khi ổn định đủ lâu",
                history.get(history.size() - 1) == OptimizationEngine.Level.NORMAL);
        System.out.println();
    }

    /** TPS dao động lên xuống quanh ngưỡng MILD -> không được nhấp nháy (flap) qua lại liên tục. */
    private static void scenario4_ChongNhapNhay() {
        System.out.println("Kịch bản 4: TPS dao động quanh ngưỡng MILD (chống flap)");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        int changes = 0;
        OptimizationEngine.Level prev = engine.getCurrentLevel();

        double[] tpsSeries = {19.5, 18.9, 19.6, 18.8, 19.7, 18.9, 19.6, 18.8};
        for (double tps : tpsSeries) {
            OptimizationEngine.Level lvl = engine.onTpsSample(tps).level;
            if (lvl != prev) changes++;
            prev = lvl;
        }
        System.out.println("  -> Số lần đổi level trong 8 mẫu dao động: " + changes);
        check("Số lần đổi level phải <= 2 (không nhấp nháy liên tục)", changes <= 2);
        System.out.println();
    }

    /** TPS xuống rất thấp (5.0) -> phải áp dụng mức SEVERE, các giá trị không được âm hoặc vô lý. */
    private static void scenario5_LagCucDoan() {
        System.out.println("Kịch bản 5: TPS cực thấp (5.0) - kiểm tra giá trị an toàn");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        OptimizationEngine.Settings s = engine.onTpsSample(5.0);
        System.out.println("  -> " + s);
        check("View distance không được nhỏ hơn 4 (tối thiểu an toàn)", s.viewDistance >= 4);
        check("Simulation distance không được nhỏ hơn 3", s.simulationDistance >= 3);
        check("Hopper throttle chance phải <= 1.0 (là xác suất hợp lệ)", s.hopperThrottleChance <= 1.0);
        check("Mob spawn multiplier phải > 0 (không tắt hoàn toàn spawn)", s.mobSpawnMultiplier > 0);
        System.out.println();
    }
}
