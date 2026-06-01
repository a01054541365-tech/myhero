package com.jjk.combat;

import com.jjk.api.combat.IDamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public class DamageContext {

    // Mutable output fields — set by DamageCalculator.calculatePure()
    public float rawDamage;
    public float finalDamage;
    // Stage 4b 주구 효과 출력 필드
    public float rangeMultiplier      = 1.0f;  // 스킬 범위 배율 (HitValidator에서 읽음)
    public int   extraBlackFlashBonus = 0;     // 주구 흑섬 확률 추가

    public final ServerPlayerEntity attacker;
    public final LivingEntity target;
    public final IDamageSource sourceType;
    public final float baseDamage;
    // isSoulDirect / defenseMultiplier: Stage 4b 주구 처리로 변경 가능 → non-final
    public boolean isSoulDirect;
    public final boolean bypassRCT;
    public final boolean isBlackFlash;
    public final boolean blackFlashPerfect;
    public final int hitIndex;
    public final String skillName;
    public final float externalBuffMult;
    public float defenseMultiplier;
    public final int keyId;
    public final boolean nKeyApplied;
    // §E 2단계 CE 검증 + §13-1 복사 술식 지원
    public final boolean alreadyConsumedCE;  // true(기본) = process()가 CE/쿨타임 재검증 스킵
    public final int ceCost;                  // 스킬 CE 비용 (0 = 비전투 스킬)
    public final int cooldownTicks;           // 스킬 쿨타임 틱
    public final boolean isDomainSkill;       // 영역 스킬 여부 (복사 불가)

    private DamageContext(Builder b) {
        this.attacker = b.attacker;
        this.target = b.target;
        this.sourceType = b.sourceType;
        this.baseDamage = b.baseDamage;
        this.isSoulDirect = b.isSoulDirect;
        this.bypassRCT = b.bypassRCT;
        this.isBlackFlash = b.isBlackFlash;
        this.blackFlashPerfect = b.blackFlashPerfect;
        this.hitIndex = b.hitIndex;
        this.skillName = b.skillName;
        this.externalBuffMult = b.externalBuffMult;
        this.defenseMultiplier = b.defenseMultiplier;
        this.keyId = b.keyId;
        this.nKeyApplied = b.nKeyApplied;
        this.alreadyConsumedCE = b.alreadyConsumedCE;
        this.ceCost = b.ceCost;
        this.cooldownTicks = b.cooldownTicks;
        this.isDomainSkill = b.isDomainSkill;
    }

    public static Builder builder(ServerPlayerEntity attacker, LivingEntity target,
                                  IDamageSource sourceType, float baseDamage) {
        return new Builder(attacker, target, sourceType, baseDamage);
    }

    public static class Builder {
        final ServerPlayerEntity attacker;
        final LivingEntity target;
        final IDamageSource sourceType;
        final float baseDamage;
        boolean isSoulDirect = false;
        boolean bypassRCT = false;
        boolean isBlackFlash = false;
        boolean blackFlashPerfect = false;
        int hitIndex = 0;
        String skillName = null;
        float externalBuffMult = 1.0f;
        float defenseMultiplier = 1.0f;
        int keyId = 0;
        boolean nKeyApplied = false;
        boolean alreadyConsumedCE = true;  // 기존 호출부 보호: 기본값 true = 2단계 스킵
        int ceCost = 0;
        int cooldownTicks = 0;
        boolean isDomainSkill = false;

        private Builder(ServerPlayerEntity attacker, LivingEntity target,
                        IDamageSource sourceType, float baseDamage) {
            this.attacker = attacker;
            this.target = target;
            this.sourceType = sourceType;
            this.baseDamage = baseDamage;
        }

        public Builder soulDirect() { this.isSoulDirect = true; return this; }
        public Builder bypassRCT() { this.bypassRCT = true; return this; }
        public Builder blackFlash(boolean perfect) { this.isBlackFlash = true; this.blackFlashPerfect = perfect; return this; }
        public Builder blackFlash() { return blackFlash(true); }
        public Builder blackFlashGreat() { return blackFlash(false); }
        public Builder hitIndex(int index) { this.hitIndex = index; return this; }
        public Builder skillName(String name) { this.skillName = name; return this; }
        public Builder externalBuffMult(float mult) { this.externalBuffMult = mult; return this; }
        public Builder defenseMultiplier(float m) { this.defenseMultiplier = m; return this; }
        public Builder keyId(int id) { this.keyId = id; return this; }
        public Builder nKeyApplied(boolean v) { this.nKeyApplied = v; return this; }
        public Builder alreadyConsumedCE(boolean v) { this.alreadyConsumedCE = v; return this; }
        public Builder ceCost(int v) { this.ceCost = v; return this; }
        public Builder cooldownTicks(int v) { this.cooldownTicks = v; return this; }
        public Builder isDomainSkill(boolean v) { this.isDomainSkill = v; return this; }
        public DamageContext build() { return new DamageContext(this); }
    }
}
