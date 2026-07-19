package com.exotic.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla Invisibility hides armor but NOT whatever is in the player's hand -
 * that's a hardcoded client render behavior with no Bukkit hook. The only way
 * to guarantee a player is fully invisible (armor, held item, everything) is
 * to remove them from other players' client view entirely.
 */
public final class VisibilityManager {

    private VisibilityManager() {}

    public static void hideFromEveryone(Plugin plugin, Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) viewer.hidePlayer(plugin, target);
        }
    }

    public static void showToEveryone(Plugin plugin, Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) viewer.showPlayer(plugin, target);
        }
    }

    /** Hides every online player from every other online player (mutual blackout). */
    public static void blackoutServer(Plugin plugin, long ticks) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player viewer : players) {
            for (Player target : players) {
                if (!viewer.equals(target)) viewer.hidePlayer(plugin, target);
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            for (Player viewer : online) {
                for (Player target : online) {
                    if (!viewer.equals(target)) viewer.showPlayer(plugin, target);
                }
            }
        }, ticks);
    }
}
