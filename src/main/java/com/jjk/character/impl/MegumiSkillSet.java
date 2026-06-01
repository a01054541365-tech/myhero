package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.data.PlayerData;
import com.jjk.entity.ShikigamiEntity;
import com.jjk.entity.ShikigamiEntityTypes;
import net.minecraft.server.world.ServerWorld;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.UUID;

public class MegumiSkillSet implements ISkillSet {

    private static final String SHIKIGAMI_NUE        = "nue";
    private static final String SHIKIGAMI_WHITE_DOG  = "white_dog";
    private static final String SHIKIGAMI_RABBIT_MASK = "rabbit_mask";

    // key 0: nue, 1: divine_dog, 2: mahoraga_adaptation, 3: chimera_shadow_garden, 4: shadow_move
    // §6-4 메구미 기준
    private static final int CE_0 = 110,  CD_0 = 8;    // 200→110, 20→8 (§6-4 메구미 기준)
    private static final int CE_1 = 90,   CD_1 = 6;    // 180→90, 16→6 (§6-4 메구미 기준)
    private static final int CE_2 = 900,  CD_2 = 180;  // 0→900, 300→180 (§6-4 메구미 기준)
    private static final int CE_3 = 2600, CD_3 = 360;  // 3000→2600, 600→360 (§6-4 메구미 기준)
    private static final int CE_4 = 140,  CD_4 = 12;   // 100→140, 8→12 (§6-4 메구미 기준)

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useNue(player);
            case 1 -> useDivineDog(player);
            case 2 -> useMaharagaAdaptation(player);
            case 3 -> useChimeraShadowGarden(player);
            case 4 -> useShadowMove(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return CooldownManager.isReady(data, cdKey(keyId), tick)
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
            case 0 -> "nue"; case 1 -> "divine_dog"; case 2 -> "mahoraga_adaptation";
            case 3 -> "chimera_shadow_garden"; case 4 -> "shadow_move"; default -> "unknown";
        };
    }

    public void onWhiteDogDeath(UUID ownerUuid) {
        PlayerData data = JJKMod.getPlayerRepository().load(ownerUuid);
        data.shikigamiDmgBoost = 1.5f;
        JJKMod.getPlayerRepository().saveImmediate(data);
    }

    public float getShikigamiDmgBoost(UUID ownerUuid) {
        return JJKMod.getPlayerRepository().load(ownerUuid).shikigamiDmgBoost;
    }

    private static String cdKey(int keyId) { return "cd_megumi_" + keyId; }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        AnimationTriggerS2CPacket pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    private LivingEntity findNearestEnemy(ServerPlayerEntity owner, ShikigamiEntity shikigami, double radius) {
        Box box = shikigami.getBoundingBox().expand(radius);
        return owner.getServerWorld().getEntitiesByClass(LivingEntity.class, box, e -> {
            if (!e.isAlive() || e.getUuid().equals(owner.getUuid())) return false;
            if (e instanceof ShikigamiEntity se && owner.getUuid().equals(se.getOwnerUuid())) return false;
            if (e instanceof ServerPlayerEntity targetPlayer) {
                return JJKMod.getTeamManager().isEnemy(owner, targetPlayer);
            }
            return true;
        }).stream()
          .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(shikigami)))
          .orElse(null);
    }

    private SkillResult useNue(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.deadShikigamiIds.contains(SHIKIGAMI_NUE)) return SkillResult.FAIL;
        if (countActiveShikigami(player) >= 3) return SkillResult.FAIL;
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(0), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_0)) return SkillResult.CE_INSUFFICIENT;

        ShikigamiEntity entity = new ShikigamiEntity(
                ShikigamiEntityTypes.NUE,
                player.getServerWorld(),
                player.getUuid(),
                SHIKIGAMI_NUE
        );
        entity.refreshPositionAndAngles(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), 0f);
        player.getServerWorld().spawnEntity(entity);

        LivingEntity target = findNearestEnemy(player, entity, 10.0);
        if (target != null) entity.setTarget(target);

        JJKMod.getCEManager().consume(player, CE_0);
        CooldownManager.set(data, cdKey(0), tick, CD_0);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, 16);
        return SkillResult.SUCCESS;
    }

    private SkillResult useDivineDog(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.deadShikigamiIds.contains(SHIKIGAMI_WHITE_DOG)) return SkillResult.FAIL;
        if (countActiveShikigami(player) >= 3) return SkillResult.FAIL;
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(1), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_1)) return SkillResult.CE_INSUFFICIENT;

        ShikigamiEntity entity = new ShikigamiEntity(
                ShikigamiEntityTypes.WHITE_DOG,
                player.getServerWorld(),
                player.getUuid(),
                SHIKIGAMI_WHITE_DOG
        );
        entity.refreshPositionAndAngles(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), 0f);
        player.getServerWorld().spawnEntity(entity);

        LivingEntity target = findNearestEnemy(player, entity, 10.0);
        if (target != null) entity.setTarget(target);

        JJKMod.getCEManager().consume(player, CE_1);
        CooldownManager.set(data, cdKey(1), tick, CD_1);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, 17);
        return SkillResult.SUCCESS;
    }

    private int countActiveShikigami(ServerPlayerEntity player) {
        UUID ownerUuid = player.getUuid();
        Box box = player.getBoundingBox().expand(64);
        return player.getServerWorld()
            .getEntitiesByClass(ShikigamiEntity.class, box,
                e -> ownerUuid.equals(e.getOwnerUuid()))
            .size();
    }

    private SkillResult useMaharagaAdaptation(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(2), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_2)) return SkillResult.CE_INSUFFICIENT;

        data.cooldowns.put("mahoraga_adaptation_start", tick);
        data.cooldowns.put("mahoraga_hit_count", 0L);
        data.cooldowns.put("mahoraga_active", 1L);
        data.cooldowns.put("mahoraga_start_tick", tick);

        // 마허라가 엔티티 소환
        ShikigamiEntity mahoraga = new ShikigamiEntity(
                ShikigamiEntityTypes.MAHORAGA,
                player.getServerWorld(),
                player.getUuid(),
                "mahoraga"
        );
        mahoraga.refreshPositionAndAngles(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), 0f);
        player.getServerWorld().spawnEntity(mahoraga);

        CooldownManager.set(data, cdKey(2), tick, CD_2);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    /** TickScheduler period=20 — 마허라가 조복 실패 조건 체크. */
    public static void tickMaharagaFailCheck(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld world)) return;
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!"megumi".equals(data.characterId)) return;
        if (data.cooldowns.getOrDefault("mahoraga_active", 0L) != 1L) return;

        long currentTick = world.getTime();
        long startTick = data.cooldowns.getOrDefault("mahoraga_start_tick", 0L);
        boolean timeout = (currentTick - startTick) >= JJKMod.getConfig().getMaharagaTimeoutTicks();
        boolean ceExhausted = data.ceCurrent <= 0f;
        boolean hpLow = data.hpCurrent <= data.hpMax * 0.50f;

        if (timeout || ceExhausted || hpLow) {
            data.ceCurrent = 0f;
            data.cooldowns.put("skill_seal", currentTick + JJKMod.getConfig().sealDurationTicks);
            data.cooldowns.remove("mahoraga_active");
            data.cooldowns.remove("mahoraga_hit_count");
            data.cooldowns.remove("mahoraga_start_tick");
            data.cooldowns.remove("mahoraga_adaptation_start");
            despawnMahoraga(player, world);
            JJKMod.getPlayerRepository().saveImmediate(data);
        }
    }

    private static void despawnMahoraga(ServerPlayerEntity player, ServerWorld world) {
        UUID ownerUuid = player.getUuid();
        Box box = player.getBoundingBox().expand(64);
        world.getEntitiesByClass(ShikigamiEntity.class, box,
                e -> ownerUuid.equals(e.getOwnerUuid()) && "mahoraga".equals(e.getShikigamiId()))
             .forEach(e -> e.discard());
    }

    private SkillResult useChimeraShadowGarden(ServerPlayerEntity player) {
        boolean deployed = JJKMod.getDomainManager()
                .deployDomain("megumi_chimera_shadow", player);
        if (!deployed) return SkillResult.FAIL;
        return SkillResult.SUCCESS;
    }

    private SkillResult useShadowMove(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_4)) return SkillResult.CE_INSUFFICIENT;

        Vec3d dest = player.getPos().add(player.getRotationVec(1.0f).multiply(10.0));
        player.teleport(player.getServerWorld(), dest.x, dest.y, dest.z,
                player.getYaw(), player.getPitch());

        JJKMod.getCEManager().consume(player, CE_4);
        CooldownManager.set(data, cdKey(4), tick, CD_4);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    // ─── 순수 PlayerData 경로 (ISkillSet §default 오버라이드) ──────────────────
    // onF: 죽은 식신("nue") 재소환 금지 체크 — MC 없이 순수 PlayerData로 처리 가능
    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.deadShikigamiIds.contains(SHIKIGAMI_NUE)) return SkillResult.FAIL_CONDITION;
        return player == null ? SkillResult.SUCCESS : useNue(player);
    }
    @Override public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useDivineDog(player); }
    @Override public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useMaharagaAdaptation(player); }
    @Override public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useChimeraShadowGarden(player); }
    @Override public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useShadowMove(player); }
}
