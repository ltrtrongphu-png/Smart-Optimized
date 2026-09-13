package com.smartopt.gui;

import com.smartopt.SmartOptimizerPlugin;
import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import com.smartopt.util.GradientUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Xây dựng (và làm mới) nội dung GUI quản lý SmartOptimizer.
 * Kích thước 5 hàng (45 ô), toàn bộ tên/tiêu đề dùng chữ Hex chuyển sắc (gradient)
 * qua {@link GradientUtil} - định dạng §x hợp lệ trên mọi client Minecraft 1.16+.
 *
 * Toàn bộ 10 nút bật/tắt module được vẽ TỰ ĐỘNG từ {@link ToggleDefinitions#ALL}
 * (xem lớp đó để biết chi tiết từng module) - file này CHỈ lo phần bố cục/hình
 * ảnh, không còn code riêng cho từng module như bản trước.
 *
 *  Hàng 0 (0-8)   : viền kính gradient, ô giữa (4) là trạng thái tổng quan
 *  Hàng 1 (9-17)  : 9 nút bật/tắt module chính
 *  Hàng 2 (18-26) : viền kính gradient, ô giữa (22) là nút module thứ 10
 *  Hàng 3 (27-35) : 4 nút mô phỏng nhanh MILD/MODERATE/SEVERE/Reset, ở giữa hàng
 *  Hàng 4 (36-44) : nút tải lại config, Top Chunks, thông tin phiên bản, nút đóng
 */
public final class OptGuiMenu {

    private static final int SIZE = 45;

    public static final int SLOT_STATUS = 4;

    public static final int SLOT_TOGGLE_AUTO = 9;
    public static final int SLOT_TOGGLE_VIEW = 10;
    public static final int SLOT_TOGGLE_MOB_SPAWN = 11;
    public static final int SLOT_TOGGLE_ITEM_MERGE = 12;
    public static final int SLOT_TOGGLE_HOPPER = 13;
    public static final int SLOT_TOGGLE_REDSTONE = 14;
    public static final int SLOT_TOGGLE_MOBCAP = 15;
    public static final int SLOT_TOGGLE_XPORB = 16;
    public static final int SLOT_TOGGLE_FALLINGBLOCK = 17;
    public static final int SLOT_TOGGLE_ARMORSTAND = 22;

    public static final int SLOT_SIM_MILD = 29;
    public static final int SLOT_SIM_MODERATE = 30;
    public static final int SLOT_SIM_SEVERE = 31;
    public static final int SLOT_SIM_RESET = 32;

    public static final int SLOT_RELOAD = 38;
    public static final int SLOT_INFO = 40;
    public static final int SLOT_TOPCHUNKS = 41;
    public static final int SLOT_CLOSE = 42;

    private static final String PLUGIN_VERSION = "2.2.0";

    /** Viền kính chuyển sắc (gradient) bao quanh GUI — tông sky -> indigo -> violet, khớp bảng màu thương hiệu mới. */
    private static final Material[] BORDER_GRADIENT = {
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
    };

    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            18, 26,
            27, 35,
            36, 37, 39, 43, 44
    };

    private OptGuiMenu() {}

    public static Inventory build(SmartOptimizerPlugin plugin, SmartOptGuiHolder holder) {
        String title = GradientUtil.BOLD + GradientUtil.gradient(" SmartOptimizer  \u2699 Bảng điều khiển ",
                GradientUtil.BRAND_A, GradientUtil.BRAND_B, GradientUtil.BRAND_C);
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);
        refresh(plugin, inv);
        return inv;
    }

    public static void refresh(SmartOptimizerPlugin plugin, Inventory inv) {
        drawBorder(inv);
        drawStatusPanel(plugin, inv);
        drawToggles(plugin, inv);
        drawSimulationButtons(inv);
        drawUtilityButtons(inv);
    }

    // ================== CÁC KHỐI VẼ GIAO DIỆN ==================

    private static void drawStatusPanel(SmartOptimizerPlugin plugin, Inventory inv) {
        OptConfig c = plugin.getOptConfig();
        double tps = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
        OptimizationEngine.Level level = plugin.getEngine().getCurrentLevel();

        String tpsGradient = GradientUtil.hexPrefix(
                GradientUtil.healthColor(healthRatio(tps, c.mildThreshold, c.severeThreshold)))
                + String.format("%.1f", tps);

        long enabledCount = ToggleDefinitions.ALL.stream()
                .filter(spec -> spec.slot() != SLOT_TOGGLE_AUTO)
                .filter(spec -> spec.isEnabled(plugin))
                .count();
        long totalModules = ToggleDefinitions.ALL.size() - 1; // trừ Auto-Optimize (không tính là 1 module)

        inv.setItem(SLOT_STATUS, item(Material.CLOCK,
                GradientUtil.gradientBold("Trạng thái server", GradientUtil.BRAND_A, GradientUtil.BRAND_C),
                line(ChatColor.GRAY + "TPS: " + tpsGradient),
                line(ChatColor.GRAY + "Mức hiện tại: " + levelGradient(level)),
                line(ChatColor.GRAY + "Auto-optimize: " + onOffGradient(plugin.isRunning())),
                line(ChatColor.GRAY + "Module đang bật: " + GradientUtil.hexPrefix(GradientUtil.BRAND_A)
                        + enabledCount + ChatColor.GRAY + "/" + totalModules),
                line(plugin.isSimulating()
                        ? GradientUtil.gradient("(đang mô phỏng TPS giả)", GradientUtil.BRAND_B, GradientUtil.BRAND_C)
                        : "")));
    }

    private static void drawToggles(SmartOptimizerPlugin plugin, Inventory inv) {
        for (ToggleSpec spec : ToggleDefinitions.ALL) {
            boolean enabled = spec.isEnabled(plugin);
            inv.setItem(spec.slot(), toggleItem(spec.material(), spec.label(), enabled, spec.description()));
        }
    }

    private static void drawSimulationButtons(Inventory inv) {
        inv.setItem(SLOT_SIM_MILD, item(Material.YELLOW_DYE,
                GradientUtil.gradient("Mô phỏng: MILD", GradientUtil.WARN_A, GradientUtil.WARN_B),
                line(ChatColor.GRAY + "Xem thử cấu hình ở mức MILD")));
        inv.setItem(SLOT_SIM_MODERATE, item(Material.ORANGE_DYE,
                GradientUtil.gradient("Mô phỏng: MODERATE", GradientUtil.WARN_B, GradientUtil.DANGER_A),
                line(ChatColor.GRAY + "Xem thử cấu hình ở mức MODERATE")));
        inv.setItem(SLOT_SIM_SEVERE, item(Material.RED_DYE,
                GradientUtil.gradient("Mô phỏng: SEVERE", GradientUtil.DANGER_A, GradientUtil.DANGER_B),
                line(ChatColor.GRAY + "Xem thử cấu hình ở mức SEVERE (tối ưu tối đa)")));
        inv.setItem(SLOT_SIM_RESET, item(Material.LIME_DYE,
                GradientUtil.gradient("Quay về trạng thái thật", GradientUtil.OK_A, GradientUtil.OK_B),
                line(ChatColor.GRAY + "Hủy mô phỏng, dùng TPS thực tế")));
    }

    private static void drawUtilityButtons(Inventory inv) {
        inv.setItem(SLOT_RELOAD, item(Material.BOOK,
                GradientUtil.gradient("Tải lại config.yml", GradientUtil.BRAND_A, GradientUtil.BRAND_B),
                line(ChatColor.GRAY + "Đọc lại file cấu hình")));
        inv.setItem(SLOT_INFO, item(Material.PAPER,
                GradientUtil.gradient("SmartOptimizer v" + PLUGIN_VERSION, GradientUtil.BRAND_B, GradientUtil.BRAND_C),
                line(ChatColor.GRAY + "GUI + chữ Hex chuyển sắc"),
                line(ChatColor.GRAY + "" + (ToggleDefinitions.ALL.size() - 1) + " module tối ưu tự động")));
        inv.setItem(SLOT_TOPCHUNKS, item(Material.COMPARATOR,
                GradientUtil.gradient("Top Chunk Lag", GradientUtil.BRAND_A, GradientUtil.OK_A),
                line(ChatColor.GRAY + "Quét chunk đang tải, xếp hạng theo"),
                line(ChatColor.GRAY + "số entity - CHỈ BÁO CÁO, không xóa gì."),
                line(""),
                line(ChatColor.YELLOW + "-> Bấm để xem trong chat")));
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER,
                GradientUtil.gradient("Đóng", GradientUtil.DANGER_A, GradientUtil.DANGER_B), line("")));
    }

    private static void drawBorder(Inventory inv) {
        for (int i = 0; i < BORDER_SLOTS.length; i++) {
            int slot = BORDER_SLOTS[i];
            Material pane = BORDER_GRADIENT[i % BORDER_GRADIENT.length];
            inv.setItem(slot, item(pane, " "));
        }
    }

    // ================== TIỆN ÍCH MÀU/CHỮ ==================

    private static double healthRatio(double tps, double mild, double severe) {
        if (tps >= mild) return 0.0;
        if (tps <= severe) return 1.0;
        return 1.0 - ((tps - severe) / (mild - severe));
    }

    private static String levelGradient(OptimizationEngine.Level level) {
        switch (level) {
            case MILD:
                return GradientUtil.gradient(level.name(), GradientUtil.WARN_A, GradientUtil.WARN_B);
            case MODERATE:
                return GradientUtil.gradient(level.name(), GradientUtil.WARN_B, GradientUtil.DANGER_A);
            case SEVERE:
                return GradientUtil.gradient(level.name(), GradientUtil.DANGER_A, GradientUtil.DANGER_B);
            default:
                return GradientUtil.gradient(level.name(), GradientUtil.OK_A, GradientUtil.OK_B);
        }
    }

    private static String onOffGradient(boolean on) {
        return on
                ? GradientUtil.hexPrefix(GradientUtil.OK_A) + "BẬT"
                : GradientUtil.hexPrefix(GradientUtil.DANGER_B) + "TẮT";
    }

    private static ItemStack toggleItem(Material material, String name, boolean enabled, String desc) {
        String coloredName = enabled
                ? GradientUtil.gradientBold(name, GradientUtil.OK_A, GradientUtil.OK_B)
                : GradientUtil.gradientBold(name, GradientUtil.DANGER_A, GradientUtil.DANGER_B);
        return item(material,
                coloredName,
                line(ChatColor.GRAY + desc),
                line(""),
                line(ChatColor.GRAY + "Trạng thái: " + onOffGradient(enabled)),
                line(ChatColor.YELLOW + "-> Bấm để " + (enabled ? "TẮT" : "BẬT")));
    }

    private static String line(String s) {
        return s;
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>(lore.length);
            for (String l : lore) loreList.add(l);
            meta.setLore(loreList);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
