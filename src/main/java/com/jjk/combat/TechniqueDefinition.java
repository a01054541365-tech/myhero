package com.jjk.combat;

public class TechniqueDefinition {
    public String character    = "";
    public int    keyId        = -1;
    public String skillName    = "";
    public float  baseDamage   = 0f;
    public float  ceCost       = 0f;
    public long   cooldownTicks = 0L;
    public int    animId       = -1;
    public String unlockGrade  = "";
    public boolean notImplemented = false;
    public boolean isContinuous   = false;  // 드레인형 토글 여부
    public float  cePerTick      = 0f;     // isContinuous=true일 때 틱당 CE 소모
    public float  burstMultiplier = 1.0f;  // burstActive 조건부 데미지 배율
    // 확장 스킬 전용 필드 (extendedSkills 배열에서 파싱)
    public boolean isSoulDirect             = false;
    public float   soulPenetration          = 0f;
    public float   blackFlashComboMultiplier= 1.0f;
    public int     multiHitCount            = 1;
    public float[] multiHitDecay            = null; // null = 단일 히트
}
