package com.jjk.domain;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 영역 구체 좌표 계산 및 블록 테마 매핑.
 * 명세: docs/jjk_spec_06_decisions.md § 영역 전개 블록 생성
 */
public final class SphereBuilder {

    private SphereBuilder() {}

    // ─── 좌표 계산 ────────────────────────────────────────────────────────────

    /**
     * 구체 표면(hollow=true) 또는 전체(hollow=false) 블록 좌표를 반환한다.
     * hollow=true  → dist in [radius-1.0, radius] (두께 1블록 표면)
     * hollow=false → dist <= radius (내부 포함 전체)
     */
    public static List<BlockPos> getSpherePositions(BlockPos center, int radius, boolean hollow) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (hollow) {
                        if (dist <= radius && dist >= radius - 1.0) {
                            positions.add(center.add(x, y, z));
                        }
                    } else {
                        if (dist <= radius) {
                            positions.add(center.add(x, y, z));
                        }
                    }
                }
            }
        }
        return positions;
    }

    // ─── 블록 테마 매핑 ────────────────────────────────────────────────────────

    public record ThemeBlocks(BlockState surface, BlockState interior) {}

    /**
     * blockTheme 문자열 → 표면·내부 BlockState 쌍 반환.
     * 매핑 누락 시 기본값: STONE(표면) + AIR(내부).
     * volcanic 내부: 명세에 따라 LAVA → ORANGE_CONCRETE 대체.
     */
    public static ThemeBlocks resolveTheme(String blockTheme) {
        if (blockTheme == null) blockTheme = "";
        return switch (blockTheme) {
            case "void"      -> new ThemeBlocks(Blocks.OBSIDIAN.getDefaultState(),
                                                Blocks.BLACK_CONCRETE.getDefaultState());
            case "destroyed" -> new ThemeBlocks(Blocks.CRACKED_STONE_BRICKS.getDefaultState(),
                                                Blocks.GRAVEL.getDefaultState());
            case "shadow"    -> new ThemeBlocks(Blocks.BLACK_STAINED_GLASS.getDefaultState(),
                                                Blocks.AIR.getDefaultState()); // 속 빈 구체
            case "cursed"    -> new ThemeBlocks(Blocks.PURPLE_CONCRETE.getDefaultState(),
                                                Blocks.SOUL_SAND.getDefaultState());
            case "flesh"     -> new ThemeBlocks(Blocks.RED_CONCRETE.getDefaultState(),
                                                Blocks.NETHERRACK.getDefaultState());
            case "volcanic"  -> new ThemeBlocks(Blocks.MAGMA_BLOCK.getDefaultState(),
                                                Blocks.ORANGE_CONCRETE.getDefaultState()); // LAVA 대체
            case "casino"    -> new ThemeBlocks(Blocks.GOLD_BLOCK.getDefaultState(),
                                                Blocks.YELLOW_CONCRETE.getDefaultState());
            case "courtroom" -> new ThemeBlocks(Blocks.IRON_BLOCK.getDefaultState(),
                                                Blocks.WHITE_CONCRETE.getDefaultState());
            default          -> new ThemeBlocks(Blocks.STONE.getDefaultState(),
                                                Blocks.AIR.getDefaultState());
        };
    }

    // ─── 영역 전개 시 호출 ────────────────────────────────────────────────────

    /**
     * 비개방형 영역의 구체 블록을 DomainBlockQueue에 예약한다.
     * - 개방형(isOpen=true): 즉시 반환 (블록 생성 없음)
     * - 표면 블록: 항상 enqueue
     * - 내부 블록: theme.interior()가 AIR면 생략 (자연적으로 빈 공간 유지)
     *
     * 보호 블록(BEDROCK·BARRIER·END_PORTAL_FRAME)은 DomainBlockQueue.enqueue()에서 걸러진다.
     * bucketKey로 instanceId를 사용 — 같은 도메인 타입 다중 인스턴스 충돌 방지.
     */
    public static void enqueueSphere(DomainInstance instance, ServerWorld world,
                                     DomainBlockQueue queue, String blockTheme) {
        if (instance.isOpen) return;

        int radius = (int) instance.maxRadius;
        ThemeBlocks theme = resolveTheme(blockTheme);
        String bucketKey = instance.instanceId.toString();

        List<BlockPos> surfacePos = new ArrayList<>();
        List<BlockPos> interiorPos = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist > radius) continue;
                    BlockPos p = instance.center.add(x, y, z);
                    if (dist >= radius - 1.0) {
                        surfacePos.add(p);
                    } else {
                        interiorPos.add(p);
                    }
                }
            }
        }

        if (!surfacePos.isEmpty()) {
            queue.enqueue(bucketKey, world, surfacePos, theme.surface());
        }
        if (!theme.interior().isAir() && !interiorPos.isEmpty()) {
            queue.enqueue(bucketKey, world, interiorPos, theme.interior());
        }
    }
}
