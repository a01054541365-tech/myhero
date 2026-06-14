package com.jjk.world.spawn;

import com.jjk.JJKMod;
import com.jjk.entity.npc.SorcererNPCEntity;
import com.jjk.entity.npc.SorcererNPCEntity.SorcererNPCGrade;
import com.jjk.entity.JJKEntities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

// 주술사 NPC 야생 스폰. 200틱 주기로 TickScheduler에 등록.
public final class SorcererNPCSpawnManager {

    private static final int   MAX_TOTAL       = 20;
    private static final int   MIN_RANGE       = 80;
    private static final int   MAX_RANGE       = 150;
    private static final int   SPAWN_ATTEMPTS  = 8;
    private static final float TPS_LOW_THRESHOLD = 62.5f; // ms/tick → TPS < 16

    // 등급 확률: 4급=50%, 3급=30%, 2급=15%, 1급=5%
    private static final float[] GRADE_WEIGHTS = { 0.50f, 0.30f, 0.15f, 0.05f };

    private SorcererNPCSpawnManager() {}

    public static void tick(MinecraftServer server) {
        if (JJKMod.getInstance() == null) return;
        if (!JJKMod.getConfig().cursedSpiritSpawnEnabled) return;
        if (server.getAverageTickTime() > TPS_LOW_THRESHOLD) return;

        ServerWorld world = server.getOverworld();
        Box global = new Box(-30000, -64, -30000, 30000, 320, 30000);
        int total = world.getEntitiesByClass(SorcererNPCEntity.class, global, e -> true).size();
        if (total >= MAX_TOTAL) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            trySpawnNear(world, player);
        }
    }

    private static void trySpawnNear(ServerWorld world, ServerPlayerEntity player) {
        net.minecraft.util.math.random.Random rng = world.getRandom();
        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            int dx = rng.nextBetween(-MAX_RANGE, MAX_RANGE);
            int dz = rng.nextBetween(-MAX_RANGE, MAX_RANGE);
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (dist < MIN_RANGE || dist > MAX_RANGE) continue;

            int wx = (int) player.getX() + dx;
            int wz = (int) player.getZ() + dz;
            BlockPos pos = new BlockPos(wx, world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz), wz);

            if (!isValidSpawnPos(world, pos)) continue;

            spawnNpc(world, pos, rollGrade(rng));
            return;
        }
    }

    private static boolean isValidSpawnPos(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) return false;
        if (!world.getBlockState(pos).isAir()) return false;
        if (JJKMod.getDomainManager().isInsideAnyDomain(pos.toCenterPos())) return false;
        return true;
    }

    private static SorcererNPCGrade rollGrade(net.minecraft.util.math.random.Random rng) {
        float roll = rng.nextFloat();
        float cumulative = 0f;
        SorcererNPCGrade[] grades = SorcererNPCGrade.values();
        for (int i = 0; i < grades.length; i++) {
            cumulative += GRADE_WEIGHTS[i];
            if (roll < cumulative) return grades[i];
        }
        return SorcererNPCGrade.GRADE_4;
    }

    private static void spawnNpc(ServerWorld world, BlockPos pos, SorcererNPCGrade grade) {
        net.minecraft.entity.EntityType<SorcererNPCEntity> type = switch (grade) {
            case GRADE_4 -> JJKEntities.SORCERER_NPC_4;
            case GRADE_3 -> JJKEntities.SORCERER_NPC_3;
            case GRADE_2 -> JJKEntities.SORCERER_NPC_2;
            case GRADE_1 -> JJKEntities.SORCERER_NPC_1;
        };
        SorcererNPCEntity npc = type.create(world);
        if (npc == null) return;
        npc.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        world.spawnEntity(npc);
    }
}
