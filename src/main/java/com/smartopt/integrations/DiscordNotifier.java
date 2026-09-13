package com.smartopt.integrations;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Gửi tin nhắn qua Discord Webhook khi server chuyển sang mức lag nặng (SEVERE)
 * hoặc khi phục hồi hoàn toàn về NORMAL. Chỉ dùng java.net thuần (không cần thư
 * viện ngoài), chạy BẤT ĐỒNG BỘ để không bao giờ làm treo main thread của server.
 */
public class DiscordNotifier {

    private final JavaPlugin plugin;
    private final OptConfig config;

    public DiscordNotifier(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void notifyLevelChange(OptimizationEngine.Level from, OptimizationEngine.Level to, double tps) {
        if (!config.discordWebhookEnabled) return;
        if (config.discordWebhookUrl == null || config.discordWebhookUrl.isBlank()) return;

        boolean shouldNotify =
                (to == OptimizationEngine.Level.SEVERE && config.discordNotifyOnSevere) ||
                (to == OptimizationEngine.Level.NORMAL && from != OptimizationEngine.Level.NORMAL
                        && config.discordNotifyOnRecoveryToNormal);

        if (!shouldNotify) return;

        String content = String.format(
                "**SmartOptimizer**: TPS=%.1f — chuyển từ `%s` sang `%s`",
                tps, from, to
        );

        // Chạy bất đồng bộ, không được phép chặn main thread server vì đây là network I/O
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sendWebhook(content));
    }

    private void sendWebhook(String content) {
        try {
            String json = "{\"content\": \"" + escapeJson(content) + "\"}";
            HttpURLConnection conn = (HttpURLConnection) URI.create(config.discordWebhookUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode(); // trigger request
            conn.disconnect();
        } catch (Exception ex) {
            plugin.getLogger().warning("[SmartOptimizer] Không thể gửi Discord webhook: " + ex.getMessage());
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
