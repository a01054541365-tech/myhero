package com.jjk.test;

import com.jjk.JJKMod;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import com.jjk.economy.CursedStoneManager;
import com.jjk.quest.QuestDef;
import com.jjk.quest.QuestManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyQuestTest {

    private PlayerRepository repo;
    private CursedStoneManager csm;
    private QuestManager qm;

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        repo = new PlayerRepository(conn);
        csm = new CursedStoneManager(repo);
        JJKMod.initForTest(csm, repo);
        qm = JJKMod.getQuestManager();
    }

    private PlayerData newPlayer() {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = "itadori";
        return d;
    }

    @Test
    void weeklyQuest_assignment_fixed_by_uuid_and_week() {
        int week = 100;
        PlayerData d = newPlayer();
        QuestDef q1 = qm.getWeeklyQuest(d.uuid, week);
        QuestDef q2 = qm.getWeeklyQuest(d.uuid, week);
        assertEquals(q1.questId(), q2.questId(), "같은 UUID+주 → 동일 퀘스트");
        assertNotNull(q1.type(), "퀘스트 타입 null 아님");
    }

    @Test
    void weeklyQuest_typeMismatch_noProgress() {
        PlayerData d = newPlayer();
        d.weeklyQuestDone = -1L;
        long stonesInit = d.cursedStones;

        // 존재하지 않는 타입 → 진행 없음
        qm.progressWeekly(d, "nonexistent_type_xyz", null);
        assertEquals(-1L, d.weeklyQuestDone, "타입 불일치 → weeklyQuestDone 변화 없음");
        assertEquals(stonesInit, d.cursedStones, "타입 불일치 → 보상 없음");
    }

    @Test
    void weeklyQuest_alreadyCompleted_noProgress() {
        PlayerData d = newPlayer();
        int week = QuestManager.currentEpochWeek();
        d.weeklyQuestDone = week;
        long stonesBefore = d.cursedStones;

        qm.progressWeekly(d, "kill_special", null);
        qm.progressWeekly(d, "grade_up", null);
        qm.progressWeekly(d, "ce_spend", null);

        assertEquals(stonesBefore, d.cursedStones, "완료된 주 → 재진행 보상 없음");
        assertEquals(week, d.weeklyQuestDone, "weeklyQuestDone 변화 없음");
    }

    @Test
    void weeklyQuest_completion_storesEpochWeek() {
        PlayerData d = newPlayer();
        d.weeklyQuestDone = -1L;
        int thisWeek = QuestManager.currentEpochWeek();

        QuestDef quest = qm.getWeeklyQuest(d.uuid, thisWeek);
        if ("ce_spend".equals(quest.type())) {
            d.cooldowns.put("ce_spend_total", (long) quest.target());
            qm.progressWeekly(d, "ce_spend", null);
        } else {
            // target - 1 선설정 후 마지막 1회
            d.cooldowns.put("quest_weekly_prog", (long)(quest.target() - 1));
            qm.progressWeekly(d, quest.type(), null);
        }

        assertEquals(thisWeek, d.weeklyQuestDone, "완료 시 weeklyQuestDone = thisWeek");
    }

    @Test
    void weeklyQuest_reward_given_on_completion() {
        PlayerData d = newPlayer();
        d.weeklyQuestDone = -1L;
        int thisWeek = QuestManager.currentEpochWeek();
        QuestDef quest = qm.getWeeklyQuest(d.uuid, thisWeek);

        long stonesBefore = d.cursedStones;

        if ("ce_spend".equals(quest.type())) {
            d.cooldowns.put("ce_spend_total", (long) quest.target());
            qm.progressWeekly(d, "ce_spend", null);
        } else {
            d.cooldowns.put("quest_weekly_prog", (long)(quest.target() - 1));
            qm.progressWeekly(d, quest.type(), null);
        }

        assertTrue(d.cursedStones >= stonesBefore + quest.stoneReward(),
            "완료 시 stoneReward 지급됨");
    }

    @Test
    void weeklyQuestDone_persists_on_save_reload() throws Exception {
        PlayerData d = newPlayer();
        int week = QuestManager.currentEpochWeek();
        d.weeklyQuestDone = week;
        repo.saveImmediate(d);

        PlayerData loaded = repo.load(d.uuid);
        assertEquals(week, loaded.weeklyQuestDone, "weeklyQuestDone 저장/재로드 유지");
    }
}
