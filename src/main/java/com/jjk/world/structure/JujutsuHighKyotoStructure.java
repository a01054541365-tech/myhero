package com.jjk.world.structure;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * 교토 제3고등학교.
 * 크기: 30×10×25 블록. 소재: quartz_block/quartz_slab.
 */
public final class JujutsuHighKyotoStructure {
    private JujutsuHighKyotoStructure() {}

    public static List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin) {
        buildExterior(world, origin);
        buildCorridor(world, origin);
        buildClassrooms(world, origin);
        buildDojo(world, origin);
        placeLighting(world, origin);
        return List.of(
            new NpcSpawnPoint("kusakabe", "쿠사카베 아츠야", 0, 1, 13, 180f)
        );
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

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
        hollow(w, o, -15, 0, -12, 15, 9, 12,
            Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_SLAB);
        fill(w, o, -14, 1, -11, 14, 8, 11, Blocks.AIR);

        // 정문 (남쪽)
        fill(w, o, -2, 0, -12, 2, 3, -12, Blocks.AIR);
        setState(w, o, -1, 1, -12, Blocks.QUARTZ_PILLAR.getDefaultState());
        setState(w, o,  1, 1, -12, Blocks.QUARTZ_PILLAR.getDefaultState());
        setState(w, o,  0, 1, -12, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        setState(w, o,  0, 2, -12, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));

        // 창문 (동서 벽)
        fill(w, o, -15, 3, -8, -15, 6, -3, Blocks.GLASS_PANE);
        fill(w, o, -15, 3,  3, -15, 6,  8, Blocks.GLASS_PANE);
        fill(w, o,  15, 3, -8,  15, 6, -3, Blocks.GLASS_PANE);
        fill(w, o,  15, 3,  3,  15, 6,  8, Blocks.GLASS_PANE);
    }

    // ── 복도 ────────────────────────────────────────────────────────────────

    private static void buildCorridor(ServerWorld w, BlockPos o) {
        fill(w, o, -14, 0, -2, 14, 0, 2, Blocks.WHITE_CONCRETE);
        // 복도 양쪽 기둥
        for (int x : new int[]{-10, 0, 10}) {
            set(w, o, x, 1, -1, Blocks.QUARTZ_PILLAR);
            set(w, o, x, 1,  1, Blocks.QUARTZ_PILLAR);
        }
    }

    // ── 교실 2개 ────────────────────────────────────────────────────────────

    private static void buildClassrooms(ServerWorld w, BlockPos o) {
        // 서쪽 교실 (x=-14 ~ -4, z=-11 ~ -3)
        fill(w, o, -13, 0, -10, -5, 0, -4, Blocks.OAK_PLANKS);
        // 의자 배열 (oak_stairs)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                setState(w, o, -12 + col * 3, 1, -9 + row * 2,
                    Blocks.OAK_STAIRS.getDefaultState()
                        .with(Properties.HORIZONTAL_FACING, Direction.NORTH));
            }
        }
        // 칠판 (north wall)
        fill(w, o, -13, 2, -10, -5, 4, -10, Blocks.BLACK_CONCRETE);

        // 동쪽 교실 (x=4 ~ 14, z=-11 ~ -3)
        fill(w, o, 5, 0, -10, 13, 0, -4, Blocks.OAK_PLANKS);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                setState(w, o, 6 + col * 3, 1, -9 + row * 2,
                    Blocks.OAK_STAIRS.getDefaultState()
                        .with(Properties.HORIZONTAL_FACING, Direction.NORTH));
            }
        }
        fill(w, o, 5, 2, -10, 13, 4, -10, Blocks.BLACK_CONCRETE);
    }

    // ── 도장 (훈련실) ────────────────────────────────────────────────────────

    private static void buildDojo(ServerWorld w, BlockPos o) {
        // 도장: z=3 ~ 11 전체 너비
        fill(w, o, -14, 0, 4, 14, 0, 11, Blocks.SMOOTH_STONE);
        // 표적 마커
        for (int x : new int[]{-10, -5, 0, 5, 10}) {
            set(w, o, x, 0, 8, Blocks.RED_CONCRETE);
        }
        // 무기 걸이 (wall에 item_frame 방향 표현 — oak_fence로 대체)
        for (int x : new int[]{-12, -8, 8, 12}) {
            set(w, o, x, 2, 11, Blocks.OAK_FENCE);
        }
        // 도장 조명 토치
        for (int x : new int[]{-10, 0, 10}) {
            setState(w, o, x, 8, 11,
                Blocks.WALL_TORCH.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
        }
    }

    // ── 조명 ────────────────────────────────────────────────────────────────

    private static void placeLighting(ServerWorld w, BlockPos o) {
        for (int[] pos : new int[][]{{-10, 8, -8}, {10, 8, -8}, {-10, 8, 8}, {10, 8, 8}}) {
            set(w, o, pos[0], pos[1], pos[2], Blocks.SEA_LANTERN);
        }
    }
}
