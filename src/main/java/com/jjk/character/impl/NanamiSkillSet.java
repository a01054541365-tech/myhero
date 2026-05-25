package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class NanamiSkillSet implements ISkillSet {

    // 吏쟊OCK: techniques.json ??륂뒄 ???袁⑹벥 癰궰野?疫뀀뜆?
    private static final float BD_F  = 32f;
    private static final float BD_SF = 45f;

    private static final int CE_F  = 100, CD_F  = 6,  ANIM_F  = 25;
    private static final int CE_SF = 160, CD_SF = 20, ANIM_SF = 26;
    private static final int CE_SR = 220, CD_SR = 25, ANIM_SR = 54;
    private static final int CE_R  = 0,   CD_R  = 0,  ANIM_R  = 27;
    private static final int CE_V  = 70,  CD_V  = 8,  ANIM_V  = 28;

    // §LOCK: technique base damage
    private static final long NIGHT_START = 13000L;

    // ??λ뻻?? ratio_attack ?곕떽? 燁살꼶梨?? ?類ｌぇ 30%

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useRatioAttack(player);
            case 1 -> useOvertimeWork(player);
            case 2 -> useBladeOfSevens(player);
            case 3 -> useDomainDeploy(player);
            case 4 -> useRCT(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return true; }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_F; case 1 -> CD_SF; case 2 -> CD_SR;
            case 3 -> CD_R; case 4 -> CD_V; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_F; case 1 -> CE_SF; case 2 -> CE_SR;
            case 3 -> CE_R; case 4 -> CE_V; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "ratio_attack"; case 1 -> "overtime_work";
            case 2 -> "blade_of_sevens"; case 3 -> "domain_deploy";
            case 4 -> "rct"; default -> "unknown";
        };
    }

    // F ??ratio_attack (7:3 ?브쑵釉?: Y??域뱀눘沅???뚯젎 ?癒?젟 + ??λ뻻???곕떽? 燁살꼶梨?? 30%
    private SkillResult useRatioAttack(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_nanami_0";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_F)) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> targets = HitValidator.getNearby(player, 3.0);
        if (targets.isEmpty()) return SkillResult.FAIL;

        LivingEntity target = targets.get(0);

        // 7:3 ??뚯젎 ?癒?젟: ?????怨룸뼊 30% (Y??域뱀눘沅?
        double targetFeetY = target.getY();
        double targetHeight = target.getHeight();
        double weakZoneBottom = targetFeetY + targetHeight * 0.70;
        double attackY = player.getEyeY() - player.getRotationVec(1.0f).y * 2.0;
        boolean isWeakSpot = attackY >= weakZoneBottom;
        // ??λ뻻?? +30% ?곕떽? ?類ｌぇ嚥?燁살꼶梨??
        float buffMult = getOvertimeMult(data, tick, player.getWorld().getTimeOfDay());

        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_F)
                .externalBuffMult(buffMult * (isWeakSpot ? 1.5f : 1.0f))
                .skillName("ratio_attack")
                .build();
        JJKMod.getCombatPipeline().process(ctx);

        JJKMod.getCEManager().consume(player, CE_F);
        CooldownManager.set(data, cdKey, tick, CD_F);
        applyNightFlag(data, player);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    // SF ??overtime_work: ?⑤벀爰??+25%, 300??筌왖?? ??⑥퍢 ?곕떽? +10% = ??+35%
    private SkillResult useOvertimeWork(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_nanami_1";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SF)) return SkillResult.CE_INSUFFICIENT;

        data.cooldowns.put("buff_overtime", tick + 300);

        JJKMod.getCEManager().consume(player, CE_SF);
        CooldownManager.set(data, cdKey, tick, CD_SF);
        applyNightFlag(data, player);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    // SR ??blade_of_sevens: ?袁④컩 5?됰뗀以?????4?됰뗀以?野껋럡???筌앸맩???癒?젟
    private SkillResult useBladeOfSevens(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_nanami_2";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SR)) return SkillResult.CE_INSUFFICIENT;

        Vec3d facing = player.getRotationVec(1.0f);
        List<LivingEntity> nearby = HitValidator.getNearby(player, 6.0);
        for (LivingEntity target : nearby) {
            Vec3d toTarget = target.getPos().subtract(player.getPos());
            double forward = toTarget.dotProduct(facing);
            if (forward <= 0 || forward > 5.0) continue;
            Vec3d lateral = toTarget.subtract(facing.multiply(forward));
            if (lateral.length() > 2.0) continue; // ??4?됰뗀以?吏?)
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, 60f)
                    .skillName("blade_of_sevens")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_SR);
        CooldownManager.set(data, cdKey, tick, CD_SR);
        applyNightFlag(data, player);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    // R: domain_deploy — CE check + cooldown guard
    private SkillResult useDomainDeploy(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_nanami_3";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_R)) return SkillResult.CE_INSUFFICIENT;

        JJKMod.getDomainManager().deployDomain("nanami_domain", player);

        JJKMod.getCEManager().consume(player, CE_R);
        CooldownManager.set(data, cdKey, tick, CD_R);
        applyNightFlag(data, player);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    // V ??rct: healingActive=true, 10?源낆춳??+2 HP, CE ceCost/10 ???걟
    private SkillResult useRCT(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_nanami_4";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_V)) return SkillResult.CE_INSUFFICIENT;

        JJKMod.getCEManager().consume(player, CE_V);
        data.healingActive = true;
        CooldownManager.set(data, cdKey, tick, CD_V);
        applyNightFlag(data, player);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // RCT ??筌ｌ꼶?? 10?源낆춳??+2 HP, CE -7 ???걟. JJKMod?癒?퐣 period=10 ?源낆쨯.
    public static void tickRCT(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!"nanami".equals(data.characterId) || !data.healingActive) return;

        float cePerInterval = CE_V / 10f; // 70/10 = 7 CE per 10??
        if (!JJKMod.getCEManager().canAfford(player, cePerInterval)) {
            data.healingActive = false;
            JJKMod.getPlayerRepository().save(data);
            return;
        }
        JJKMod.getCEManager().consume(player, cePerInterval);
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(2.0f);
        }
        JJKMod.getPlayerRepository().save(data);
    }

    // overtime_work 甕곌쑵遊?獄쏄퀣???④쑴沅?(DamageCalculator ??????쎄텢????곷퓠??externalBuffMult嚥??袁⑤뼎)
    private float getOvertimeMult(PlayerData data, long currentTick, long timeOfDay) {
        Long overtimeUntil = data.cooldowns.get("buff_overtime");
        if (overtimeUntil == null || currentTick >= overtimeUntil) return 1.0f;
        boolean isNight = timeOfDay >= NIGHT_START;
        return isNight ? 1.35f : 1.25f;
    }

    private static void applyNightFlag(PlayerData data, ServerPlayerEntity player) {
        long timeOfDay = player.getWorld().getTimeOfDay() % 24000;
        data.overtimeWork = timeOfDay >= 13000 && timeOfDay <= 23000;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
