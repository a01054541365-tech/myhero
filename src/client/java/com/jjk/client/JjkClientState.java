package com.jjk.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** 로컬 플레이어의 서버 사이드 상태 캐시 (CharacterInfoS2CPacket으로 갱신). */
@Environment(EnvType.CLIENT)
public final class JjkClientState {
    private JjkClientState() {}

    private static String characterId = null;
    private static String grade = null;
    private static float ceMax = 100f;
    private static float ceCurrent = 100f;

    public static void update(String charId, String gr, float max, float current) {
        characterId = charId;
        grade = gr;
        ceMax = max;
        ceCurrent = current;
    }

    public static void setCharacterId(String charId) {
        characterId = charId;
    }

    public static String getCharacterId() { return characterId; }
    public static String getGrade()       { return grade; }
    public static float  getCeMax()       { return ceMax; }
    public static float  getCeCurrent()   { return ceCurrent; }
}
