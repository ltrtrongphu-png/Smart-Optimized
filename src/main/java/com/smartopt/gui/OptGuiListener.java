package com.smartopt.gui;

import com.smartopt.SmartOptimizerPlugin;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Xử lý click chuột trong GUI SmartOptimizer.
 *
 * Toàn bộ 10 nút bật/tắt module được xử lý TỰ ĐỘNG bằng cách đối chiếu với
 * {@link ToggleDefinitions#ALL} - không còn switch-case riêng cho từng module
 * như bản trước. Chỉ còn lại các nút "đặc biệt" (mô phỏng, tải lại, đóng, top
 * chunks) cần logic riêng vì không phải là 1 công tắc bật/tắt đơn giản.
 */
public class OptGuiListener implements Listener {

    private final SmartOptimizerPlugin plugin;

    public OptGuiListener(SmartOptimizerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        SmartOptGuiHolder holder = new SmartOptGuiHolder();
        Inventory inv = OptGuiMenu.build(plugin, holder);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SmartOptGuiHolder)) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) return;
        Player player = (Player) clicker;

        // 1) Kiểm tra trước: slot này có phải 1 trong 10 nút bật/tắt module không?
        for (ToggleSpec spec : ToggleDefinitions.ALL) {
            if (spec.slot() == slot) {
                spec.toggle(plugin);
                OptGuiMenu.refresh(plugin, event.getInventory());
                return;
            }
        }

        // 2) Các nút đặc biệt còn lại, không phải công tắc bật/tắt đơn giản.
        switch (slot) {
            case OptGuiMenu.SLOT_SIM_MILD:
                plugin.simulateLevel(OptimizationEngine.Level.MILD, player);
                break;
            case OptGuiMenu.SLOT_SIM_MODERATE:
                plugin.simulateLevel(OptimizationEngine.Level.MODERATE, player);
                break;
            case OptGuiMenu.SLOT_SIM_SEVERE:
                plugin.simulateLevel(OptimizationEngine.Level.SEVERE, player);
                break;
            case OptGuiMenu.SLOT_SIM_RESET:
                plugin.simulateLevel(null, player);
                break;
            case OptGuiMenu.SLOT_RELOAD:
                plugin.reloadEverything();
                break;
            case OptGuiMenu.SLOT_TOPCHUNKS:
                player.closeInventory();
                player.performCommand("sopt topchunks");
                return;
            case OptGuiMenu.SLOT_CLOSE:
                player.closeInventory();
                return;
            default:
                return;
        }

        OptGuiMenu.refresh(plugin, event.getInventory());
    }
}
