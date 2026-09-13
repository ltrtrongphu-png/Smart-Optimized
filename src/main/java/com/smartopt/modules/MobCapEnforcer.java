package com.smartopt.modules;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import com.smartopt.core.ProtectionRules;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Giới hạn số lượng mob vanilla (Monster/Animal) KHÔNG ĐƯỢC BẢO VỆ trong mỗi chunk,
 * dọn bớt mob dư thừa khi vượt ngưỡng (thường là hậu quả của trại mob/farm chạy lâu ngày).
 *
 * MẶC ĐỊNH TẮT trong config vì đây là can thiệp mạnh nhất (xoá entity thật).
 * Tuyệt đối bỏ qua (không bao giờ động tới):
 *  - Mob có tên riêng (custom name)
 *  - Mob có PersistentDataContainer khác rỗng (dữ liệu plugin khác gắn vào)
 *  - Mob đã bị thuần hoá (Tameable#isTamed) hoặc đang bị dắt dây (isLeashed)
 *  - Mob đang có passenger (đang cưỡi/being ridden)
 *  - Mob của MythicMobs (phát hiện qua metadata "MythicMobs", theo đúng chuẩn
 *    tương thích được các plugin khác dùng, không cần phụ thuộc cứng vào MythicMobs API)
 */
public class MobCapEnforcer {

    private final JavaPlugin plugin;
    private final OptConfig config;
    private volatile OptimizationEngine.Level currentLevel = OptimizationEngine.Level.NORMAL;

    public MobCapEnforcer(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateLevel(OptimizationEngine.Level level) {
        this.currentLevel = level;
    }

    public void runOnce() {
        if (!config.moduleMobCap) return;
        if (config.mobCapOnlyWhenLagging
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
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity e : chunk.getEntities()) {
            if (!(e instanceof Monster) && !(e instanceof Animals)) continue;
            LivingEntity le = (LivingEntity) e;
            if (isProtected(le)) continue;
            candidates.add(le);
        }

        int surplus = candidates.size() - config.mobCapPerChunk;
        if (surplus <= 0) return;

        // Xóa các mob "dư thừa" nhất (danh sách trả về không đảm bảo thứ tự tuổi,
        // nhưng vì đã loại hết mob đặc biệt nên an toàn để xóa bất kỳ con nào trong số dư)
        for (int i = 0; i < surplus && i < candidates.size(); i++) {
            candidates.get(i).remove();
        }
    }

    private boolean isProtected(LivingEntity entity) {
        ProtectionRules.MobFlags f = new ProtectionRules.MobFlags();
        f.hasCustomName = entity.getCustomName() != null;
        f.hasPdc = !entity.getPersistentDataContainer().getKeys().isEmpty();
        f.isLeashed = entity.isLeashed();
        f.isTamed = (entity instanceof Tameable) && ((Tameable) entity).isTamed();
        f.hasPassenger = !entity.getPassengers().isEmpty();
        // Chuẩn tương thích phổ biến để nhận diện mob MythicMobs mà không cần
        // phụ thuộc cứng vào API của MythicMobs (tránh lỗi nếu server không cài nó).
        f.hasMythicMobsTag = entity.hasMetadata("MythicMobs") || entity.hasMetadata("MM_ID");
        return ProtectionRules.isProtectedMob(f);
    }
}
