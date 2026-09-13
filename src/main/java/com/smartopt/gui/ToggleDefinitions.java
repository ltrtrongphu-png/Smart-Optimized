package com.smartopt.gui;

import com.smartopt.SmartOptimizerPlugin;
import org.bukkit.Material;

import java.util.List;

/**
 * Danh sách toàn bộ module bật/tắt trong GUI. Muốn thêm 1 module mới vào GUI:
 * chỉ cần thêm 1 dòng {@link ToggleSpec} vào list bên dưới, KHÔNG cần sửa
 * OptGuiMenu hay OptGuiListener.
 */
public final class ToggleDefinitions {

    public static final List<ToggleSpec> ALL = List.of(
            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_AUTO, Material.LEVER,
                    "Auto-Optimize (tổng)",
                    "Bật/tắt toàn bộ tính năng tự động tối ưu",
                    SmartOptimizerPlugin::isRunning,
                    (p, v) -> p.setRunning(v)),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_VIEW, Material.ENDER_EYE,
                    "View/Sim Distance",
                    "Tự động giảm view/simulation distance khi lag",
                    p -> p.getOptConfig().moduleViewDistance,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleViewDistance = v, "modules.view-distance")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_MOB_SPAWN, Material.ZOMBIE_HEAD,
                    "Giới hạn Mob Spawn",
                    "Giảm số mob được phép spawn thêm khi lag",
                    p -> p.getOptConfig().moduleMobSpawnLimit,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleMobSpawnLimit = v, "modules.mob-spawn-limit")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_ITEM_MERGE, Material.HOPPER,
                    "Gộp Item Rơi",
                    "Gộp item rơi gần nhau, bỏ qua đồ RPG đặc biệt",
                    p -> p.getOptConfig().moduleItemMerge,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleItemMerge = v, "modules.item-merge")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_HOPPER, Material.CHEST,
                    "Giảm tải Hopper",
                    "Bỏ qua bớt lượt chuyển item qua hopper khi lag nặng",
                    p -> p.getOptConfig().moduleHopperThrottle,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleHopperThrottle = v, "modules.hopper-throttle")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_REDSTONE, Material.REDSTONE,
                    "Chống máy lag Redstone",
                    "Chặn clock redstone đổi tín hiệu quá nhanh",
                    p -> p.getOptConfig().moduleRedstoneLimiter,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleRedstoneLimiter = v, "modules.redstone-limiter")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_MOBCAP, Material.BARRIER,
                    "Giới hạn Mob/Chunk",
                    "CẢNH BÁO: xóa bớt mob vanilla dư thừa (mặc định tắt)",
                    p -> p.getOptConfig().moduleMobCap,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleMobCap = v, "modules.mob-cap")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_XPORB, Material.EXPERIENCE_BOTTLE,
                    "Gộp Orb Kinh Nghiệm",
                    "Gộp XP orb gần nhau tại farm mob, không mất EXP",
                    p -> p.getOptConfig().moduleXpOrbMerge,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleXpOrbMerge = v, "modules.xp-orb-merge")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_FALLINGBLOCK, Material.SAND,
                    "Giới hạn Khối Đang Rơi",
                    "Giới hạn cát/sỏi/TNT-dup đang rơi mỗi chunk (mặc định tắt)",
                    p -> p.getOptConfig().moduleFallingBlockLimiter,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleFallingBlockLimiter = v, "modules.falling-block-limiter")),

            new ToggleSpec(OptGuiMenu.SLOT_TOGGLE_ARMORSTAND, Material.ARMOR_STAND,
                    "Giới hạn Armor Stand",
                    "Giới hạn 16 armor stand/chunk, ưu tiên giữ cái có tên (mặc định tắt)",
                    p -> p.getOptConfig().moduleArmorStandLimiter,
                    (p, v) -> setAndPersist(p, v, c -> c.moduleArmorStandLimiter = v, "modules.armor-stand-limiter"))
    );

    private ToggleDefinitions() {}

    /** Interface nội bộ giúp gán giá trị mới vào 1 field của OptConfig mà không cần viết setter riêng cho từng field. */
    private interface FieldMutator {
        void apply(com.smartopt.core.OptConfig config);
    }

    private static void setAndPersist(SmartOptimizerPlugin plugin, boolean value, FieldMutator mutator, String key) {
        mutator.apply(plugin.getOptConfig());
        plugin.persistModuleToggle(key, value);
    }
}
