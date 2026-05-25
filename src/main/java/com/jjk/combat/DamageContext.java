package com.jjk.combat;

import com.jjk.api.combat.IDamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public class DamageContext {

    public final ServerPlayerEntity attacker;
    public final LivingEntity target;
    public final IDamageSource sourceType;
    public final float baseDamage;
    public final boolean isSoulDirect;
    public final boolean bypassRCT;
    public final boolean isBlackFlash;
    public final boolean blackFlashPerfect;
    public final int hitIndex;
    public final String skillName;
    public final float externalBuffMult;
    public final int keyId;
    public final boolean nKeyApplied;

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
        this.keyId = b.keyId;
        this.nKeyApplied = b.nKeyApplied;
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
        int keyId = 0;
        boolean nKeyApplied = false;

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
        public Builder keyId(int id) { this.keyId = id; return this; }
        public Builder nKeyApplied(boolean v) { this.nKeyApplied = v; return this; }
        public DamageContext build() { return new DamageContext(this); }
    }
}
