package com.jjk.domain;

import com.jjk.data.PlayerData;


public class DomainPriorityCalculator {

    /**
     * §8-2 공식: priority = (grade×0.30) + (ceInvested×0.25) + (mastery×0.30) + (wallHpRatio×0.15)
     * isOpen=true 이면 wallHpRatio 가중치 = 0
     * gradeValue: 4급=1, 3급=2, 2급=3, 1급=4, 준특급=5, 특급=6 (정규화: /6)
     * ceInvested: ceCurrent/ceMax (0~1)
     * mastery: mastery/100 (0~1)
     * wallHpRatio: wallHp/1500 (개방형=0)
     */
    public static float calculate(PlayerData owner, DomainInstance domain) {
        float gradeNorm  = gradeToValue(owner.grade) / 6f;
        float ceInvested = owner.ceMax > 0 ? owner.ceCurrent / owner.ceMax : 0f;
        float masteryNorm = owner.mastery / 100f;
        float wallHpRatio = domain.isOpen ? 0f : (domain.wallHp / 1500f);

        return (gradeNorm * 0.30f) + (ceInvested * 0.25f) + (masteryNorm * 0.30f) + (wallHpRatio * 0.15f);
    }

    private static int gradeToValue(String grade) {
        if (grade == null) return 1;
        return switch (grade) {
            case "4급", "grade_4"       -> 1;
            case "3급", "grade_3"       -> 2;
            case "2급", "grade_2"       -> 3;
            case "1급", "grade_1"       -> 4;
            case "준특급", "semi_grade_1" -> 5;
            case "특급", "special_grade" -> 6;
            default -> 1;
        };
    }

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
        float p1 = calculate(dataA, a);
        float p2 = calculate(dataB, b);
        if (Math.abs(p1 - p2) < 0.001f) {
            return a.ownerUuid.hashCode() >= b.ownerUuid.hashCode() ? a : b;
        }
        return p1 >= p2 ? a : b;
    }
}
