package dev.narzhanp.crimsonTalismans;

import dev.narzhanp.crimsonTalismans.commands.TalismanCommands;
import dev.narzhanp.crimsonTalismans.listener.TalismanGUIListener;
import dev.narzhanp.crimsonTalismans.listener.TalismanListener;
import dev.narzhanp.crimsonTalismans.manager.TalismanManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class CrimsonTalismans extends JavaPlugin implements Listener {
    private TalismanGUIListener guiListener;
    private TalismanManager talismanManager;
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        loadLangConfig();
        guiListener = new TalismanGUIListener(this);
        talismanManager = new TalismanManager(this);
        talismanManager.loadTalismans();
        talismanManager.registerRecipes();
        getServer().getPluginManager().registerEvents(new TalismanListener(talismanManager, this), this);
        getCommand("crimsontalismans").setExecutor(new TalismanCommands(this));
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Loaded CrimsonTalismans!");
    }

    public TalismanGUIListener getTalismanGUIListener() {
        return guiListener;
    }

    public TalismanManager getTalismanManager() {
        return talismanManager;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    public void loadLangConfig() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        Map<String, String> defaultLangValues = getStringStringMap();

        // Check and update missing keys
        boolean updated = false;
        for (Map.Entry<String, String> entry : defaultLangValues.entrySet()) {
            String key = entry.getKey();
            String defaultValue = entry.getValue();
            if (!langConfig.isSet(key)) {
                langConfig.set(key, defaultValue);
                getLogger().info("Added missing lang.yml key: " + key + " with default value: " + defaultValue);
                updated = true;
            }
        }

        // Save the config if any changes were made
        if (updated) {
            try {
                langConfig.save(langFile);
                getLogger().info("Updated lang.yml with missing keys and saved.");
            } catch (IOException e) {
                getLogger().severe("Failed to save updated lang.yml: " + e.getMessage());
            }
        }
    }

    private static @NotNull Map<String, String> getStringStringMap() {
        Map<String, String> defaultLangValues = new HashMap<>();
        defaultLangValues.put("messages.prefix", "&c[CrimsonTalismans] ");
        defaultLangValues.put("messages.commands.help", "&7Use /crimt [reload|gui]");
        defaultLangValues.put("messages.no-permission", "&cYou don't have permission!");
        defaultLangValues.put("messages.player-only", "&cThis command is for players only!");
        defaultLangValues.put("messages.reload-success", "&aConfiguration reloaded!");
        defaultLangValues.put("messages.invalid-command", "&cInvalid subcommand! Use /crimt [reload|gui]");
        defaultLangValues.put("messages.talisman-given", "&aTalisman %talisman% given!");
        defaultLangValues.put("gui.title", "&8Talismans");
        defaultLangValues.put("gui.recipe-title", "&8Recipe: %talisman%");
        defaultLangValues.put("gui.back", "&cBack");
        return defaultLangValues;
    }
}
