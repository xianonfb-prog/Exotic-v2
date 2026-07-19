package com.exotic.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

public class TrialSystem {

    public enum ObjectiveType {
        RAID_WON("Pillager Raids Won"),
        VILLAGER_KILLS("Villagers Slain"),
        CATS_TAMED("Cats Tamed"),
        SPEED_POTIONS_DRUNK("Speed Potions Drunk"),
        INVIS_POTIONS_DRUNK("Invisibility Potions Drunk"),
        ICE_BLOCKS_COLLECTED("Ice Blocks Collected");

        public final String label;
        ObjectiveType(String label) { this.label = label; }
    }

    /** Static objective requirements per sword id. */
    public static final Map<String, Map<ObjectiveType, Integer>> REQUIREMENTS = new HashMap<>();
    static {
        Map<ObjectiveType, Integer> s1 = new LinkedHashMap<>();
        s1.put(ObjectiveType.RAID_WON, 1);
        s1.put(ObjectiveType.VILLAGER_KILLS, 25);
        REQUIREMENTS.put("sword1", s1);

        Map<ObjectiveType, Integer> s2 = new LinkedHashMap<>();
        s2.put(ObjectiveType.CATS_TAMED, 30);
        REQUIREMENTS.put("sword2", s2);

        Map<ObjectiveType, Integer> s3 = new LinkedHashMap<>();
        s3.put(ObjectiveType.SPEED_POTIONS_DRUNK, 35);
        REQUIREMENTS.put("sword3", s3);

        Map<ObjectiveType, Integer> s4 = new LinkedHashMap<>();
        s4.put(ObjectiveType.INVIS_POTIONS_DRUNK, 30);
        REQUIREMENTS.put("sword4", s4);

        Map<ObjectiveType, Integer> s5 = new LinkedHashMap<>();
        s5.put(ObjectiveType.RAID_WON, 5);
        REQUIREMENTS.put("sword5", s5);

        Map<ObjectiveType, Integer> t1 = new LinkedHashMap<>();
        t1.put(ObjectiveType.ICE_BLOCKS_COLLECTED, 700);
        REQUIREMENTS.put("tome1", t1);
    }

    public static class ActiveTrial {
        public final String swordId;
        public final Map<ObjectiveType, Integer> progress = new LinkedHashMap<>();

        public ActiveTrial(String swordId) {
            this.swordId = swordId;
            for (ObjectiveType type : REQUIREMENTS.get(swordId).keySet()) {
                progress.put(type, 0);
            }
        }

        public boolean isComplete() {
            Map<ObjectiveType, Integer> required = REQUIREMENTS.get(swordId);
            for (var e : required.entrySet()) {
                if (progress.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
            }
            return true;
        }
    }

    private final com.exotic.plugin.ExoticPlugin plugin;
    private final Map<UUID, ActiveTrial> active = new HashMap<>();

    public TrialSystem(com.exotic.plugin.ExoticPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasActiveTrial(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public ActiveTrial get(Player player) {
        return active.get(player.getUniqueId());
    }

    public boolean start(Player player, ExoticItem type) {
        if (hasActiveTrial(player)) return false;
        active.put(player.getUniqueId(), new ActiveTrial(type.id()));
        player.sendMessage(Component.text("Your trial for ", NamedTextColor.YELLOW)
                .append(Component.text(type.styledName(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" has begun.", NamedTextColor.YELLOW)));
        return true;
    }

    public void cancel(Player player) {
        active.remove(player.getUniqueId());
    }

    /** Manually complete a MANUAL-style trial (not currently used by any of the 5 swords, kept for future use). */
    public void forceComplete(Player player) {
        ActiveTrial trial = active.get(player.getUniqueId());
        if (trial == null) return;
        for (ObjectiveType t : REQUIREMENTS.get(trial.swordId).keySet()) {
            trial.progress.put(t, REQUIREMENTS.get(trial.swordId).get(t));
        }
        checkCompletion(player);
    }

    /** Adds progress toward an objective for the player's active trial, if it applies, then checks completion. */
    public void progress(Player player, ObjectiveType type, int amount) {
        ActiveTrial trial = active.get(player.getUniqueId());
        if (trial == null) return;
        if (!trial.progress.containsKey(type)) return;
        int required = REQUIREMENTS.get(trial.swordId).get(type);
        int current = trial.progress.get(type);
        trial.progress.put(type, Math.min(required, current + amount));
        checkCompletion(player);
    }

    private void checkCompletion(Player player) {
        ActiveTrial trial = active.get(player.getUniqueId());
        if (trial == null || !trial.isComplete()) return;

        active.remove(player.getUniqueId());

        ExoticItem item = ExoticItem.byId(trial.swordId);
        ItemStack built = item.build();
        SwordUtil.bindToOwner(built, player.getUniqueId());
        player.getInventory().addItem(built);
        player.sendMessage(Component.text("Trial complete! You have received ", NamedTextColor.GREEN)
                .append(Component.text(item.styledName(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(".", NamedTextColor.GREEN)));

        // Vague global announcement tied to the item - styled in its own color, small-caps font
        Bukkit.broadcast(Component.text(TextStyle.toSmallCaps(item.announcement()), item.color())
                .decorate(TextDecoration.ITALIC));
    }
}
