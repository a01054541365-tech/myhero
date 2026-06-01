package com.jjk.test;

import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.impl.HigurumaskillSet;
import com.jjk.character.impl.InumakiSkillSet;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NOT_IMPLEMENTED 잔존 키 공식 문서화.
 * 이 테스트는 "의도적으로 미구현된 키"를 추적하는 레지스트리다.
 * 새 구현이 추가되면 이 목록에서 제거하고 실 테스트로 이동한다.
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

    /**
     * 히구루마 Shift+F (keyId=1):
     * 원작에서 히구루마의 Shift+F에 해당하는 명시적 스킬 없음.
     * 재판 StateMachine(F·Shift+R·V)으로 충분.
     * 추후 "증인 소환" 등 재판 보조 스킬 추가 예약.
     */
    @Test
    void higuruma_ShiftF_isIntentionallyEmpty() {
        ISkillSet skillSet = new HigurumaskillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.characterId = "higuruma";
        SkillResult result = skillSet.onShiftF(data, null, 0L);
        assertEquals(SkillResult.NOT_IMPLEMENTED, result,
            "히구루마 Shift+F — 원작 근거 스킬 없음, 추후 예약");
    }

    /**
     * 히구루마 R (keyId=2):
     * 동일 사유. 재판 StateMachine 외 추가 스킬 원작 미확인.
     * 추후 "판결문 소환" 등 추가 예약.
     */
    @Test
    void higuruma_R_isIntentionallyEmpty() {
        ISkillSet skillSet = new HigurumaskillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.characterId = "higuruma";
        SkillResult result = skillSet.onR(data, null, 0L);
        assertEquals(SkillResult.NOT_IMPLEMENTED, result,
            "히구루마 R — 원작 근거 스킬 없음, 추후 예약");
    }
}
