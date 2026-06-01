package com.jjk.entity;

public enum CursedSpiritGrade {
    GRADE_4("4급",  15f,  4f, 0.25f, 12,  10, AiTier.BASIC),
    GRADE_3("3급",  30f,  9f, 0.28f, 16,  25, AiTier.RANGED),
    GRADE_2("2급",  55f, 17f, 0.30f, 20,  60, AiTier.EVASIVE),
    GRADE_1("1급",  90f, 28f, 0.32f, 24, 150, AiTier.SKILLED),
    SPECIAL("특급", 200f, 48f, 0.35f, 30, 400, AiTier.BOSS);

    public enum AiTier { BASIC, RANGED, EVASIVE, SKILLED, BOSS }

    public final String  label;
    public final float   maxHp;
    public final float   attackDamage;
    public final float   movementSpeed;
    public final int     detectionRange;
    public final int     xpDrop;
    public final AiTier  aiTier;

    CursedSpiritGrade(String label, float maxHp, float attackDamage,
                      float movementSpeed, int detectionRange, int xpDrop,
                      AiTier aiTier) {
        this.label          = label;
        this.maxHp          = maxHp;
        this.attackDamage   = attackDamage;
        this.movementSpeed  = movementSpeed;
        this.detectionRange = detectionRange;
        this.xpDrop         = xpDrop;
        this.aiTier         = aiTier;
    }
}
