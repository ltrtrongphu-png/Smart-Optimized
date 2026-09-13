package com.smartopt.modules;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Giới hạn số lượng entity "khối đang rơi" (cát/sỏi/bê tông bột đang rơi, thường sinh ra
 * hàng loạt bởi TNT duplicator, tháp cát/sỏi tự động, hoặc máy nông trại) trong mỗi chunk.
 *
 * Đây là nguồn lag phần vật lý (physics tick) rất phổ biến trên server SMP nhưng lại ít
 * được các plugin tối ưu khác để ý tới. Chỉ hoạt động khi đang lag (mặc định MODERATE/SEVERE
 * trở lên, giống triết lý của Mob Cap) và xoá bớt các khối dư thừa nhất trong chunk — khối
 * bị xoá vẫn rơi/vỡ tự nhiên ở các khối còn lại nên không phá vỡ cơ chế xây dựng thông thường.
 */
public class FallingBlockLimiter {

    private final JavaPlugin plugin;
    private final OptConfig config;
    private volatile OptimizationEngine.Level currentLevel = OptimizationEngine.Level.NORMAL;

    public FallingBlockLimiter(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateLevel(OptimizationEngine.Level level) {
        this.currentLevel = level;
    }

    public void runOnce() {
        if (!config.moduleFallingBlockLimiter) return;
        if (config.fallingBlockOnlyWhenLagging
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
        List<FallingBlock> candidates = new ArrayList<>();
        for (Entity e : chunk.getEntities()) {
            if (e instanceof FallingBlock && e.isValid()) {
                candidates.add((FallingBlock) e);
            }
        }

        int surplus = candidates.size() - config.fallingBlockCapPerChunk;
        if (surplus <= 0) return;

        for (int i = 0; i < surplus && i < candidates.size(); i++) {
            candidates.get(i).remove();
        }
    }
}
