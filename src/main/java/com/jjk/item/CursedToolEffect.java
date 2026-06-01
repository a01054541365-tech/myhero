package com.jjk.item;

/**
 * 주구 장착 효과 데이터.
 * attackBonus: 공격력 배율 증가 (0.08 = +8%)
 * ceReduction: CE 소모 감소 배율 (0.08 = -8%)
 * rangeBonus: 스킬 범위 배율 증가
 * defPenetration: 방어 관통 (defenseMultiplier 감소분)
 * blackFlashBonus: 흑섬 확률 추가 (%)
 * sealCooldownTicks: 술식 무효화 쿨타임 (0 = 미지원)
 * forceSoulDirect: true 시 모든 스킬에 isSoulDirect 강제
 * requiredGrade: 등급 조건 (int, GradeManager.Grade.rank 기준)
 */
public record CursedToolEffect(
    float   attackBonus,
    float   ceReduction,
    float   rangeBonus,
    float   defPenetration,
    int     blackFlashBonus,
    int     sealCooldownTicks,
    boolean forceSoulDirect,
    int     requiredGrade
) {
    public static final CursedToolEffect NONE = new CursedToolEffect(
        0f, 0f, 0f, 0f, 0, 0, false, 0);
}
