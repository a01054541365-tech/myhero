package com.jjk.test;

import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import com.jjk.economy.CursedStoneManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CursedStoneManagerTest {

    private CursedStoneManager csm;
    private PlayerData data;

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        PlayerRepository repo = new PlayerRepository(conn);
        csm = new CursedStoneManager(repo);
        data = PlayerData.createDefault(UUID.randomUUID());
    }

    @Test
    void give_increasesStones() {
        csm.give(data, 100L, "test", null);
        assertEquals(100L, data.cursedStones, "give(100) → cursedStones == 100");
    }

    @Test
    void spend_sufficient_decreasesAndReturnsTrue() {
        data.cursedStones = 200L;
        boolean result = csm.spend(data, 50L, "test", null);
        assertTrue(result, "잔액 충분 → true");
        assertEquals(150L, data.cursedStones, "cursedStones -= 50");
    }

    @Test
    void spend_insufficient_returnsFalse_noChange() {
        data.cursedStones = 100L;
        boolean result = csm.spend(data, 200L, "test", null);
        assertFalse(result, "잔액 부족 → false");
        assertEquals(100L, data.cursedStones, "cursedStones 변화 없음");
    }

    @Test
    void addBounty_increasesBounty() {
        csm.addBounty(data, 50L);
        assertEquals(50L, data.bounty, "addBounty(50) → bounty == 50");
    }

    @Test
    void settleBounty_transfersToCursedStones() {
        data.bounty = 100L;
        data.cursedStones = 0L;
        long settled = csm.settleBounty(data, null);
        assertEquals(100L, settled, "정산량 == 이전 bounty");
        assertEquals(100L, data.cursedStones, "bounty → cursedStones");
        assertEquals(0L, data.bounty, "bounty == 0");
    }

    @Test
    void give_negativeAmount_noChange() {
        data.cursedStones = 50L;
        csm.give(data, -10L, "test", null);
        assertEquals(50L, data.cursedStones, "음수 give → 변화 없음");
    }
}
