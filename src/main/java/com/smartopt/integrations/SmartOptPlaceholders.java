package com.smartopt.integrations;

import com.smartopt.SmartOptimizerPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Expansion cho PlaceholderAPI, để server có thể hiển thị TPS / mức tối ưu hiện tại
 * lên scoreboard, tab-list, hologram, v.v.
 *
 * Placeholder hỗ trợ:
 *   %smartopt_tps%          - TPS trung bình 1 phút (1 chữ số thập phân)
 *   %smartopt_level%        - Mức tối ưu hiện tại (NORMAL/MILD/MODERATE/SEVERE)
 *   %smartopt_status%       - "BAT" hoac "TAT"
 *
 * Chỉ được đăng ký nếu plugin PlaceholderAPI thực sự có trên server (kiểm tra ở
 * SmartOptimizerPlugin trước khi khởi tạo class này), nên không gây lỗi thiếu
 * dependency nếu admin không cài PlaceholderAPI.
 */
public class SmartOptPlaceholders extends PlaceholderExpansion {

    private final SmartOptimizerPlugin plugin;

    public SmartOptPlaceholders(SmartOptimizerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "smartopt";
    }

    @Override
    public String getAuthor() {
        return "SmartOptimizer";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        switch (params.toLowerCase()) {
            case "tps":
                double tps = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
                return String.format("%.1f", tps);
            case "level":
                return plugin.getEngine().getCurrentLevel().name();
            case "status":
                return plugin.isRunning() ? "BAT" : "TAT";
            default:
                return "";
        }
    }
}
