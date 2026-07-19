package com.exotic.plugin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Handles one frozen entity for the Tome of Subzero's Ice Age ability.
 * No block manipulation - just locks movement and blocks item use/attacks
 * (attack-blocking and interact-blocking are handled via CombatListener.frozen
 * and PassiveListener's interact check) for the duration.
 */
public class IceCageTask extends BukkitRunnable {

    private final ExoticPlugin plugin;
    private final CombatListener combat;
    private final LivingEntity target;
    private final long durationTicks;
    private long elapsed = 0;

    public IceCageTask(ExoticPlugin plugin, CombatListener combat, LivingEntity target, long durationTicks) {
        this.plugin = plugin;
        this.combat = combat;
        this.target = target;
        this.durationTicks = durationTicks;
    }

    public void start() {
        combat.frozen.add(target.getUniqueId());
        int effectDuration = (int) durationTicks + 10;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, effectDuration, 250, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, effectDuration, 250, false, false));
        runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void run() {
        elapsed++;
        if (!target.isValid() || target.isDead() || elapsed >= durationTicks) {
            finish();
            return;
        }
        // Hard-lock horizontal movement each tick; allow gravity so they don't hover.
        Vector v = target.getVelocity();
        target.setVelocity(new Vector(0, Math.min(v.getY(), 0), 0));
    }

    private void finish() {
        combat.frozen.remove(target.getUniqueId());
        cancel();
    }
}
