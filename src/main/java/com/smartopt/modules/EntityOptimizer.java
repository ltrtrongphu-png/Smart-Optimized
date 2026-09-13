package com.smartopt.modules;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.World;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Giảm giới hạn SỐ LƯỢNG MOB ĐƯỢC PHÉP SPAWN THÊM khi TPS thấp (không giết/despawn mob
 * đang có sẵn, kể cả mob RPG/boss của plugin khác - an toàn tuyệt đối cho gameplay).
 * Khi TPS ổn định trở lại, giới hạn được trả về đúng giá trị gốc của server.
 */
public class EntityOptimizer {

    private static final SpawnCategory[] CATEGORIES = {
            SpawnCategory.MONSTER,
            SpawnCategory.ANIMAL,
            SpawnCategory.WATER_ANIMAL,
            SpawnCategory.AMBIENT
    };

    private final JavaPlugin plugin;
    private final OptConfig config;

    private final Map<String, Map<SpawnCategory, Integer>> baseLimits = new HashMap<>();

    public EntityOptimizer(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    private void ensureBaseCaptured(World world) {
        baseLimits.computeIfAbsent(world.getName(), k -> {
            Map<SpawnCategory, Integer> map = new HashMap<>();
            for (SpawnCategory cat : CATEGORIES) {
                try {
                    map.put(cat, world.getSpawnLimit(cat));
                } catch (Exception ignored) {
                    // Một số category có thể không được hỗ trợ trên version/world nhất định
                }
            }
            return map;
        });
    }

    public void apply(OptimizationEngine.Settings settings) {
        if (!config.moduleMobSpawnLimit) return;

        for (World world : plugin.getServer().getWorlds()) {
            if (config.worldExclusions.contains(world.getName())) continue;
            ensureBaseCaptured(world);
            Map<SpawnCategory, Integer> base = baseLimits.get(world.getName());
            if (base == null) continue;

            for (Map.Entry<SpawnCategory, Integer> entry : base.entrySet()) {
                int target = (int) Math.max(1, Math.round(entry.getValue() * settings.mobSpawnMultiplier));
                try {
                    if (world.getSpawnLimit(entry.getKey()) != target) {
                        world.setSpawnLimit(entry.getKey(), target);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void restoreAllToBase() {
        for (World world : plugin.getServer().getWorlds()) {
            Map<SpawnCategory, Integer> base = baseLimits.get(world.getName());
            if (base == null) continue;
            for (Map.Entry<SpawnCategory, Integer> entry : base.entrySet()) {
                try {
                    world.setSpawnLimit(entry.getKey(), entry.getValue());
                } catch (Exception ignored) {
                }
            }
        }
    }
}
