package com.jjk.character;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CharacterRegistry {

    private static final Map<String, CharacterMeta> REGISTRY = new HashMap<>();

    public static void register(String id, CharacterMeta meta) {
        REGISTRY.put(id, meta);
    }

    public static CharacterMeta get(String id) {
        return REGISTRY.get(id);
    }

    public static Set<String> ids() {
        return REGISTRY.keySet();
    }

    public record CharacterMeta(String id, String displayName, String defaultGrade, float ceMax) {}
}
