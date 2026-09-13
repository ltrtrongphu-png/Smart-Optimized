package com.smartopt.commands;

import com.smartopt.SmartOptimizerPlugin;
import com.smartopt.core.OptimizationEngine;
import com.smartopt.util.GradientUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "status", "toggle", "reload", "simulate", "diag", "gui", "history", "topchunks");

    private final SmartOptimizerPlugin plugin;

    public OptCommand(SmartOptimizerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smartoptimizer.admin")) {
            sender.sendMessage(ChatColor.RED + "Bạn không có quyền dùng lệnh này.");
            return true;
        }

        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                sendStatus(sender);
                return true;
            case "toggle":
                plugin.setRunning(!plugin.isRunning());
                sender.sendMessage(ChatColor.YELLOW + "[SmartOptimizer] Auto-optimize hiện là: "
                        + (plugin.isRunning() ? ChatColor.GREEN + "BẬT" : ChatColor.RED + "TẮT"));
                return true;
            case "reload":
                plugin.reloadEverything();
                sender.sendMessage(ChatColor.GREEN + "[SmartOptimizer] Đã tải lại config.yml.");
                return true;
            case "simulate":
                return handleSimulate(sender, args);
            case "diag":
                sendDiagnostics(sender);
                return true;
            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dùng được trong game.");
                    return true;
                }
                plugin.getGuiListener().open((Player) sender);
                return true;
            case "history":
                sendHistory(sender);
                return true;
            case "topchunks":
                sendTopChunks(sender, args);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Lệnh không hợp lệ. Dùng: /sopt <status|toggle|reload|simulate|diag|gui|history|topchunks>");
                return true;
        }
    }

    private static String header(String text) {
        return GradientUtil.gradientBold(text, GradientUtil.BRAND_A, GradientUtil.BRAND_C);
    }

    private static String levelColor(OptimizationEngine.Level level) {
        switch (level) {
            case MILD: return GradientUtil.gradient(level.name(), GradientUtil.WARN_A, GradientUtil.WARN_B);
            case MODERATE: return GradientUtil.gradient(level.name(), GradientUtil.WARN_B, GradientUtil.DANGER_A);
            case SEVERE: return GradientUtil.gradient(level.name(), GradientUtil.DANGER_A, GradientUtil.DANGER_B);
            default: return GradientUtil.gradient(level.name(), GradientUtil.OK_A, GradientUtil.OK_B);
        }
    }

    private void sendStatus(CommandSender sender) {
        double[] tps = Bukkit.getServer().getTPS();
        OptimizationEngine.Level level = plugin.getEngine().getCurrentLevel();
        double mild = plugin.getOptConfig().mildThreshold;
        double severe = plugin.getOptConfig().severeThreshold;

        sender.sendMessage(header("===== SmartOptimizer Status ====="));
        sender.sendMessage(ChatColor.GRAY + "TPS (1m/5m/15m): "
                + GradientUtil.colorizeTps(tps[0], mild, severe) + ChatColor.WHITE
                + " / " + String.format("%.2f", tps[1]) + " / " + String.format("%.2f", tps[2]));
        sender.sendMessage(ChatColor.GRAY + "Auto-optimize: " + (plugin.isRunning()
                ? ChatColor.GREEN + "BẬT" : ChatColor.RED + "TẮT")
                + (plugin.isSimulating() ? ChatColor.LIGHT_PURPLE + " (ĐANG MÔ PHỎNG)" : ""));
        sender.sendMessage(ChatColor.GRAY + "Mức hiện tại: " + levelColor(level));
        sender.sendMessage(ChatColor.GRAY + "MythicMobs: " + (plugin.isMythicMobsDetected()
                ? ChatColor.GREEN + "phát hiện - đã bật chế độ bảo vệ" : ChatColor.GRAY + "không có"));
        sender.sendMessage(ChatColor.GRAY + "PlaceholderAPI: " + (plugin.isPlaceholderApiHooked()
                ? ChatColor.GREEN + "đã kết nối" : ChatColor.GRAY + "không có / chưa bật"));

        for (World world : Bukkit.getWorlds()) {
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + world.getName() + ": "
                    + ChatColor.GRAY + "view=" + world.getViewDistance()
                    + " sim=" + world.getSimulationDistance()
                    + " entities=" + world.getEntities().size()
                    + " chunks_loaded=" + world.getLoadedChunks().length);
        }
    }

    private void sendDiagnostics(CommandSender sender) {
        sender.sendMessage(header("===== SmartOptimizer Diagnostics ====="));
        for (World world : Bukkit.getWorlds()) {
            long items = world.getEntitiesByClass(org.bukkit.entity.Item.class).size();
            int total = world.getEntities().size();
            sender.sendMessage(ChatColor.WHITE + world.getName() + ChatColor.GRAY
                    + " -> tổng entity=" + total + ", item rơi=" + items
                    + ", chunk loaded=" + world.getLoadedChunks().length
                    + ", players=" + world.getPlayers().size());
        }
        sender.sendMessage(ChatColor.YELLOW
                + "Gợi ý: nếu 'item rơi' rất cao -> bật modules.item-merge. "
                + "Nếu 'tổng entity' cao chủ yếu là mob -> cần người chơi dọn dẹp farm/trại mob "
                + "hoặc bật modules.mob-cap (mặc định tắt vì là can thiệp mạnh nhất).");
    }

    private void sendHistory(CommandSender sender) {
        List<SmartOptimizerPlugin.HistoryPoint> hist = plugin.getHistory();
        double mild = plugin.getOptConfig().mildThreshold;
        double severe = plugin.getOptConfig().severeThreshold;
        sender.sendMessage(header("===== SmartOptimizer History (gần nhất trước) ====="));
        if (hist.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Chưa có dữ liệu, chờ vài chu kỳ kiểm tra đầu tiên.");
            return;
        }
        int shown = 0;
        for (int i = hist.size() - 1; i >= 0 && shown < 15; i--, shown++) {
            SmartOptimizerPlugin.HistoryPoint p = hist.get(i);
            sender.sendMessage(ChatColor.GRAY + "  TPS=" + GradientUtil.colorizeTps(p.tps, mild, severe)
                    + ChatColor.GRAY + " -> " + levelColor(p.level));
        }
    }

    private void sendTopChunks(CommandSender sender, String[] args) {
        int limit = plugin.getOptConfig().topChunksDefaultLimit;
        if (args.length >= 2) {
            try {
                limit = Math.max(1, Math.min(30, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Số lượng không hợp lệ, dùng mặc định: " + limit);
            }
        }

        List<com.smartopt.modules.ChunkHealthReporter.ChunkReport> top =
                plugin.getChunkHealthReporter().topChunks(Bukkit.getWorlds(), limit);

        sender.sendMessage(header("===== Top " + limit + " Chunk Nhiều Entity Nhất ====="));
        sender.sendMessage(ChatColor.GRAY + "(Chỉ báo cáo - KHÔNG tự động xóa gì cả, bạn tự quyết định)");
        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Không có chunk nào đang tải có entity.");
            return;
        }

        int rank = 1;
        for (com.smartopt.modules.ChunkHealthReporter.ChunkReport r : top) {
            sender.sendMessage(ChatColor.WHITE + "#" + rank + " " + ChatColor.YELLOW + r.world
                    + ChatColor.GRAY + " (chunk " + r.x + "," + r.z + ") "
                    + ChatColor.WHITE + "tổng=" + r.total);
            sender.sendMessage(ChatColor.DARK_GRAY + "     mob=" + r.mobs
                    + " item=" + r.items + " orbXP=" + r.xpOrbs
                    + " armorStand=" + r.armorStands + " khốiĐangRơi=" + r.fallingBlocks
                    + " khác=" + r.other);
            rank++;
        }
    }

    private boolean handleSimulate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Dùng: /sopt simulate <NORMAL|MILD|MODERATE|SEVERE|RESET>");
            return true;
        }

        if (args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("off")) {
            plugin.simulateLevel(null, sender);
            return true;
        }

        OptimizationEngine.Level lvl;
        try {
            lvl = OptimizationEngine.Level.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Mức không hợp lệ. Chọn: NORMAL, MILD, MODERATE, SEVERE, RESET");
            return true;
        }

        plugin.simulateLevel(lvl, sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("simulate")) {
            for (OptimizationEngine.Level l : OptimizationEngine.Level.values()) {
                if (l.name().startsWith(args[1].toUpperCase())) out.add(l.name());
            }
            if ("RESET".startsWith(args[1].toUpperCase())) out.add("RESET");
        }
        return out;
    }
}
