package com.jjk.api.team;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ITeamProvider {
    TeamType getTeam(ServerPlayerEntity player);
    boolean isEnemy(ServerPlayerEntity attacker, ServerPlayerEntity target);

    enum TeamType {
        JUJUTSU_SORCERER,
        CURSED_SPIRIT,
        NON_SORCERER
    }
}
