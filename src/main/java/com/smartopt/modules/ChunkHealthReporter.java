package com.smartopt.modules;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Module CHỈ ĐỌC (không xóa/sửa bất cứ thứ gì) - quét các chunk đang tải và
 * xếp hạng theo tổng số entity, kèm phân loại (mob/item/orb XP/armor
 * stand/khối đang rơi/khác) để admin TỰ QUYẾT ĐỊNH nên làm gì.
 *
 * Đây là lựa chọn an toàn hơn nhiều so với việc tự động xóa Armor Stand hay
 * item frame (những thứ này thường là trang trí/xây dựng có chủ đích của
 * người chơi) - plugin chỉ báo cáo, không bao giờ tự ý đụng vào chúng.
 */
public class ChunkHealthReporter {

    public static final class ChunkReport {
        public final String world;
        public final int x;
        public final int z;
        public final int total;
        public final int mobs;
        public final int items;
        public final int xpOrbs;
        public final int armorStands;
        public final int fallingBlocks;
        public final int other;

        ChunkReport(String world, int x, int z, int total, int mobs, int items,
                    int xpOrbs, int armorStands, int fallingBlocks, int other) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.total = total;
            this.mobs = mobs;
            this.items = items;
            this.xpOrbs = xpOrbs;
            this.armorStands = armorStands;
            this.fallingBlocks = fallingBlocks;
            this.other = other;
        }
    }

    /** Quét toàn bộ world được truyền vào, trả về top {@code limit} chunk nhiều entity nhất. */
    public List<ChunkReport> topChunks(Iterable<World> worlds, int limit) {
        List<ChunkReport> all = new ArrayList<>();

        for (World world : worlds) {
            for (Chunk chunk : world.getLoadedChunks()) {
                ChunkReport report = analyze(world.getName(), chunk);
                if (report.total > 0) {
                    all.add(report);
                }
            }
        }

        all.sort(Comparator.comparingInt((ChunkReport r) -> r.total).reversed());
        if (all.size() > limit) {
            return new ArrayList<>(all.subList(0, limit));
        }
        return all;
    }

    private ChunkReport analyze(String worldName, Chunk chunk) {
        Entity[] entities = chunk.getEntities();
        int mobs = 0, items = 0, xpOrbs = 0, armorStands = 0, fallingBlocks = 0, other = 0;

        for (Entity e : entities) {
            if (e instanceof Player) {
                continue; // không tính người chơi vào "rác" entity
            } else if (e instanceof Monster) {
                mobs++;
            } else if (e instanceof Item) {
                items++;
            } else if (e instanceof ExperienceOrb) {
                xpOrbs++;
            } else if (e instanceof ArmorStand) {
                armorStands++;
            } else if (e instanceof FallingBlock) {
                fallingBlocks++;
            } else {
                other++;
            }
        }

        int total = mobs + items + xpOrbs + armorStands + fallingBlocks + other;
        return new ChunkReport(worldName, chunk.getX(), chunk.getZ(), total,
                mobs, items, xpOrbs, armorStands, fallingBlocks, other);
    }
}
