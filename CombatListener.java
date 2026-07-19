package com.exotic.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.Particle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class CombatListener implements Listener {

    private final ExoticPlugin plugin;

    // sword1 Karma: victim -> attacker -> consecutive-hits-not-returned
    private final Map<UUID, Map<UUID, Integer>> karmaCounters = new HashMap<>();
    // sword1 ability: player -> invincible-until epoch millis
    public final Map<UUID, Long> retributionActive = new HashMap<>();
    // guard against infinite reflect loops between two retribution-active players
    private final Set<UUID> reflecting = new HashSet<>();
    // sword3 ability: player -> auto-crit-until epoch millis
    public final Map<UUID, Long> hypersonicActive = new HashMap<>();
    // sword4 passive: wielder -> total landed hits with sword4 (resets each 7th)
    private final Map<UUID, Integer> shadowsHitCount = new HashMap<>();
    // tome1 ability: entities currently frozen by Ice Age - can't attack while frozen
    public final Set<UUID> frozen = new HashSet<>();
    // players currently true-invisible (Lurker or Deception's death-save) - used to
    // re-hide them from anyone who joins mid-effect
    public final Set<UUID> trueInvisible = new HashSet<>();

    // Shared "ability currently active" windows, used only for the HUD (Active) indicator.
    public final Map<UUID, Long> swarmActive = new HashMap<>();  // sword2
    public final Map<UUID, Long> lurkerActive = new HashMap<>(); // sword4
    public final Map<UUID, Long> decreeActive = new HashMap<>(); // sword5

    private static final List<PotionEffectType> POSITIVE_POOL = List.of(
            PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.STRENGTH,
            PotionEffectType.JUMP_BOOST, PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE,
            PotionEffectType.FIRE_RESISTANCE, PotionEffectType.WATER_BREATHING, PotionEffectType.INVISIBILITY,
            PotionEffectType.NIGHT_VISION, PotionEffectType.ABSORPTION, PotionEffectType.LUCK,
            PotionEffectType.SLOW_FALLING
    );

    private static final List<PotionEffectType> NEGATIVE_POOL = List.of(
            PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.NAUSEA,
            PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.WEAKNESS,
            PotionEffectType.POISON, PotionEffectType.GLOWING, PotionEffectType.LEVITATION,
            PotionEffectType.UNLUCK, PotionEffectType.BAD_OMEN, PotionEffectType.DARKNESS
    );

    private final Random random = new Random();

    public CombatListener(ExoticPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Deception's Fatal Blow save - listens on the general EntityDamageEvent
     * (not just entity-vs-entity) since a fatal hit can come from anything:
     * fall damage, lava, mobs, players, etc. Runs at HIGH priority so it sees
     * the final computed damage before it would kill the player.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFatalBlow(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!SwordUtil.hasSwordInInventory(player, SwordType.SWORD4)) return;
        if (event.getFinalDamage() < player.getHealth()) return; // not fatal

        if (plugin.cooldowns().isOnCooldown(player.getUniqueId(), "sword4_totem")) return;
        plugin.cooldowns().trigger(player.getUniqueId(), "sword4_totem");

        event.setCancelled(true);
        player.setHealth(10.0); // 5 hearts
        player.setFireTicks(0);

        // Swap places with a random online player within 20 blocks, if any.
        List<Player> nearby = new ArrayList<>();
        for (Player other : player.getWorld().getPlayers()) {
            if (!other.equals(player) && other.getLocation().distanceSquared(player.getLocation()) <= 20 * 20) {
                nearby.add(other);
            }
        }
        if (!nearby.isEmpty()) {
            Player swapTarget = nearby.get(random.nextInt(nearby.size()));
            Location a = player.getLocation().clone();
            Location b = swapTarget.getLocation().clone();
            player.teleport(b);
            swapTarget.teleport(a);
        }

        player.sendMessage(Component.text("Deception spares you... for now.", NamedTextColor.DARK_RED));

        // Everyone, including the wielder, goes true-invisible for 15 seconds.
        for (Player p : Bukkit.getOnlinePlayers()) {
            trueInvisible.add(p.getUniqueId());
        }
        VisibilityManager.blackoutServer(plugin, 300L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                trueInvisible.remove(p.getUniqueId());
            }
        }, 300L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        // --- Tome of Subzero: frozen entities can't land attacks ---
        if (frozen.contains(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // --- Sword3 Hypersonic ability: auto-crit visuals/damage while active.
        // Runs against ANY LivingEntity target (mobs included), not just players.
        if (event.getDamager() instanceof Player attacker) {
            Long critUntil = hypersonicActive.get(attacker.getUniqueId());
            if (critUntil != null && critUntil > System.currentTimeMillis()
                    && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                event.setDamage(event.getDamage() * 1.5);
                if (event.getEntity() instanceof LivingEntity target) {
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                }
            }
        }

        if (!(event.getEntity() instanceof Player victim)) return;

        // --- Karmic Retribution: invincibility + reflect ---
        Long until = retributionActive.get(victim.getUniqueId());
        if (until != null && until > System.currentTimeMillis()) {
            double dealt = event.getFinalDamage();
            event.setCancelled(true);

            if (event.getDamager() instanceof LivingEntity attackerEntity) {
                // Guard keyed by the attacker's own UUID - prevents two retribution-active
                // players from reflecting the same hit back and forth infinitely.
                if (!reflecting.contains(attackerEntity.getUniqueId())) {
                    reflecting.add(attackerEntity.getUniqueId());
                    attackerEntity.setNoDamageTicks(0); // bypass hurt-invulnerability so the reflect isn't swallowed
                    attackerEntity.damage(dealt, victim);
                    reflecting.remove(attackerEntity.getUniqueId());
                }
            }
            return;
        }

        if (!(event.getDamager() instanceof Player attacker2)) return;

        // --- Sword1 Karma passive: only applies if victim carries Judgement ---
        if (SwordUtil.hasSwordInInventory(victim, SwordType.SWORD1)) {
            Map<UUID, Integer> perAttacker = karmaCounters.computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>());
            int count = perAttacker.getOrDefault(attacker2.getUniqueId(), 0) + 1;
            if (count > 5) {
                applyRandom(attacker2, NEGATIVE_POOL);
                applyRandom(victim, POSITIVE_POOL);
                count = 0; // reset after triggering
            }
            perAttacker.put(attacker2.getUniqueId(), count);
        }

        // --- Sword4 Shadows passive: every 7th hit landed WITH sword4 ---
        if (SwordUtil.isSword(attacker2.getInventory().getItemInMainHand(), SwordType.SWORD4)) {
            int hits = shadowsHitCount.getOrDefault(attacker2.getUniqueId(), 0) + 1;
            if (hits >= 7) {
                hits = 0;
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 4, false, true));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 4, false, true));
            }
            shadowsHitCount.put(attacker2.getUniqueId(), hits);
        }
    }

    /** Reset the karma counter for victim->attacker when the VICTIM hits back. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRetaliate(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player retaliator)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        Map<UUID, Integer> retaliatorCounters = karmaCounters.get(retaliator.getUniqueId());
        if (retaliatorCounters != null) retaliatorCounters.remove(target.getUniqueId());
    }

    private void applyRandom(Player target, List<PotionEffectType> pool) {
        PotionEffectType type = pool.get(random.nextInt(pool.size()));
        target.addPotionEffect(new PotionEffect(type, 600, 0, false, true, true));
    }
}
