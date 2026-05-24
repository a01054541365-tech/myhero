package com.jjk.character;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public class CharacterCommandService {

    public enum SelectResult {
        OK, DUPLICATE_BLOCKED, GRADE_INSUFFICIENT, ALREADY_SELECTED, RESELECT_DISABLED
    }

    public SelectResult select(ServerPlayerEntity player, String characterId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());

        if (data.characterId != null && !JJKMod.getConfig().allowCharacterReselect) {
            return SelectResult.RESELECT_DISABLED;
        }

        if (!JJKMod.getConfig().allowDuplicateCharacter && isDuplicate(characterId, player)) {
            return SelectResult.DUPLICATE_BLOCKED;
        }

        // TODO: grade check
        data.characterId = characterId;
        JJKMod.getPlayerRepository().saveImmediate(data);
        return SelectResult.OK;
    }

    private boolean isDuplicate(String characterId, ServerPlayerEntity requester) {
        // TODO: check all online players
        return false;
    }
}
