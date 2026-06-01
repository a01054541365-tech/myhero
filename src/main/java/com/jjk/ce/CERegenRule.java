package com.jjk.ce;

import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;

public class CERegenRule {

    // decisions §2-4: 전투 중 판정 타임아웃
    private static final long COMBAT_TIMEOUT_TICKS = 100L;

    public final float outOfCombatPerTick;
    public final float inCombatPerTick;
    public final float maxCe;

    public CERegenRule(float outOfCombatPerTick, float inCombatPerTick, float maxCe) {
        this.outOfCombatPerTick = outOfCombatPerTick;
        this.inCombatPerTick = inCombatPerTick;
        this.maxCe = maxCe;
    }

    // lastCombatTick 기준 전투 중 판정: currentTick - lastCombatTick <= 100틱
    public float regenPerTick(PlayerData data, long currentTick, JjkConfig config) {
        boolean inCombat = (currentTick - data.lastCombatTick) <= COMBAT_TIMEOUT_TICKS;
        // per-character 재생 수치 우선; fallback = config §LOCK
        if (inCombatPerTick > 0f || outOfCombatPerTick > 0f) {
            return inCombat ? inCombatPerTick : outOfCombatPerTick;
        }
        return inCombat ? config.ceRegenInCombat : config.ceRegenOutOfCombat;
    }
}
