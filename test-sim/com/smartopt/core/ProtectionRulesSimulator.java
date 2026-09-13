package com.smartopt.core;

public class ProtectionRulesSimulator {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("===== MO PHONG ProtectionRules =====\n");
        scenario1_ItemThuong();
        scenario2_ItemRPGCoTenRieng();
        scenario3_ItemCoPdcTrenEntity();
        scenario4_MobVanillaThuong();
        scenario5_MobMythicMobs();
        scenario6_MobDaThuanHoaHoacDayXich();

        System.out.println("\n===== KET QUA: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }

    private static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }

    private static void scenario1_ItemThuong() {
        System.out.println("Kich ban 1: Item vanilla thuong (vd: cobblestone roi tu mo)");
        ProtectionRules.ItemFlags f = new ProtectionRules.ItemFlags();
        boolean protectedItem = ProtectionRules.isProtectedItem(f);
        System.out.println("  -> protected=" + protectedItem);
        check("Item thuong PHAI duoc phep gop (khong protected)", !protectedItem);
        System.out.println();
    }

    private static void scenario2_ItemRPGCoTenRieng() {
        System.out.println("Kich ban 2: Item RPG co ten rieng + lore (vd: 'Kiem Rong Huyen Thoai')");
        ProtectionRules.ItemFlags f = new ProtectionRules.ItemFlags();
        f.hasDisplayName = true;
        f.hasLore = true;
        boolean protectedItem = ProtectionRules.isProtectedItem(f);
        System.out.println("  -> protected=" + protectedItem);
        check("Item RPG co ten rieng PHAI duoc bao ve (khong bi gop)", protectedItem);
        System.out.println();
    }

    private static void scenario3_ItemCoPdcTrenEntity() {
        System.out.println("Kich ban 3: Item khong co ten nhung co PDC gan tren entity (plugin RPG gan ngam)");
        ProtectionRules.ItemFlags f = new ProtectionRules.ItemFlags();
        f.hasEntityPdc = true;
        boolean protectedItem = ProtectionRules.isProtectedItem(f);
        check("Item co PDC ngam tren entity van PHAI duoc bao ve", protectedItem);
        System.out.println();
    }

    private static void scenario4_MobVanillaThuong() {
        System.out.println("Kich ban 4: Mob vanilla thuong tu farm (zombie khong ten, khong PDC)");
        ProtectionRules.MobFlags f = new ProtectionRules.MobFlags();
        boolean protectedMob = ProtectionRules.isProtectedMob(f);
        check("Mob thuong tu farm PHAI duoc phep don boi mob-cap", !protectedMob);
        System.out.println();
    }

    private static void scenario5_MobMythicMobs() {
        System.out.println("Kich ban 5: Mob boss cua MythicMobs (co tag dac trung)");
        ProtectionRules.MobFlags f = new ProtectionRules.MobFlags();
        f.hasMythicMobsTag = true;
        boolean protectedMob = ProtectionRules.isProtectedMob(f);
        check("Mob MythicMobs PHAI duoc bao ve tuyet doi", protectedMob);
        System.out.println();
    }

    private static void scenario6_MobDaThuanHoaHoacDayXich() {
        System.out.println("Kich ban 6: Thu cung da thuan hoa / bi day xich cua nguoi choi");
        ProtectionRules.MobFlags tamed = new ProtectionRules.MobFlags();
        tamed.isTamed = true;
        ProtectionRules.MobFlags leashed = new ProtectionRules.MobFlags();
        leashed.isLeashed = true;

        check("Thu cung da thuan hoa PHAI duoc bao ve", ProtectionRules.isProtectedMob(tamed));
        check("Mob bi day xich PHAI duoc bao ve", ProtectionRules.isProtectedMob(leashed));
        System.out.println();
    }
}
