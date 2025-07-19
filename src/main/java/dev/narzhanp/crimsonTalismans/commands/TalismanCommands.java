package dev.narzhanp.crimsonTalismans.commands;

import dev.narzhanp.crimsonTalismans.CrimsonTalismans;
import dev.narzhanp.crimsonTalismans.gui.TalismanGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TalismanCommands implements CommandExecutor, TabCompleter {
    private final CrimsonTalismans plugin;

    public TalismanCommands(CrimsonTalismans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (sender instanceof Player player) {
                if (player.hasPermission("crimsontalismans.gui")) {
                    TalismanGUI gui = new TalismanGUI(plugin, player);
                    plugin.getTalismanGUIListener().registerGUI(player, gui);
                    gui.open();
                } else {
                    player.sendMessage(color(plugin.getLangConfig().getString("messages.prefix", "&c[CrimsonTalismans] ") +
                            plugin.getLangConfig().getString("messages.no-permission", "&cYou don't have permission!")));
                }
            } else {
                sender.sendMessage(color(plugin.getLangConfig().getString("messages.prefix", "&c[CrimsonTalismans] ") +
                        plugin.getLangConfig().getString("messages.player-only", "&cThis command can only be used by players!")));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("crimsontalismans.reload")) {
                sender.sendMessage(color(plugin.getLangConfig().getString("messages.prefix", "&c[CrimsonTalismans] ") +
                        plugin.getLangConfig().getString("messages.no-permission", "&cYou don't have permission!")));
                return true;
            }

            plugin.getLogger().info("Reloading CrimsonTalismans configurations...");
            plugin.loadLangConfig();
            plugin.getTalismanManager().loadTalismans();
            plugin.getTalismanManager().clearRecipes();
            plugin.getTalismanManager().registerRecipes();
            sender.sendMessage(color(plugin.getLangConfig().getString("messages.prefix", "&c[CrimsonTalismans] ") +
                    plugin.getLangConfig().getString("messages.reload-success", "&aPlugin reloaded successfully!")));
            plugin.getLogger().info("Reload completed successfully");
            return true;
        }

        sender.sendMessage(color(plugin.getLangConfig().getString("messages.prefix", "&c[CrimsonTalismans] ") +
                plugin.getLangConfig().getString("messages.invalid-command", "&cInvalid command! Use /crimt [gui|reload]")));
        return false;
    }

    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("crimsontalismans.gui")) {
                completions.add("gui");
            }
            if (sender.hasPermission("crimsontalismans.reload")) {
                completions.add("reload");
            }
        }

        List<String> filteredCompletions = new ArrayList<>();
        for (String completion : completions) {
            if (args.length == 0 || completion.toLowerCase().startsWith(args[0].toLowerCase())) {
                filteredCompletions.add(completion);
            }
        }

        return filteredCompletions;
    }
}