package com.exotic.plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

public interface ExoticItem {
    String id();
    String displayName();
    String announcement();
    NamedTextColor color();
    ItemStack build();

    /** Small-caps styled version of displayName(), for use in all player-facing text. */
    default String styledName() {
        return TextStyle.toSmallCaps(displayName());
    }

    static ExoticItem byId(String id) {
        SwordType sword = SwordType.byId(id);
        if (sword != null) return sword;
        return TomeType.byId(id);
    }
}
