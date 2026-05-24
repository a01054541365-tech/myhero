package com.jjk.character;

import com.jjk.api.skill.ISkillSet;
import java.util.HashMap;
import java.util.Map;

public class SkillRegistry {

    private static final Map<String, ISkillSet> REGISTRY = new HashMap<>();

    public static void register(String characterId, ISkillSet skillSet) {
        REGISTRY.put(characterId, skillSet);
    }

    public static ISkillSet get(String characterId) {
        return REGISTRY.get(characterId);
    }
}
