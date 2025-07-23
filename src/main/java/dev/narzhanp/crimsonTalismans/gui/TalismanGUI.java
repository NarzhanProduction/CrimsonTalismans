package dev.narzhanp.crimsonTalismans.gui;

import dev.narzhanp.crimsonTalismans.CrimsonTalismans;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TalismanGUI {
    private final CrimsonTalismans plugin;
    private final Player player;
    private final Inventory inventory;
    private final TalismanGUIHolder holder;
    private final Map<String, ItemStack> talismanItems;

    public TalismanGUI(CrimsonTalismans plugin, Player player, boolean isRecipeGUI, String talismanName) {
        this.plugin = plugin;
        this.player = player;
        this.holder = new TalismanGUIHolder(isRecipeGUI);
        this.talismanItems = new HashMap<>();
        if (isRecipeGUI) {
            if (talismanName == null) {
                throw new IllegalArgumentException("talismanName cannot be null for recipe GUI");
            }
            this.inventory = Bukkit.createInventory(holder, 54, color(plugin.getLangConfig().getString("gui.recipe-title", "&8Recipe: %talisman%")
                    .replace("%talisman%", talismanName)));
            initializeRecipeGUI(talismanName);
        } else {
            this.inventory = Bukkit.createInventory(holder, 54, color(plugin.getLangConfig().getString("gui.title", "&8Talismans")));
            initializeMainGUI();
        }
    }

    public TalismanGUI(CrimsonTalismans plugin, Player player) {
        this(plugin, player, false, null);
    }

    private void initializeMainGUI() {
        int slot = 0;
        for (String talisman : plugin.getTalismanManager().getTalismanNames()) {
            if (slot >= inventory.getSize()) {
                plugin.getLogger().warning("Too many talismans to display in GUI for player " + player.getName());
                break;
            }
            ItemStack item = plugin.getTalismanManager().createTalismanItem(talisman);
            talismanItems.put(talisman, item);
            inventory.setItem(slot, item);
            slot++;
        }
        fillEmptySlots(inventory, createPanel(Material.GRAY_STAINED_GLASS_PANE, "&7"));
    }

    private void initializeRecipeGUI(String talismanName) {
        fillEmptySlots(inventory, createPanel(Material.BLACK_STAINED_GLASS_PANE, "&7"));
        int[] frameSlots = new int[]{9, 10, 11, 12, 13, 18, 22, 27, 31, 36, 37, 38, 39, 40};
        for (int slot : frameSlots) {
            inventory.setItem(slot, createPanel(Material.GRAY_STAINED_GLASS_PANE, "&7"));
        }

        int[] craftingGrid = new int[]{19, 20, 21, 28, 29, 30, 37, 38, 39};
        List<String> shape = plugin.getTalismanManager().getRecipeShape(talismanName);
        Map<Character, Material> ingredients = plugin.getTalismanManager().getRecipeIngredients(talismanName);

        if (shape.size() >= 3) {
            for (int i = 0; i < 3; i++) {
                String row = shape.get(i);
                if (row.length() != 3) {
                    plugin.getLogger().warning("Invalid recipe shape row length for talisman " + talismanName + ": " + row);
                    return;
                }
                for (int j = 0; j < 3; j++) {
                    char symbol = row.charAt(j);
                    Material material = ingredients.getOrDefault(symbol, Material.AIR);
                    ItemStack item = new ItemStack(material);
                    inventory.setItem(craftingGrid[i * 3 + j], item);
                }
            }
            ItemStack result = plugin.getTalismanManager().createTalismanItem(talismanName).clone();
            talismanItems.put(talismanName, result);
            inventory.setItem(34, result);
            inventory.setItem(32, new ItemStack(Material.ARROW));
            ItemStack backButton = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(color(
                    plugin.getLangConfig().getString("messages.back", "&cBack")));
            backButton.setItemMeta(backMeta);
            inventory.setItem(45, backButton);
        } else {
            player.closeInventory();
            plugin.getLogger().warning("Invalid recipe shape for talisman " + talismanName + " in GUI");
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public Map<String, ItemStack> getTalismanItems() {
        return talismanItems;
    }

    private ItemStack createPanel(Material material, String name) {
        ItemStack panel = new ItemStack(material);
        ItemMeta meta = panel.getItemMeta();
        meta.setDisplayName(color(name));
        panel.setItemMeta(meta);
        return panel;
    }

    private void fillEmptySlots(Inventory inventory, ItemStack panel) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, panel);
            }
        }
    }

    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    public static class TalismanGUIHolder implements org.bukkit.inventory.InventoryHolder {
        private final boolean isRecipeGUI;

        public TalismanGUIHolder(boolean isRecipeGUI) {
            this.isRecipeGUI = isRecipeGUI;
        }

        public boolean isRecipeGUI() {
            return isRecipeGUI;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}