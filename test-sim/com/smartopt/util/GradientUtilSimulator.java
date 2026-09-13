package com.smartopt.util;

/**
 * Mô phỏng (simulation) kiểm tra GradientUtil - thuần Java, không cần Bukkit.
 * Chạy: java -cp out com.smartopt.util.GradientUtilSimulator
 */
public class GradientUtilSimulator {
    static int pass=0, fail=0;
    static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }
    public static void main(String[] args) {
        String hp = GradientUtil.hexPrefix("#FF00AA");
        check("hexPrefix đúng định dạng legacy hex (16 ký tự)", hp.length() == 14 && hp.startsWith("\u00A7x"));
        check("hexPrefix bắt đầu bằng §x§F§F§0§0§A§A", hp.equals("\u00A7x\u00A7F\u00A7F\u00A70\u00A70\u00A7A\u00A7A"));

        String g = GradientUtil.gradient("ABCDE", "#FF0000", "#00FF00");
        check("gradient không null/rỗng", g != null && !g.isEmpty());
        check("gradient chứa ký tự gốc A..E", g.contains("A") && g.contains("E"));

        String multi = GradientUtil.gradient("SmartOptimizer", GradientUtil.BRAND_A, GradientUtil.BRAND_B, GradientUtil.BRAND_C);
        check("gradient 3 điểm đúng không lỗi với text dài", multi != null && multi.length() > 0);

        String tps1 = GradientUtil.colorizeTps(20.0, 19.3, 14.0);
        String tps2 = GradientUtil.colorizeTps(5.0, 19.3, 14.0);
        check("colorizeTps ở TPS cao chứa số đúng dạng", tps1.endsWith("20.0"));
        check("colorizeTps ở TPS thấp chứa số đúng dạng", tps2.endsWith("5.0"));
        check("colorizeTps TPS cao và thấp cho ra màu khác nhau", !tps1.substring(0, tps1.indexOf("2")).equals(tps2.substring(0, tps2.indexOf("5"))));

        try {
            GradientUtil.hexPrefix("bad");
            check("hexPrefix phải ném lỗi với hex không hợp lệ", false);
        } catch (IllegalArgumentException ex) {
            check("hexPrefix phải ném lỗi với hex không hợp lệ", true);
        }

        System.out.println("\n===== Kết Quả GradientUtil: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }
}
