package com.jjk.entity;

import com.jjk.JJKMod;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

public final class CursedSpiritSpawnManager {

    /** NightEventManager가 야간 시 1.5f 로 설정. 기본 1.0f. */
    public static float nightMultiplier = 1.0f;

    private CursedSpiritSpawnManager() {}

    // TickScheduler.register(CursedSpiritSpawnManager::tick, 200) 로 등록
    public static void tick(ServerPlayerEntity player) {
        if (JJKMod.getInstance() == null) return;
        if (!JJKMod.getConfig().cursedSpiritSpawnEnabled) return;

        MinecraftServer server = JJKMod.getServer();
        if (server == null) return;
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        // 전체 주령 수 vs 플레이어 수 × 3
        int playerCount = server.getCurrentPlayerCount();
        int limit = playerCount * 3;
        List<CursedSpiritEntity> allSpirits = globalSpirits(sw);
        int spiritCount = allSpirits.size();

        if (spiritCount >= limit) {
            // 한도 초과: 가장 오래된 주령 1마리 제거
            allSpirits.stream()
                .min(Comparator.comparingInt(e -> e.age))
                .ifPresent(oldest -> oldest.discard());
        }

        // 이 플레이어 근처에 스폰 시도
        trySpawnNear(sw, player, server);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    private static List<CursedSpiritEntity> globalSpirits(ServerWorld world) {
        return world.getEntitiesByClass(CursedSpiritEntity.class,
            new Box(-30000, -64, -30000, 30000, 320, 30000), e -> true);
    }

    private static void trySpawnNear(ServerWorld world, ServerPlayerEntity player,
                                      MinecraftServer server) {
        Random rng = world.getRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = rng.nextBetween(-80, 80);
            int dz = rng.nextBetween(-80, 80);
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);

            // 16~80블록 범위만 허용
            if (dist < 16 || dist > 80) continue;

            int wx = (int) player.getX() + dx;
            int wz = (int) player.getZ() + dz;
            int wy = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz);
            BlockPos pos = new BlockPos(wx, wy, wz);

            if (!isValidSpawnPos(world, pos)) continue;

            long elapsedMinutes = world.getTime() / (20L * 60L);
            CursedSpiritGrade grade = rollGrade(rng, elapsedMinutes);
            spawnSpirit(world, pos, grade);
            return;
        }
    }

    private static boolean isValidSpawnPos(ServerWorld world, BlockPos pos) {
        // 발 아래 블록이 solid
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) return false;
        // 스폰 위치가 공기
        if (!world.getBlockState(pos).isAir()) return false;
        // 활성 영역(Domain) 내부 금지
        if (JJKMod.getDomainManager().isInsideAnyDomain(pos.toCenterPos())) return false;
        return true;
    }

    private static CursedSpiritGrade rollGrade(Random rng, long minutes) {
        float roll = rng.nextFloat();
        if (minutes < 10) {
            return roll < 0.80f ? CursedSpiritGrade.GRADE_4 : CursedSpiritGrade.GRADE_3;
        } else if (minutes < 30) {
            if (roll < 0.50f) return CursedSpiritGrade.GRADE_4;
            if (roll < 0.90f) return CursedSpiritGrade.GRADE_3;
            return CursedSpiritGrade.GRADE_2;
        } else {
            if (roll < 0.30f) return CursedSpiritGrade.GRADE_4;
            if (roll < 0.70f) return CursedSpiritGrade.GRADE_3;
            if (roll < 0.90f) return CursedSpiritGrade.GRADE_2;
            return CursedSpiritGrade.GRADE_1;
        }
    }

    private static void spawnSpirit(ServerWorld world, BlockPos pos, CursedSpiritGrade grade) {
        EntityType<CursedSpiritEntity> type = switch (grade) {
            case GRADE_3 -> CursedSpiritEntityTypes.GRADE_3;
            case GRADE_2 -> CursedSpiritEntityTypes.GRADE_2;
            case GRADE_1 -> CursedSpiritEntityTypes.GRADE_1;
            default      -> CursedSpiritEntityTypes.GRADE_4;
        };
        CursedSpiritEntity spirit = type.create(world);
        if (spirit == null) return;
        spirit.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        world.spawnEntity(spirit);
    }
}
