package com.jjk.entity.cursed;

public enum CursedSpiritGrade {
    GRADE_4_BELOW(  5f, 0.00f),
    GRADE_4      ( 40f, 1.00f),
    GRADE_3      ( 60f, 1.15f),
    GRADE_2      ( 90f, 1.30f),
    GRADE_1      (130f, 1.50f),
    SEMI_SPECIAL (200f, 1.65f),
    SPECIAL_GRADE(350f, 1.80f);

    private final float hp;
    private final float dmg;

    CursedSpiritGrade(float hp, float dmg) {
        this.hp  = hp;
        this.dmg = dmg;
    }

    public float hpMultiplier()     { return hp; }
    public float damageMultiplier() { return dmg; }
}
