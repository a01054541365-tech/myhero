package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;

public class CombatPipeline {

    private final DamageCalculator calculator = new DamageCalculator();
    private final TickDamageCap damageCap = new TickDamageCap();

    public void process(DamageContext ctx) {
        // Stage 1: validate context
        // Stage 2: check immunity (infinity, etc.)
        // Stage 3: calculate base damage
        float damage = calculator.calculate(ctx);
        // Stage 4: apply black flash zone bonus
        // Stage 5: apply grade scaling
        // Stage 6: apply RCT
        // Stage 7: apply PvP damage cap
        damage = damageCap.apply(damage, ctx.target);
        // Stage 8: apply damage to target
        // Stage 9: audit log + network sync
        AuditLogger.logEvent("DAMAGE", ctx.attacker.getUuid(),
                String.format("target=%s dmg=%.1f src=%s",
                        ctx.target.getUuid(), damage, ctx.sourceType));
        // TODO: ctx.target.damage(...)
    }
}
