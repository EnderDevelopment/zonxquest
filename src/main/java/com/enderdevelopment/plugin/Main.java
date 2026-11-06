package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {
    private QuestManager questManager;
    private EconomyBridge economy;
    private final Map<Player, EditPrompt> prompts = new HashMap<>();
    private final Map<UUID, Integer> killStreaks = new HashMap<>();

    @Override public void onEnable() {
        questManager = new QuestManager(this);
        economy = new EconomyBridge(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("quests").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) { sender.sendMessage("Only players can use this command."); return true; }
            if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
                if (!player.hasPermission("questgui.admin")) { player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED)); return true; }
                openAdmin(player); return true;
            }
            openQuests(player); return true;
        });
    }

    @Override public void onDisable() { if (questManager != null) questManager.save(); }

    private void openQuests(Player player) {
        Inventory inventory = Bukkit.createInventory(new QuestHolder(false, ""), 54, Component.text("Quests", NamedTextColor.GOLD));
        int slot = 0;
        for (Quest quest : questManager.getQuests()) {
            if (slot >= 45) break;
            PlayerQuestProgress progress = questManager.progress(player, quest);
            inventory.setItem(slot++, item(Material.WRITABLE_BOOK, quest.getName(), List.of(quest.getDescription(), "", "Progress: " + progress.getProgress() + "/" + quest.getTarget(), "Period: " + quest.getPeriod().name().toLowerCase(Locale.ROOT), "Reward: $" + formatMoney(quest.getReward()), progress.isPaid() ? "Completed" : "Click to claim when complete"), progress.isPaid() ? NamedTextColor.GRAY : NamedTextColor.YELLOW));
        }
        player.openInventory(inventory);
    }

    private void openAdmin(Player player) {
        Inventory inventory = Bukkit.createInventory(new QuestHolder(true, ""), 54, Component.text("Quest Admin", NamedTextColor.RED));
        int slot = 0;
        for (Quest quest : questManager.getQuests()) {
            if (slot >= 45) break;
            inventory.setItem(slot++, item(Material.BOOK, quest.getName(), List.of("ID: " + quest.getId(), "Target: " + quest.getTarget(), "Reward: $" + formatMoney(quest.getReward()), "Click to edit", "Shift-click to delete"), NamedTextColor.AQUA));
        }
        inventory.setItem(49, item(Material.EMERALD, "Create quest", List.of("Click, then type the quest ID in chat"), NamedTextColor.GREEN));
        player.openInventory(inventory);
    }

    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getView().getTopInventory().getHolder() instanceof QuestHolder holder)) return;
        if (holder.isAdmin() && !holder.getQuestId().isEmpty()) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!holder.isAdmin()) {
            if (event.getRawSlot() >= 45) return;
            List<Quest> quests = new ArrayList<>(questManager.getQuests());
            if (event.getRawSlot() >= quests.size()) return;
            Quest quest = quests.get(event.getRawSlot());
            PlayerQuestProgress progress = questManager.progress(player, quest);
            if (progress.getProgress() >= quest.getTarget() && !progress.isPaid()) {
                if (economy.deposit(player, quest.getReward())) {
                    progress.setPaid(true); questManager.save(); player.sendMessage(Component.text("Quest complete. You received $" + formatMoney(quest.getReward()) + ".", NamedTextColor.GREEN)); openQuests(player);
                } else player.sendMessage(Component.text("Vault economy was not found. Install Vault and an economy plugin.", NamedTextColor.RED));
            }
            return;
        }
        if (event.getRawSlot() == 49) { prompt(player, new EditPrompt(null, "id")); return; }
        List<Quest> quests = new ArrayList<>(questManager.getQuests());
        if (event.getRawSlot() >= quests.size() || event.getRawSlot() >= 45) return;
        Quest quest = quests.get(event.getRawSlot());
        if (event.isShiftClick()) { questManager.remove(quest.getId()); player.sendMessage(Component.text("Quest deleted.", NamedTextColor.GREEN)); openAdmin(player); return; }
        openEditor(player, quest);
    }

    private void openEditor(Player player, Quest quest) {
        Inventory inventory = Bukkit.createInventory(new QuestHolder(true, quest.getId()), 27, Component.text("Edit: " + quest.getName(), NamedTextColor.RED));
        inventory.setItem(10, item(Material.NAME_TAG, "Name", List.of(quest.getName(), "Click to change"), NamedTextColor.YELLOW));
        inventory.setItem(11, item(Material.PAPER, "Description", List.of(quest.getDescription(), "Click to change"), NamedTextColor.YELLOW));
        inventory.setItem(12, item(Material.COMPASS, "Category", List.of(quest.getCategory(), "Click to change"), NamedTextColor.YELLOW));
        inventory.setItem(13, item(Material.CLOCK, "Period", List.of(quest.getPeriod().name(), "Click to cycle"), NamedTextColor.YELLOW));
        inventory.setItem(14, item(Material.IRON_INGOT, "Target", List.of(String.valueOf(quest.getTarget()), "Click to change"), NamedTextColor.YELLOW));
        inventory.setItem(15, item(Material.EMERALD, "Reward", List.of("$" + formatMoney(quest.getReward()), "Click to change"), NamedTextColor.GREEN));
        inventory.setItem(22, item(Material.ARROW, "Back", List.of(), NamedTextColor.GRAY));
        player.openInventory(inventory);
    }

    @EventHandler public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getView().getTopInventory().getHolder() instanceof QuestHolder holder) || holder.getQuestId().isEmpty()) return;
        event.setCancelled(true);
        Quest quest = questManager.get(holder.getQuestId()); if (quest == null) return;
        switch (event.getRawSlot()) {
            case 10 -> prompt(player, new EditPrompt(quest, "name"));
            case 11 -> prompt(player, new EditPrompt(quest, "description"));
            case 12 -> prompt(player, new EditPrompt(quest, "category"));
            case 13 -> { quest.setPeriod(switch (quest.getPeriod()) { case HOURLY -> Quest.Period.DAILY; case DAILY -> Quest.Period.WEEKLY; case WEEKLY -> Quest.Period.HOURLY; }); questManager.save(); openEditor(player, quest); }
            case 14 -> prompt(player, new EditPrompt(quest, "target"));
            case 15 -> prompt(player, new EditPrompt(quest, "reward"));
            case 22 -> openAdmin(player);
            default -> { }
        }
    }

    private void prompt(Player player, EditPrompt prompt) {
        prompts.put(player, prompt); player.closeInventory(); player.sendMessage(Component.text("Type the new " + prompt.field + " in chat, or type cancel.", NamedTextColor.YELLOW));
    }

    @EventHandler public void onChat(AsyncPlayerChatEvent event) {
        EditPrompt prompt = prompts.remove(event.getPlayer()); if (prompt == null) return;
        event.setCancelled(true); String input = event.getMessage().trim();
        Bukkit.getScheduler().runTask(this, () -> applyPrompt(event.getPlayer(), prompt, input));
    }

    private void applyPrompt(Player player, EditPrompt prompt, String input) {
        if (input.equalsIgnoreCase("cancel")) { openAdmin(player); return; }
        try {
            if (prompt.quest == null) {
                if (!input.matches("[a-z0-9_-]+") || questManager.get(input) != null) throw new IllegalArgumentException();
                Quest quest = new Quest(input, input, "Complete the objective.", "kills", Quest.Period.DAILY, 1, 0); questManager.put(quest); player.sendMessage(Component.text("Quest created. Set its values in the editor.", NamedTextColor.GREEN)); openEditor(player, quest); return;
            }
            switch (prompt.field) { case "name" -> prompt.quest.setName(input); case "description" -> prompt.quest.setDescription(input); case "category" -> prompt.quest.setCategory(input); case "target" -> prompt.quest.setTarget(Math.max(1, Integer.parseInt(input))); case "reward" -> prompt.quest.setReward(Math.max(0, Double.parseDouble(input))); default -> { } }
            questManager.save(); openEditor(player, prompt.quest);
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Invalid value. Please try again.", NamedTextColor.RED)); prompt(player, prompt); }
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { economy.refresh(); }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        killStreaks.remove(victim.getUniqueId());
        if (killer == null) return;
        int streak = killStreaks.merge(killer.getUniqueId(), 1, Integer::sum);
        String category = killer.getInventory().getItemInMainHand().getType().name().toLowerCase(Locale.ROOT);
        if (category.contains("sword")) questManager.addProgress(killer, "sword_kills", 1);
        if (category.contains("axe")) questManager.addProgress(killer, "axe_kills", 1);
        if (category.contains("mace")) questManager.addProgress(killer, "mace_kills", 1);
        questManager.addProgress(killer, "kills", 1);
        PlayerQuestProgress streakProgress = null;
        Quest streakQuest = questManager.get("kill_streak");
        if (streakQuest != null) streakProgress = questManager.progress(killer, streakQuest);
        if (streakProgress != null && !streakProgress.isPaid()) streakProgress.setProgress(Math.min(streakQuest.getTarget(), streak));
        questManager.save();
    }

    private ItemStack item(Material material, String name, List<String> lore, NamedTextColor color) { ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta(); meta.displayName(Component.text(name, color)); meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList()); stack.setItemMeta(meta); return stack; }
    private String formatMoney(double amount) { return String.format(Locale.US, "%.2f", amount); }
    private record EditPrompt(Quest quest, String field) { }
}
