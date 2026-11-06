# ZonxQuest Minecraft Quest GUI Plugin

ZonxQuest is a Minecraft quest GUI plugin for Paper servers that lets players view, track, and claim configurable quests while administrators create, edit, delete, schedule, and assign money rewards to quests through an in-game editor.

## Install

1. Use Java 21 and a Paper 1.21.x Minecraft server.
2. Install Vault and a Vault-compatible economy plugin.
3. Build the plugin with Maven:
   `mvn clean package`
4. Copy the generated `target/plugin.jar` file into the server's `plugins` folder.
5. Start or restart the server.
6. Confirm that the plugin creates its quest and progress data files.

## Use

Players can use:

`/quests`

This opens the quest GUI, showing each quest's name, description, progress, period, and reward. When a quest is complete, click it to claim the reward.

Administrators with the `questgui.admin` permission can use:

`/quests admin`

The admin GUI allows you to:

- Create quests by clicking Create quest and entering a unique lowercase ID using letters, numbers, underscores, or hyphens.
- Edit the quest name.
- Edit the description.
- Change the progress category.
- Cycle the period between hourly, daily, and weekly.
- Set the target amount.
- Set the money reward.
- Delete quests by shift-clicking them.

The included quests track mace kills, sword kills, axe kills, and kill streaks. Kill progress is recorded from player deaths based on the weapon held in the killer's main hand. General kill progress uses the `kills` category. Quest definitions and player progress are saved in YAML files and remain available after server restarts.

Administrators need the `questgui.admin` permission. This permission defaults to server operators.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/9d66ba8a-ff7d-4dea-b7eb-c6ef608d3a34?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=9d66ba8a-ff7d-4dea-b7eb-c6ef608d3a34
