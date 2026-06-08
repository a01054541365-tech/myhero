package com.jjk.world.structure;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * 소년원·수용시설.
 * 크기: 20×8×20 블록. 외벽: deepslate_bricks.
 * 내부: stone_bricks 바닥, iron_bars 창문, 철제 문.
 * 지하층(-4~-1): 비어있는 공간.
 */
public final class DetentionFacilityStructure {
    private DetentionFacilityStructure() {}

    public static List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin) {
        buildExterior(world, origin);
        buildInterior(world, origin);
        buildCells(world, origin);
        buildBasement(world, origin);
        placeLighting(world, origin);
        return List.of(
            new NpcSpawnPoint("zenin_storage", "젠인 창고지기",  -2, 1, -11, 180f),
            new NpcSpawnPoint("nahobino",      "나호비노 아오이",  2, 1, -11, 180f)
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

    // ── 외벽 ────────────────────────────────────────────────────────────────

    private static void buildExterior(ServerWorld w, BlockPos o) {
        hollow(w, o, -10, 0, -10, 10, 7, 10,
            Blocks.DEEPSLATE_BRICKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_BRICKS);
        fill(w, o, -9, 1, -9, 9, 6, 9, Blocks.AIR);

        // 정문 (남쪽)
        setState(w, o, 0, 1, -10, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        setState(w, o, 0, 2, -10, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));

        // 창문 (iron_bars) — 동서 벽
        fill(w, o, -10, 3, -6,  -10, 5, -2, Blocks.IRON_BARS);
        fill(w, o, -10, 3,  2,  -10, 5,  6, Blocks.IRON_BARS);
        fill(w, o,  10, 3, -6,   10, 5, -2, Blocks.IRON_BARS);
        fill(w, o,  10, 3,  2,   10, 5,  6, Blocks.IRON_BARS);
    }

    // ── 내부 (중앙 복도) ─────────────────────────────────────────────────────

    private static void buildInterior(ServerWorld w, BlockPos o) {
        // 중앙 복도 (stone_slab 바닥)
        fill(w, o, -2, 0, -8, 2, 0, 8, Blocks.STONE_SLAB);
    }

    // ── 감방 4칸 ─────────────────────────────────────────────────────────────

    private static void buildCells(ServerWorld w, BlockPos o) {
        // 서쪽 감방 2칸
        buildCell(w, o, -9, 0, -7, -4, 4, -3);
        buildCell(w, o, -9, 0,  3, -4, 4,  7);
        // 동쪽 감방 2칸
        buildCell(w, o,  4, 0, -7,  9, 4, -3);
        buildCell(w, o,  4, 0,  3,  9, 4,  7);
    }

    private static void buildCell(ServerWorld w, BlockPos o,
                                   int x1, int y1, int z1, int x2, int y2, int z2) {
        // 철창 칸막이 (복도 쪽 면)
        int xFace = (x1 < 0) ? x2 : x1; // 복도 쪽 X
        fill(w, o, xFace, y1 + 1, z1, xFace, y2 - 1, z2, Blocks.IRON_BARS);
        // 내부 벽
        fill(w, o, x1, y1 + 1, z1, x2, y2 - 1, z1, Blocks.STONE_BRICKS);
        fill(w, o, x1, y1 + 1, z2, x2, y2 - 1, z2, Blocks.STONE_BRICKS);
        // 철창 문 (없으면 iron_bars로 대체)
        // 침대 (하나)
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        set(w, o, cx, y1 + 1, cz, Blocks.RED_CARPET);
    }

    // ── 지하층 B1 (-4 ~ -1) ─────────────────────────────────────────────────

    private static void buildBasement(ServerWorld w, BlockPos o) {
        hollow(w, o, -9, -4, -9, 9, -1, 9,
            Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);
        fill(w, o, -8, -3, -8, 8, -2, 8, Blocks.AIR);

        // 지하 진입로 (북쪽 벽 사다리)
        for (int dy = -3; dy <= 0; dy++) {
            setState(w, o, 5, dy, -9,
                Blocks.LADDER.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
        }
    }

    // ── 조명 ────────────────────────────────────────────────────────────────

    private static void placeLighting(ServerWorld w, BlockPos o) {
        // wall_torch (규칙 배치)
        for (int z : new int[]{-7, -3, 3, 7}) {
            setState(w, o, -9, 5, z,
                Blocks.WALL_TORCH.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, Direction.EAST));
            setState(w, o, 9, 5, z,
                Blocks.WALL_TORCH.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, Direction.WEST));
        }
        set(w, o, 0, 6, 0, Blocks.SEA_LANTERN);
    }
}
