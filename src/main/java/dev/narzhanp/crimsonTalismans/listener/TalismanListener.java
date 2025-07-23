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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;


public class TalismanListener implements Listener {
    private final TalismanManager talismanManager;
    private final CrimsonTalismans plugin;
    private final NamespacedKey modifierKey;
    private final NamespacedKey talismanKey;
    private final Map<UUID, BukkitRunnable> playerEffectTasks;

    public TalismanListener(TalismanManager talismanManager, CrimsonTalismans plugin) {
        this.talismanManager = talismanManager;
        this.plugin = plugin;
        this.modifierKey = new NamespacedKey(plugin, "talisman_modifier");
        this.talismanKey = new NamespacedKey(plugin, "talisman_id");
        this.playerEffectTasks = new HashMap<>();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        BukkitRunnable task = playerEffectTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
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

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !result.hasItemMeta()) return;

        String talismanId = talismanManager.getTalismanId(result);
        if (talismanId == null || !talismanManager.getTalismanNames().contains(talismanId)) return;

        String permission = talismanManager.getCraftPermission(talismanId);
        if (permission != null && !permission.isEmpty()) {
            Player player = (Player) event.getView().getPlayer();
            if (!player.hasPermission(permission)) {
                event.getInventory().setResult(null);
            }
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
        UUID playerId = player.getUniqueId();

        // Remove existing modifiers and potion effects
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
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().getKey().getNamespace().equals(talismanKey.getNamespace())) {
                player.removePotionEffect(effect.getType());
            }
        }

        BukkitRunnable existingTask = playerEffectTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Apply new modifiers and effects
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

            // Apply potion effects and schedule renewal
            List<PotionEffect> effects = talismanManager.getTalismanPotionEffects(talismanId);
            if (!effects.isEmpty()) {
                for (PotionEffect effect : effects) {
                    try {
                        player.addPotionEffect(effect);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to apply potion effect '" + effect.getType().getName() + "' for talisman " + talismanId + ": " + e.getMessage());
                    }
                }

                // Calculate the minimum duration minus 1 second (20 ticks)
                long minDuration = effects.stream()
                        .mapToLong(PotionEffect::getDuration)
                        .min()
                        .orElse(200L); // Default to 200 ticks if no effects (shouldn't happen)
                long renewalInterval = Math.max(minDuration - 20L, 20L); // Ensure at least 20 ticks

                BukkitRunnable renewalTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            playerEffectTasks.remove(playerId);
                            cancel();
                            return;
                        }
                        ItemStack currentOffHand = player.getInventory().getItemInOffHand();
                        String currentTalismanId = talismanManager.getTalismanId(currentOffHand);
                        if (talismanId.equals(currentTalismanId)) {
                            for (PotionEffect effect : effects) {
                                try {
                                    player.addPotionEffect(effect);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to renew potion effect '" + effect.getType().getName() + "' for talisman " + talismanId + ": " + e.getMessage());
                                }
                            }
                        } else {
                            playerEffectTasks.remove(playerId);
                            cancel();
                        }
                    }
                };
                playerEffectTasks.put(playerId, renewalTask);
                renewalTask.runTaskTimer(plugin, renewalInterval, renewalInterval);
            }
        }
    }

    // Prohibit placing talismans
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        String talismanName;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        talismanName = pdc.get(talismanKey, PersistentDataType.STRING);

        if (talismanName != null) {
            event.setCancelled(true);
        }
    }
}
