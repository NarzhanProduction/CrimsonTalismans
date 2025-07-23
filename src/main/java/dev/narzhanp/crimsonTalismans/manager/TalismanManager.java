package dev.narzhanp.crimsonTalismans.manager;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import dev.narzhanp.crimsonTalismans.CrimsonTalismans;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.net.URL;
import java.util.*;

public class TalismanManager {
    private final CrimsonTalismans plugin;
    private FileConfiguration talismansConfig;
    private List<String> cachedTalismanNames;
    private static final NamespacedKey TALISMAN_KEY = new NamespacedKey("crimsontalismans", "talisman_id");

    public TalismanManager(CrimsonTalismans plugin) {
        this.plugin = plugin;
        this.cachedTalismanNames = new ArrayList<>();
    }

    public void loadTalismans() {
        File configFile = new File(plugin.getDataFolder(), "talismans.yml");
        if (!configFile.exists()) {
            plugin.saveResource("talismans.yml", true);
        }
        talismansConfig = YamlConfiguration.loadConfiguration(configFile);
        cachedTalismanNames = new ArrayList<>(talismansConfig.getConfigurationSection("talismans").getKeys(false));
        plugin.getLogger().info("Loaded " + cachedTalismanNames.size() + " talismans from configuration");
    }

    public void clearRecipes() {
        for (String talisman : cachedTalismanNames) {
            if (talismansConfig.getBoolean("talismans." + talisman + ".Recipe.Enabled")) {
                NamespacedKey key = new NamespacedKey(plugin, talisman);
                plugin.getServer().removeRecipe(key);
                plugin.getLogger().info("Removed recipe for talisman " + talisman);
            }
        }
        plugin.getLogger().info("Cleared all CrimsonTalismans recipes");
    }

    public void registerRecipes() {
        for (String talisman : cachedTalismanNames) {
            if (!talismansConfig.getBoolean("talismans." + talisman + ".Recipe.Enabled")) continue;

            NamespacedKey key = new NamespacedKey(plugin, talisman);
            ItemStack item = createTalismanItem(talisman);
            ShapedRecipe recipe = new ShapedRecipe(key, item);

            List<String> shape = talismansConfig.getStringList("talismans." + talisman + ".Recipe.Shape");
            if (shape.size() != 3) {
                plugin.getLogger().warning("Invalid shape configuration for talisman " + talisman + ": expected 3 rows, got " + shape.size());
                continue;
            }

            boolean validShape = true;
            for (String row : shape) {
                if (row.length() != 3) {
                    plugin.getLogger().warning("Invalid shape length for talisman " + talisman + ": row '" + row + "' has length " + row.length() + ", expected 3");
                    validShape = false;
                    break;
                }
            }
            if (!validShape) continue;

            try {
                recipe.shape(shape.get(0), shape.get(1), shape.get(2));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to register shape for talisman " + talisman + ": " + e.getMessage());
                continue;
            }

            for (String ingredient : talismansConfig.getConfigurationSection("talismans." + talisman + ".Recipe").getKeys(false)) {
                if (!ingredient.equals("Enabled") && !ingredient.equals("Title") && !ingredient.equals("Shape") && !ingredient.equals("Permission")) {
                    String material = talismansConfig.getString("talismans." + talisman + ".Recipe." + ingredient);
                    if (material != null && !material.isEmpty()) {
                        try {
                            recipe.setIngredient(ingredient.charAt(0), Material.valueOf(material));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid material '" + material + "' for talisman " + talisman + ", ingredient " + ingredient);
                        }
                    }
                }
            }

            try {
                plugin.getServer().addRecipe(recipe);
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("Failed to register recipe for talisman " + talisman + ": " + e.getMessage());
            }
        }
    }

    public ItemStack createTalismanItem(String talisman) {
        String path = "talismans." + talisman + ".Item";
        String materialStr = talismansConfig.getString(path + ".Material", "TOTEM_OF_UNDYING");
        ItemStack item;
        try {
            item = new ItemStack(Material.valueOf(materialStr));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialStr + "' for talisman " + talisman + ", defaulting to TOTEM_OF_UNDYING");
            item = new ItemStack(Material.TOTEM_OF_UNDYING);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            plugin.getLogger().warning("Failed to get ItemMeta for talisman " + talisman);
            return item;
        }

        // Add PDC to mark the item as a talisman
        meta.getPersistentDataContainer().set(TALISMAN_KEY, PersistentDataType.STRING, talisman);

        meta.setDisplayName(color(talismansConfig.getString(path + ".Displayname", "&cTalisman")));
        meta.setLore(color(talismansConfig.getStringList(path + ".Lore")));

        if (item.getType() == Material.PLAYER_HEAD) {
            String texture = talismansConfig.getString(path + ".Texture");
            if (texture != null && !texture.isEmpty()) {
                try {
                    SkullMeta skullMeta = (SkullMeta) meta;
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                    PlayerTextures textures = profile.getTextures();

                    if (texture.startsWith("http")) {
                        textures.setSkin(new URL(texture));
                    } else {
                        String decoded = new String(Base64.getDecoder().decode(texture));
                        int urlIndex = decoded.indexOf("http://textures.minecraft.net/");
                        if (urlIndex != -1) {
                            int endIndex = decoded.indexOf("\"", urlIndex);
                            String extractedUrl = decoded.substring(urlIndex, endIndex);
                            textures.setSkin(new URL(extractedUrl));
                        } else {
                            throw new IllegalArgumentException("No valid texture URL found in base64.");
                        }
                    }

                    profile.setTextures(textures);
                    skullMeta.setPlayerProfile(profile);
                    meta = skullMeta;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to apply texture for talisman " + talisman + ": " + e.getMessage());
                    item.setType(Material.PLAYER_HEAD);
                }
            }
        }

        List<String> enchantments = talismansConfig.getStringList(path + ".Enchantments");
        for (String enchant : enchantments) {
            try {
                String[] parts = enchant.split(",");
                if (parts.length != 2) {
                    plugin.getLogger().warning("Invalid enchantment format '" + enchant + "' for talisman " + talisman);
                    continue;
                }
                String enchantName = parts[0].trim().toUpperCase();
                int level = Integer.parseInt(parts[1].trim());
                Enchantment enchantment = Enchantment.getByName(enchantName);
                if (enchantment != null) {
                    meta.addEnchant(enchantment, level, true);
                } else {
                    plugin.getLogger().warning("Invalid enchantment type: " + enchantName + " for talisman " + talisman);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid enchantment level in '" + enchant + "' for talisman " + talisman);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to apply enchantment '" + enchant + "' for talisman " + talisman + ": " + e.getMessage());
            }
        }

        meta.getItemFlags().forEach(meta::removeItemFlags);
        boolean glow = talismansConfig.getBoolean(path + ".Glow", false);
        if (glow) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        if (talismansConfig.getBoolean(path + ".Hide-attributes")) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        }

        meta.setCustomModelData(talismansConfig.getInt(path + ".CustomModelData", 0));
        item.setItemMeta(meta);
        return item;
    }

    public String getTalismanId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(TALISMAN_KEY, PersistentDataType.STRING);
    }

    public Map<String, Double> getTalismanModifiers(String talisman) {
        Map<String, Double> modifiers = new HashMap<>();
        for (String modifier : talismansConfig.getStringList("talismans." + talisman + ".Modifiers")) {
            String[] parts = modifier.split("\\|");
            if (parts.length == 3) {
                String attribute = parts[0];
                try {
                    double amount = Double.parseDouble(parts[1]);
                    modifiers.put(attribute, amount);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid modifier amount for talisman " + talisman + ": " + parts[1]);
                }
            } else {
                plugin.getLogger().warning("Invalid modifier format for talisman " + talisman + ": " + modifier);
            }
        }
        return modifiers;
    }

    public List<PotionEffect> getTalismanPotionEffects(String talisman) {
        List<PotionEffect> effects = new ArrayList<>();
        for (String effect : talismansConfig.getStringList("talismans." + talisman + ".PotionEffects")) {
            String[] parts = effect.split("\\|");
            if (parts.length == 3) {
                String effectType = parts[0].trim().toUpperCase();
                try {
                    PotionEffectType type = PotionEffectType.getByName(effectType);
                    if (type == null) {
                        plugin.getLogger().warning("Invalid potion effect type: " + effectType + " for talisman " + talisman);
                        continue;
                    }
                    int duration = Integer.parseInt(parts[1].trim());
                    int amplifier = Integer.parseInt(parts[2].trim());
                    effects.add(new PotionEffect(type, duration, amplifier, true, false));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid number format in potion effect '" + effect + "' for talisman " + talisman);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse potion effect '" + effect + "' for talisman " + talisman + ": " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("Invalid potion effect format for talisman " + talisman + ": " + effect);
            }
        }
        return effects;
    }

    public String getCraftPermission(String talisman) {
        String path = "talismans." + talisman + ".Recipe";
        return talismansConfig.getString(path + ".Permission", null);
    }

    public Map<Character, Material> getRecipeIngredients(String talisman) {
        Map<Character, Material> ingredients = new HashMap<>();
        for (String ingredient : talismansConfig.getConfigurationSection("talismans." + talisman + ".Recipe").getKeys(false)) {
            if (!ingredient.equals("Enabled") && !ingredient.equals("Title") && !ingredient.equals("Shape") && !ingredient.equals("Permission")) {
                String material = talismansConfig.getString("talismans." + talisman + ".Recipe." + ingredient);
                if (material != null && !material.isEmpty()) {
                    try {
                        ingredients.put(ingredient.charAt(0), Material.valueOf(material));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material '" + material + "' for talisman " + talisman + ", ingredient " + ingredient);
                    }
                }
            }
        }
        return ingredients;
    }

    public List<String> getTalismanNames() {
        return new ArrayList<>(cachedTalismanNames);
    }

    public boolean hasRecipe(String talisman) {
        return talismansConfig.getBoolean("talismans." + talisman + ".Recipe.Enabled");
    }

    public List<String> getRecipeShape(String talisman) {
        return talismansConfig.getStringList("talismans." + talisman + ".Recipe.Shape");
    }

    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    private List<String> color(List<String> text) {
        return text.stream().map(this::color).collect(java.util.stream.Collectors.toList());
    }
}