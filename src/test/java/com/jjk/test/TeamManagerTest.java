package com.jjk.test;

import com.jjk.data.PlayerData;
import com.jjk.domain.DomainInstance;
import com.jjk.team.TeamManager;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamManagerTest {

    private static PlayerData player(String characterId) {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = characterId;
        return d;
    }

    // 1. gojo(JUJUTSU_SORCERER) vs itadori(JUJUTSU_SORCERER) → isSameTeam=true
    @Test
    void testSameTeamAttackBlocked() {
        PlayerData gojo    = player("gojo");
        PlayerData itadori = player("itadori");
        TeamManager mgr = new TeamManager();
        assertTrue(mgr.isSameTeam(gojo, itadori), "고죠 vs 이타도리 → 같은 진영");
    }

    // 2. gojo(JUJUTSU_SORCERER) vs sukuna(CURSED_SPIRIT) → isSameTeam=false
    @Test
    void testCrossTeamAttackAllowed() {
        PlayerData gojo   = player("gojo");
        PlayerData sukuna = player("sukuna");
        TeamManager mgr = new TeamManager();
        assertFalse(mgr.isSameTeam(gojo, sukuna), "고죠 vs 스쿠나 → 다른 진영");
    }

    // 3. 캐릭터별 진영 배정 확인
    @Test
    void testTeamAssignment() {
        assertEquals(TeamManager.Team.JUJUTSU_SORCERER, TeamManager.getTeam("gojo"),    "고죠 = 주술사");
        assertEquals(TeamManager.Team.CURSED_SPIRIT,    TeamManager.getTeam("mahito"),   "마히토 = 주술적");
        assertEquals(TeamManager.Team.CURSED_SPIRIT,    TeamManager.getTeam("sukuna"),   "스쿠나 = 주술적");
        assertEquals(TeamManager.Team.JUJUTSU_SORCERER, TeamManager.getTeam("megumi"),   "메구미 = 주술사");
        assertEquals(TeamManager.Team.NON_SORCERER,     TeamManager.getTeam((String) null), "null = 비주술사");
        assertEquals(TeamManager.Team.NON_SORCERER,     TeamManager.getTeam("unknown"),  "미등록 = 비주술사");
    }

    // 4. 팀별 활성 영역 수 카운트 정확성
    @Test
    void testDomainCountPerTeam() {
        Map<UUID, DomainInstance> domains = new HashMap<>();

        // 주술사 팀 영역 2개
        DomainInstance d1 = new DomainInstance(UUID.randomUUID(), "gojo_unlimited_void",
                new BlockPos(0, 0, 0), 1500f, 20f, false, false, true, false);
        d1.team = TeamManager.Team.JUJUTSU_SORCERER;

        DomainInstance d2 = new DomainInstance(UUID.randomUUID(), "itadori_unnamed",
                new BlockPos(100, 0, 0), 1500f, 20f, false, false, true, false);
        d2.team = TeamManager.Team.JUJUTSU_SORCERER;

        domains.put(d1.instanceId, d1);
        domains.put(d2.instanceId, d2);

        assertEquals(2, TeamManager.getDomainCount(TeamManager.Team.JUJUTSU_SORCERER, domains),
                "주술사 팀 영역 2개");
        assertEquals(0, TeamManager.getDomainCount(TeamManager.Team.CURSED_SPIRIT, domains),
                "저주영 팀 영역 0개");
    }
}
