package com.jjk.world;

import com.jjk.entity.npc.NpcEntity;
import com.jjk.entity.npc.NpcRegistry;
import com.jjk.entity.npc.SimpleNpcEntity;
import com.jjk.world.structure.BuildingInstance;
import com.jjk.world.structure.NpcSpawnPoint;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * BuildingRegistry 기반 NPC 자동 스폰 및 재스폰.
 * 서버 재시작 후 해당 위치에 NPC가 없으면 재스폰.
 */
public final class BuildingNpcSpawner {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");

    private BuildingNpcSpawner() {}

    // 기존 buildings.json에 NPC가 없는 건물 타입을 위한 canonical 정의
    private static final java.util.Map<String, List<NpcSpawnPoint>> CANONICAL_NPCS =
        java.util.Map.of(
            "training_dojo", List.of(new NpcSpawnPoint("kusakabe",   "쿠사카베 아츠야", 0, 1, 0, 180f)),
            "black_market",  List.of(new NpcSpawnPoint("gojo_shiyu", "공시우",          0, 1, 0,   0f))
        );

    private static List<NpcSpawnPoint> effectiveNpcs(BuildingInstance b) {
        return b.npcSpawnPoints.isEmpty()
            ? CANONICAL_NPCS.getOrDefault(b.type, List.of())
            : b.npcSpawnPoints;
    }

    /** 등록된 모든 건물의 NPC를 점검하고 없으면 재스폰. */
    public static void recheckAndRespawn(ServerWorld world, BuildingRegistry registry) {
        int spawned = 0;
        for (BuildingInstance b : registry.getAll()) {
            for (NpcSpawnPoint sp : effectiveNpcs(b)) {
                int ax = b.x + sp.dx();
                int ay = b.y + sp.dy();
                int az = b.z + sp.dz();
                if (!hasNpc(world, ax, ay, az, sp.npcId())) {
                    spawnNpc(world, new BlockPos(ax, ay, az), sp);
                    spawned++;
                }
            }
        }
        if (spawned > 0) {
            LOGGER.info("[JJK] BuildingNpcSpawner: NPC {}개 재스폰", spawned);
        }
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────────────────────

    private static boolean hasNpc(ServerWorld world, int x, int y, int z, String npcId) {
        Box searchBox = new Box(x - 3, y - 3, z - 3, x + 3, y + 3, z + 3);
        List<NpcEntity> found = world.getEntitiesByClass(
            NpcEntity.class, searchBox,
            e -> npcId.equals(e.getNpcId())
        );
        return !found.isEmpty();
    }

    private static void spawnNpc(ServerWorld world, BlockPos pos, NpcSpawnPoint sp) {
        EntityType<SimpleNpcEntity> type = NpcRegistry.byId(sp.npcId());
        if (type == null) {
            LOGGER.warn("[JJK] BuildingNpcSpawner: 알 수 없는 npcId={}", sp.npcId());
            return;
        }
        SimpleNpcEntity npc = type.create(world);
        if (npc == null) return;
        npc.refreshPositionAndAngles(
            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sp.yaw(), 0f);
        world.spawnEntity(npc);
    }
}
