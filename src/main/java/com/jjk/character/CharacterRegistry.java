package com.jjk.character;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CharacterRegistry {

    private static final Map<String, CharacterMeta>  REGISTRY = new HashMap<>();
    private static final Map<String, CharacterStats> STATS    = new HashMap<>();

    public static void register(String id, CharacterMeta meta) {
        REGISTRY.put(id, meta);
    }

    public static CharacterMeta get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean isRegistered(String id) {
        return REGISTRY.containsKey(id);
    }

    public static Set<String> ids() {
        return REGISTRY.keySet();
    }

    public static Set<String> getAllIds() {
        return REGISTRY.keySet();
    }

    public static void registerStats(String id, CharacterStats stats) {
        STATS.put(id, stats);
    }

    public static CharacterStats getStats(String id) {
        return STATS.get(id);
    }

    public record CharacterMeta(String id, String displayName, String defaultGrade, float ceMax) {}

    public record CharacterStats(
            float maxCe,
            float regenOut,
            float regenIn,
            int attack,
            int defense,
            int hp
    ) {}
}
