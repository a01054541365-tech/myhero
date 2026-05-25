package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.domain.DomainInstance;
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Optional;

public class CombatPipeline {

    private final DamageCalculator calculator = new DamageCalculator();
    private final TickDamageCap damageCap = new TickDamageCap();

    public void process(DamageContext ctx) {
        // Stage 1: validate context
        if (ctx.target == null) return;

        // Stage 2: check immunity (infinity)
        if (ctx.target instanceof ServerPlayerEntity targetPlayer) {
            PlayerData targetData2 = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
            if (targetData2.infinityActive) {
                boolean nullified = false;
                // 조건 1: isSoulDirect=true
                if (ctx.isSoulDirect) nullified = true;
                // 조건 2: isBlackFlash=true (흑섬 직격)
                if (ctx.isBlackFlash) nullified = true;
                // 조건 3: nKeyApplied=true (영역전연)
                if (ctx.nKeyApplied) nullified = true;
                // 조건 4: 공격자가 영역 내부에 있음
                if (ctx.attacker != null) {
                    Optional<DomainInstance> domain =
                        JJKMod.getDomainManager()
                            .getDomainAt(ctx.attacker.getBlockPos());
                    if (domain.isPresent()) nullified = true;
                }
                if (!nullified) return;
            }
        }

        // Stage 3: calculate base damage
        float damage = calculator.calculate(ctx);
        if (damage <= 0f) return;

        // Stage 4: zone bonus / penalty  (attacker-only; null-safe)
        if (ctx.attacker != null) {
            PlayerData attackerData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            long currentTick = ctx.attacker.getWorld().getTime();
            if (attackerData.zoneActive && currentTick < attackerData.zoneEndTick) {
                damage *= 1.20f;
            }
            if (currentTick < attackerData.zonePenaltyUntilTick) {
                damage *= 0.5f;
            }

            // Stage 5: grade scaling
            // awakeningActive already applied in DamageCalculator — not repeated here
            // §LOCK clamp ×4.0 already applied in DamageCalculator — not repeated here
            float gradeMultiplier = 1.00f;
            if (attackerData.grade != null) {
                gradeMultiplier = switch (attackerData.grade) {
                    case "grade_4"      -> 1.00f;
                    case "grade_3"      -> 1.15f;
                    case "grade_2"      -> 1.30f;
                    case "grade_1"      -> 1.50f;
                    case "semi_grade_1" -> 1.65f;
                    case "special_grade"-> 1.80f;
                    default             -> 1.00f;
                };
            }
            damage *= gradeMultiplier;
        }

        // Stage 6: RCT reduction
        PlayerData targetData = JJKMod.getPlayerRepository().load(ctx.target.getUuid());
        if (targetData.healingActive && !ctx.bypassRCT) {
            damage *= 0.85f;
        }

        // Stage 6b: falling blossom emotion — 40% damage mitigation
        if (targetData.fallingBlossomActive
                && ctx.target.getWorld().getTime() < targetData.fallingBlossomUntil) {
            damage *= 0.60f;
        }

        // Stage 7: §LOCK PvP cap max_hp×0.40 (player targets only)
        if (ctx.target instanceof ServerPlayerEntity p) {
            damage = damageCap.apply(damage, p);
            if (damage <= 0f) return;
        }

        // Stage 8: apply damage to target
        DamageSource source = ctx.attacker != null
                ? ctx.target.getDamageSources().playerAttack(ctx.attacker)
                : ctx.target.getDamageSources().magic();
        ctx.target.damage(source, damage);

        // Stage 9: audit log + notify attacker
        String attackerStr = ctx.attacker != null ? ctx.attacker.getUuid().toString() : "environment";
        AuditLogger.logEvent("DAMAGE", ctx.target.getUuid(),
                String.format("attacker=%s dmg=%.1f src=%s", attackerStr, damage, ctx.sourceType));
        if (ctx.attacker instanceof ServerPlayerEntity attackerPlayer) {
            String resultStr = ctx.isBlackFlash
                    ? (ctx.blackFlashPerfect ? "BLACK_FLASH_PERFECT" : "BLACK_FLASH_GREAT")
                    : "HIT";
            ServerPlayNetworking.send(attackerPlayer,
                    new SkillResultS2CPacket(ctx.keyId, resultStr, damage));
        }
    }
}
