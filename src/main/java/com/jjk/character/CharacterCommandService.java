package com.jjk.character;

import com.jjk.JJKMod;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.PlayerData;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.Map;

public class CharacterCommandService {

    private static final Identifier ITADORI_JUMP_MODIFIER_ID =
            Identifier.of("jjk", "itadori_jump_boost");

    // grade_4 < grade_3 < grade_2 < grade_1 < semi_grade_1 < special_grade
    private static final Map<String, Integer> GRADE_ORDER = Map.of(
            "grade_4", 0, "grade_3", 1, "grade_2", 2,
            "grade_1", 3, "semi_grade_1", 4, "special_grade", 5
    );

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

        // grade check (§26-2): player's current grade must meet the character's minimum
        CharacterRegistry.CharacterMeta meta = CharacterRegistry.get(characterId);
        int requiredRank = GRADE_ORDER.getOrDefault(meta.defaultGrade(), 0);
        int playerRank = data.grade != null ? GRADE_ORDER.getOrDefault(data.grade, 0) : 0;
        if (playerRank < requiredRank) {
            return SelectResult.GRADE_INSUFFICIENT;
        }

        data.characterId = characterId;
        data.grade = meta.defaultGrade();

        CharacterRegistry.CharacterStats stats = CharacterRegistry.getStats(characterId);
        if (stats != null) {
            data.ceMax      = stats.maxCe();
            data.ceCurrent  = stats.maxCe();
            data.hpMax      = stats.hp();
            data.hpCurrent  = stats.hp();
            data.attackStat = stats.attack();
            data.defenseStat = stats.defense();
        } else {
            data.ceMax = meta.ceMax();
            data.ceCurrent = meta.ceMax();
            data.hpMax = 20f;
            data.hpCurrent = 20f;
        }
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(data.hpMax);
        player.setHealth(data.hpMax);

        if ("itadori".equals(characterId)) {
            // 이타도리 점프력 버프 유지 (체력은 stats에서 이미 설정)
            var jumpAttr = player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
            if (jumpAttr != null) {
                jumpAttr.removeModifier(ITADORI_JUMP_MODIFIER_ID);
                jumpAttr.addPersistentModifier(new EntityAttributeModifier(
                        ITADORI_JUMP_MODIFIER_ID,
                        0.30,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }
        }

        JJKMod.getPlayerRepository().saveImmediate(data);
        return SelectResult.OK;
    }

    // §23: allowDuplicateCharacter=false → block if any other online player already holds this character
    private boolean isDuplicate(String characterId, ServerPlayerEntity requester) {
        return JJKMod.getServer().getPlayerManager().getPlayerList().stream()
                .filter(p -> !p.getUuid().equals(requester.getUuid()))
                .map(p -> JJKMod.getPlayerRepository().load(p.getUuid()))
                .anyMatch(d -> characterId.equals(d.characterId));
    }
}
