package com.exotic.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreboardManager {

    private final ExoticPlugin plugin;
    private final Map<java.util.UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(ExoticPlugin plugin) {
        this.plugin = plugin;
    }

    /** Rebuilds and (re)sends the sidebar for a player based on their active trial, if any. */
    public void update(Player player) {
        TrialSystem.ActiveTrial trial = plugin.trials().get(player);
        if (trial == null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            return;
        }

        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective obj = board.getObjective("exotic_trial");
        if (obj != null) obj.unregister();
        obj = board.registerNewObjective("exotic_trial", Criteria.DUMMY,
                net.kyori.adventure.text.Component.text("§6§l" + TextStyle.toSmallCaps("Exotic Trial")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        ExoticItem item = ExoticItem.byId(trial.swordId);
        Map<TrialSystem.ObjectiveType, Integer> required = TrialSystem.REQUIREMENTS.get(trial.swordId);

        List<String> lines = new ArrayList<>();
        lines.add("§f§l" + item.styledName());
        lines.add("§7§m----------------");

        for (var entry : trial.progress.entrySet()) {
            int need = required.get(entry.getKey());
            int have = entry.getValue();
            boolean done = have >= need;
            String bullet = done ? "§a\u2714 " : "§e\u27A4 ";
            String amount = (done ? "§a" : "§f") + have + "§7/§f" + need;
            lines.add(bullet + "§7" + entry.getKey().label + ": " + amount);
        }

        int score = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            // Append an invisible unique color-code suffix so identical-looking
            // lines never collide as scoreboard entries (Bukkit requires unique strings).
            String uniqueSuffix = "§" + Integer.toHexString(i % 16) + "§r";
            obj.getScore(lines.get(i) + uniqueSuffix).setScore(score--);
        }

        player.setScoreboard(board);
    }

    /** Temporarily hides a player's nametag for the given duration (Lurker ability). */
    public void hideNameTag(Player player, long ticks) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainBoard.getTeam("exotic_hidden");
        if (team == null) {
            team = mainBoard.registerNewTeam("exotic_hidden");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        team.addEntry(player.getName());

        Team finalTeam = team;
        Bukkit.getScheduler().runTaskLater(plugin, () -> finalTeam.removeEntry(player.getName()), ticks);
    }

    public void clear(Player player) {
        boards.remove(player.getUniqueId());
    }
}
