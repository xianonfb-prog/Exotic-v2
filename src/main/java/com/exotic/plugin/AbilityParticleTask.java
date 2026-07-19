package com.exotic.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityParticleTask extends BukkitRunnable {

    private final CombatListener combat;
    private static final Particle.DustOptions GOLD_DUST = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f);

    public AbilityParticleTask(CombatListener combat) {
        this.combat = combat;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (var entry : combat.hypersonicActive.entrySet()) {
            if (entry.getValue() <= now) continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    player.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.02);
        }

        for (var entry : combat.decreeActive.entrySet()) {
            if (entry.getValue() <= now) continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            player.getWorld().spawnParticle(Particle.DUST,
                    player.getLocation().add(0, 1.2, 0), 8, 0.4, 0.6, 0.4, 0, GOLD_DUST);
        }
    }
}
