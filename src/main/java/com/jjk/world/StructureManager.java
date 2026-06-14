package com.jjk.world;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.world.loot.JJKLootTable;
import com.jjk.world.structure.DetentionFacilityStructure;
import com.jjk.world.structure.NanamiOfficeStructure;
import com.jjk.world.structure.ShibuyaCityStructure;
import com.jjk.world.structure.ShibuyaUndergroundStructure;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StructureManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");
    private final Connection conn;

    public StructureManager(Connection conn) {
        this.conn = conn;
        createTableIfAbsent();
    }

    private void createTableIfAbsent() {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS placed_structures (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    structure_type TEXT    NOT NULL,
                    world_key      TEXT    NOT NULL,
                    x              INTEGER NOT NULL,
                    y              INTEGER NOT NULL,
                    z              INTEGER NOT NULL,
                    placed_at      INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create placed_structures table", e);
        }
    }

    public void initialize(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            LOGGER.warn("[JJK] StructureManager: overworld 없음 — 초기화 건너뜀");
            return;
        }

        JjkConfig cfg = JJKMod.getConfig();
        if (cfg.jjtBuildingEnabled() && !isAlreadyPlaced(conn, "jujutsu_high_tokyo")) {
            BlockPos origin = new BlockPos(
                cfg.jjtBuildingCenterX(), cfg.jjtBuildingCenterY(), cfg.jjtBuildingCenterZ());
            new JjkStructureBuilder(overworld, origin).buildAll();
            // 주술고전 내 지정 위치 체스트 배치
            ChestPlacer cp = new ChestPlacer();
            Random rng = new Random();
            cp.placeChest(overworld, origin.add(3, 1, 3), "building_chest_3grade", rng);
            cp.placeChest(overworld, origin.add(-2, 1, 4), "building_chest_2grade", rng);
            recordPlacement("jujutsu_high_tokyo", "minecraft:overworld",
                origin.getX(), origin.getY(), origin.getZ());
            LOGGER.info("[JJK] StructureManager: jujutsu_high_tokyo 배치 완료 @ ({},{},{})",
                origin.getX(), origin.getY(), origin.getZ());
        }

        placeWildBuildings(overworld);

        ChunkForceLoader.forceLoadSpawnChunks(overworld);
        LOGGER.info("[JJK] StructureManager initialized");
    }

    private void placeWildBuildings(ServerWorld world) {
        JjkConfig cfg = JJKMod.getConfig();
        if (!cfg.buildingSpawnEnabled) return;
        if (isAlreadyPlaced(conn, "wild_buildings_complete")) return;

        List<int[]> existing = queryAllPositions(conn);
        Random random = new Random();
        ChestPlacer cp = new ChestPlacer();
        int target = 20;
        int maxAttempts = 200;
        int placed = 0;
        int totalChests = 0;

        for (int attempt = 0; attempt < maxAttempts && placed < target; attempt++) {
            int range = 500;
            int x = random.nextInt(range * 2 + 1) - range;
            int z = random.nextInt(range * 2 + 1) - range;

            boolean tooClose = false;
            for (int[] pos : existing) {
                int dx = pos[0] - x;
                int dz = pos[1] - z;
                if (dx * dx + dz * dz < 50 * 50) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            if (y < 5 || y > 300) y = 64;
            BlockPos origin = new BlockPos(x, y, z);

            String structureId;
            switch (random.nextInt(4)) {
                case 0 -> { structureId = "detention_facility"; DetentionFacilityStructure.build(world, origin); }
                case 1 -> { structureId = "shibuya_underground"; ShibuyaUndergroundStructure.build(world, origin); }
                case 2 -> { structureId = "nanami_office"; NanamiOfficeStructure.build(world, origin); }
                default -> { structureId = "shibuya_city"; ShibuyaCityStructure.build(world, origin); }
            }

            int chestCount = 1 + random.nextInt(2); // 1~2개
            for (int c = 0; c < chestCount; c++) {
                int ox = 2 + random.nextInt(3); // 2~4
                int oz = 2 + random.nextInt(3); // 2~4
                String tableKey = JJKLootTable.selectGradeTableKey(random);
                cp.placeChest(world, origin.add(ox, 1, oz), tableKey, random);
                totalChests++;
            }

            recordPlacement(structureId, "minecraft:overworld", x, y, z);
            existing.add(new int[]{x, z});
            placed++;
        }

        recordPlacement("wild_buildings_complete", "minecraft:overworld", 0, 0, 0);
        LOGGER.info("[JJK] Placed {} chests in wild buildings ({} structures)", totalChests, placed);
    }

    private List<int[]> queryAllPositions(Connection conn) {
        List<int[]> positions = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT x, z FROM placed_structures WHERE world_key = 'minecraft:overworld'")) {
            while (rs.next()) {
                positions.add(new int[]{rs.getInt("x"), rs.getInt("z")});
            }
        } catch (SQLException e) {
            LOGGER.warn("[JJK] queryAllPositions 오류: {}", e.getMessage());
        }
        return positions;
    }

    private boolean isAlreadyPlaced(Connection conn, String structureType) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM placed_structures WHERE structure_type = ?")) {
            ps.setString(1, structureType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.warn("[JJK] StructureManager.isAlreadyPlaced 오류: {}", e.getMessage());
            return false;
        }
    }

    private void recordPlacement(String structureType, String worldKey, int x, int y, int z) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO placed_structures (structure_type, world_key, x, y, z, placed_at) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, structureType);
            ps.setString(2, worldKey);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[JJK] StructureManager.recordPlacement 오류: {}", e.getMessage());
        }
    }
}
