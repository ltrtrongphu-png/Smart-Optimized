package com.smartopt.modules;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Giới hạn số lượng Armor Stand tối đa trong mỗi chunk (mặc định 16 - đủ để
 * trưng bày/trang trí thông thường, nhưng chặn được các trường hợp spam hàng
 * trăm armor stand làm lag server, thường gặp ở máy/dupe lỗi hoặc plugin khác
 * lỗi xung đột).
 *
 * Để giảm thiểu rủi ro phá vỡ công trình người chơi đã dùng công đặt tên/mặc
 * đồ cho armor stand, khi cần xóa bớt surplus, ưu tiên xóa CÁC ARMOR STAND
 * KHÔNG CÓ TÊN RIÊNG trước (nhiều khả năng là rác/tạm thời hơn). Chỉ khi đã
 * xóa hết armor stand không tên mà vẫn còn dư (surplus), mới bắt buộc phải
 * đụng đến cả armor stand có tên - lúc đó sẽ ưu tiên giữ lại những cái có
 * trang bị (hasEquipment()) vì thường là trưng bày có chủ đích hơn.
 */
public class ArmorStandLimiter {

    private final JavaPlugin plugin;
    private final OptConfig config;
    private volatile OptimizationEngine.Level currentLevel = OptimizationEngine.Level.NORMAL;

    public ArmorStandLimiter(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateLevel(OptimizationEngine.Level level) {
        this.currentLevel = level;
    }

    public void runOnce() {
        if (!config.moduleArmorStandLimiter) return;
        if (config.armorStandOnlyWhenLagging
                && currentLevel != OptimizationEngine.Level.MODERATE
                && currentLevel != OptimizationEngine.Level.SEVERE) {
            return;
        }

        for (World world : plugin.getServer().getWorlds()) {
            if (config.worldExclusions.contains(world.getName())) continue;
            for (Chunk chunk : world.getLoadedChunks()) {
                enforceInChunk(chunk);
            }
        }
    }

    private void enforceInChunk(Chunk chunk) {
        List<ArmorStand> stands = new ArrayList<>();
        for (Entity e : chunk.getEntities()) {
            if (e instanceof ArmorStand && e.isValid()) {
                stands.add((ArmorStand) e);
            }
        }

        int surplus = stands.size() - config.armorStandCapPerChunk;
        if (surplus <= 0) return;

        // Ưu tiên xóa: không tên -> có tên nhưng không trang bị -> có tên và có trang bị (giữ lại sau cùng).
        stands.sort(Comparator
                .comparing((ArmorStand a) -> a.getCustomName() != null)
                .thenComparing(ArmorStandLimiter::hasEquipment));

        for (int i = 0; i < surplus && i < stands.size(); i++) {
            stands.get(i).remove();
        }
    }

    /**
     * ArmorStand không có sẵn hasEquipment() trong Bukkit/Paper API,
     * nên tự kiểm tra từng slot (giáp + 2 tay) thông qua EntityEquipment.
     */
    private static boolean hasEquipment(ArmorStand stand) {
        EntityEquipment eq = stand.getEquipment();
        if (eq == null) return false;
        return isPresent(eq.getHelmet())
                || isPresent(eq.getChestplate())
                || isPresent(eq.getLeggings())
                || isPresent(eq.getBoots())
                || isPresent(eq.getItemInMainHand())
                || isPresent(eq.getItemInOffHand());
    }

    private static boolean isPresent(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
}
