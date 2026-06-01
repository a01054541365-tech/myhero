package com.jjk.api.skill;

public enum SkillResult {
    // 기존 값 (하위 호환)
    SUCCESS,
    FAIL,
    CE_INSUFFICIENT,
    ON_COOLDOWN,
    GRADE_LOCKED,
    SEALED,
    // 추가 값 (STEP 5-3)
    FAIL_CE_INSUFFICIENT,
    FAIL_COOLDOWN,
    FAIL_OUT_OF_RANGE,
    FAIL_SAME_TEAM,
    FAIL_INVALID_CHARACTER,
    FAIL_INFINITY,
    FAIL_SKILL_SEALED,
    FAIL_NO_TARGET,
    FAIL_CONDITION,
    NOT_IMPLEMENTED
}
