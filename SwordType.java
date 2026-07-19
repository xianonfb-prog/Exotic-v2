package com.exotic.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.UUID;

/**
 * All 5 Exotic swords: identity, fixed base stats, and lore.
 * Every sword shares: 8 Attack Damage, 1.6 Attack Speed, Unbreakable,
 * Sharpness V, Fire Aspect II, non-renameable (enforced in listener), and lore.
 */
public enum SwordType implements ExoticItem {

    SWORD1("sword1", "Judgement", Material.NETHERITE_SWORD, NamedTextColor.WHITE,
            List.of("The Equalization, Karma."),
            "The Scales Have Been Balanced."),

    SWORD2("sword2", "Pretty Kitty Princess Blade", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE,
            List.of("The First Princess, Prince."),
            "A New Princess Has Claimed Her Throne."),

    SWORD3("sword3", "Hypersonic Devastator", Material.NETHERITE_SWORD, NamedTextColor.AQUA,
            List.of("The Fastest, Exo"),
            "Sound Itself Could Not Keep Pace."),

    SWORD4("sword4", "Deception", Material.IRON_SWORD, NamedTextColor.DARK_RED,
            List.of("The Great Deceiver, Magma."),
            "The Shadows Have Chosen Their Vessel."),

    SWORD5("sword5", "Bane Of The Emperor", Material.NETHERITE_SWORD, NamedTextColor.RED,
            List.of("The True Emperor, Kaizer"),
            "Countless Empires Rise And Fall.");

    public static final NamespacedKey KEY_SWORD_ID = new NamespacedKey("exotic", "sword_id");
    public static final NamespacedKey KEY_OWNER = new NamespacedKey("exotic", "owner");

    private final String id;
    private final String displayName;
    private final Material material;
    private final NamedTextColor color;
    private final List<String> subtitleLore; // the italic "The X, Y." line(s) - never altered
    private final String announcement; // vague global broadcast on trial completion

    SwordType(String id, String displayName, Material material, NamedTextColor color,
              List<String> subtitleLore, String announcement) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.color = color;
        this.subtitleLore = subtitleLore;
        this.announcement = announcement;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String announcement() { return announcement; }
    public NamedTextColor color() { return color; }

    public static SwordType byId(String id) {
        for (SwordType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return null;
    }

    /**
     * Builds a fresh copy of this sword's ItemStack with all fixed generalities applied.
     */
    public ItemStack build() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(TextStyle.toSmallCaps(displayName), color)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        // Lore: subtitle line(s) first, italic/gray, exactly as provided - never altered.
        java.util.List<Component> lore = new java.util.ArrayList<>();
        for (String line : subtitleLore) {
            lore.add(Component.text(line, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true));
        }
        meta.lore(lore);

        // Fixed enchants
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);

        // Unbreakable by default
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        // Fixed 8 Attack Damage / 1.6 Attack Speed via attribute modifiers.
        // Base fist damage = 1.0, base attack speed = 4.0 -> offset to hit exact displayed values.
        meta.setAttributeModifiers(null); // clear any material-default modifiers first
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
                new AttributeModifier(UUID.nameUUIDFromBytes((id + "-dmg").getBytes()),
                        "exotic.attack_damage", 7.0d,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
                new AttributeModifier(UUID.nameUUIDFromBytes((id + "-spd").getBytes()),
                        "exotic.attack_speed", -2.4d,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        meta.getPersistentDataContainer().set(KEY_SWORD_ID, PersistentDataType.STRING, id);

        item.setItemMeta(meta);
        return item;
    }
}
