package com.jjk.dungeon;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.entity.CursedSpiritEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DungeonManager {

    private final Map<UUID, DungeonProgress> playerProgress = new HashMap<>();

    public record DungeonProgress(int currentRoom, long enteredTick, boolean cleared) {}

    // ── 입장 ─────────────────────────────────────────────────────────────────

    public void enterDungeon(UUID uuid, long tick) {
        playerProgress.put(uuid, new DungeonProgress(0, tick, false));
    }

    public void enterDungeon(ServerPlayerEntity player, long tick) {
        UUID uuid = player.getUuid();
        playerProgress.put(uuid, new DungeonProgress(0, tick, false));
        player.requestTeleport(0.5, 20.0, 150.5);
        logEvent("dungeon_enter", uuid, tick);
    }

    // ── 진행 ─────────────────────────────────────────────────────────────────

    public void onRoomCleared(UUID uuid, int room, long tick) {
        DungeonProgress prev = playerProgress.get(uuid);
        if (prev == null) return;
        playerProgress.put(uuid, new DungeonProgress(room + 1, prev.enteredTick(), false));
    }

    public void onRoomCleared(ServerPlayerEntity player, int room, long tick) {
        onRoomCleared(player.getUuid(), room, tick);
        openNextRoom(player, room + 1);
    }

    // ── 완료 ─────────────────────────────────────────────────────────────────

    public void onDungeonCleared(UUID uuid, PlayerData data, ServerPlayerEntity player) {
        playerProgress.remove(uuid);

        if (JJKMod.getCursedStoneManager() != null) {
            JJKMod.getCursedStoneManager().give(data, 500L, "dungeon_clear", player);
        }
        if (JJKMod.getGradeManager() != null) {
            JJKMod.getGradeManager().addXp(data, 200, player);
        }
        if (JJKMod.getQuestManager() != null) {
            JJKMod.getQuestManager().progressWeekly(data, "dungeon_clear", player);
        }

        logEvent("dungeon_clear", uuid, 0L);
    }

    // ── 조회 ─────────────────────────────────────────────────────────────────

    public DungeonProgress getDungeonProgress(UUID uuid) {
        return playerProgress.get(uuid);
    }

    public boolean isTracked(UUID uuid) {
        return playerProgress.containsKey(uuid);
    }

    public void exitDungeon(UUID uuid) {
        playerProgress.remove(uuid);
    }

    public Map<UUID, DungeonProgress> getActiveProgress() {
        return Collections.unmodifiableMap(playerProgress);
    }

    /** 방 내 살아있는 CursedSpiritEntity 수 = 0 이면 클리어 */
    public boolean isRoomCleared(ServerWorld world, int room) {
        Box box = getRoomBox(room);
        if (box == null) return false;
        return world.getEntitiesByClass(CursedSpiritEntity.class, box, e -> e.isAlive()).isEmpty();
    }

    /** period=20 (1초) 주기로 호출. 방 클리어 감지 → onRoomCleared() 자동 호출. */
    public void tickDungeons(ServerWorld world, long tick) {
        if (tick % 20 != 0) return;
        for (Map.Entry<UUID, DungeonProgress> entry : new ArrayList<>(playerProgress.entrySet())) {
            UUID playerUuid = entry.getKey();
            DungeonProgress prog = entry.getValue();
            if (prog.currentRoom() >= 5) continue;
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerUuid);
            if (player == null) continue;
            if (isRoomCleared(world, prog.currentRoom())) {
                onRoomCleared(player, prog.currentRoom(), tick);
            }
        }
    }

    /** 방 내 주령 0마리 → 클리어 (순수 데이터 판정용). */
    public static boolean isRoomClearedByCount(int remainingMonsters) {
        return remainingMonsters == 0;
    }

    private static Box getRoomBox(int room) {
        return switch (room) {
            case 1 -> new Box(-10, 10, 115, 30, 40, 132);
            case 2 -> new Box(-10, 10, 132, 30, 40, 151);
            case 3 -> new Box(-10, 10, 151, 30, 40, 166);
            case 4 -> new Box(-15, 10, 166, 35, 50, 181);
            case 5 -> new Box(-10, 10, 181, 30, 40, 196);
            default -> null;
        };
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    private static BlockPos getDoorPos(int room) {
        return switch (room) {
            case 1 -> new BlockPos(10, 21, 120);
            case 2 -> new BlockPos(10, 21, 140);
            case 3 -> new BlockPos(10, 21, 160);
            case 4 -> new BlockPos(10, 21, 175);
            case 5 -> new BlockPos(10, 21, 190);
            default -> null;
        };
    }

    private static void openNextRoom(ServerPlayerEntity player, int room) {
        BlockPos door = getDoorPos(room);
        if (door == null) return;
        ServerWorld world = (ServerWorld) player.getWorld();
        world.setBlockState(door, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        world.setBlockState(door.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static void logEvent(String type, UUID uuid, long tick) {
        AuditLogger logger = JJKMod.getAuditLogger();
        if (logger != null) {
            logger.logEvent(type, uuid, String.format("{\"tick\":%d}", tick), tick);
        }
    }
}
