package com.jjk.test;

import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.impl.HigurumaSkillSet;
import com.jjk.character.impl.InumakiSkillSet;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NOT_IMPLEMENTED 잔존 키 공식 문서화.
 * 이 테스트는 "의도적으로 미구현된 키"를 추적하는 레지스트리다.
 * 새 구현이 추가되면 이 목록에서 제거하고 실 테스트로 이동한다.
 *
 * 히구루마 Shift+F·R → TASK-102에서 구현 완료, 이 목록에서 제거됨.
 */
class NotImplementedKeyDocTest {

    /**
     * 이누마키 R (keyId=2):
     * 원작에서 R 슬롯에 해당하는 전투 스킬 없음.
     * 주언(呪言) 계열은 F/Shift+F/Shift+R/V 4개로 충분.
     * 의도적 공백 — 추후 "공명(共鳴)" 등 원작 2부 스킬 추가 예약.
     */
    @Test
    void inumaki_R_isIntentionallyEmpty() {
        ISkillSet skillSet = new InumakiSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.characterId = "inumaki";
        SkillResult result = skillSet.onR(data, null, 0L);
        assertEquals(SkillResult.NOT_IMPLEMENTED, result,
            "이누마키 R — 원작 근거 스킬 없음, 추후 예약");
    }
}
