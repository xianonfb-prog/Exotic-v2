package com.exotic.plugin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

public final class SwordUtil {

    private SwordUtil() {}

    /** Returns the sword id (e.g. "sword1") stored on this item, or null if it isn't an Exotic sword. */
    public static String getSwordId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(SwordType.KEY_SWORD_ID, PersistentDataType.STRING);
    }

    public static boolean isSword(ItemStack item, SwordType type) {
        String id = getSwordId(item);
        return id != null && id.equalsIgnoreCase(type.id());
    }

    /** True if the player has the given sword anywhere in their inventory (main + offhand + armor not checked). */
    public static boolean hasSwordInInventory(Player player, SwordType type) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getStorageContents()) {
            if (isSword(item, type)) return true;
        }
        return isSword(inv.getItemInOffHand(), type);
    }

    /** Returns the SwordType the player is currently holding in their main hand, or null. */
    public static SwordType heldSword(Player player) {
        String id = getSwordId(player.getInventory().getItemInMainHand());
        return id == null ? null : SwordType.byId(id);
    }

    // ---------------------------------------------------------------
    // Soulbound
    // ---------------------------------------------------------------

    /** Binds this exotic item to a specific player - whoever earned the trial or received it via /exotic give. */
    public static void bindToOwner(ItemStack item, java.util.UUID owner) {
        if (item == null || !item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(SwordType.KEY_OWNER, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
    }

    /** Returns the UUID this item is soulbound to, or null if it isn't bound. */
    public static java.util.UUID getOwner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(SwordType.KEY_OWNER, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return java.util.UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isSoulbound(ItemStack item) {
        return getOwner(item) != null;
    }

    public static boolean isOwner(ItemStack item, Player player) {
        java.util.UUID owner = getOwner(item);
        return owner != null && owner.equals(player.getUniqueId());
    }
}
