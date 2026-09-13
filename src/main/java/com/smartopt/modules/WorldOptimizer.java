package com.smartopt.modules;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Điều chỉnh view-distance / simulation-distance của các world dựa trên mức tối ưu hiện tại.
 * Dùng đúng API công khai của Paper: World#setViewDistance / World#setSimulationDistance
 * (không đụng NMS), nên an toàn và không lo sập khi Paper cập nhật version.
 */
public class WorldOptimizer {

    private final JavaPlugin plugin;
    private final OptConfig config;

    // Lưu lại view/sim distance gốc của từng world để khôi phục khi tắt plugin / TPS ổn định
    private final Map<String, Integer> baseViewDistance = new HashMap<>();
    private final Map<String, Integer> baseSimDistance = new HashMap<>();

    public WorldOptimizer(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void detectBaseDistancesIfNeeded() {
        for (World world : plugin.getServer().getWorlds()) {
            int vd = config.baseViewDistance > 0 ? config.baseViewDistance : world.getViewDistance();
            int sd = config.baseSimulationDistance > 0 ? config.baseSimulationDistance : world.getSimulationDistance();
            baseViewDistance.put(world.getName(), vd);
            baseSimDistance.put(world.getName(), sd);
        }
    }

    /**
     * Áp dụng settings. Khi level = NORMAL sẽ trả world về đúng giá trị gốc (base),
     * các mức khác dùng tỉ lệ giảm được OptimizationEngine tính sẵn.
     */
    public void apply(OptimizationEngine.Settings settings) {
        if (!config.moduleViewDistance) return;

        for (World world : plugin.getServer().getWorlds()) {
            if (config.worldExclusions.contains(world.getName())) continue;
            int base = baseViewDistance.getOrDefault(world.getName(), world.getViewDistance());
            int baseSim = baseSimDistance.getOrDefault(world.getName(), world.getSimulationDistance());

            int targetView;
            int targetSim;
            if (settings.level == OptimizationEngine.Level.NORMAL) {
                targetView = base;
                targetSim = baseSim;
            } else {
                // settings.viewDistance/simulationDistance được tính trên baseViewDistance mặc định (10/8)
                // trong OptimizationEngine, nên ở đây ta tỉ lệ lại theo base thực tế của world
                double ratioView = (double) settings.viewDistance / 10.0;
                double ratioSim = (double) settings.simulationDistance / 8.0;
                targetView = Math.max(3, (int) Math.round(base * ratioView));
                targetSim = Math.max(2, (int) Math.round(baseSim * ratioSim));
            }

            if (world.getViewDistance() != targetView) {
                world.setViewDistance(targetView);
            }
            if (world.getSimulationDistance() != targetSim) {
                world.setSimulationDistance(targetSim);
            }
        }
    }

    public void restoreAllToBase() {
        for (World world : plugin.getServer().getWorlds()) {
            Integer vd = baseViewDistance.get(world.getName());
            Integer sd = baseSimDistance.get(world.getName());
            if (vd != null) world.setViewDistance(vd);
            if (sd != null) world.setSimulationDistance(sd);
        }
    }

    public Map<String, Integer> getBaseViewDistanceMap() {
        return baseViewDistance;
    }

    public Map<String, Integer> getBaseSimDistanceMap() {
        return baseSimDistance;
    }
}
