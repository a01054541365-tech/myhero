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
 * BuildingRegistry(buildings.json)가 비어 있는 경우에만 생성.
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

        if (registry.isEmpty()) {
            generateAll(world, cfg, registry);
            registry.save();
            LOGGER.info("[JJK] WorldGenerationManager: 전체 건물 생성 완료");
        } else {
            LOGGER.info("[JJK] WorldGenerationManager: buildings.json 존재 — 생성 건너뜀");
        }

        BuildingNpcSpawner.recheckAndRespawn(world, registry);
    }

    // ── 전체 생성 ───────────────────────────────────────────────────────────

    private static void generateAll(ServerWorld world, JjkConfig cfg, BuildingRegistry registry) {
        long seed = world.getSeed();
        int minDist = cfg.buildingMinDistanceBlocks;
        List<BuildingInstance> placed = new ArrayList<>();

        place(world, seed, "jujutsu_high_tokyo",  cfg.jujutsuHighTokyoCount,    minDist, placed, registry,
            JujutsuHighTokyoStructure::build);
        place(world, seed, "jujutsu_high_kyoto",  1,                             minDist, placed, registry,
            JujutsuHighKyotoStructure::build);
        place(world, seed, "detention_facility",  cfg.detentionFacilityCount,    minDist, placed, registry,
            DetentionFacilityStructure::build);
        place(world, seed, "shibuya_underground", cfg.shibuyaUndergroundCount,   minDist, placed, registry,
            ShibuyaUndergroundStructure::build);
        place(world, seed, "nanami_office",       cfg.nanamiOfficeCount,         minDist, placed, registry,
            NanamiOfficeStructure::build);
    }

    @FunctionalInterface
    private interface StructureBuilder {
        List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin);
    }

    private static void place(ServerWorld world, long seed, String type, int count,
                               int minDist, List<BuildingInstance> placed,
                               BuildingRegistry registry, StructureBuilder builder) {
        for (int i = 0; i < count; i++) {
            BlockPos origin = findPosition(world, seed, type, i, placed, minDist);
            if (origin == null) {
                LOGGER.warn("[JJK] WorldGen: {} 인스턴스 {} 위치 탐색 실패 (건너뜀)", type, i);
                continue;
            }
            List<NpcSpawnPoint> points = builder.build(world, origin);
            BuildingInstance inst = new BuildingInstance(type, origin.getX(), origin.getY(), origin.getZ(), points);
            placed.add(inst);
            registry.add(inst);
            LOGGER.info("[JJK] WorldGen: {} #{} @ ({},{},{})",
                type, i, origin.getX(), origin.getY(), origin.getZ());
        }
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
}
