package com.enderdevelopment.plugin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class QuestHolder implements InventoryHolder {
    private final boolean admin;
    private final String questId;
    public QuestHolder(boolean admin, String questId) { this.admin = admin; this.questId = questId; }
    public boolean isAdmin() { return admin; }
    public String getQuestId() { return questId; }
    @Override public Inventory getInventory() { return null; }
}
