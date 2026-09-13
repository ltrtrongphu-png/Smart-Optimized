package com.smartopt.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marker rỗng để nhận diện đâu là Inventory thuộc GUI của SmartOptimizer khi xử lý click. */
public class SmartOptGuiHolder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
