package com.jjk.entity.spawn;

import com.jjk.JJKMod;
import com.jjk.entity.JJKEntities;
import com.jjk.entity.cursed.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

// 명칭 주령 야생 스폰 규칙 관리
// 등록: JJKMod.onInitialize() 에서 tickScheduler.register(CursedEntitySpawnManager::tick, 200)
public final class CursedEntitySpawnManager {

    private CursedEntitySpawnManager() {}

    // TickScheduler에서 200틱마다 플레이어별 호출
    public static void tick(ServerPlayerEntity player) {
        if (JJKMod.getInstance() == null) return;

        MinecraftServer server = JJKMod.getServer();
        if (server == null) return;

        // TPS 가드: TPS < 15이면 스폰 중단
        float mspt = server.getAverageTickTime();
        float tps  = mspt > 0 ? Math.min(20f, 1000f / mspt) : 20f;
        if (tps < 15f) return;

        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        long timeOfDay = sw.getTime() % 24000L;
        boolean isNight = timeOfDay >= 13000L && timeOfDay <= 23000L;
        if (!isNight) return;

        Random rng = sw.getRandom();

        // 날씨 체크 (3급 HomurakuiEntity 스폰 확률 증가 조건)
        boolean badWeather = sw.isRaining();

        trySpawnGrade4(sw, player, rng);
        if (badWeather || rng.nextFloat() < 0.3f) {
            trySpawnGrade3(sw, player, rng);
        }
        // 2급: 야간 5% 확률
        if (rng.nextFloat() < 0.05f) {
            trySpawnGrade2(sw, player, rng);
        }
        // 특급(죠고, 하난미): 자연 스폰 없음 — /jj spawncursed 전용
    }

    // ─── 4급 스폰 (MukiEntity, KotsibakuEntity) ─────────────────────────────

    private static void trySpawnGrade4(ServerWorld sw, ServerPlayerEntity player, Random rng) {
        BlockPos pos = findSpawnPos(sw, player, 24, 48, rng);
        if (pos == null) return;

        // 청크당 최대 3마리
        if (countEntitiesInChunk(sw, pos, MukiEntity.class) + countEntitiesInChunk(sw, pos, KotsibakuEntity.class) >= 3) return;

        // 무키는 SWAMP·RIVER biome에 추가 가중치
        RegistryEntry<Biome> biome = sw.getBiome(pos);
        boolean isSwampOrRiver = biome.matchesKey(BiomeKeys.SWAMP)
            || biome.matchesKey(BiomeKeys.RIVER)
            || biome.matchesKey(BiomeKeys.MANGROVE_SWAMP);

        EntityType<? extends HostileEntity> type;
        if (isSwampOrRiver) {
            type = rng.nextFloat() < 0.67f ? JJKEntities.MUKI : JJKEntities.KOTSIBAKU;
        } else {
            type = rng.nextBoolean() ? JJKEntities.MUKI : JJKEntities.KOTSIBAKU;
        }
        spawnAt(sw, pos, type);
    }

    // ─── 3급 스폰 (HomurakuiEntity) ─────────────────────────────────────────

    private static void trySpawnGrade3(ServerWorld sw, ServerPlayerEntity player, Random rng) {
        BlockPos pos = findSpawnPos(sw, player, 32, 64, rng);
        if (pos == null) return;
        if (countEntitiesInChunk(sw, pos, HomurakuiEntity.class) >= 1) return;
        spawnAt(sw, pos, JJKEntities.HOMURAKU);
    }

    // ─── 2급 스폰 (JuugoNpcEntity) ──────────────────────────────────────────

    private static void trySpawnGrade2(ServerWorld sw, ServerPlayerEntity player, Random rng) {
        BlockPos pos = findSpawnPos(sw, player, 32, 64, rng);
        if (pos == null) return;
        if (countEntitiesInChunk(sw, pos, JuugoNpcEntity.class) >= 1) return;
        spawnAt(sw, pos, JJKEntities.JUUGO_NPC);
    }

    // ─── 공통 헬퍼 ────────────────────────────────────────────────────────────

    private static BlockPos findSpawnPos(ServerWorld world, ServerPlayerEntity player,
                                          int minDist, int maxDist, Random rng) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = rng.nextBetween(-maxDist, maxDist);
            int dz = rng.nextBetween(-maxDist, maxDist);
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (dist < minDist || dist > maxDist) continue;

            int wx = (int) player.getX() + dx;
            int wz = (int) player.getZ() + dz;
            int wy = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz);
            BlockPos pos = new BlockPos(wx, wy, wz);

            if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) continue;
            if (!world.getBlockState(pos).isAir()) continue;
            if (JJKMod.getDomainManager() != null
                    && JJKMod.getDomainManager().isInsideAnyDomain(pos.toCenterPos())) continue;
            return pos;
        }
        return null;
    }

    private static <T extends HostileEntity> int countEntitiesInChunk(
            ServerWorld world, BlockPos pos, Class<T> cls) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        Box chunkBox = new Box(cx * 16.0, -64, cz * 16.0,
                               cx * 16.0 + 16, 320, cz * 16.0 + 16);
        return world.getEntitiesByClass(cls, chunkBox, e -> true).size();
    }

    private static <T extends HostileEntity> void spawnAt(ServerWorld world, BlockPos pos,
                                                            EntityType<T> type) {
        T entity = type.create(world);
        if (entity == null) return;
        entity.refreshPositionAndAngles(
            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        world.spawnEntity(entity);
    }
}
