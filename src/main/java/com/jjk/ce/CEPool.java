package com.jjk.ce;

import java.util.HashMap;
import java.util.Map;

public class CEPool {

    private final Map<String, CERegenRule> rules = new HashMap<>();

    public void put(String characterId, CERegenRule rule) {
        rules.put(characterId, rule);
    }

    public CERegenRule get(String characterId) {
        return rules.get(characterId);
    }
}
