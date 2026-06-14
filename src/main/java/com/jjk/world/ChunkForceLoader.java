package com.jjk.world;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class ChunkForceLoader {

    private ChunkForceLoader() {}

    public static void forceLoadSpawnChunks(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        int centerX = spawn.getX() >> 4;
        int centerZ = spawn.getZ() >> 4;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                world.setChunkForced(centerX + dx, centerZ + dz, true);
            }
        }
    }

    public static void forceLoadForPlayer(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld sw)) return;
        int centerX = player.getBlockX() >> 4;
        int centerZ = player.getBlockZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                sw.setChunkForced(centerX + dx, centerZ + dz, true);
            }
        }
    }
}
