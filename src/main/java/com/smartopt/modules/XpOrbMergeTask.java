package com.smartopt.modules;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Gộp các quả cầu kinh nghiệm (XP orb) đứng gần nhau thành 1 quả duy nhất cộng dồn EXP.
 * Đây là nguồn entity rác RẤT phổ biến ở farm mob/grinder RPG (hàng chục orb rơi cùng lúc)
 * nhưng gần như KHÔNG có rủi ro gameplay vì orb XP không có tên riêng/lore/PDC để mất —
 * gộp chỉ đổi số lượng entity, không đổi tổng EXP người chơi nhận được.
 *
 * Bán kính gộp dùng chung hệ số với Item Merge (itemMergeRadius) nhân thêm 1 chút vì orb
 * thường sinh ra dày đặc hơn item rơi thường.
 */
public class XpOrbMergeTask {

    private final JavaPlugin plugin;
    private final OptConfig config;
    private volatile OptimizationEngine.Settings current;

    public XpOrbMergeTask(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateSettings(OptimizationEngine.Settings settings) {
        this.current = settings;
    }

    public void runOnce() {
        OptimizationEngine.Settings settings = this.current;
        if (settings == null || settings.itemMergeRadius <= 0) return;
        if (!config.moduleXpOrbMerge) return;

        double radius = settings.itemMergeRadius * 1.2;
        for (World world : plugin.getServer().getWorlds()) {
            if (config.worldExclusions.contains(world.getName())) continue;
            for (Chunk chunk : world.getLoadedChunks()) {
                mergeInChunk(chunk, radius);
            }
        }
    }

    private void mergeInChunk(Chunk chunk, double radius) {
        Entity[] entities = chunk.getEntities();
        List<ExperienceOrb> orbs = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof ExperienceOrb && e.isValid()) {
                orbs.add((ExperienceOrb) e);
            }
        }
        if (orbs.size() < 2) return;

        double radiusSq = radius * radius;
        for (int i = 0; i < orbs.size(); i++) {
            ExperienceOrb a = orbs.get(i);
            if (a.isDead() || !a.isValid()) continue;

            for (int j = i + 1; j < orbs.size(); j++) {
                ExperienceOrb b = orbs.get(j);
                if (b.isDead() || !b.isValid()) continue;
                if (a.getLocation().distanceSquared(b.getLocation()) > radiusSq) continue;

                a.setExperience(a.getExperience() + b.getExperience());
                b.remove();
            }
        }
    }
}
