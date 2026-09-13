package com.smartopt.util;

/**
 * Mo phong (simulation) kiem tra GradientUtil - thuan Java, khong can Bukkit.
 * Chay: java -cp out com.smartopt.util.GradientUtilSimulator
 */
public class GradientUtilSimulator {
    static int pass=0, fail=0;
    static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }
    public static void main(String[] args) {
        String hp = GradientUtil.hexPrefix("#FF00AA");
        check("hexPrefix dung dinh dang legacy hex (16 ky tu)", hp.length() == 14 && hp.startsWith("\u00A7x"));
        check("hexPrefix bat dau bang §x§F§F§0§0§A§A", hp.equals("\u00A7x\u00A7F\u00A7F\u00A70\u00A70\u00A7A\u00A7A"));

        String g = GradientUtil.gradient("ABCDE", "#FF0000", "#00FF00");
        check("gradient khong null/rong", g != null && !g.isEmpty());
        check("gradient chua ky tu goc A..E", g.contains("A") && g.contains("E"));

        String multi = GradientUtil.gradient("SmartOptimizer", GradientUtil.BRAND_A, GradientUtil.BRAND_B, GradientUtil.BRAND_C);
        check("gradient 3 diem dung khong loi voi text dai", multi != null && multi.length() > 0);

        String tps1 = GradientUtil.colorizeTps(20.0, 19.3, 14.0);
        String tps2 = GradientUtil.colorizeTps(5.0, 19.3, 14.0);
        check("colorizeTps o TPS cao chua so dung dang", tps1.endsWith("20.0"));
        check("colorizeTps o TPS thap chua so dung dang", tps2.endsWith("5.0"));
        check("colorizeTps TPS cao va thap cho ra mau khac nhau", !tps1.substring(0, tps1.indexOf("2")).equals(tps2.substring(0, tps2.indexOf("5"))));

        try {
            GradientUtil.hexPrefix("bad");
            check("hexPrefix phai nem loi voi hex khong hop le", false);
        } catch (IllegalArgumentException ex) {
            check("hexPrefix phai nem loi voi hex khong hop le", true);
        }

        System.out.println("\n===== KET QUA GradientUtil: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }
}
