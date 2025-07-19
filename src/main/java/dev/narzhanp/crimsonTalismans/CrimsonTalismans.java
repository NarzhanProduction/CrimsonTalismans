package dev.narzhanp.crimsonTalismans;

import dev.narzhanp.crimsonTalismans.commands.TalismanCommands;
import dev.narzhanp.crimsonTalismans.listener.TalismanGUIListener;
import dev.narzhanp.crimsonTalismans.listener.TalismanListener;
import dev.narzhanp.crimsonTalismans.manager.TalismanManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CrimsonTalismans extends JavaPlugin {
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
    }
}
