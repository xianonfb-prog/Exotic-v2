package com.exotic.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SoulboundListener implements Listener {

    private final ExoticPlugin plugin;

    // Soulbound items pulled off a player's death drops, waiting to be handed back on respawn.
    private final Map<UUID, List<ItemStack>> pendingReturn = new HashMap<>();

    public SoulboundListener(ExoticPlugin plugin) {
        this.plugin = plugin;
    }

    /** Nobody but the owner can pick a soulbound item up off the ground. */
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();
        if (!SwordUtil.isSoulbound(stack)) return;
        if (!SwordUtil.isOwner(stack, player)) {
            event.setCancelled(true);
        }
    }

    /** A soulbound item can't even be dropped by its own owner - it's bound to them, not just protected. */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (!SwordUtil.isSoulbound(stack)) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text("This item is soulbound to you and cannot be dropped.", NamedTextColor.RED));
    }

    /** Soulbound items never drop on death - pull them out of the drop list and hand them back on respawn. */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> keep = new ArrayList<>();
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (SwordUtil.isSoulbound(stack) && SwordUtil.isOwner(stack, player)) {
                keep.add(stack);
                it.remove();
            }
        }
        if (!keep.isEmpty()) {
            pendingReturn.put(player.getUniqueId(), keep);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> owed = pendingReturn.remove(player.getUniqueId());
        if (owed == null) return;
        for (ItemStack stack : owed) {
            player.getInventory().addItem(stack);
        }
    }
}
