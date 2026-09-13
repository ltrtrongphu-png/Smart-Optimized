package com.smartopt.core;

/**
 * Nguồn quy tắc DUY NHẤT quyết định 1 item/entity có nên được coi là "đặc biệt"
 * (và do đó KHÔNG được gộp / KHÔNG được dọn bởi mob-cap) hay không.
 * Dùng chung cho ItemMergeTask và MobCapEnforcer để đảm bảo nhất quán.
 *
 * Thuần Java, không phụ thuộc Bukkit -> test độc lập bằng dữ liệu giả lập.
 */
public class ProtectionRules {

    public static final class ItemFlags {
        public boolean hasDisplayName;
        public boolean hasLore;
        public boolean hasEnchants;
        public boolean hasItemPdc;
        public boolean hasEntityPdc; // PDC gắn trên chính Item entity (một số plugin gắn ở đây)
    }

    public static final class MobFlags {
        public boolean hasCustomName;
        public boolean hasPdc;
        public boolean isLeashed;
        public boolean isTamed;
        public boolean hasPassenger;
        public boolean hasMythicMobsTag; // scoreboard tag / PDC key đặc trưng của MythicMobs
        public boolean isFromSpawner; // mob spawn từ spawner có thể là farm hợp lệ, vẫn cho phép cap
    }

    /** Item rơi có nên được coi là "đặc biệt" (bỏ qua khi gộp) không? */
    public static boolean isProtectedItem(ItemFlags f) {
        if (f == null) return true;
        return f.hasDisplayName || f.hasLore || f.hasEnchants || f.hasItemPdc || f.hasEntityPdc;
    }

    /** Mob có nên được coi là "đặc biệt" (không bị mob-cap dọn) không? */
    public static boolean isProtectedMob(MobFlags f) {
        if (f == null) return true;
        return f.hasCustomName || f.hasPdc || f.isLeashed || f.isTamed
                || f.hasPassenger || f.hasMythicMobsTag;
    }
}
