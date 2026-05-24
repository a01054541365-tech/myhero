package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

// 吏쟊OCK: ?臾믨퐶 base ??2.5 獄쏄퀣??癰귣똾??????륂뒄 癰궰野???DamageCalculatorTest????ｍ뜞 ??륁젟
public class DamageCalculator {

    private static final float BLACK_FLASH_MULTIPLIER = 2.5f;

    public float calculate(DamageContext ctx) {
        float damage = ctx.baseDamage;

        if (ctx.isBlackFlash) {
            damage *= BLACK_FLASH_MULTIPLIER;
        }

        boolean modEnabled = JJKMod.getInstance() != null;

        // Jogo passive: fire skill immunity (target=jogo) and rain debuff (attacker=jogo)
        if (modEnabled && ctx.skillName != null && isFireSkill(ctx.skillName)) {
            if (ctx.target != null && "jogo".equals(getCharId(ctx.target))) {
                return 0f;
            }
            if (ctx.attacker != null && "jogo".equals(getCharId(ctx.attacker))
                    && ctx.attacker.getServerWorld().isRaining()) {
                damage *= 0.80f;
            }
        }

        // Soul resist (Mahito R skill) + Mahito soul-direct vulnerability (passive)
        if (modEnabled && ctx.isSoulDirect && ctx.target != null) {
            PlayerData targetData = JJKMod.getPlayerRepository().load(ctx.target.getUuid());
            long now = ctx.target.getWorld().getTime();
            Long soulResist = targetData.cooldowns.get("status_soul_resist");
            if (soulResist != null && now < soulResist) {
                damage *= 0.5f;
            }
            if ("mahito".equals(targetData.characterId)) {
                damage *= 1.2f;
            }
        }

        // Mahito passive: +20% damage from gojo/itadori techniques
        if (modEnabled && ctx.attacker != null && ctx.target != null
                && "mahito".equals(getCharId(ctx.target))) {
            String attackerChar = getCharId(ctx.attacker);
            if ("gojo".equals(attackerChar) || "itadori".equals(attackerChar)) {
                damage *= 1.2f;
            }
        }

        // External buff multiplier (Nanami overtime_work, etc.)
        damage *= ctx.externalBuffMult;

        // TODO: apply grade scaling (吏?)
        // TODO: apply RCT reduction if !ctx.bypassRCT

        return damage;
    }

    private static boolean isFireSkill(String skillName) {
        return skillName.contains("fire") || skillName.contains("burn")
                || skillName.contains("flame") || skillName.contains("volcanic")
                || skillName.contains("coffin") || skillName.contains("ring_of")
                || skillName.contains("volcano") || skillName.contains("meteor");
    }

    private static String getCharId(ServerPlayerEntity player) {
        if (player == null) return null;
        return JJKMod.getPlayerRepository().load(player.getUuid()).characterId;
    }
}
