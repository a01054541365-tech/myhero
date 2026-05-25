package com.jjk.domain;

import com.jjk.data.PlayerData;

import java.util.concurrent.ThreadLocalRandom;

public class DomainPriorityCalculator {

    public float calculate(DomainInstance domain, PlayerData ownerData) {
        float priority = 0f;
        // 1. 술식완성도 = 숙련도/10
        priority += ownerData.mastery / 10f;
        // 2. 결계외피견고함 = 현재CE/maxCE
        if (ownerData.ceMax > 0f) {
            priority += ownerData.ceCurrent / ownerData.ceMax;
        }
        // 3. 정신집중도 = 1-(현재HP/maxHP×0.5)
        if (ownerData.hpMax > 0f) {
            priority += 1f - (ownerData.hpCurrent / ownerData.hpMax * 0.5f);
        }
        // 4. 완성형 vs 미완성형 +20%
        if (!domain.isIncomplete) priority *= 1.20f;
        // 5. 결계형 vs 개방형 +10%
        if (!domain.isOpen) priority *= 1.10f;
        return priority;
    }

    public DomainInstance resolveConflict(DomainInstance a, PlayerData dataA,
                                          DomainInstance b, PlayerData dataB) {
        float p1 = calculate(a, dataA);
        float p2 = calculate(b, dataB);
        if (Math.abs(p1 - p2) < 0.001f) {
            return ThreadLocalRandom.current().nextBoolean() ? a : b;
        }
        return p1 >= p2 ? a : b;
    }
}
