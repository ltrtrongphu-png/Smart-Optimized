package com.smartopt.modules;

import com.smartopt.core.OptConfig;
import com.smartopt.core.OptimizationEngine;
import com.smartopt.core.ProtectionRules;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Gộp các item rơi (dropped item) đứng gần nhau thành 1 stack để giảm số lượng entity
 * khi TPS thấp. CHỦ ĐỘNG BỎ QUA item có tên riêng, có lore, hoặc có PersistentDataContainer
 * (thường là item RPG đặc biệt/custom của plugin khác) để không làm hỏng gameplay RPG.
 */
public class ItemMergeTask {

    private final JavaPlugin plugin;
    private final OptConfig config;
    private volatile OptimizationEngine.Settings current;

    public ItemMergeTask(JavaPlugin plugin, OptConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateSettings(OptimizationEngine.Settings settings) {
        this.current = settings;
    }

    public void runOnce() {
        OptimizationEngine.Settings settings = this.current;
        if (settings == null || settings.itemMergeRadius <= 0) return;

        for (World world : plugin.getServer().getWorlds()) {
            if (config.worldExclusions.contains(world.getName())) continue;
            for (Chunk chunk : world.getLoadedChunks()) {
                mergeInChunk(chunk, settings.itemMergeRadius);
            }
        }
    }

    private void mergeInChunk(Chunk chunk, double radius) {
        Entity[] entities = chunk.getEntities();
        List<Item> items = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof Item && e.isValid()) {
                items.add((Item) e);
            }
        }
        if (items.size() < 2) return;

        double radiusSq = radius * radius;
        for (int i = 0; i < items.size(); i++) {
            Item a = items.get(i);
            if (a.isDead() || !a.isValid()) continue;
            ItemStack aStack = a.getItemStack();
            if (isProtected(a, aStack)) continue;

            for (int j = i + 1; j < items.size(); j++) {
                Item b = items.get(j);
                if (b.isDead() || !b.isValid()) continue;
                ItemStack bStack = b.getItemStack();
                if (isProtected(b, bStack)) continue;

                if (!aStack.isSimilar(bStack)) continue;
                if (aStack.getAmount() + bStack.getAmount() > aStack.getMaxStackSize()) continue;
                if (a.getLocation().distanceSquared(b.getLocation()) > radiusSq) continue;

                aStack.setAmount(aStack.getAmount() + bStack.getAmount());
                a.setItemStack(aStack);
                b.remove();
            }
        }
    }

    /** True nếu item này không nên bị gộp (do là item đặc biệt của RPG/plugin khác). */
    private boolean isProtected(Item entity, ItemStack stack) {
        if (stack == null) return true;
        if (!config.itemMergeSkipNamedOrEnchanted) return false;

        ProtectionRules.ItemFlags f = new ProtectionRules.ItemFlags();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            f.hasDisplayName = meta.hasDisplayName();
            f.hasLore = meta.hasLore();
            f.hasEnchants = meta.hasEnchants();
            f.hasItemPdc = !meta.getPersistentDataContainer().getKeys().isEmpty();
        }
        // Một số plugin RPG gắn PDC/metadata ngay trên THỰC THỂ item rơi (không phải trên ItemStack)
        f.hasEntityPdc = !entity.getPersistentDataContainer().getKeys().isEmpty()
                || entity.hasMetadata("MythicMobs");

        return ProtectionRules.isProtectedItem(f);
    }
}
