package dev.narzhanp.crimsonTalismans.listener;

import dev.narzhanp.crimsonTalismans.CrimsonTalismans;
import dev.narzhanp.crimsonTalismans.gui.TalismanGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TalismanGUIListener implements Listener {
    private final CrimsonTalismans plugin;
    private final Map<UUID, TalismanGUI> activeGUIs;
    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    public TalismanGUIListener(CrimsonTalismans plugin) {
        this.plugin = plugin;
        this.activeGUIs = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Registered TalismanGUIListener");
    }

    public void registerGUI(Player player, TalismanGUI gui) { activeGUIs.put(player.getUniqueId(), gui); }

    public void unregisterGUI(Player player) { activeGUIs.remove(player.getUniqueId()); }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TalismanGUI.TalismanGUIHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player clicker)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        TalismanGUI gui = activeGUIs.get(clicker.getUniqueId());
        if (gui == null) {
            plugin.getLogger().info("No active GUI found for player " + clicker.getName());
            return;
        }

        boolean isMainGUI = !holder.isRecipeGUI();

        if (isMainGUI) {
            String talismanName = plugin.getTalismanManager().getTalismanId(clickedItem);
            if (talismanName == null || !gui.getTalismanItems().containsKey(talismanName)) {
                return;
            }

            if (event.isRightClick()) {
                if (!clicker.hasPermission("crimsontalismans.gettalisman")) {
                    clicker.sendMessage(color(plugin.getLangConfig().getString("messages.prefix", "&c[CrimsonTalismans] ") +
                            plugin.getLangConfig().getString("messages.no-permission", "&cYou don't have permission!")));
                    return;
                }

                ItemStack talismanItem = plugin.getTalismanManager().createTalismanItem(talismanName);
                clicker.getInventory().addItem(talismanItem);
                clicker.sendMessage(color(plugin.getLangConfig().getString("messages.prefix", "&c[CrimsonTalismans] ") +
                        plugin.getLangConfig().getString("messages.talisman-given", "&aTalisman %talisman% given!")
                                .replace("%talisman%", talismanName)));
                plugin.getLogger().info("Gave talisman " + talismanName + " to player " + clicker.getName());
            } else if (event.isLeftClick() && plugin.getTalismanManager().hasRecipe(talismanName)) {
                TalismanGUI recipeGUI = new TalismanGUI(plugin, clicker, true, talismanName);
                recipeGUI.openAndRegister();
                Bukkit.getScheduler().runTask(plugin, () -> registerGUI(clicker, recipeGUI));
            }
        } else {
            String backButtonName = color(plugin.getLangConfig().getString("gui.back", "&cBack"));
            ItemMeta meta = clickedItem.getItemMeta();
            if (event.getSlot() == 0 && meta != null && meta.hasDisplayName() && backButtonName.equals(meta.getDisplayName())) {
                TalismanGUI mainGUI = new TalismanGUI(plugin, clicker, false, null);
                mainGUI.openAndRegister();
                Bukkit.getScheduler().runTask(plugin, () -> registerGUI(clicker, mainGUI));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TalismanGUI.TalismanGUIHolder) {
            unregisterGUI((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TalismanGUI.TalismanGUIHolder) {
            event.setCancelled(true);
        }
    }
}