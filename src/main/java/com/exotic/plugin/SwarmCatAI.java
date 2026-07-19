package com.exotic.plugin;

import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Cats have no native "attack hostiles" AI, so this task manually paths a
 * summoned cat toward the nearest hostile mob or enemy player within range
 * and applies melee damage when close. Despawns after durationTicks.
 */
public class SwarmCatAI extends BukkitRunnable {

    private final ExoticPlugin plugin;
    private final Cat cat;
    private final Player owner;
    private final double range;
    private long ticksRemaining;

    public SwarmCatAI(ExoticPlugin plugin, Cat cat, Player owner, double range, long durationTicks) {
        this.plugin = plugin;
        this.cat = cat;
        this.owner = owner;
        this.range = range;
        this.ticksRemaining = durationTicks;
        cat.addScoreboardTag("exotic_summoned");
    }

    @Override
    public void run() {
        ticksRemaining -= 5L;
        if (!cat.isValid() || cat.isDead() || ticksRemaining <= 0) {
            cat.remove();
            cancel();
            return;
        }

        LivingEntity target = findTarget();
        if (target == null) return;

        double distSq = cat.getLocation().distanceSquared(target.getLocation());
        if (distSq <= 4.0) {
            target.damage(7.0, cat);
            org.bukkit.util.Vector kb = target.getLocation().toVector()
                    .subtract(cat.getLocation().toVector()).normalize().multiply(0.4).setY(0.2);
            target.setVelocity(target.getVelocity().add(kb));
        } else {
            cat.getPathfinder().moveTo(target.getLocation(), 1.4);
        }
    }

    private LivingEntity findTarget() {
        LivingEntity closest = null;
        double closestDist = range * range;
        for (org.bukkit.entity.Entity e : cat.getNearbyEntities(range, range, range)) {
            boolean valid = (e instanceof Monster) || (e instanceof Player p && !p.equals(owner));
            if (!valid || !(e instanceof LivingEntity le)) continue;
            double d = cat.getLocation().distanceSquared(le.getLocation());
            if (d < closestDist) {
                closestDist = d;
                closest = le;
            }
        }
        return closest;
    }
}
