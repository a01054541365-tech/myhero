package com.jjk.test;

import com.jjk.JJKMod;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import com.jjk.dungeon.DungeonManager;
import com.jjk.economy.CursedStoneManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DungeonManagerTest {

    private PlayerRepository repo;
    private CursedStoneManager csm;

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        repo = new PlayerRepository(conn);
        csm = new CursedStoneManager(repo);
        JJKMod.initForTest(csm, repo);
    }

    @Test
    void enterDungeon_registersProgress_room0() {
        DungeonManager mgr = new DungeonManager();
        UUID uuid = UUID.randomUUID();
        mgr.enterDungeon(uuid, 100L);

        DungeonManager.DungeonProgress p = mgr.getDungeonProgress(uuid);
        assertNotNull(p, "진행도 등록됨");
        assertEquals(0, p.currentRoom(), "입장 시 방 = 0");
        assertEquals(100L, p.enteredTick(), "입장 틱 기록");
    }

    @Test
    void onRoomCleared_advancesRoom() {
        DungeonManager mgr = new DungeonManager();
        UUID uuid = UUID.randomUUID();
        mgr.enterDungeon(uuid, 0L);

        mgr.onRoomCleared(uuid, 1, 100L);

        assertEquals(2, mgr.getDungeonProgress(uuid).currentRoom(), "방 1 클리어 → 방 2");
    }

    @Test
    void isRoomCleared_byCount() {
        assertTrue(DungeonManager.isRoomClearedByCount(0),  "주령 0마리 → 클리어");
        assertFalse(DungeonManager.isRoomClearedByCount(1), "주령 1마리 → 미클리어");
        assertFalse(DungeonManager.isRoomClearedByCount(4), "주령 4마리 → 미클리어");
    }

    @Test
    void onDungeonCleared_givesReward() {
        DungeonManager mgr = new DungeonManager();
        UUID uuid = UUID.randomUUID();
        PlayerData data = PlayerData.createDefault(uuid);
        data.characterId = "itadori";
        mgr.enterDungeon(uuid, 0L);

        long stonesBefore = data.cursedStones;

        mgr.onDungeonCleared(uuid, data, null);

        assertEquals(stonesBefore + 500L, data.cursedStones, "던전 클리어 → 500석 지급");
        assertFalse(mgr.isTracked(uuid), "클리어 후 진행도 제거");
    }

    @Test
    void exitDungeon_removesTracking() {
        DungeonManager mgr = new DungeonManager();
        UUID uuid = UUID.randomUUID();
        mgr.enterDungeon(uuid, 0L);

        assertTrue(mgr.isTracked(uuid), "입장 중 추적됨");

        mgr.exitDungeon(uuid);
        assertFalse(mgr.isTracked(uuid), "퇴장 후 미추적");
    }

    @Test
    void enterDungeon_overwritesExisting() {
        DungeonManager mgr = new DungeonManager();
        UUID uuid = UUID.randomUUID();
        mgr.enterDungeon(uuid, 0L);
        mgr.onRoomCleared(uuid, 1, 10L); // room → 2

        // 재입장
        mgr.enterDungeon(uuid, 200L);
        DungeonManager.DungeonProgress p = mgr.getDungeonProgress(uuid);
        assertEquals(0, p.currentRoom(), "재입장 → 방 초기화");
        assertEquals(200L, p.enteredTick(), "재입장 틱 갱신");
    }

    @Test
    void getActiveProgress_reflectsState() {
        DungeonManager mgr = new DungeonManager();
        UUID uuid = UUID.randomUUID();
        mgr.enterDungeon(uuid, 100L);

        var progress = mgr.getActiveProgress();
        assertEquals(1, progress.size(), "입장 후 size=1");
        assertTrue(progress.containsKey(uuid), "입장한 UUID 존재");
        assertEquals(100L, progress.get(uuid).enteredTick(), "진입 틱 일치");
    }

    @Test
    void getActiveProgress_isUnmodifiable() {
        DungeonManager mgr = new DungeonManager();
        UUID uuid = UUID.randomUUID();
        mgr.enterDungeon(uuid, 0L);

        var progress = mgr.getActiveProgress();
        assertThrows(UnsupportedOperationException.class,
            () -> progress.remove(uuid),
            "getActiveProgress() 반환값은 수정 불가");
    }
}
