package com.enderdevelopment.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestManager {
    private final Main plugin;
    private final File questFile;
    private final File progressFile;
    private final Map<String, Quest> quests = new HashMap<>();
    private final Map<UUID, Map<String, PlayerQuestProgress>> progress = new HashMap<>();

    public QuestManager(Main plugin) {
        this.plugin = plugin;
        questFile = new File(plugin.getDataFolder(), "quests.yml");
        progressFile = new File(plugin.getDataFolder(), "progress.yml");
        load();
    }

    public Collection<Quest> getQuests() { return new ArrayList<>(quests.values()); }
    public Quest get(String id) { return quests.get(id.toLowerCase()); }
    public void put(Quest quest) { quests.put(quest.getId().toLowerCase(), quest); save(); }
    public void remove(String id) { quests.remove(id.toLowerCase()); save(); }

    public PlayerQuestProgress progress(Player player, Quest quest) {
        Map<String, PlayerQuestProgress> playerProgress = progress.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
        PlayerQuestProgress value = playerProgress.computeIfAbsent(quest.getId(), key -> new PlayerQuestProgress(nowPeriodStart(quest.getPeriod())));
        if (value.getPeriodStarted() != nowPeriodStart(quest.getPeriod())) value.reset(nowPeriodStart(quest.getPeriod()));
        return value;
    }

    public void addProgress(Player player, String category, int amount) {
        for (Quest quest : quests.values()) {
            if (!quest.getCategory().equalsIgnoreCase(category)) continue;
            PlayerQuestProgress value = progress(player, quest);
            if (value.isPaid()) continue;
            value.setProgress(Math.min(quest.getTarget(), value.getProgress() + amount));
        }
        save();
    }

    public void save() {
        YamlConfiguration questsConfig = new YamlConfiguration();
        for (Quest quest : quests.values()) {
            String path = "quests." + quest.getId();
            questsConfig.set(path + ".name", quest.getName());
            questsConfig.set(path + ".description", quest.getDescription());
            questsConfig.set(path + ".category", quest.getCategory());
            questsConfig.set(path + ".period", quest.getPeriod().name());
            questsConfig.set(path + ".target", quest.getTarget());
            questsConfig.set(path + ".reward", quest.getReward());
        }
        YamlConfiguration progressConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, PlayerQuestProgress>> entry : progress.entrySet()) {
            for (Map.Entry<String, PlayerQuestProgress> item : entry.getValue().entrySet()) {
                String path = "players." + entry.getKey() + "." + item.getKey();
                PlayerQuestProgress value = item.getValue();
                progressConfig.set(path + ".progress", value.getProgress());
                progressConfig.set(path + ".started", value.getPeriodStarted());
                progressConfig.set(path + ".paid", value.isPaid());
            }
        }
        try { questsConfig.save(questFile); progressConfig.save(progressFile); }
        catch (IOException exception) { plugin.getLogger().severe("Could not save quest data: " + exception.getMessage()); }
    }

    private void load() {
        if (!questFile.exists()) plugin.saveResource("quests.yml", false);
        YamlConfiguration questsConfig = YamlConfiguration.loadConfiguration(questFile);
        ConfigurationSection section = questsConfig.getConfigurationSection("quests");
        if (section != null) for (String id : section.getKeys(false)) {
            String path = "quests." + id;
            try {
                Quest quest = new Quest(id, questsConfig.getString(path + ".name", id), questsConfig.getString(path + ".description", "Complete the objective."),
                        questsConfig.getString(path + ".category", "kills"), Quest.Period.valueOf(questsConfig.getString(path + ".period", "DAILY").toUpperCase()),
                        questsConfig.getInt(path + ".target", 1), questsConfig.getDouble(path + ".reward", 0));
                quests.put(id.toLowerCase(), quest);
            } catch (IllegalArgumentException ignored) { plugin.getLogger().warning("Skipping invalid quest: " + id); }
        }
        if (!progressFile.exists()) return;
        YamlConfiguration progressConfig = YamlConfiguration.loadConfiguration(progressFile);
        ConfigurationSection players = progressConfig.getConfigurationSection("players");
        if (players == null) return;
        for (String uuidText : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                Map<String, PlayerQuestProgress> playerProgress = progress.computeIfAbsent(uuid, key -> new HashMap<>());
                for (String id : progressConfig.getConfigurationSection("players." + uuidText).getKeys(false)) {
                    String path = "players." + uuidText + "." + id;
                    PlayerQuestProgress value = new PlayerQuestProgress(progressConfig.getLong(path + ".started"));
                    value.setProgress(progressConfig.getInt(path + ".progress")); value.setPaid(progressConfig.getBoolean(path + ".paid"));
                    playerProgress.put(id, value);
                }
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private long nowPeriodStart(Quest.Period period) {
        long now = System.currentTimeMillis();
        long length = switch (period) { case HOURLY -> 3600000L; case DAILY -> 86400000L; case WEEKLY -> 604800000L; };
        return now / length * length;
    }
}
