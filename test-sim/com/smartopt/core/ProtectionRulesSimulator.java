package com.smartopt.core;

public class ProtectionRulesSimulator {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("===== Mô Phỏng ProtectionRules =====\n");
        scenario1_ItemThuong();
        scenario2_ItemRPGCoTenRieng();
        scenario3_ItemCoPdcTrenEntity();
        scenario4_MobVanillaThuong();
        scenario5_MobMythicMobs();
        scenario6_MobDaThuanHoaHoacDayXich();

        System.out.println("\n===== Kết Quả: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }

    private static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }

    private static void scenario1_ItemThuong() {
        System.out.println("Kịch bản 1: Item vanilla thường (vd: cobblestone rơi từ mỏ)");
        ProtectionRules.ItemFlags f = new ProtectionRules.ItemFlags();
        boolean protectedItem = ProtectionRules.isProtectedItem(f);
        System.out.println("  -> protected=" + protectedItem);
        check("Item thường PHẢI được phép gộp (không protected)", !protectedItem);
        System.out.println();
    }

    private static void scenario2_ItemRPGCoTenRieng() {
        System.out.println("Kịch bản 2: Item RPG có tên riêng + lore (vd: 'Kiếm Rồng Huyền Thoại')");
        ProtectionRules.ItemFlags f = new ProtectionRules.ItemFlags();
        f.hasDisplayName = true;
        f.hasLore = true;
        boolean protectedItem = ProtectionRules.isProtectedItem(f);
        System.out.println("  -> protected=" + protectedItem);
        check("Item RPG có tên riêng phải được bảo vệ (không bị gộp)", protectedItem);
        System.out.println();
    }

    private static void scenario3_ItemCoPdcTrenEntity() {
        System.out.println("Kịch bản 3: Item không có tên nhưng có PDC gắn trên entity (plugin RPG gắn ngầm)");
        ProtectionRules.ItemFlags f = new ProtectionRules.ItemFlags();
        f.hasEntityPdc = true;
        boolean protectedItem = ProtectionRules.isProtectedItem(f);
        check("Item có PDC ngầm trên entity vẫn PHẢI được bảo vệ", protectedItem);
        System.out.println();
    }

    private static void scenario4_MobVanillaThuong() {
        System.out.println("Kịch bản 4: Mob vanilla thường từ farm (zombie không tên, không PDC)");
        ProtectionRules.MobFlags f = new ProtectionRules.MobFlags();
        boolean protectedMob = ProtectionRules.isProtectedMob(f);
        check("Mob thường từ farm PHẢI được phép dọn bởi mob-cap", !protectedMob);
        System.out.println();
    }

    private static void scenario5_MobMythicMobs() {
        System.out.println("Kịch bản 5: Mob boss của MythicMobs (có tag đặc trưng)");
        ProtectionRules.MobFlags f = new ProtectionRules.MobFlags();
        f.hasMythicMobsTag = true;
        boolean protectedMob = ProtectionRules.isProtectedMob(f);
        check("Mob MythicMobs phài được bảo vệ tuyệt đối", protectedMob);
        System.out.println();
    }

    private static void scenario6_MobDaThuanHoaHoacDayXich() {
        System.out.println("Kịch bản 6: Thú cưng đã thuần hóa / bị dây xích của người chơi");
        ProtectionRules.MobFlags tamed = new ProtectionRules.MobFlags();
        tamed.isTamed = true;
        ProtectionRules.MobFlags leashed = new ProtectionRules.MobFlags();
        leashed.isLeashed = true;

        check("Thú cưng đã thuần hóa phải được bảo vệ", ProtectionRules.isProtectedMob(tamed));
        check("Mob bị dây xích PHẢI được bảo vệ", ProtectionRules.isProtectedMob(leashed));
        System.out.println();
    }
}
