package com.jjk.combat;

import com.jjk.api.combat.IDamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public class DamageContext {

    public final ServerPlayerEntity attacker;
    public final ServerPlayerEntity target;
    public final IDamageSource sourceType;
    public final float baseDamage;
    public final boolean isSoulDirect;
    public final boolean bypassRCT;
    public final boolean isBlackFlash;
    public final String skillName;
    public final float externalBuffMult;

    private DamageContext(Builder b) {
        this.attacker = b.attacker;
        this.target = b.target;
        this.sourceType = b.sourceType;
        this.baseDamage = b.baseDamage;
        this.isSoulDirect = b.isSoulDirect;
        this.bypassRCT = b.bypassRCT;
        this.isBlackFlash = b.isBlackFlash;
        this.skillName = b.skillName;
        this.externalBuffMult = b.externalBuffMult;
    }

    public static Builder builder(ServerPlayerEntity attacker, ServerPlayerEntity target,
                                  IDamageSource sourceType, float baseDamage) {
        return new Builder(attacker, target, sourceType, baseDamage);
    }

    public static class Builder {
        final ServerPlayerEntity attacker;
        final ServerPlayerEntity target;
        final IDamageSource sourceType;
        final float baseDamage;
        boolean isSoulDirect = false;
        boolean bypassRCT = false;
        boolean isBlackFlash = false;
        String skillName = null;
        float externalBuffMult = 1.0f;

        private Builder(ServerPlayerEntity attacker, ServerPlayerEntity target,
                        IDamageSource sourceType, float baseDamage) {
            this.attacker = attacker;
            this.target = target;
            this.sourceType = sourceType;
            this.baseDamage = baseDamage;
        }

        public Builder soulDirect() { this.isSoulDirect = true; return this; }
        public Builder bypassRCT() { this.bypassRCT = true; return this; }
        public Builder blackFlash() { this.isBlackFlash = true; return this; }
        public Builder skillName(String name) { this.skillName = name; return this; }
        public Builder externalBuffMult(float mult) { this.externalBuffMult = mult; return this; }
        public DamageContext build() { return new DamageContext(this); }
    }
}
