package com.jjk.ce;

public class CERegenRule {

    public final float outOfCombatPerTick;
    public final float inCombatPerTick;
    public final float maxCe;

    public CERegenRule(float outOfCombatPerTick, float inCombatPerTick, float maxCe) {
        this.outOfCombatPerTick = outOfCombatPerTick;
        this.inCombatPerTick = inCombatPerTick;
        this.maxCe = maxCe;
    }
}
