package com.smartopt.listeners;

import com.smartopt.core.OptConfig;
import com.smartopt.core.RateLimiter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Phát hiện và chặn các "máy lag redstone" (clock đổi tín hiệu quá nhanh tại 1 vị trí)
 * bằng cách đếm số lần đổi tín hiệu trong 1 cửa sổ thời gian ngắn (RateLimiter).
 * Khi vượt ngưỡng, giữ nguyên tín hiệu cũ (không cho đổi thêm) cho tới khi hết cửa sổ.
 *
 * Đây là kỹ thuật phổ biến, an toàn, dùng 100% API công khai của Bukkit
 * (BlockRedstoneEvent) - không đụng NMS, không ảnh hưởng tới redstone chơi bình thường.
 */
public class RedstoneLimiterListener implements Listener {

    private final JavaPlugin plugin;
    private final OptConfig config;
    private final RateLimiter limiter;

    private long lastCleanup = 0;

    public RedstoneLimiterListener(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.limiter = new RateLimiter(config.redstoneWindowMillis);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (!config.moduleRedstoneLimiter) return;

        Block block = event.getBlock();
        Location loc = block.getLocation();
        String key = loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();

        long now = System.currentTimeMillis();
        boolean over = limiter.isOverLimit(key, config.redstoneMaxEventsPerWindow, now);
        if (over) {
            // Giữ nguyên tín hiệu cũ, không cho đổi thêm trong cửa sổ này
            event.setNewCurrent(event.getOldCurrent());
        }

        // Dọn dẹp định kỳ để tránh rò rỉ bộ nhớ (không làm mỗi tick, chỉ mỗi ~30s)
        if (now - lastCleanup > 30_000) {
            limiter.cleanupStale(now, config.redstoneWindowMillis * 10);
            lastCleanup = now;
        }
    }
}
