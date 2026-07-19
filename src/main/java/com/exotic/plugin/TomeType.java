package com.exotic.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-sword Exotic items. Currently just the Tome of Subzero, but structured
 * the same way as SwordType so more can be added later (potions, bows, etc.)
 * without touching the trial/command/ability dispatch code.
 */
public enum TomeType implements ExoticItem {

    TOME1("tome1", "Tome Of Subzero", NamedTextColor.DARK_BLUE,
            List.of("The Frozen Warlock, Ico"),
            "Winter Has Found Its Warlock.");

    private final String id;
    private final String displayName;
    private final NamedTextColor color;
    private final List<String> subtitleLore;
    private final String announcement;

    TomeType(String id, String displayName, NamedTextColor color, List<String> subtitleLore, String announcement) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.subtitleLore = subtitleLore;
        this.announcement = announcement;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String announcement() { return announcement; }
    @Override public NamedTextColor color() { return color; }

    public static TomeType byId(String id) {
        for (TomeType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return null;
    }

    @Override
    public ItemStack build() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(TextStyle.toSmallCaps(displayName), color)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : subtitleLore) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Not renameable - same enforcement as swords (blocked in PrepareAnvilEvent by PDC tag presence)
        meta.getPersistentDataContainer().set(SwordType.KEY_SWORD_ID, PersistentDataType.STRING, id);

        item.setItemMeta(meta);
        return item;
    }
}
