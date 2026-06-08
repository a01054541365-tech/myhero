package com.jjk.world.structure;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.List;

/**
 * 시부야 지하시설.
 * 크기: 40×6×40 블록. Y=-20 고정 생성.
 * 소재: deepslate, cracked_deepslate_tiles.
 * 지상 진입구: 2×2 구멍 + 사다리 연결.
 */
public final class ShibuyaUndergroundStructure {
    private ShibuyaUndergroundStructure() {}

    public static List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin) {
        // origin.y = -20 (WorldGenerationManager 에서 강제 설정)
        buildHall(world, origin);
        buildFightScenery(world, origin);
        buildSurfaceEntrance(world, origin);
        placeLighting(world, origin);
        return List.of(
            new NpcSpawnPoint("gojo_shiyu", "공시우", 0, 1, -15, 0f)
        );
    }

    private static void set(ServerWorld w, BlockPos o, int dx, int dy, int dz, Block block) {
        w.setBlockState(o.add(dx, dy, dz), block.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static void setState(ServerWorld w, BlockPos o, int dx, int dy, int dz, BlockState state) {
        w.setBlockState(o.add(dx, dy, dz), state, Block.NOTIFY_LISTENERS);
    }

    private static void fill(ServerWorld w, BlockPos o,
                              int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        BlockState s = block.getDefaultState();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    w.setBlockState(o.add(x, y, z), s, Block.NOTIFY_LISTENERS);
    }

    private static void hollow(ServerWorld w, BlockPos o,
                                int x1, int y1, int z1, int x2, int y2, int z2,
                                Block wall, Block floor, Block ceiling) {
        fill(w, o, x1, y1, z1, x2, y1, z2, floor);
        fill(w, o, x1, y2, z1, x2, y2, z2, ceiling);
        fill(w, o, x1, y1 + 1, z1, x1, y2 - 1, z2, wall);
        fill(w, o, x2, y1 + 1, z1, x2, y2 - 1, z2, wall);
        fill(w, o, x1 + 1, y1 + 1, z1, x2 - 1, y2 - 1, z1, wall);
        fill(w, o, x1 + 1, y1 + 1, z2, x2 - 1, y2 - 1, z2, wall);
    }

    // ── 넓은 홀 ─────────────────────────────────────────────────────────────

    private static void buildHall(ServerWorld w, BlockPos o) {
        hollow(w, o, -20, 0, -20, 20, 5, 20,
            Blocks.DEEPSLATE, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.DEEPSLATE);
        fill(w, o, -19, 1, -19, 19, 4, 19, Blocks.AIR);
    }

    // ── 전투 흔적 연출 ───────────────────────────────────────────────────────

    private static void buildFightScenery(ServerWorld w, BlockPos o) {
        // 부서진 블록 (깨진 딥슬레이트)
        for (int[] pos : new int[][]{
            {-12, 0, -12}, {-8, 0, 5}, {3, 0, -15}, {10, 0, 8}, {-5, 0, 12}
        }) {
            set(w, o, pos[0], pos[1], pos[2], Blocks.CRACKED_DEEPSLATE_TILES);
        }

        // 영혼 모닥불 (전투 흔적)
        for (int[] pos : new int[][]{{-10, 1, 0}, {5, 1, -10}, {15, 1, 10}}) {
            setState(w, o, pos[0], pos[1], pos[2],
                Blocks.SOUL_CAMPFIRE.getDefaultState()
                    .with(Properties.LIT, true));
        }

        // 잔해 더미 (자갈)
        for (int[] pos : new int[][]{{-15, 1, 15}, {12, 1, -8}, {-3, 1, 18}}) {
            set(w, o, pos[0], pos[1], pos[2], Blocks.GRAVEL);
        }
    }

    // ── 지상 진입구 (2×2 구멍 + 사다리) ─────────────────────────────────────

    private static void buildSurfaceEntrance(ServerWorld w, BlockPos o) {
        int surfaceY = w.getTopY(Heightmap.Type.WORLD_SURFACE, o.getX(), o.getZ());
        // 지상 → 지하 수직 샤프트 (2×2)
        for (int y = o.getY() + 5; y <= surfaceY; y++) {
            w.setBlockState(o.add(0, y - o.getY(), 0), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            w.setBlockState(o.add(1, y - o.getY(), 0), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            w.setBlockState(o.add(0, y - o.getY(), 1), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            w.setBlockState(o.add(1, y - o.getY(), 1), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        // 사다리 (북쪽 벽)
        for (int y = o.getY() + 5; y <= surfaceY; y++) {
            int dy = y - o.getY();
            setState(w, o, 0, dy, 0,
                Blocks.LADDER.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
        }
        // 지상 테두리
        for (int dx : new int[]{-1, 2}) {
            for (int dz = -1; dz <= 2; dz++) {
                w.setBlockState(
                    o.add(dx, surfaceY - o.getY(), dz),
                    Blocks.DEEPSLATE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
        for (int dz : new int[]{-1, 2}) {
            for (int dx = 0; dx <= 1; dx++) {
                w.setBlockState(
                    o.add(dx, surfaceY - o.getY(), dz),
                    Blocks.DEEPSLATE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    // ── 조명 ────────────────────────────────────────────────────────────────

    private static void placeLighting(ServerWorld w, BlockPos o) {
        for (int[] pos : new int[][]{
            {-15, 4, -15}, {15, 4, -15}, {-15, 4, 15}, {15, 4, 15},
            {0, 4, 0}, {-10, 4, 0}, {10, 4, 0}, {0, 4, -10}, {0, 4, 10}
        }) {
            set(w, o, pos[0], pos[1], pos[2], Blocks.SOUL_LANTERN);
        }
    }
}
