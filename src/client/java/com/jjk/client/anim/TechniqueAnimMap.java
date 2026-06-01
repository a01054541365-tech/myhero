package com.jjk.client.anim;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.HashMap;
import java.util.Map;

/** (characterId, keyId) → animId 매핑. spec §11-5 기반. */
@Environment(EnvType.CLIENT)
public final class TechniqueAnimMap {
    private TechniqueAnimMap() {}

    private static final Map<String, Integer> MAP = new HashMap<>(32);

    static {
        MAP.put("gojo:0",    15);
        MAP.put("gojo:1",    16);
        MAP.put("gojo:2",    17);
        MAP.put("gojo:3",    18);
        MAP.put("gojo:4",    20);
        MAP.put("itadori:0", 10);
        MAP.put("itadori:1", 11);
        MAP.put("itadori:2", 12);
        MAP.put("itadori:3", 13);
        MAP.put("itadori:4", 14);
        MAP.put("megumi:0",  25);
        MAP.put("megumi:1",  26);
        MAP.put("megumi:2",  27);
        MAP.put("megumi:3",  28);
        MAP.put("megumi:4",  29);
        MAP.put("okkotsu:0",  1);
        MAP.put("okkotsu:1", 40);
        MAP.put("okkotsu:2",  1);
        MAP.put("okkotsu:3", 41);
        MAP.put("okkotsu:4", 42);
        MAP.put("sukuna:0",  21);
        MAP.put("sukuna:1",  22);
        MAP.put("sukuna:2",  23);
        MAP.put("sukuna:3",  24);
        MAP.put("sukuna:4",   0);
    }

    /** 없으면 1(공통 cast) 반환 */
    public static int getAnimId(String characterId, int keyId) {
        if (characterId == null) return 1;
        return MAP.getOrDefault(characterId + ":" + keyId, 1);
    }
}
