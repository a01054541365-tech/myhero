package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.effect.EffectManager;
import com.jjk.effect.EffectType;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class JogoSkillSet implements ISkillSet {

    // 吏쟊OCK: techniques.json ??륂뒄 ???袁⑹벥 癰궰野?疫뀀뜆?
    private static final float BD_F  = 46f;
    private static final float BD_SF = 80f;
    private static final float BD_R  = 120f;
    private static final float BD_SR = 54f;

    private static final int CE_F  = 130,  CD_F  = 6,   ANIM_F  = 30;
    private static final int CE_SF = 280,  CD_SF = 20,  ANIM_SF = 46;
    private static final int CE_R  = 600,  CD_R  = 75,  ANIM_R  = 31;
    private static final int CE_SR = 230,  CD_SR = 16,  ANIM_SR = 47;
    private static final int CE_V  = 2700, CD_V  = 360, ANIM_V  = 32;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useVolcanicBullet(player);
            case 1 -> useCoffinOfIronMountain(player);
            case 2 -> useMeteor(player);
            case 3 -> useRingOfFlames(player);
            case 4 -> useVolcanoDomain(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return true; }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_F; case 1 -> CD_SF; case 2 -> CD_R;
            case 3 -> CD_SR; case 4 -> CD_V; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_F; case 1 -> CE_SF; case 2 -> CE_R;
            case 3 -> CE_SR; case 4 -> CE_V; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "volcanic_bullet";
            case 1 -> "coffin_of_iron_mountain";
            case 2 -> "meteor";
            case 3 -> "ring_of_flames";
            case 4 -> "volcano_domain";
            default -> "unknown";
        };
    }

    // skill method
    private SkillResult useVolcanicBullet(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_jogo_0";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_F)) return SkillResult.CE_INSUFFICIENT;

        float rainMult = player.getServerWorld().isRaining() ? 0.80f : 1.0f;
        List<LivingEntity> targets = HitValidator.getNearby(player, 4.0);
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayerEntity p) EffectManager.apply(p, EffectType.BURN, 80);
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_F)
                    .skillName("volcanic_bullet")
                    .externalBuffMult(rainMult)
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_F);
        CooldownManager.set(data, cdKey, tick, CD_F);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    // skill method
    private SkillResult useCoffinOfIronMountain(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_jogo_1";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SF)) return SkillResult.CE_INSUFFICIENT;

        float rainMult = player.getServerWorld().isRaining() ? 0.80f : 1.0f;
        Vec3d facing = player.getRotationVec(1.0f);
        List<LivingEntity> nearby = HitValidator.getNearby(player, 10.0);
        for (LivingEntity target : nearby) {
            Vec3d toTarget = target.getPos().subtract(player.getPos());
            double forward = toTarget.dotProduct(facing);
            if (forward < 0 || forward > 10.0) continue;
            Vec3d lateral = toTarget.subtract(facing.multiply(forward));
            if (lateral.length() > 2.0) continue;
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SF)
                    .skillName("coffin_of_iron_mountain")
                    .externalBuffMult(rainMult)
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_SF);
        CooldownManager.set(data, cdKey, tick, CD_SF);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    // R ??meteor: 40????뉙???獄쏆꼵瑗?6?됰뗀以??용쵐肉???노퉸 (EffectDeferQueue ????
    private SkillResult useMeteor(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_jogo_2";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_R)) return SkillResult.CE_INSUFFICIENT;

        // compute strike position 10 blocks ahead of player facing
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d strikePos = player.getPos().add(direction.multiply(10.0));
        JJKMod.getEffectDeferQueue().schedule(
                net.minecraft.util.math.BlockPos.ofFloored(strikePos),
                BD_R, 40, player.getUuid(), tick);

        JJKMod.getCEManager().consume(player, CE_R);
        CooldownManager.set(data, cdKey, tick, CD_R);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    // skill method
    private SkillResult useRingOfFlames(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_jogo_3";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SR)) return SkillResult.CE_INSUFFICIENT;

        float rainMult = player.getServerWorld().isRaining() ? 0.80f : 1.0f;
        List<LivingEntity> targets = HitValidator.getNearby(player, 6.0);
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SR)
                    .skillName("ring_of_flames")
                    .externalBuffMult(rainMult)
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_SR);
        CooldownManager.set(data, cdKey, tick, CD_SR);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    // V ??volcano_domain: DomainManager ?袁⑹뿫
    private SkillResult useVolcanoDomain(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_jogo_4";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_V)) return SkillResult.CE_INSUFFICIENT;

        JJKMod.getDomainManager().deployDomain("jogo_domain", player);

        JJKMod.getCEManager().consume(player, CE_V);
        CooldownManager.set(data, cdKey, tick, CD_V);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
