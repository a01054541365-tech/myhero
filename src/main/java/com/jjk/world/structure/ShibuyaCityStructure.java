package com.jjk.world.structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.List;

/**
 * 시부야 도심 거리 (스크램블 교차로).
 * 크기: 121×121, 최고 높이 ~60. origin = 교차로 중심 지표면.
 * 십자 대로 + 보조 도로, 마천루 12동, 전광판 랜드마크 타워, 가로등·횡단보도.
 */
public final class ShibuyaCityStructure {
    private ShibuyaCityStructure() {}

    private static final int HALF = 60;        // 부지 반폭
    private static final int CLEAR_HEIGHT = 70; // 지상 정리 높이

    /** 타워 1동 정의: 중심(cx,cz), 반폭, 높이, 외벽/유리 팔레트. */
    private record Tower(int cx, int cz, int halfW, int halfD, int height,
                         Block wall, Block glass, boolean antenna) {}

    private static final List<Tower> TOWERS = List.of(
        // 북서 블록
        new Tower(-34, -34, 9, 9, 56, Blocks.GRAY_CONCRETE,       Blocks.LIGHT_BLUE_STAINED_GLASS, true),
        new Tower(-50, -18, 6, 6, 32, Blocks.WHITE_CONCRETE,      Blocks.GLASS,                    false),
        new Tower(-16, -50, 6, 7, 40, Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_STAINED_GLASS,       false),
        // 북동 블록
        new Tower( 34, -34, 8, 8, 48, Blocks.BLACK_CONCRETE,      Blocks.ORANGE_STAINED_GLASS,     true),
        new Tower( 50, -16, 5, 6, 26, Blocks.BROWN_CONCRETE,      Blocks.GLASS,                    false),
        new Tower( 16, -50, 7, 6, 36, Blocks.WHITE_CONCRETE,      Blocks.LIGHT_BLUE_STAINED_GLASS, false),
        // 남서 블록
        new Tower(-34,  34, 8, 8, 44, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GLASS,                    false),
        new Tower(-50,  16, 6, 5, 28, Blocks.GRAY_CONCRETE,       Blocks.GRAY_STAINED_GLASS,       false),
        new Tower(-16,  50, 6, 6, 34, Blocks.WHITE_CONCRETE,      Blocks.CYAN_STAINED_GLASS,       true),
        // 남동 블록 (랜드마크는 별도 생성이므로 2동)
        new Tower( 50,  34, 6, 6, 30, Blocks.GRAY_CONCRETE,       Blocks.GLASS,                    false),
        new Tower( 34,  50, 7, 7, 42, Blocks.BLACK_CONCRETE,      Blocks.WHITE_STAINED_GLASS,      false)
    );

    public static List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin) {
        forceLoadChunks(world, origin);
        prepareGround(world, origin);
        buildRoads(world, origin);
        buildCrossing(world, origin);
        for (Tower t : TOWERS) {
            buildTower(world, origin, t);
        }
        buildLandmark(world, origin, 34, 34); // 109풍 전광판 타워 (남동 블록)
        placeStreetLights(world, origin);
        placePlanters(world, origin);
        return List.of();
    }

    // ── 공통 헬퍼 ────────────────────────────────────────────────────────────

    private static void set(ServerWorld w, BlockPos o, int dx, int dy, int dz, Block block) {
        w.setBlockState(o.add(dx, dy, dz), block.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static void fill(ServerWorld w, BlockPos o,
                              int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        BlockState s = block.getDefaultState();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    w.setBlockState(o.add(x, y, z), s, Block.NOTIFY_LISTENERS);
    }

    private static void forceLoadChunks(ServerWorld w, BlockPos o) {
        for (int cx = (o.getX() - HALF) >> 4; cx <= (o.getX() + HALF) >> 4; cx++)
            for (int cz = (o.getZ() - HALF) >> 4; cz <= (o.getZ() + HALF) >> 4; cz++)
                w.getChunk(cx, cz, ChunkStatus.FULL, true);
    }

    // ── 부지 정리: 지형 메우기 + 지상 비우기 + 보도 평탄화 ──────────────────

    private static void prepareGround(ServerWorld w, BlockPos o) {
        int oy = o.getY();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                int wx = o.getX() + dx, wz = o.getZ() + dz;
                int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                // 골짜기 메움 (지표보다 낮으면 돌로 채움)
                for (int y = Math.min(top, oy - 4); y <= oy - 2; y++)
                    w.setBlockState(new BlockPos(wx, y, wz), Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                // 보도 기본면
                w.setBlockState(new BlockPos(wx, oy - 1, wz),
                    Blocks.SMOOTH_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                // 지상 정리 (언덕·나무 제거)
                for (int y = oy; y <= oy + CLEAR_HEIGHT; y++)
                    w.setBlockState(new BlockPos(wx, y, wz), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    // ── 도로: 십자 대로(폭 9) + 보조 도로(폭 5) + 차선 ──────────────────────

    private static void buildRoads(ServerWorld w, BlockPos o) {
        // 대로 (남북·동서)
        fill(w, o, -4, -1, -HALF, 4, -1, HALF, Blocks.BLACK_CONCRETE);
        fill(w, o, -HALF, -1, -4, HALF, -1, 4, Blocks.BLACK_CONCRETE);
        // 보조 도로 (x=±34, z=±34 축, 폭 5)
        for (int c : new int[]{-34, 34}) {
            fill(w, o, c - 2, -1, -HALF, c + 2, -1, HALF, Blocks.BLACK_CONCRETE);
            fill(w, o, -HALF, -1, c - 2, HALF, -1, c + 2, Blocks.BLACK_CONCRETE);
        }
        // 대로 중앙 차선 (점선)
        for (int d = -HALF; d <= HALF; d += 4) {
            if (Math.abs(d) <= 6) continue; // 교차로 내부 제외
            set(w, o, 0, -1, d, Blocks.WHITE_CONCRETE);
            set(w, o, d, -1, 0, Blocks.WHITE_CONCRETE);
        }
    }

    // ── 스크램블 교차로: 횡단보도 줄무늬 4방향 + 대각선 ─────────────────────

    private static void buildCrossing(ServerWorld w, BlockPos o) {
        for (int lane = -4; lane <= 4; lane += 2) {
            for (int off = 6; off <= 9; off++) {
                set(w, o, lane, -1,  off, Blocks.WHITE_CONCRETE);
                set(w, o, lane, -1, -off, Blocks.WHITE_CONCRETE);
                set(w, o,  off, -1, lane, Blocks.WHITE_CONCRETE);
                set(w, o, -off, -1, lane, Blocks.WHITE_CONCRETE);
            }
        }
        // 대각선 횡단 (스크램블)
        for (int t = 2; t <= 5; t++) {
            set(w, o,  t, -1,  t, Blocks.WHITE_CONCRETE);
            set(w, o, -t, -1,  t, Blocks.WHITE_CONCRETE);
            set(w, o,  t, -1, -t, Blocks.WHITE_CONCRETE);
            set(w, o, -t, -1, -t, Blocks.WHITE_CONCRETE);
        }
    }

    // ── 마천루 1동: 유리 커튼월 + 층별 바닥/조명 + 입구 + 옥상 ──────────────

    private static void buildTower(ServerWorld w, BlockPos o, Tower t) {
        int x1 = t.cx - t.halfW, x2 = t.cx + t.halfW;
        int z1 = t.cz - t.halfD, z2 = t.cz + t.halfD;

        // 외벽: 층(4블록)마다 하단 1줄 콘크리트 + 상단 3줄 유리, 모서리는 콘크리트 기둥
        for (int y = 0; y < t.height; y++) {
            Block band = (y % 4 == 0) ? t.wall : t.glass;
            for (int x = x1; x <= x2; x++) {
                set(w, o, x, y, z1, (x == x1 || x == x2) ? t.wall : band);
                set(w, o, x, y, z2, (x == x1 || x == x2) ? t.wall : band);
            }
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                set(w, o, x1, y, z, band == t.glass && z != z1 && z != z2 ? t.glass : t.wall);
                set(w, o, x2, y, z, band == t.glass && z != z1 && z != z2 ? t.glass : t.wall);
            }
        }
        // 내부: 층 바닥 + 천장 조명
        fill(w, o, x1 + 1, 0, z1 + 1, x2 - 1, t.height - 1, z2 - 1, Blocks.AIR);
        for (int fy = 0; fy < t.height - 2; fy += 4) {
            if (fy > 0)
                fill(w, o, x1 + 1, fy, z1 + 1, x2 - 1, fy, z2 - 1, Blocks.SMOOTH_STONE);
            set(w, o, t.cx, fy + 3, t.cz, Blocks.SEA_LANTERN);
        }
        // 1층 바닥 + 입구 (교차로 쪽 면, 3×3)
        fill(w, o, x1 + 1, -1, z1 + 1, x2 - 1, -1, z2 - 1, Blocks.POLISHED_ANDESITE);
        int doorZ = (t.cz > 0) ? z1 : z2;
        fill(w, o, t.cx - 1, 0, doorZ, t.cx + 1, 2, doorZ, Blocks.AIR);
        // 옥상: 슬래브 + 테두리 + 안테나
        fill(w, o, x1, t.height, z1, x2, t.height, z2, t.wall);
        fill(w, o, x1, t.height + 1, z1, x2, t.height + 1, z1, t.wall);
        fill(w, o, x1, t.height + 1, z2, x2, t.height + 1, z2, t.wall);
        fill(w, o, x1, t.height + 1, z1, x1, t.height + 1, z2, t.wall);
        fill(w, o, x2, t.height + 1, z1, x2, t.height + 1, z2, t.wall);
        if (t.antenna) {
            fill(w, o, t.cx, t.height + 1, t.cz, t.cx, t.height + 7, t.cz, Blocks.IRON_BARS);
            set(w, o, t.cx, t.height + 8, t.cz, Blocks.REDSTONE_BLOCK);
            set(w, o, t.cx, t.height + 9, t.cz, Blocks.SEA_LANTERN);
        }
    }

    // ── 랜드마크: 원통형 타워 + 대형 전광판 (109풍) ──────────────────────────

    private static void buildLandmark(ServerWorld w, BlockPos o, int cx, int cz) {
        int radius = 9, height = 60;
        for (int y = 0; y < height; y++) {
            boolean glassBand = (y % 5 != 0);
            for (int a = 0; a < 64; a++) {
                double ang = 2 * Math.PI * a / 64;
                int dx = (int) Math.round(Math.cos(ang) * radius);
                int dz = (int) Math.round(Math.sin(ang) * radius);
                Block b = glassBand && ((a / 8) % 2 == 0) ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.QUARTZ_BLOCK;
                set(w, o, cx + dx, y, cz + dz, b);
            }
        }
        // 내부 비우기 + 층 조명
        for (int y = 0; y < height; y++)
            for (int dx = -radius + 1; dx <= radius - 1; dx++)
                for (int dz = -radius + 1; dz <= radius - 1; dz++)
                    if (dx * dx + dz * dz < (radius - 1) * (radius - 1))
                        set(w, o, cx + dx, y, cz + dz, Blocks.AIR);
        for (int fy = 4; fy < height; fy += 5) {
            for (int dx = -radius + 2; dx <= radius - 2; dx++)
                for (int dz = -radius + 2; dz <= radius - 2; dz++)
                    if (dx * dx + dz * dz < (radius - 2) * (radius - 2))
                        set(w, o, cx + dx, fy, cz + dz, Blocks.SMOOTH_STONE);
            set(w, o, cx, fy + 2, cz, Blocks.SEA_LANTERN);
        }
        // 입구 (교차로를 향한 북서면)
        fill(w, o, cx - radius, 0, cz - 1, cx - radius + 1, 2, cz + 1, Blocks.AIR);
        // 옥상 돔 + 첨탑
        fill(w, o, cx - 2, height, cz - 2, cx + 2, height, cz + 2, Blocks.QUARTZ_BLOCK);
        fill(w, o, cx, height + 1, cz, cx, height + 5, cz, Blocks.IRON_BARS);
        set(w, o, cx, height + 6, cz, Blocks.SEA_LANTERN);
        // 대형 전광판: 교차로를 향한 면 (북서쪽 외벽), 검은 프레임 + 발광 패널
        int sx = cx - radius - 1;
        fill(w, o, sx, 14, cz - 6, sx, 24, cz + 6, Blocks.BLACK_CONCRETE);
        fill(w, o, sx - 1, 15, cz - 5, sx - 1, 23, cz + 5, Blocks.SEA_LANTERN);
    }

    // ── 가로등: 대로변 양측 12블록 간격 ─────────────────────────────────────

    private static void placeStreetLights(ServerWorld w, BlockPos o) {
        for (int d = -48; d <= 48; d += 12) {
            if (Math.abs(d) <= 8) continue; // 교차로 부근 제외
            for (int side : new int[]{-6, 6}) {
                streetLight(w, o, side, d);
                streetLight(w, o, d, side);
            }
        }
    }

    private static void streetLight(ServerWorld w, BlockPos o, int dx, int dz) {
        fill(w, o, dx, 0, dz, dx, 3, dz, Blocks.ANDESITE_WALL);
        set(w, o, dx, 4, dz, Blocks.SEA_LANTERN);
    }

    // ── 가로수 화단: 보도 모서리 ─────────────────────────────────────────────

    private static void placePlanters(ServerWorld w, BlockPos o) {
        for (int[] p : new int[][]{
            {-12, -12}, {12, -12}, {-12, 12}, {12, 12},
            {-22, -8}, {22, -8}, {-22, 8}, {22, 8},
            {-8, -22}, {8, -22}, {-8, 22}, {8, 22}
        }) {
            set(w, o, p[0], -1, p[1], Blocks.GRASS_BLOCK);
            fill(w, o, p[0], 0, p[1], p[0], 2, p[1], Blocks.OAK_LOG);
            fill(w, o, p[0] - 1, 3, p[1] - 1, p[0] + 1, 4, p[1] + 1, Blocks.OAK_LEAVES);
            set(w, o, p[0], 5, p[1], Blocks.OAK_LEAVES);
        }
    }
}
