package com.jjk.team;

import com.jjk.JJKMod;
import com.jjk.api.team.ITeamProvider;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

public class TeamManager implements ITeamProvider {

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
}
