package com.smartopt.modules;

import com.smartopt.core.JoinRampCalculator;
import com.smartopt.core.OptConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Khi người chơi vào server, thay vì gửi full view-distance/simulation-distance ngay
 * lập tức (có thể gây spike TPS do phải gửi hàng loạt chunk cùng lúc, đặc biệt khi
 * nhiều người vào cùng lúc giờ cao điểm), plugin sẽ tăng dần trong vài giây đầu.
 *
 * Dùng đúng API công khai Player#setViewDistance / setSimulationDistance của Paper.
 */
public class JoinRampOptimizer implements Listener {

    private final JavaPlugin plugin;
    private final OptConfig config;

    private final Map<UUID, BukkitTask> activeRamps = new HashMap<>();
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public JoinRampOptimizer(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.moduleJoinRamp) return;
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (config.worldExclusions.contains(world.getName())) return;

        int targetView = world.getViewDistance();
        int targetSim = world.getSimulationDistance();
        int startView = Math.min(config.joinRampStartDistance, targetView);
        int startSim = Math.min(config.joinRampStartDistance, targetSim);

        long rampMillis = Math.max(1000, config.joinRampDurationSeconds * 1000);
        JoinRampCalculator viewRamp = new JoinRampCalculator(startView, targetView, rampMillis);
        JoinRampCalculator simRamp = new JoinRampCalculator(startSim, targetSim, rampMillis);

        long start = System.currentTimeMillis();
        joinTimes.put(player.getUniqueId(), start);

        player.setViewDistance(startView);
        player.setSimulationDistance(startSim);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            long elapsed = System.currentTimeMillis() - start;
            player.setViewDistance(viewRamp.distanceAt(elapsed));
            player.setSimulationDistance(simRamp.distanceAt(elapsed));

            if (viewRamp.isComplete(elapsed) && simRamp.isComplete(elapsed)) {
                stopRamp(player.getUniqueId());
            }
        }, 20L, 20L);

        activeRamps.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopRamp(event.getPlayer().getUniqueId());
    }

    private void stopRamp(UUID uuid) {
        BukkitTask task = activeRamps.remove(uuid);
        if (task != null) task.cancel();
        joinTimes.remove(uuid);
    }

    /** Huỷ toàn bộ ramp đang chạy, dùng khi tắt plugin. */
    public void shutdown() {
        for (BukkitTask task : activeRamps.values()) {
            task.cancel();
        }
        activeRamps.clear();
        joinTimes.clear();
    }
}
