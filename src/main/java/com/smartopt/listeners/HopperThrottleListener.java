package com.smartopt.listeners;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Khi TPS thấp (MODERATE/SEVERE), chủ động bỏ qua một tỉ lệ % các lượt chuyển item
 * qua hopper/dropper để giảm tải, thay vì để hopper chạy full tốc độ mỗi 8 tick.
 * CHỈ áp dụng cho container thật (hopper/chest/furnace...), KHÔNG đụng tới các
 * inventory ảo do plugin khác tạo ra (ví dụ GUI shop) để tránh xung đột.
 */
public class HopperThrottleListener implements Listener {

    private final OptConfig config;
    private volatile double throttleChance = 0.0;

    public HopperThrottleListener(OptConfig config) {
        this.config = config;
    }

    public void updateSettings(OptimizationEngine.Settings settings) {
        this.throttleChance = config.moduleHopperThrottle ? settings.hopperThrottleChance : 0.0;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        double chance = throttleChance;
        if (chance <= 0.0) return;

        InventoryHolder src = event.getSource().getHolder();
        InventoryHolder dst = event.getDestination().getHolder();
        // Chỉ throttle giữa các container thật (hopper/chest/furnace/...),
        // không đụng tay vào inventory ảo của plugin khác (shop, custom GUI, v.v.)
        if (!(src instanceof Container) || !(dst instanceof Container)) return;

        Location loc = event.getSource().getLocation();
        if (loc != null && loc.getWorld() != null && config.worldExclusions.contains(loc.getWorld().getName())) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() < chance) {
            event.setCancelled(true);
        }
    }
}
