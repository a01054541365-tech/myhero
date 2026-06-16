package com.jjk.world;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.world.structure.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 서버 시작 시 WorldSeed 기반 결정론적 위치 계산으로 건물을 다중 생성.
 * 타입별로 BuildingRegistry(buildings.json)의 기존 개수와 목표 개수를 비교해 부족분만 생성
 * (기존 월드에 새 건물 타입이 추가돼도 자동 보충됨).
 * 항상 BuildingNpcSpawner.recheckAndRespawn() 호출 (재시작 후 NPC 복구).
 */
public final class WorldGenerationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");

    private WorldGenerationManager() {}

    public static void onServerStarted(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return;
        JjkConfig cfg = JJKMod.getConfig();
        if (!cfg.buildingSpawnEnabled) return;

        BuildingRegistry registry = BuildingRegistry.load();

        int added = generateMissing(world, cfg, registry);
        if (added > 0) {
            registry.save();
            LOGGER.info("[JJK] WorldGenerationManager: 건물 {}개 신규 생성 완료", added);
        } else {
            LOGGER.info("[JJK] WorldGenerationManager: 모든 건물 타입 충족 — 생성 건너뜀");
        }

        BuildingNpcSpawner.recheckAndRespawn(world, registry);
    }

    // ── 타입별 부족분 생성 ───────────────────────────────────────────────────

    private static int generateMissing(ServerWorld world, JjkConfig cfg, BuildingRegistry registry) {
        long seed = world.getSeed();
        int minDist = cfg.buildingMinDistanceBlocks;
        // 기존 건물도 최소 거리 검사 대상에 포함
        List<BuildingInstance> placed = new ArrayList<>(registry.getAll());
        int added = 0;

        added += place(world, seed, "jujutsu_high_tokyo",  cfg.jujutsuHighTokyoCount,    minDist, placed, registry,
            JujutsuHighTokyoStructure::build);
        added += place(world, seed, "jujutsu_high_kyoto",  1,                             minDist, placed, registry,
            JujutsuHighKyotoStructure::build);
        added += place(world, seed, "detention_facility",  cfg.detentionFacilityCount,    minDist, placed, registry,
            DetentionFacilityStructure::build);
        added += place(world, seed, "shibuya_underground", cfg.shibuyaUndergroundCount,   minDist, placed, registry,
            ShibuyaUndergroundStructure::build);
        added += place(world, seed, "nanami_office",       cfg.nanamiOfficeCount,         minDist, placed, registry,
            NanamiOfficeStructure::build);
        added += place(world, seed, "shibuya_city",        cfg.shibuyaCityCount,          minDist, placed, registry,
            ShibuyaCityStructure::build);
        added += place(world, seed, "shibuya_station",     1,                             minDist, placed, registry,
            BuildingGenerator::buildShibuyaStation);
        added += place(world, seed, "jogo_volcano",        1,                             minDist, placed, registry,
            BuildingGenerator::buildJogoVolcano);
        added += place(world, seed, "training_dojo",       1,                             minDist, placed, registry,
            BuildingGenerator::buildTrainingDojo);
        added += place(world, seed, "black_market",        1,                             minDist, placed, registry,
            BuildingGenerator::buildBlackMarket);
        return added;
    }

    @FunctionalInterface
    private interface StructureBuilder {
        List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin);
    }

    /** 기존 개수를 제외한 부족분만 생성. 생성한 개수를 반환. */
    private static int place(ServerWorld world, long seed, String type, int count,
                               int minDist, List<BuildingInstance> placed,
                               BuildingRegistry registry, StructureBuilder builder) {
        int existing = 0;
        for (BuildingInstance b : placed) {
            if (type.equals(b.type)) existing++;
        }
        int addedCount = 0;
        for (int i = existing; i < count; i++) {
            BlockPos origin = findPosition(world, seed, type, i, placed, minDist);
            if (origin == null) {
                LOGGER.warn("[JJK] WorldGen: {} 인스턴스 {} 위치 탐색 실패 (건너뜀)", type, i);
                continue;
            }
            List<NpcSpawnPoint> points = builder.build(world, origin);
            BuildingInstance inst = new BuildingInstance(type, origin.getX(), origin.getY(), origin.getZ(), points);
            placed.add(inst);
            registry.add(inst);
            addedCount++;
            LOGGER.info("[JJK] WorldGen: {} #{} @ ({},{},{})",
                type, i, origin.getX(), origin.getY(), origin.getZ());
        }
        return addedCount;
    }

    // ── 위치 결정 (결정론적 해시 기반) ──────────────────────────────────────

    private static BlockPos findPosition(ServerWorld world, long seed, String type,
                                          int instanceIndex, List<BuildingInstance> placed,
                                          int minDist) {
        for (int attempt = 0; attempt < 50; attempt++) {
            long hash = (seed * 6364136223846793005L + 1442695040888963407L)
                      ^ ((long) type.hashCode() * 2654435761L)
                      ^ ((long) instanceIndex * 1000003L)
                      ^ ((long) attempt * 179426549L);
            Random rng = new Random(hash);
            int x = rng.nextInt(4001) - 2000;
            int z = rng.nextInt(4001) - 2000;

            if (isTooClose(x, z, placed, minDist)) continue;

            int spawnMin = minSpawnDist(type);
            if (spawnMin > 0 && (long) x * x + (long) z * z < (long) spawnMin * spawnMin) continue;

            // 청크 강제 로드 후 지면 높이 조회
            world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
            int y = "shibuya_underground".equals(type)
                ? -20
                : world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            return new BlockPos(x, y, z);
        }
        return null;
    }

    private static boolean isTooClose(int x, int z, List<BuildingInstance> placed, int minDist) {
        long minDistSq = (long) minDist * minDist;
        for (BuildingInstance b : placed) {
            long dx = x - b.x;
            long dz = z - b.z;
            if (dx * dx + dz * dz < minDistSq) return true;
        }
        return false;
    }

    // 건물 타입별 스폰 지점(0,0)으로부터의 최소 거리
    private static int minSpawnDist(String type) {
        return switch (type) {
            case "jujutsu_high_tokyo", "jujutsu_high_kyoto" -> 0;   // 스폰 인근 유지
            case "training_dojo"    -> 150;
            case "nanami_office"    -> 100;
            case "black_market", "shibuya_underground" -> 200;
            case "shibuya_city"     -> 300;
            default                 -> 120;
        };
    }
}
