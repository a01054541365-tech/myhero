package com.jjk.ce;

import com.jjk.character.CharacterRegistry;

import java.util.HashMap;
import java.util.Map;

public class CEPool {

    // decisions §2-4: §LOCK 기본값 — 전투 외 1.0/틱, 전투 중 0.2/틱
    private static final CERegenRule DEFAULT_RULE = new CERegenRule(1.0f, 0.2f, 100f);

    private final Map<String, CERegenRule> rules = new HashMap<>();

    public void put(String characterId, CERegenRule rule) {
        rules.put(characterId, rule);
    }

    public CERegenRule get(String characterId) {
        return rules.get(characterId);
    }

    public CERegenRule getRule(String characterId) {
        // techniques.json characterStats에서 per-character 재생 수치 우선 사용
        CharacterRegistry.CharacterStats stats = CharacterRegistry.getStats(characterId);
        if (stats != null) {
            return new CERegenRule(stats.regenOut(), stats.regenIn(), stats.maxCe());
        }
        CERegenRule rule = rules.get(characterId);
        return rule != null ? rule : DEFAULT_RULE;
    }
}
