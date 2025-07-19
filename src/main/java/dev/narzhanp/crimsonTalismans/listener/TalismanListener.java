package dev.narzhanp.crimsonTalismans.listener;

import dev.narzhanp.crimsonTalismans.CrimsonTalismans;
import dev.narzhanp.crimsonTalismans.manager.TalismanManager;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TalismanListener implements Listener {
    private final TalismanManager talismanManager;
    private final CrimsonTalismans plugin;
    private final NamespacedKey modifierKey;

    public TalismanListener(TalismanManager talismanManager, CrimsonTalismans plugin) {
        this.talismanManager = talismanManager;
        this.plugin = plugin;
        this.modifierKey = new NamespacedKey(plugin, "talisman_modifier");
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        scheduleUpdateTalismanEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        scheduleUpdateTalismanEffects(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleUpdateTalismanEffects(player);
        }
    }

    private void scheduleUpdateTalismanEffects(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateTalismanEffects(player);
            }
        }.runTask(plugin);
    }

    private void updateTalismanEffects(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String talismanId = talismanManager.getTalismanId(offHand);

        // Remove existing modifiers
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attrInstance = player.getAttribute(attribute);
            if (attrInstance != null) {
                List<AttributeModifier> toRemove = new ArrayList<>();
                for (AttributeModifier modifier : attrInstance.getModifiers()) {
                    if (modifier.getKey().equals(modifierKey)) {
                        toRemove.add(modifier);
                    }
                }
                for (AttributeModifier modifier : toRemove) {
                    attrInstance.removeModifier(modifier);
                }
            }
        }

        // Apply new modifiers if valid talisman
        if (talismanId != null && talismanManager.getTalismanNames().contains(talismanId)) {
            Map<String, Double> modifiers = talismanManager.getTalismanModifiers(talismanId);
            for (Map.Entry<String, Double> entry : modifiers.entrySet()) {
                try {
                    Attribute attribute = Attribute.valueOf(entry.getKey());
                    AttributeInstance attrInstance = player.getAttribute(attribute);
                    if (attrInstance != null) {
                        attrInstance.addModifier(new AttributeModifier(
                                modifierKey,
                                entry.getValue(),
                                AttributeModifier.Operation.ADD_NUMBER
                        ));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid attribute: " + entry.getKey() + " for talisman: " + talismanId);
                }
            }
        }
    }
}
