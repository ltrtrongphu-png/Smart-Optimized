package com.smartopt;

import com.smartopt.commands.OptCommand;
import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import com.smartopt.gui.OptGuiListener;
import com.smartopt.integrations.DiscordNotifier;
import com.smartopt.integrations.SmartOptPlaceholders;
import com.smartopt.listeners.HopperThrottleListener;
import com.smartopt.listeners.RedstoneLimiterListener;
import com.smartopt.modules.ArmorStandLimiter;
import com.smartopt.modules.ChunkHealthReporter;
import com.smartopt.modules.EntityOptimizer;
import com.smartopt.modules.FallingBlockLimiter;
import com.smartopt.modules.ItemMergeTask;
import com.smartopt.modules.JoinRampOptimizer;
import com.smartopt.modules.MobCapEnforcer;
import com.smartopt.modules.WorldOptimizer;
import com.smartopt.modules.XpOrbMergeTask;
import com.smartopt.core.HistoryFileWriter;
import com.smartopt.util.GradientUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * SmartOptimizer - Tự động tối ưu server Paper 1.21.4 dựa trên TPS thực tế.
 * Thiết kế cho server SMP + RPG (tương thích MythicMobs): KHÔNG can thiệp
 * NMS/reflection, chỉ dùng API công khai của Paper để đảm bảo ổn định lâu dài.
 */
public class SmartOptimizerPlugin extends JavaPlugin {

    /** 1 điểm dữ liệu lịch sử, dùng cho lệnh /sopt history. */
    public static final class HistoryPoint {
        public final long timestamp;
        public final double tps;
        public final OptimizationEngine.Level level;

        public HistoryPoint(long timestamp, double tps, OptimizationEngine.Level level) {
            this.timestamp = timestamp;
            this.tps = tps;
            this.level = level;
        }
    }

    private OptConfig config;
    private OptimizationEngine engine;

    private WorldOptimizer worldOptimizer;
    private EntityOptimizer entityOptimizer;
    private ItemMergeTask itemMergeTask;
    private HopperThrottleListener hopperListener;
    private RedstoneLimiterListener redstoneListener;
    private MobCapEnforcer mobCapEnforcer;
    private JoinRampOptimizer joinRampOptimizer;
    private DiscordNotifier discordNotifier;
    private OptGuiListener guiListener;
    private XpOrbMergeTask xpOrbMergeTask;
    private FallingBlockLimiter fallingBlockLimiter;
    private ChunkHealthReporter chunkHealthReporter;
    private HistoryFileWriter historyFileWriter;
    private ArmorStandLimiter armorStandLimiter;

    private BukkitTask monitorTask;
    private BukkitTask itemMergeSchedule;
    private BukkitTask mobCapSchedule;
    private BukkitTask xpOrbMergeSchedule;
    private BukkitTask fallingBlockSchedule;
    private BukkitTask armorStandSchedule;

    private final Deque<HistoryPoint> history = new ArrayDeque<>();
    private static final int HISTORY_MAX = 60;

    private OptimizationEngine.Level lastAnnouncedLevel = OptimizationEngine.Level.NORMAL;
    /** true khi đang ở chế độ mô phỏng (tạm thời bỏ qua TPS thật trong 1 khoảng thời gian). */
    private boolean simulating = false;

    private boolean running = true;
    private boolean mythicMobsDetected = false;
    private boolean placeholderApiHooked = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        this.engine = new OptimizationEngine();
        this.worldOptimizer = new WorldOptimizer(this, config);
        this.entityOptimizer = new EntityOptimizer(this, config);
        this.itemMergeTask = new ItemMergeTask(this, config);
        this.hopperListener = new HopperThrottleListener(config);
        this.redstoneListener = new RedstoneLimiterListener(this, config);
        this.mobCapEnforcer = new MobCapEnforcer(this, config);
        this.joinRampOptimizer = new JoinRampOptimizer(this, config);
        this.discordNotifier = new DiscordNotifier(this, config);
        this.guiListener = new OptGuiListener(this);
        this.xpOrbMergeTask = new XpOrbMergeTask(this, config);
        this.fallingBlockLimiter = new FallingBlockLimiter(this, config);
        this.chunkHealthReporter = new ChunkHealthReporter();
        this.historyFileWriter = new HistoryFileWriter(getDataFolder(), config.historyFileName,
                config.historyFileMaxLines, getLogger());
        this.armorStandLimiter = new ArmorStandLimiter(this, config);

        worldOptimizer.detectBaseDistancesIfNeeded();
        engine.setThresholds(config.mildThreshold, config.moderateThreshold, config.severeThreshold);

        detectSoftDependencies();

        getServer().getPluginManager().registerEvents(hopperListener, this);
        getServer().getPluginManager().registerEvents(redstoneListener, this);
        getServer().getPluginManager().registerEvents(joinRampOptimizer, this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        OptCommand cmd = new OptCommand(this);
        if (getCommand("smartoptimizer") != null) {
            getCommand("smartoptimizer").setExecutor(cmd);
            getCommand("smartoptimizer").setTabCompleter(cmd);
        }

        startMonitorTask();
        startItemMergeTask();
        startMobCapTask();
        startXpOrbMergeTask();
        startFallingBlockTask();
        startArmorStandTask();
        setupBStats();

        printBanner();
        getLogger().info("SmartOptimizer đã khởi động. Ngưỡng: mild=" + config.mildThreshold
                + " moderate=" + config.moderateThreshold + " severe=" + config.severeThreshold);
        if (mythicMobsDetected) {
            getLogger().info("Đã phát hiện MythicMobs - bật chế độ bảo vệ mob/item đặc biệt nghiêm ngặt hơn.");
        }
    }

    private void printBanner() {
        // Console không thể render mã màu §x nên banner khởi động được in dạng chữ thường,
        // màu gradient thực sự được dành cho GUI/chat trong game (xem OptGuiMenu, OptCommand).
        getLogger().info("========================================");
        getLogger().info(" SmartOptimizer v2.2.0 - Auto TPS Optimizer");
        getLogger().info(" Giao diện /sopt gui đã nâng cấp: chữ Hex chuyển sắc.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (monitorTask != null) monitorTask.cancel();
        if (itemMergeSchedule != null) itemMergeSchedule.cancel();
        if (mobCapSchedule != null) mobCapSchedule.cancel();
        if (xpOrbMergeSchedule != null) xpOrbMergeSchedule.cancel();
        if (fallingBlockSchedule != null) fallingBlockSchedule.cancel();
        if (armorStandSchedule != null) armorStandSchedule.cancel();
        if (joinRampOptimizer != null) joinRampOptimizer.shutdown();
        if (worldOptimizer != null) worldOptimizer.restoreAllToBase();
        if (entityOptimizer != null) entityOptimizer.restoreAllToBase();
        getLogger().info("SmartOptimizer đã tắt, đã khôi phục cấu hình gốc cho các world.");
    }

    // ================== KHỞI TẠO / CẤU HÌNH ==================

    private void detectSoftDependencies() {
        mythicMobsDetected = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;

        if (config.placeholderApiEnabled && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new SmartOptPlaceholders(this).register();
                placeholderApiHooked = true;
                getLogger().info("Đã đăng ký PlaceholderAPI expansion (%smartopt_tps%, %smartopt_level%, %smartopt_status%).");
            } catch (Throwable t) {
                getLogger().warning("Không thể đăng ký PlaceholderAPI expansion: " + t.getMessage());
            }
        }
    }

    private void setupBStats() {
        if (!config.bstatsEnabled) return;
        try {
            // Plugin ID ví dụ - người dùng nên đăng ký plugin trên bstats.org và thay số này
            new org.bstats.bukkit.Metrics(this, 00000);
        } catch (Throwable t) {
            getLogger().warning("Không thể khởi tạo bStats: " + t.getMessage());
        }
    }

    private void loadConfigValues() {
        reloadConfig();
        FileConfiguration f = getConfig();
        OptConfig c = new OptConfig();
        c.enabled = f.getBoolean("enabled", true);
        c.mildThreshold = f.getDouble("thresholds.mild", 19.3);
        c.moderateThreshold = f.getDouble("thresholds.moderate", 17.0);
        c.severeThreshold = f.getDouble("thresholds.severe", 14.0);
        c.checkIntervalSeconds = f.getInt("check-interval-seconds", 5);
        c.baseViewDistance = f.getInt("base-view-distance", -1);
        c.baseSimulationDistance = f.getInt("base-simulation-distance", -1);

        c.moduleViewDistance = f.getBoolean("modules.view-distance", true);
        c.moduleMobSpawnLimit = f.getBoolean("modules.mob-spawn-limit", true);
        c.moduleItemMerge = f.getBoolean("modules.item-merge", true);
        c.moduleHopperThrottle = f.getBoolean("modules.hopper-throttle", true);
        c.moduleRedstoneLimiter = f.getBoolean("modules.redstone-limiter", true);
        c.moduleMobCap = f.getBoolean("modules.mob-cap", false);
        c.moduleJoinRamp = f.getBoolean("modules.join-ramp", true);

        c.itemMergeCheckIntervalSeconds = f.getDouble("item-merge.check-interval-seconds", 3);
        c.itemMergeSkipNamedOrEnchanted = f.getBoolean("item-merge.skip-named-or-lored-items", true);

        c.redstoneMaxEventsPerWindow = f.getInt("redstone-limiter.max-events-per-window", 12);
        c.redstoneWindowMillis = f.getInt("redstone-limiter.window-millis", 1000);

        c.mobCapPerChunk = f.getInt("mob-cap.per-chunk", 12);
        c.mobCapCheckIntervalSeconds = f.getDouble("mob-cap.check-interval-seconds", 20);
        c.mobCapOnlyWhenLagging = f.getBoolean("mob-cap.only-when-lagging", true);

        c.moduleXpOrbMerge = f.getBoolean("modules.xp-orb-merge", true);

        c.moduleFallingBlockLimiter = f.getBoolean("modules.falling-block-limiter", false);
        c.fallingBlockCapPerChunk = f.getInt("falling-block-limiter.cap-per-chunk", 40);
        c.fallingBlockCheckIntervalSeconds = f.getDouble("falling-block-limiter.check-interval-seconds", 10);
        c.fallingBlockOnlyWhenLagging = f.getBoolean("falling-block-limiter.only-when-lagging", true);

        c.moduleArmorStandLimiter = f.getBoolean("modules.armor-stand-limiter", false);
        c.armorStandCapPerChunk = f.getInt("armor-stand-limiter.cap-per-chunk", 16);
        c.armorStandCheckIntervalSeconds = f.getDouble("armor-stand-limiter.check-interval-seconds", 15);
        c.armorStandOnlyWhenLagging = f.getBoolean("armor-stand-limiter.only-when-lagging", true);

        c.historyFileEnabled = f.getBoolean("history-file.enabled", true);
        c.historyFileName = f.getString("history-file.file-name", "history.csv");
        c.historyFileMaxLines = f.getInt("history-file.max-lines", 20000);

        c.topChunksDefaultLimit = f.getInt("top-chunks.default-limit", 5);

        c.joinRampStartDistance = f.getInt("join-ramp.start-distance", 4);
        c.joinRampDurationSeconds = f.getInt("join-ramp.duration-seconds", 12);

        c.mythicMobsCompatAutoDetect = f.getBoolean("mythicmobs-compat.auto-detect", true);

        c.worldExclusions = new ArrayList<>(f.getStringList("world-exclusions"));

        c.discordWebhookEnabled = f.getBoolean("discord.enabled", false);
        c.discordWebhookUrl = f.getString("discord.webhook-url", "");
        c.discordNotifyOnSevere = f.getBoolean("discord.notify-on-severe", true);
        c.discordNotifyOnRecoveryToNormal = f.getBoolean("discord.notify-on-recovery", true);

        c.placeholderApiEnabled = f.getBoolean("placeholderapi.enabled", true);
        c.bstatsEnabled = f.getBoolean("bstats-enabled", true);

        c.notifyAdminsOnLevelChange = f.getBoolean("notify-admins-on-level-change", true);
        c.logToConsoleOnLevelChange = f.getBoolean("log-to-console-on-level-change", true);
        this.config = c;
    }

    public void reloadEverything() {
        loadConfigValues();
        engine.setThresholds(config.mildThreshold, config.moderateThreshold, config.severeThreshold);
        this.historyFileWriter = new HistoryFileWriter(getDataFolder(), config.historyFileName,
                config.historyFileMaxLines, getLogger());
        if (monitorTask != null) monitorTask.cancel();
        if (itemMergeSchedule != null) itemMergeSchedule.cancel();
        if (mobCapSchedule != null) mobCapSchedule.cancel();
        if (xpOrbMergeSchedule != null) xpOrbMergeSchedule.cancel();
        if (fallingBlockSchedule != null) fallingBlockSchedule.cancel();
        if (armorStandSchedule != null) armorStandSchedule.cancel();
        startMonitorTask();
        startItemMergeTask();
        startMobCapTask();
        startXpOrbMergeTask();
        startFallingBlockTask();
        startArmorStandTask();
    }

    /** Dùng bởi GUI: bật/tắt 1 module và ghi luôn xuống config.yml để không mất khi restart. */
    public void persistModuleToggle(String path, boolean value) {
        getConfig().set(path, value);
        saveConfig();
    }

    // ================== VÒNG LẶP CHÍNH ==================

    private void startMonitorTask() {
        long period = Math.max(1, config.checkIntervalSeconds) * 20L;
        monitorTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!running || !config.enabled || simulating) return;

            double tps1m = Bukkit.getServer().getTPS()[0];
            double sampledTps = Math.min(20.0, tps1m);

            OptimizationEngine.Settings settings = engine.onTpsSample(sampledTps);
            applySettings(settings);

            recordHistory(sampledTps, settings.level);

            if (settings.level != lastAnnouncedLevel) {
                announceLevelChange(lastAnnouncedLevel, settings, sampledTps);
                discordNotifier.notifyLevelChange(lastAnnouncedLevel, settings.level, sampledTps);
                lastAnnouncedLevel = settings.level;
            }
        }, 100L, period);
    }

    private void applySettings(OptimizationEngine.Settings settings) {
        worldOptimizer.apply(settings);
        entityOptimizer.apply(settings);
        hopperListener.updateSettings(settings);
        itemMergeTask.updateSettings(settings);
        mobCapEnforcer.updateLevel(settings.level);
        xpOrbMergeTask.updateSettings(settings);
        fallingBlockLimiter.updateLevel(settings.level);
        armorStandLimiter.updateLevel(settings.level);
    }

    private void startItemMergeTask() {
        long period = Math.max(1, (long) (config.itemMergeCheckIntervalSeconds * 20L));
        itemMergeSchedule = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!running || !config.enabled || !config.moduleItemMerge) return;
            itemMergeTask.runOnce();
        }, 200L, period);
    }

    private void startMobCapTask() {
        long period = Math.max(1, (long) (config.mobCapCheckIntervalSeconds * 20L));
        mobCapSchedule = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!running || !config.enabled) return;
            mobCapEnforcer.runOnce();
        }, 400L, period);
    }

    private void startXpOrbMergeTask() {
        long period = Math.max(1, (long) (config.itemMergeCheckIntervalSeconds * 20L));
        xpOrbMergeSchedule = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!running || !config.enabled || !config.moduleXpOrbMerge) return;
            xpOrbMergeTask.runOnce();
        }, 220L, period);
    }

    private void startFallingBlockTask() {
        long period = Math.max(1, (long) (config.fallingBlockCheckIntervalSeconds * 20L));
        fallingBlockSchedule = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!running || !config.enabled) return;
            fallingBlockLimiter.runOnce();
        }, 500L, period);
    }

    private void startArmorStandTask() {
        long period = Math.max(1, (long) (config.armorStandCheckIntervalSeconds * 20L));
        armorStandSchedule = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!running || !config.enabled) return;
            armorStandLimiter.runOnce();
        }, 600L, period);
    }

    private void recordHistory(double tps, OptimizationEngine.Level level) {
        history.addLast(new HistoryPoint(System.currentTimeMillis(), tps, level));
        while (history.size() > HISTORY_MAX) {
            history.removeFirst();
        }
        if (config.historyFileEnabled) {
            historyFileWriter.append(System.currentTimeMillis(), tps, level.name());
        }
    }

    private void announceLevelChange(OptimizationEngine.Level from, OptimizationEngine.Settings to, double tps) {
        String tag = GradientUtil.gradientBold("[SmartOptimizer]", GradientUtil.BRAND_A, GradientUtil.BRAND_C);
        String tpsColored = GradientUtil.colorizeTps(tps, config.mildThreshold, config.severeThreshold);
        String msg = tag + GradientUtil.RESET + " " + ChatColor.YELLOW
                + "TPS=" + tpsColored + GradientUtil.RESET + ChatColor.YELLOW + " -> chuyển mức tối ưu: "
                + ChatColor.WHITE + from + ChatColor.GRAY + " => " + ChatColor.WHITE + to.level
                + ChatColor.GRAY + " (" + to + ")";

        if (config.logToConsoleOnLevelChange) {
            getLogger().info(ChatColor.stripColor(msg));
        }
        if (config.notifyAdminsOnLevelChange) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("smartoptimizer.alerts")) {
                    player.sendMessage(msg);
                }
            }
        }
    }

    /**
     * Dùng cho lệnh/GUI "simulate": áp settings tương ứng 1 mức TPS giả lập ngay lập tức,
     * và tạm dừng vòng lặp tự động trong vài giây để không bị TPS thật ghi đè lên ngay.
     * Gọi với level = null để HUỶ mô phỏng và quay lại dùng TPS thật ngay lập tức.
     */
    public void simulateLevel(OptimizationEngine.Level level, CommandSender notifyTo) {
        if (level == null) {
            simulating = false;
            double realTps = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
            OptimizationEngine.Settings real = engine.onTpsSample(realTps);
            applySettings(real);
            if (notifyTo != null) {
                notifyTo.sendMessage(ChatColor.AQUA + "[SmartOptimizer] Đã hủy mô phỏng, quay về trạng thái thực tế: " + real.level);
            }
            return;
        }

        double fakeTps;
        switch (level) {
            case MILD: fakeTps = 18.5; break;
            case MODERATE: fakeTps = 16.0; break;
            case SEVERE: fakeTps = 10.0; break;
            default: fakeTps = 20.0; break;
        }

        simulating = true;
        OptimizationEngine.Settings settings = engine.onTpsSample(fakeTps);
        applySettings(settings);

        if (notifyTo != null) {
            notifyTo.sendMessage(ChatColor.AQUA + "[SmartOptimizer] Đã MÔ PHỎNG mức " + level + ": " + settings);
            notifyTo.sendMessage(ChatColor.GRAY + "Dùng lệnh/GUI 'Quay về trạng thái thật' để hủy mô phỏng bất cứ lúc nào.");
        }
    }

    // ================== GETTER / SETTER ==================

    public void setRunning(boolean running) {
        this.running = running;
        if (!running) {
            worldOptimizer.restoreAllToBase();
            entityOptimizer.restoreAllToBase();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isSimulating() {
        return simulating;
    }

    public boolean isMythicMobsDetected() {
        return mythicMobsDetected;
    }

    public boolean isPlaceholderApiHooked() {
        return placeholderApiHooked;
    }

    public OptConfig getOptConfig() {
        return config;
    }

    public OptimizationEngine getEngine() {
        return engine;
    }

    public WorldOptimizer getWorldOptimizer() {
        return worldOptimizer;
    }

    public EntityOptimizer getEntityOptimizer() {
        return entityOptimizer;
    }

    public OptGuiListener getGuiListener() {
        return guiListener;
    }

    public XpOrbMergeTask getXpOrbMergeTask() {
        return xpOrbMergeTask;
    }

    public FallingBlockLimiter getFallingBlockLimiter() {
        return fallingBlockLimiter;
    }

    public ChunkHealthReporter getChunkHealthReporter() {
        return chunkHealthReporter;
    }

    public HistoryFileWriter getHistoryFileWriter() {
        return historyFileWriter;
    }

    public ArmorStandLimiter getArmorStandLimiter() {
        return armorStandLimiter;
    }

    public List<HistoryPoint> getHistory() {
        return new ArrayList<>(history);
    }
}
