package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SukunaSkillSet implements ISkillSet {

    private static final float BD_0 = 55f;
    private static final float BD_1 = 18f;
    private static final float BD_2 = 62f;
    private static final float BD_4 = 40f;

    private static final int CE_0 = 180,  CD_0 = 6,   ANIM_0 = 21;
    private static final int CE_1 = 320,  CD_1 = 14,  ANIM_1 = 22;
    private static final int CE_2 = 250,  CD_2 = 18,  ANIM_2 = 23;
    private static final int CE_3 = 6000, CD_3 = 360, ANIM_3 = 24;
    private static final int CE_4 = 200,  CD_4 = 10,  ANIM_4 = 60;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useDismantle(player);
            case 1 -> useCleave(player);
            case 2 -> useFireArrow(player);
            case 3 -> useMalevolentShrine(player);
            case 4 -> useReverseCursedTerritory(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return data.cooldowns.getOrDefault("skill_" + keyId, 0L) <= tick
                && JJKMod.getCEManager().canAfford(player, getCeCost(keyId));
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_0; case 1 -> CD_1; case 2 -> CD_2;
            case 3 -> CD_3; case 4 -> CD_4; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_0; case 1 -> CE_1; case 2 -> CE_2;
            case 3 -> CE_3; case 4 -> CE_4; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "dismantle";
            case 1 -> "cleave";
            case 2 -> "fire_arrow";
            case 3 -> "malevolent_shrine";
            case 4 -> "reverse_cursed_territory";
            default -> "unknown";
        };
    }

    // F — 해(解): single melee, front 3 blocks
    private SkillResult useDismantle(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_0)) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 3.0, 120f);
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_0)
                    .skillName("dismantle")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_0);
        data.cooldowns.put("skill_0", tick + CD_0);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_0);
        return SkillResult.SUCCESS;
    }

    // G/Shift+F — 팔(捌): 4-direction multi-hit, dampening per hit index
    private SkillResult useCleave(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_1)) return SkillResult.CE_INSUFFICIENT;

        // NORTH(-Z), SOUTH(+Z), EAST(+X), WEST(-X)
        Vec3d[] dirs = {
            new Vec3d(0, 0, -1),
            new Vec3d(0, 0,  1),
            new Vec3d( 1, 0, 0),
            new Vec3d(-1, 0, 0)
        };
        float[] dampening = { 1.0f, 0.85f, 0.70f, 0.70f };

        Set<UUID> hitTargets = new HashSet<>();
        List<LivingEntity> nearby = HitValidator.getNearby(player, 5.0);

        for (int i = 0; i < dirs.length; i++) {
            Vec3d dirVec = dirs[i];
            float damp = dampening[i];
            for (LivingEntity target : nearby) {
                if (hitTargets.contains(target.getUuid())) continue;
                Vec3d toTarget = target.getPos().subtract(player.getPos());
                if (toTarget.lengthSquared() < 0.001) continue;
                double dot = dirVec.dotProduct(toTarget.normalize());
                if (dot < 0.5) continue;
                hitTargets.add(target.getUuid());
                DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_1 * damp)
                        .skillName("cleave")
                        .hitIndex(i)
                        .build();
                JJKMod.getCombatPipeline().process(ctx);
            }
        }

        JJKMod.getCEManager().consume(player, CE_1);
        data.cooldowns.put("skill_1", tick + CD_1);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_1);
        return SkillResult.SUCCESS;
    }

    // R — 화염화살: deferred hit 10 ticks, 15 blocks ahead
    private SkillResult useFireArrow(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_2)) return SkillResult.CE_INSUFFICIENT;

        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d targetPos = player.getPos().add(dir.multiply(15.0));
        JJKMod.getEffectDeferQueue().schedule(BlockPos.ofFloored(targetPos), BD_2, 10, player.getUuid(), tick);

        JJKMod.getCEManager().consume(player, CE_2);
        data.cooldowns.put("skill_2", tick + CD_2);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_2);
        return SkillResult.SUCCESS;
    }

    // H/Shift+R — 복마어주자 (영역 전개), isOpen=true so CE cost is 6000
    private SkillResult useMalevolentShrine(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_3)) return SkillResult.CE_INSUFFICIENT;

        boolean deployed = JJKMod.getDomainManager().deployDomain("sukuna_malevolent_shrine", player);
        if (!deployed) return SkillResult.FAIL;

        JJKMod.getCEManager().consume(player, CE_3);
        data.domainCooldownUntil = tick + CD_3;
        data.cooldowns.put("skill_3", tick + CD_3);
        JJKMod.getPlayerRepository().saveImmediate(data);
        broadcastAnim(player, ANIM_3);
        return SkillResult.SUCCESS;
    }

    // V — 역천지변: front cone ±45°, 5 blocks
    private SkillResult useReverseCursedTerritory(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_4", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_4)) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 5.0, 90f);
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_4)
                    .skillName("reverse_cursed_territory")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_4);
        data.cooldowns.put("skill_4", tick + CD_4);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_4);
        return SkillResult.SUCCESS;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
