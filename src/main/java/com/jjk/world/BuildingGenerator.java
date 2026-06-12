package com.jjk.world;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.world.structure.NpcSpawnPoint;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.List;

public final class BuildingGenerator {

    private BuildingGenerator() {}

    // ── WorldGenerationManager 랜덤 배치용 진입점 4개 ────────────────────────

    public static List<NpcSpawnPoint> buildShibuyaStation(ServerWorld world, BlockPos origin) {
        generateShibuyaStation(world, origin.getX(), origin.getZ());
        return List.of();
    }

    public static List<NpcSpawnPoint> buildJogoVolcano(ServerWorld world, BlockPos origin) {
        generateJogoVolcano(world, origin.getX(), origin.getZ());
        return List.of();
    }

    public static List<NpcSpawnPoint> buildTrainingDojo(ServerWorld world, BlockPos origin) {
        generateTrainingDojo(world, origin.getX(), origin.getZ());
        return List.of();
    }

    public static List<NpcSpawnPoint> buildBlackMarket(ServerWorld world, BlockPos origin) {
        generateBlackMarket(world, origin.getX(), origin.getZ());
        // 공시우 NPC 텔레포트 목적지 갱신 (config blackmarketPos)
        BlockPos o = surfacePos(world, origin.getX(), origin.getZ());
        JjkConfig cfg = JJKMod.getConfig();
        cfg.blackmarketPos = new double[]{o.getX(), o.getY(), o.getZ()};
        cfg.save();
        return List.of();
    }

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private static BlockPos surfacePos(ServerWorld w, int x, int z) {
        return new BlockPos(x, w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z), z);
    }

    private static void set(ServerWorld w, BlockPos o, int dx, int dy, int dz, Block block) {
        w.setBlockState(o.add(dx, dy, dz), block.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static void setS(ServerWorld w, BlockPos o, int dx, int dy, int dz, BlockState state) {
        w.setBlockState(o.add(dx, dy, dz), state, Block.NOTIFY_LISTENERS);
    }

    private static void fill(ServerWorld w, BlockPos o,
                              int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        BlockState state = block.getDefaultState();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    w.setBlockState(o.add(x, y, z), state, Block.NOTIFY_LISTENERS);
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

    // ── G-2-2: 시부야 지하철 역 (지상부) ──────────────────────────────────────

    private static void generateShibuyaStation(ServerWorld world, int cx, int cz) {
        BlockPos o = surfacePos(world, cx, cz);
        Random rng = world.getRandom();

        // 외벽 + 바닥(POLISHED_ANDESITE) + 지붕(GRAY_CONCRETE_POWDER)
        hollow(world, o, -15, 0, -15, 15, 7, 15,
            Blocks.GRAY_CONCRETE, Blocks.POLISHED_ANDESITE, Blocks.GRAY_CONCRETE_POWDER);

        // 내부 정리
        fill(world, o, -14, 1, -14, 14, 6, 14, Blocks.AIR);

        // 지붕 파손: 10% AIR
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                if (rng.nextFloat() < 0.10f) {
                    set(world, o, dx, 7, dz, Blocks.AIR);
                }
            }
        }

        // 지하 출입구 차단 (남쪽 벽 안쪽)
        fill(world, o, -2, 1, 13, 2, 3, 15, Blocks.IRON_BARS);

        // 역 간판 (OAK_WALL_SIGN, 북쪽 내벽)
        setS(world, o, 0, 5, -14, Blocks.OAK_WALL_SIGN.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));

        // 조명
        for (int[] lp : new int[][]{{-12, 6, -12}, {12, 6, -12}, {-12, 6, 12}, {12, 6, 12}}) {
            set(world, o, lp[0], lp[1], lp[2], Blocks.SEA_LANTERN);
        }
    }

    // ── G-2-3: 죠고의 화산 영역 잔해 ─────────────────────────────────────────

    private static void generateJogoVolcano(ServerWorld world, int cx, int cz) {
        Random rng = world.getRandom();

        // 원형 지형 교체 (반경 25)
        for (int dx = -25; dx <= 25; dx++) {
            for (int dz = -25; dz <= 25; dz++) {
                float dist = (float) Math.sqrt((double) dx * dx + (double) dz * dz);
                if (dist > 25) continue;
                int wx = cx + dx, wz = cz + dz;
                int wy = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz);
                Block block = dist < 5 ? Blocks.MAGMA_BLOCK
                            : dist < 15 ? Blocks.BLACKSTONE
                            : Blocks.NETHERRACK;
                world.setBlockState(new BlockPos(wx, wy - 1, wz),
                    block.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }

        // 용암 웅덩이 중앙 3×3 (source block)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int wy = world.getTopY(Heightmap.Type.WORLD_SURFACE, cx + dx, cz + dz);
                world.setBlockState(new BlockPos(cx + dx, wy - 1, cz + dz),
                    Blocks.LAVA.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }

        // 소울 캠프파이어 ×5
        for (int i = 0; i < 5; i++) {
            int rdx = rng.nextBetween(-12, 12);
            int rdz = rng.nextBetween(-12, 12);
            int wy = world.getTopY(Heightmap.Type.WORLD_SURFACE, cx + rdx, cz + rdz);
            world.setBlockState(new BlockPos(cx + rdx, wy, cz + rdz),
                Blocks.SOUL_CAMPFIRE.getDefaultState(), Block.NOTIFY_LISTENERS);
        }

        // 현무암 기둥 ×8, 높이 2~4
        for (int i = 0; i < 8; i++) {
            int rdx = rng.nextBetween(-20, 20);
            int rdz = rng.nextBetween(-20, 20);
            int wy = world.getTopY(Heightmap.Type.WORLD_SURFACE, cx + rdx, cz + rdz);
            int h = 2 + rng.nextInt(3);
            for (int y = 0; y < h; y++) {
                world.setBlockState(new BlockPos(cx + rdx, wy + y, cz + rdz),
                    Blocks.BASALT.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    // ── G-2-4: 훈련 외부 도장 ────────────────────────────────────────────────

    private static void generateTrainingDojo(ServerWorld world, int cx, int cz) {
        BlockPos o = surfacePos(world, cx, cz);

        // 바닥 20×20
        fill(world, o, -10, 0, -10, 10, 0, 10, Blocks.SMOOTH_STONE);

        // 울타리 4면
        fill(world, o, -10, 1, -10, 10, 1, -10, Blocks.OAK_FENCE);
        fill(world, o, -10, 1, 10, 10, 1, 10, Blocks.OAK_FENCE);
        fill(world, o, -10, 1, -10, -10, 1, 10, Blocks.OAK_FENCE);
        fill(world, o, 10, 1, -10, 10, 1, 10, Blocks.OAK_FENCE);

        // 모서리 기둥 (높이 4)
        for (int[] c : new int[][]{{-10, -10}, {-10, 10}, {10, -10}, {10, 10}}) {
            fill(world, o, c[0], 1, c[1], c[0], 4, c[1], Blocks.OAK_LOG);
        }

        // 입구 게이트 (남쪽)
        setS(world, o, 0, 1, 10, Blocks.OAK_FENCE_GATE.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));

        // 랜턴 (모서리 기둥 위)
        for (int[] c : new int[][]{{-10, -10}, {-10, 10}, {10, -10}, {10, 10}}) {
            setS(world, o, c[0], 5, c[1], Blocks.LANTERN.getDefaultState());
        }

        // 중앙 십자 표식 (WHITE_CONCRETE)
        set(world, o, 0, 0, 0, Blocks.WHITE_CONCRETE);
        for (int d : new int[]{-1, 1}) {
            set(world, o, d, 0, 0, Blocks.WHITE_CONCRETE);
            set(world, o, 0, 0, d, Blocks.WHITE_CONCRETE);
        }
    }

    // ── G-2-5: 공시우 암시장 ─────────────────────────────────────────────────

    private static void generateBlackMarket(ServerWorld world, int cx, int cz) {
        BlockPos o = surfacePos(world, cx, cz);

        // 외벽 (10×5×10)
        hollow(world, o, -5, 0, -5, 5, 4, 5,
            Blocks.SPRUCE_PLANKS, Blocks.COARSE_DIRT, Blocks.SPRUCE_PLANKS);

        // 내부 정리
        fill(world, o, -4, 1, -4, 4, 3, 4, Blocks.AIR);

        // 출입문 (북쪽, 하단+상단)
        setS(world, o, 0, 1, -5, Blocks.SPRUCE_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        setS(world, o, 0, 2, -5, Blocks.SPRUCE_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));

        // 내부: 상자 ×2, 통 ×2
        set(world, o, -3, 1, 3, Blocks.CHEST);
        set(world, o, 3, 1, 3, Blocks.CHEST);
        set(world, o, -3, 1, -3, Blocks.BARREL);
        set(world, o, 3, 1, -3, Blocks.BARREL);

        // 소울 랜턴 (내부 모서리)
        for (int[] c : new int[][]{{-4, 3, -4}, {-4, 3, 4}, {4, 3, -4}, {4, 3, 4}}) {
            setS(world, o, c[0], c[1], c[2], Blocks.SOUL_LANTERN.getDefaultState());
        }

        // 간판 (내부 북쪽 벽)
        setS(world, o, 0, 3, -4, Blocks.OAK_WALL_SIGN.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH));

        // 외부 방치 연출 (거미줄)
        set(world, o, -4, 2, 5, Blocks.COBWEB);
        set(world, o, 4, 2, 5, Blocks.COBWEB);
        set(world, o, -4, 2, -5, Blocks.COBWEB);
        set(world, o, 4, 2, -5, Blocks.COBWEB);
    }
}
