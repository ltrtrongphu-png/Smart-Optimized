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
        System.out.println("===== MO PHONG SmartOptimizer - OptimizationEngine =====\n");

        scenario1_ServerKhoeManh();
        scenario2_LagDotNgot();
        scenario3_LagKeoDaiRoiHoiPhuc();
        scenario4_ChongNhapNhay();
        scenario5_LagCucDoan();

        System.out.println("\n===== KET QUA: " + pass + " PASS / " + fail + " FAIL =====");
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
        System.out.println("Kich ban 1: Server on dinh TPS ~20");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        OptimizationEngine.Settings last = null;
        for (int i = 0; i < 10; i++) {
            last = engine.onTpsSample(20.0);
        }
        System.out.println("  -> " + last);
        check("Level phai la NORMAL", last.level == OptimizationEngine.Level.NORMAL);
        check("View distance khong bi giam", last.viewDistance == 10);
        System.out.println();
    }

    /** TPS rơi đột ngột xuống 12 (SEVERE) -> engine phải hạ cấp NGAY trong tick đầu tiên, không chờ đợi. */
    private static void scenario2_LagDotNgot() {
        System.out.println("Kich ban 2: TPS rot dot ngot tu 20 xuong 12 (lag spike)");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        engine.onTpsSample(20.0);
        OptimizationEngine.Settings reaction = engine.onTpsSample(12.0);
        System.out.println("  -> phan ung ngay lap tuc: " + reaction);
        check("Phai ha cap SEVERE ngay khong delay", reaction.level == OptimizationEngine.Level.SEVERE);
        check("Mob spawn multiplier phai giam manh (<=0.35)", reaction.mobSpawnMultiplier <= 0.35);
        System.out.println();
    }

    /** TPS tụt kéo dài rồi hồi phục dần -> engine phải hạ xuống rồi phục hồi từng bậc, không nhảy thẳng về NORMAL. */
    private static void scenario3_LagKeoDaiRoiHoiPhuc() {
        System.out.println("Kich ban 3: Lag keo dai (MODERATE) roi phuc hoi dan ve NORMAL");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        List<OptimizationEngine.Level> history = new ArrayList<>();

        double[] tpsSeries = {
            20, 20, 15.5, 15.5, 15.5, 15.5,      // roi xuong MODERATE va giu nguyen
            20, 20, 20,                           // 3 lan lien tiep tot -> du dieu kien phuc hoi 1 bac
            20, 20, 20,                           // 3 lan nua -> phuc hoi tiep 1 bac (het ve NORMAL)
        };
        for (double tps : tpsSeries) {
            history.add(engine.onTpsSample(tps).level);
        }
        System.out.println("  -> Lich su level: " + history);

        check("Phai roi xuong MODERATE khi TPS=15.5", history.get(2) == OptimizationEngine.Level.MODERATE);
        check("Khong duoc nhay thang ve NORMAL ngay sau 1 lan TPS tot",
                history.get(6) != OptimizationEngine.Level.NORMAL);
        check("Cuoi cung phai phuc hoi ve NORMAL sau khi on dinh du lau",
                history.get(history.size() - 1) == OptimizationEngine.Level.NORMAL);
        System.out.println();
    }

    /** TPS dao động lên xuống quanh ngưỡng MILD -> không được nhấp nháy (flap) qua lại liên tục. */
    private static void scenario4_ChongNhapNhay() {
        System.out.println("Kich ban 4: TPS dao dong quanh nguong MILD (chong flap)");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        int changes = 0;
        OptimizationEngine.Level prev = engine.getCurrentLevel();

        double[] tpsSeries = {19.5, 18.9, 19.6, 18.8, 19.7, 18.9, 19.6, 18.8};
        for (double tps : tpsSeries) {
            OptimizationEngine.Level lvl = engine.onTpsSample(tps).level;
            if (lvl != prev) changes++;
            prev = lvl;
        }
        System.out.println("  -> So lan doi level trong 8 mau dao dong: " + changes);
        check("So lan doi level phai <= 2 (khong nhap nhay lien tuc)", changes <= 2);
        System.out.println();
    }

    /** TPS xuống rất thấp (5.0) -> phải áp dụng mức SEVERE, các giá trị không được âm hoặc vô lý. */
    private static void scenario5_LagCucDoan() {
        System.out.println("Kich ban 5: TPS cuc thap (5.0) - kiem tra gia tri an toan");
        OptimizationEngine engine = new OptimizationEngine(10, 8);
        OptimizationEngine.Settings s = engine.onTpsSample(5.0);
        System.out.println("  -> " + s);
        check("View distance khong duoc nho hon 4 (toi thieu an toan)", s.viewDistance >= 4);
        check("Simulation distance khong duoc nho hon 3", s.simulationDistance >= 3);
        check("Hopper throttle chance phai <= 1.0 (la xac suat hop le)", s.hopperThrottleChance <= 1.0);
        check("Mob spawn multiplier phai > 0 (khong tat hoan toan spawn)", s.mobSpawnMultiplier > 0);
        System.out.println();
    }
}
