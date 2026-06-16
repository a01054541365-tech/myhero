package com.jjk.domain;

public class DomainDefinition {
    public String domainId;
    public float ceCost;
    public boolean isOpen;
    public float radius;
    public int cooldownTicks;
    public float wallHp;
    public boolean sureHitActive;
    public boolean autoTargetAll;
    public boolean isIncomplete;
    public float ownerDamageReduction;
    public String blockTheme; // 구체 블록 테마 — SphereBuilder.resolveTheme() 참조
}
