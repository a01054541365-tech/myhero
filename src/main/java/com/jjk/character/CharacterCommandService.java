package com.jjk.character;

import com.jjk.JJKMod;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.PlayerData;
import net.minecraft.entity.attribute.EntityAttributes;
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
        CharacterRegistry.CharacterMeta meta = CharacterRegistry.get(characterId);
        data.grade = meta.defaultGrade();
        data.ceMax = meta.ceMax();
        data.ceCurrent = meta.ceMax();
        data.hpMax = 200f;
        data.hpCurrent = 200f;
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(data.hpMax);
        player.setHealth(data.hpMax);

        if ("itadori".equals(characterId)) {
            data.hpMax = 200f * 1.20f;
            data.hpCurrent = data.hpMax;
            player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(data.hpMax);
            data.defenseStat += 15;
            player.setHealth(data.hpMax);
        }

        JJKMod.getPlayerRepository().saveImmediate(data);
        return SelectResult.OK;
    }

    private boolean isDuplicate(String characterId, ServerPlayerEntity requester) {
        // TODO: check all online players
        return false;
    }
}
