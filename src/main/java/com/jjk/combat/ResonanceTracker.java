package com.jjk.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** CE 공명 시스템: 동일 스킬 4회 연속 피격 시 baseDamage×0.5 추가 데미지. */
public class ResonanceTracker {

    private static final int TRIGGER_COUNT = 4;
    private static final long EXPIRE_MS    = 30_000L; // 30초 = 600틱

    private final Map<UUID, Map<String, Integer>> stacks  = new HashMap<>();
    private final Map<UUID, Map<String, Long>>    expires = new HashMap<>();

    /**
     * 동일 skillId 4회째 피격 시 스택 초기화 후 추가 데미지 반환.
     * 30초 이상 해당 스킬 피격 없으면 스택 초기화.
     *
     * @return 추가 데미지 (0 또는 baseDamage×0.5 의 정수 부분)
     */
    public int checkAndApply(UUID targetId, String skillId, float baseDamage) {
        if (skillId == null || skillId.isEmpty()) return 0;

        long now = System.currentTimeMillis();
        Map<String, Integer> skillStacks = stacks.computeIfAbsent(targetId, k -> new HashMap<>());
        Map<String, Long>    expireMap   = expires.computeIfAbsent(targetId, k -> new HashMap<>());

        Long expiry = expireMap.get(skillId);
        if (expiry != null && now > expiry) {
            skillStacks.put(skillId, 0);
        }

        int count = skillStacks.getOrDefault(skillId, 0) + 1;
        expireMap.put(skillId, now + EXPIRE_MS);

        if (count >= TRIGGER_COUNT) {
            skillStacks.put(skillId, 0);
            return (int)(baseDamage * 0.5f);
        }
        skillStacks.put(skillId, count);
        return 0;
    }
}
