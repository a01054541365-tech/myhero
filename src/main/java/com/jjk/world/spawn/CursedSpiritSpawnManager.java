package com.jjk.world.spawn;

import com.jjk.JJKMod;
import com.jjk.entity.CursedSpiritEntity;
import com.jjk.entity.JJKEntities;
import com.jjk.entity.cursed.BaseCursedSpiritEntity;
import com.jjk.entity.cursed.CursedSpiritGrade;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.List;

// 주령 야생 자동 스폰. 100틱 주기로 TickScheduler에 등록.
public final class CursedSpiritSpawnManager {

    private static final int MAX_TOTAL_SPIRITS  = 30;
    private static final int MAX_PER_CHUNK      = 3;
    private static final int MIN_SPAWN_RANGE    = 64;
    private static final int MAX_SPAWN_RANGE    = 128;
    private static final int SPAWN_ATTEMPTS     = 10;
    private static final int NIGHT_ATTEMPTS     = 15; // 야간 1.5× 시도 횟수
    private static final long NIGHT_START_TICKS = 13000L;
    private static final long DAY_CYCLE_TICKS   = 24000L;
    private static final float TPS_LOW_THRESHOLD = 62.5f; // ms/tick → TPS < 16

    // 등급 확률 테이블 (CursedSpiritGrade.values() 순서와 일치)
    // GRADE_4_BELOW=5%, GRADE_4=40%, GRADE_3=30%, GRADE_2=15%, GRADE_1=7%, SEMI_SPECIAL=2%, SPECIAL_GRADE=1%
    private static final float[] GRADE_WEIGHTS = { 0.05f, 0.40f, 0.30f, 0.15f, 0.07f, 0.02f, 0.01f };

    private CursedSpiritSpawnManager() {}

    public static void tick(MinecraftServer server) {
        if (JJKMod.getInstance() == null) return;
        if (!JJKMod.getConfig().cursedSpiritSpawnEnabled) return;
        // TPS 저하 시 스폰 중단
        if (server.getAverageTickTime() > TPS_LOW_THRESHOLD) return;

        ServerWorld world = server.getOverworld();
        if (countAllSpirits(world) > MAX_TOTAL_SPIRITS) return;

        boolean night = (world.getTimeOfDay() % DAY_CYCLE_TICKS) > NIGHT_START_TICKS;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            trySpawnNear(world, player, night);
        }
    }

    private static int countAllSpirits(ServerWorld world) {
        Box global = new Box(-30000, -64, -30000, 30000, 320, 30000);
        return world.getEntitiesByClass(BaseCursedSpiritEntity.class, global, e -> true).size()
             + world.getEntitiesByClass(CursedSpiritEntity.class, global, e -> true).size();
    }

    private static void trySpawnNear(ServerWorld world, ServerPlayerEntity player, boolean night) {
        net.minecraft.util.math.random.Random rng = world.getRandom();
        int attempts = night ? NIGHT_ATTEMPTS : SPAWN_ATTEMPTS;
        for (int i = 0; i < attempts; i++) {
            int dx = rng.nextBetween(-MAX_SPAWN_RANGE, MAX_SPAWN_RANGE);
            int dz = rng.nextBetween(-MAX_SPAWN_RANGE, MAX_SPAWN_RANGE);
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (dist < MIN_SPAWN_RANGE || dist > MAX_SPAWN_RANGE) continue;

            int wx = (int) player.getX() + dx;
            int wz = (int) player.getZ() + dz;
            BlockPos pos = new BlockPos(wx, world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz), wz);

            if (isBannedChunk(pos)) continue;
            if (!isValidSpawnPos(world, pos)) continue;
            if (spiritsInChunk(world, wx, wz) > MAX_PER_CHUNK) continue;

            spawnSpirit(world, pos, rollGrade(rng));
            return;
        }
    }

    private static boolean isBannedChunk(BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        List<String> banned = JJKMod.getConfig().domainBannedChunks;
        for (String entry : banned) {
            int lastColon = entry.lastIndexOf(':');
            if (lastColon < 0) continue;
            String[] parts = entry.substring(lastColon + 1).split(",");
            if (parts.length != 2) continue;
            try {
                if (chunk.x == Integer.parseInt(parts[0].trim())
                        && chunk.z == Integer.parseInt(parts[1].trim())) {
                    return true;
                }
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    private static boolean isValidSpawnPos(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) return false;
        if (!world.getBlockState(pos).isAir()) return false;
        if (JJKMod.getDomainManager().isInsideAnyDomain(pos.toCenterPos())) return false;
        return true;
    }

    private static int spiritsInChunk(ServerWorld world, int wx, int wz) {
        int chunkBaseX = wx & ~15;
        int chunkBaseZ = wz & ~15;
        Box chunkBox = new Box(chunkBaseX, -64, chunkBaseZ, chunkBaseX + 16, 320, chunkBaseZ + 16);
        return world.getEntitiesByClass(BaseCursedSpiritEntity.class, chunkBox, e -> true).size()
             + world.getEntitiesByClass(CursedSpiritEntity.class, chunkBox, e -> true).size();
    }

    private static CursedSpiritGrade rollGrade(net.minecraft.util.math.random.Random rng) {
        float roll = rng.nextFloat();
        float cumulative = 0f;
        CursedSpiritGrade[] grades = CursedSpiritGrade.values();
        for (int i = 0; i < grades.length; i++) {
            cumulative += GRADE_WEIGHTS[i];
            if (roll < cumulative) return grades[i];
        }
        return CursedSpiritGrade.GRADE_4;
    }

    private static void spawnSpirit(ServerWorld world, BlockPos pos, CursedSpiritGrade grade) {
        EntityType<? extends BaseCursedSpiritEntity> type = switch (grade) {
            case GRADE_4_BELOW -> JJKEntities.SYOUTO;
            case GRADE_4       -> JJKEntities.MOLE_CURSED_SPIRIT;
            case GRADE_3       -> JJKEntities.SHADOW_CURSED_SPIRIT;
            case GRADE_2       -> JJKEntities.CE_ABSORBER;
            case GRADE_1       -> JJKEntities.PLANT_CURSED_SPIRIT;
            case SEMI_SPECIAL  -> JJKEntities.WATER_CURSED_SPIRIT;
            case SPECIAL_GRADE -> JJKEntities.SMALLPOX_DEITY;
        };
        BaseCursedSpiritEntity spirit = type.create(world);
        if (spirit == null) return;
        spirit.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        world.spawnEntity(spirit);
    }
}
