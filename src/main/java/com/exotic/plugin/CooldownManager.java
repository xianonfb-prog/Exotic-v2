package com.exotic.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    // playerUUID -> swordId -> epoch millis when cooldown ends
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public static final Map<String, Long> ABILITY_COOLDOWN_MS = Map.of(
            "sword1", 5 * 60 * 1000L,
            "sword2", 4 * 60 * 1000L,
            "sword3", 5 * 60 * 1000L,
            "sword4", 4 * 60 * 1000L,
            "sword5", 5 * 60 * 1000L,
            "tome1", 1 * 60 * 1000L,
            "sword4_totem", 20 * 60 * 1000L
    );

    public boolean isOnCooldown(UUID player, String swordId) {
        return remainingMs(player, swordId) > 0;
    }

    public long remainingMs(UUID player, String swordId) {
        Long until = cooldowns.getOrDefault(player, Map.of()).get(swordId);
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }

    public void trigger(UUID player, String swordId) {
        long duration = ABILITY_COOLDOWN_MS.getOrDefault(swordId, 60_000L);
        cooldowns.computeIfAbsent(player, k -> new HashMap<>())
                .put(swordId, System.currentTimeMillis() + duration);
    }

    public void clear(UUID player, String swordId) {
        Map<String, Long> map = cooldowns.get(player);
        if (map != null) map.remove(swordId);
    }
}
