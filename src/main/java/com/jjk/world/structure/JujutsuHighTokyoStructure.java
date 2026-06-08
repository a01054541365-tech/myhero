package com.jjk.world.structure;

import com.jjk.world.JjkStructureBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** 도쿄 제1고등학교 — JjkStructureBuilder 위임 래퍼. */
public final class JujutsuHighTokyoStructure {
    private JujutsuHighTokyoStructure() {}

    public static List<NpcSpawnPoint> build(ServerWorld world, BlockPos origin) {
        return new JjkStructureBuilder(world, origin).buildAll();
    }
}
