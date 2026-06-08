package com.jjk.character;

import com.jjk.JJKMod;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.PlayerData;
import com.jjk.item.JJKItems;
import com.jjk.network.s2c.OpenCharacterSelectS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
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

    /**
     * 신규 플레이어 접속 처리. 선택 책 미지급 시 책 지급 + 캐릭터 선택 화면 전송.
     * @return true if selection screen was sent (caller should not send CharacterInfo/CharacterSelect)
     */
    public boolean handleJoin(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.hasReceivedSelectionBook) {
            return false;
        }

        if (JJKItems.CHARACTER_SELECTION_BOOK != null) {
            ItemStack book = new ItemStack(JJKItems.CHARACTER_SELECTION_BOOK);
            if (!player.getInventory().insertStack(book)) {
                player.dropItem(book, false);
            }
        }
        data.hasReceivedSelectionBook = true;

        ServerPlayNetworking.send(player, new OpenCharacterSelectS2CPacket(
                new ArrayList<>(CharacterRegistry.ids()),
                data.characterId != null ? data.characterId : ""));

        JJKMod.getPlayerRepository().saveImmediate(data);
        return true;
    }

    public SelectResult select(ServerPlayerEntity player, String characterId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());

        boolean isReselect = (data.characterId != null);

        if (isReselect && !JJKMod.getConfig().allowCharacterReselect) {
            return SelectResult.RESELECT_DISABLED;
        }

        if (!JJKMod.getConfig().allowDuplicateCharacter && isDuplicate(characterId, player)) {
            return SelectResult.DUPLICATE_BLOCKED;
        }

        CharacterRegistry.CharacterMeta meta = CharacterRegistry.get(characterId);

        // grade check (§26-2): skip on first selection (characterId == null)
        if (isReselect) {
            int requiredRank = GRADE_ORDER.getOrDefault(meta.defaultGrade(), 0);
            int playerRank   = GRADE_ORDER.getOrDefault(data.grade, 0);
            if (playerRank < requiredRank) {
                return SelectResult.GRADE_INSUFFICIENT;
            }
            // 재선택 시 전체 초기화
            data.grade                       = "4급";
            data.xp                          = 0;
            data.mastery                     = 0;
            data.unlockedSkills.clear();
            data.cooldowns.clear();
            data.characterResetCount        += 1;
            data.lastCharacterResetTimestamp = System.currentTimeMillis();
            data.domainCooldownUntil         = 0L;
            data.awakeningCooldownUntil      = 0L;
            data.jackpotCooldownUntil        = 0L;
            data.curtainCooldownUntil        = 0L;
        } else {
            data.grade = meta.defaultGrade();
        }

        data.characterId = characterId;
        AdvancementTriggerManager.onCharacterTried(player, characterId);

        CharacterRegistry.CharacterStats stats = CharacterRegistry.getStats(characterId);
        if (stats != null) {
            data.ceMax       = stats.maxCe();
            data.ceCurrent   = stats.maxCe();
            data.hpMax       = stats.hp();
            data.hpCurrent   = stats.hp();
            data.attackStat  = stats.attack();
            data.defenseStat = stats.defense();
        } else {
            data.ceMax     = meta.ceMax();
            data.ceCurrent = meta.ceMax();
            data.hpMax     = 20f;
            data.hpCurrent = 20f;
        }
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(data.hpMax);
        player.setHealth(data.hpMax);

        if ("itadori".equals(characterId)) {
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
