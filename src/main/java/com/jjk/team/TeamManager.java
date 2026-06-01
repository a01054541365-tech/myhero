package com.jjk.team;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.api.team.ITeamProvider;
import com.jjk.data.PlayerData;
import com.jjk.domain.DomainInstance;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamManager implements ITeamProvider {

    public enum Team { JUJUTSU_SORCERER, CURSED_SPIRIT, NON_SORCERER }

    private static final Set<String> CURSED_SPIRITS =
            Set.of("mahito", "jogo", "hanami", "dagon", "sukuna");
    private static final Set<String> SORCERERS =
            Set.of("gojo", "itadori", "megumi", "okkotsu", "nanami", "inumaki", "hakari", "higuruma");

    @Override
    public TeamType getTeam(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.characterId == null) return TeamType.NON_SORCERER;
        if (CURSED_SPIRITS.contains(data.characterId)) return TeamType.CURSED_SPIRIT;
        if (SORCERERS.contains(data.characterId)) return TeamType.JUJUTSU_SORCERER;
        return TeamType.NON_SORCERER;
    }

    @Override
    public boolean isEnemy(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        TeamType a = getTeam(attacker);
        TeamType t = getTeam(target);
        if (a == TeamType.NON_SORCERER || t == TeamType.NON_SORCERER) return false;
        return a != t;
    }

    /** Pure-data 같은 진영 판정 (processData 및 테스트용). */
    public boolean isSameTeam(PlayerData a, PlayerData b) {
        String ac = a.characterId;
        String bc = b.characterId;
        if (ac == null || bc == null) return false;
        boolean aSpirit   = CURSED_SPIRITS.contains(ac);
        boolean bSpirit   = CURSED_SPIRITS.contains(bc);
        boolean aSorcerer = SORCERERS.contains(ac);
        boolean bSorcerer = SORCERERS.contains(bc);
        return (aSpirit && bSpirit) || (aSorcerer && bSorcerer);
    }

    // ─── Static helpers ────────────────────────────────────────────────────────

    public static Team getTeam(String characterId) {
        if (characterId == null) return Team.NON_SORCERER;
        if (CURSED_SPIRITS.contains(characterId)) return Team.CURSED_SPIRIT;
        if (SORCERERS.contains(characterId)) return Team.JUJUTSU_SORCERER;
        return Team.NON_SORCERER;
    }

    // domain.team 필드 기반 팀별 활성 영역 수 카운트
    public static int getDomainCount(Team team, Map<UUID, DomainInstance> activeDomains) {
        int count = 0;
        for (DomainInstance d : activeDomains.values()) {
            if (team == d.team) count++;
        }
        return count;
    }

    // 등급 차이 2 이상 전투 시 XP 배율 적용
    public static float getXpMultiplier(PlayerData attacker, PlayerData target, JjkConfig config) {
        int[] GRADE_ORDER = {0, 0, 0, 0, 0, 0}; // placeholder — grade string→int mapping
        int aGrade = gradeToInt(attacker.grade);
        int tGrade = gradeToInt(target.grade);
        if (Math.abs(aGrade - tGrade) >= 2) return (float) config.xpMultiplierGradeDiff;
        return 1.0f;
    }

    private static int gradeToInt(String grade) {
        if (grade == null) return 0;
        return switch (grade) {
            case "grade_4", "4급"       -> 0;
            case "grade_3", "3급"       -> 1;
            case "grade_2", "2급"       -> 2;
            case "grade_1", "1급"       -> 3;
            case "semi_grade_1", "준특급" -> 4;
            case "special_grade", "특급" -> 5;
            default -> 0;
        };
    }
}
