package com.jjk.world.structure;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * 나나미 사무소.
 * 크기: 10×5×10 블록. 소재: brick, oak_planks.
 * NPC 스폰: 이에이리 쇼코(치유), 이치지 키요타카(퀘스트).
 */
public final class NanamiOfficeStructure {
    private NanamiOfficeStructure() {}

    public static List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin) {
        buildExterior(world, origin);
        buildInterior(world, origin);
        return List.of(
            new NpcSpawnPoint("shoko",  "이에이리 쇼코",      -2, 1, -2, 90f),
            new NpcSpawnPoint("ijichi", "이치지 키요타카",      2, 1, -2, 270f)
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
        hollow(w, o, -5, 0, -5, 5, 4, 5,
            Blocks.BRICKS, Blocks.OAK_PLANKS, Blocks.BRICKS);
        fill(w, o, -4, 1, -4, 4, 3, 4, Blocks.AIR);

        // 정문 (남쪽)
        setState(w, o, 0, 1, -5, Blocks.OAK_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        setState(w, o, 0, 2, -5, Blocks.OAK_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));

        // 창문 (east wall)
        fill(w, o, 5, 2, -2, 5, 3, 2, Blocks.GLASS_PANE);
    }

    // ── 내부 ─────────────────────────────────────────────────────────────────

    private static void buildInterior(ServerWorld w, BlockPos o) {
        // 사무용 책상 2개 (oak_slab + oak_trapdoor 조합)
        // 책상 1 (왼쪽): x=-3, z=-1
        set(w, o, -3, 1, -1, Blocks.OAK_SLAB);
        setState(w, o, -3, 2, -1, Blocks.OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.EAST)
            .with(Properties.OPEN, true));

        // 책상 2 (오른쪽): x=3, z=-1
        set(w, o, 3, 1, -1, Blocks.OAK_SLAB);
        setState(w, o, 3, 2, -1, Blocks.OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.WEST)
            .with(Properties.OPEN, true));

        // 선반 (역방향 oak_stairs) — 북쪽 벽
        setState(w, o, -3, 2, 4, Blocks.OAK_STAIRS.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
        setState(w, o,  0, 2, 4, Blocks.OAK_STAIRS.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
        setState(w, o,  3, 2, 4, Blocks.OAK_STAIRS.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));

        // 서가 (북쪽 벽)
        fill(w, o, -4, 1, 4, -2, 3, 4, Blocks.BOOKSHELF);

        // 조명
        set(w, o, 0, 3, 0, Blocks.LANTERN);

        // 상자 (치료 물품)
        set(w, o, -4, 1, -3, Blocks.CHEST);
    }
}
