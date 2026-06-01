package com.jjk.combat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jjk.character.CharacterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TechniqueLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-technique");
    private static final Gson GSON = new Gson();

    // key: "character:keyId"
    private static final Map<String, TechniqueDefinition> CACHE = new HashMap<>();

    private static boolean loaded = false;

    public static void load(Path path) {
        CACHE.clear();
        if (!Files.exists(path)) {
            LOGGER.warn("[JJK] techniques.json not found at {}, using defaults", path);
            loaded = false;
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonArray arr = root.getAsJsonArray("techniques");
            for (int i = 0; i < arr.size(); i++) {
                TechniqueDefinition def = GSON.fromJson(arr.get(i), TechniqueDefinition.class);
                CACHE.put(def.character + ":" + def.keyId, def);
            }
            // characterStats 섹션 파싱
            if (root.has("characterStats")) {
                JsonObject statsMap = root.getAsJsonObject("characterStats");
                for (java.util.Map.Entry<String, JsonElement> entry : statsMap.entrySet()) {
                    JsonObject s = entry.getValue().getAsJsonObject();
                    CharacterRegistry.registerStats(entry.getKey(), new CharacterRegistry.CharacterStats(
                            s.get("maxCe").getAsFloat(),
                            s.get("regenOut").getAsFloat(),
                            s.get("regenIn").getAsFloat(),
                            s.get("attack").getAsInt(),
                            s.get("defense").getAsInt(),
                            s.get("hp").getAsInt()
                    ));
                }
            }
            loaded = true;
            LOGGER.info("[JJK] Loaded {} technique definitions", CACHE.size());
        } catch (Exception e) {
            LOGGER.warn("[JJK] Failed to load techniques.json: {}", e.getMessage());
            loaded = false;
        }
    }

    public static TechniqueDefinition get(String characterId, int keyId) {
        return CACHE.get(characterId + ":" + keyId);
    }

    public static java.util.List<TechniqueDefinition> getAllForCharacter(String characterId) {
        java.util.List<TechniqueDefinition> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, TechniqueDefinition> e : CACHE.entrySet()) {
            if (e.getKey().startsWith(characterId + ":")) result.add(e.getValue());
        }
        return result;
    }

    public static float getBaseDamage(String characterId, int keyId) {
        TechniqueDefinition def = get(characterId, keyId);
        return def != null ? def.baseDamage : 20f;
    }

    public static float getCeCost(String characterId, int keyId) {
        TechniqueDefinition def = get(characterId, keyId);
        return def != null ? def.ceCost : 10f;
    }

    public static float getRange(String characterId, int keyId) {
        return 6.0f;
    }

    public static long getCooldownTicks(String characterId, int keyId) {
        TechniqueDefinition def = get(characterId, keyId);
        return def != null ? def.cooldownTicks : 20L;
    }

    public static boolean isContinuous(String characterId, int keyId) {
        TechniqueDefinition def = get(characterId, keyId);
        return def != null && def.isContinuous;
    }

    public static float getCePerTick(String characterId, int keyId) {
        TechniqueDefinition def = get(characterId, keyId);
        return def != null ? def.cePerTick : 0f;
    }
}
