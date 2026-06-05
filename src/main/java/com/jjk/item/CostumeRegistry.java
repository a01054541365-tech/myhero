package com.jjk.item;

import java.util.Map;

public final class CostumeRegistry {
    private CostumeRegistry() {}

    // costume_id → required characterId (null = 모든 캐릭터 허용)
    private static final Map<String, String> COSTUME_CHARACTER_MAP = Map.of(
        "gojo_blindfold",    "gojo",
        "gojo_sunglasses",   "gojo",
        "sukuna_tattooed",   "sukuna",
        "itadori_uniform",   "itadori",
        "nanami_suit",       "nanami",
        "cursed_spirit_red", null,
        "cursed_spirit_blue",null
    );

    public static boolean isValid(String costumeId) {
        return "default".equals(costumeId) || COSTUME_CHARACTER_MAP.containsKey(costumeId);
    }

    public static boolean isCompatible(String costumeId, String characterId) {
        if ("default".equals(costumeId)) return true;
        String required = COSTUME_CHARACTER_MAP.get(costumeId);
        return required == null || required.equals(characterId);
    }
}
