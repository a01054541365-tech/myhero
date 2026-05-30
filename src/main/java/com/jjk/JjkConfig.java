package com.jjk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JjkConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-config");
    private static final Gson GSON = new Gson();

    private static final String[] KNOWN_KEYS = {
        "mangaExpEnabled", "allowDuplicateCharacter", "gradePvpScaling",
        "pvpDamageCapMaxHpRatio", "trialSuccessRate", "bindingVowBreakBySpecialGradeHit",
        "bindingVowTimeoutTicks", "domainBannedChunks", "jackpotDurationTicks",
        "jackpotDurationMinTicks", "jackpotDurationMaxTicks", "respawnDelayTicks",
        "respawnLocation", "respawnCePercent", "respawnHpPercent", "fingerDropRate",
        "fingerMaxCount", "blackFlashBaseRate", "blackFlashZoneBonus",
        "xpMultiplierGradeDiff", "allowCharacterReselect", "rikaLifetimeTicks",
        "maharagaThreshold", "sealDurationTicks", "zoneDurationTicks",
        "ceRegenOutOfCombat", "ceRegenInCombat"
    };

    public boolean mangaExpEnabled                 = false;
    public boolean allowDuplicateCharacter         = false;
    public boolean gradePvpScaling                 = true;
    public float   pvpDamageCapMaxHpRatio          = 0.40f;  // §LOCK
    public float   trialSuccessRate                = 0.60f;  // §LOCK
    public boolean bindingVowBreakBySpecialGradeHit = true;
    public int     bindingVowTimeoutTicks          = 300;    // §LOCK
    public List<String> domainBannedChunks         = new ArrayList<>();
    public int     jackpotDurationTicks            = 251;    // §LOCK
    public int     jackpotDurationMinTicks         = 60;
    public int     jackpotDurationMaxTicks         = 251;    // §LOCK
    public int     respawnDelayTicks               = 100;
    public String  respawnLocation                 = "SPAWN";
    public float   respawnCePercent                = 0.50f;
    public float   respawnHpPercent                = 0.50f;
    public float   fingerDropRate                  = 0.10f;  // §LOCK
    public int     fingerMaxCount                  = 20;     // §LOCK
    public int     blackFlashBaseRate              = 5;      // §LOCK
    public int     blackFlashZoneBonus             = 10;     // §LOCK
    public float   xpMultiplierGradeDiff           = 1.5f;
    public boolean allowCharacterReselect          = false;
    public int     rikaLifetimeTicks               = 200;    // §LOCK
    public int     maharagaThreshold               = 2;      // decisions §2-1
    public int     sealDurationTicks               = 400;    // decisions §2-2
    public int     zoneDurationTicks               = 300;    // decisions §2-3
    public float   ceRegenOutOfCombat              = 1.0f;   // decisions §2-4
    public float   ceRegenInCombat                 = 0.2f;   // decisions §2-4

    public static JjkConfig load(Path configPath) {
        if (!Files.exists(configPath)) {
            LOGGER.warn("[JJK] config.json not found at {}, using defaults", configPath);
            return new JjkConfig();
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            for (String key : KNOWN_KEYS) {
                if (!obj.has(key)) {
                    LOGGER.warn("[JJK] config.json missing key '{}', using default", key);
                }
            }
            return GSON.fromJson(obj, JjkConfig.class);
        } catch (Exception e) {
            LOGGER.warn("[JJK] Failed to load config.json at {}, using defaults: {}", configPath, e.getMessage());
            return new JjkConfig();
        }
    }

    public void reload(Path configPath) {
        JjkConfig fresh = load(configPath);
        this.mangaExpEnabled                  = fresh.mangaExpEnabled;
        this.allowDuplicateCharacter          = fresh.allowDuplicateCharacter;
        this.gradePvpScaling                  = fresh.gradePvpScaling;
        this.pvpDamageCapMaxHpRatio           = fresh.pvpDamageCapMaxHpRatio;
        this.trialSuccessRate                 = fresh.trialSuccessRate;
        this.bindingVowBreakBySpecialGradeHit = fresh.bindingVowBreakBySpecialGradeHit;
        this.bindingVowTimeoutTicks           = fresh.bindingVowTimeoutTicks;
        this.domainBannedChunks               = fresh.domainBannedChunks;
        this.jackpotDurationTicks             = fresh.jackpotDurationTicks;
        this.jackpotDurationMinTicks          = fresh.jackpotDurationMinTicks;
        this.jackpotDurationMaxTicks          = fresh.jackpotDurationMaxTicks;
        this.respawnDelayTicks                = fresh.respawnDelayTicks;
        this.respawnLocation                  = fresh.respawnLocation;
        this.respawnCePercent                 = fresh.respawnCePercent;
        this.respawnHpPercent                 = fresh.respawnHpPercent;
        this.fingerDropRate                   = fresh.fingerDropRate;
        this.fingerMaxCount                   = fresh.fingerMaxCount;
        this.blackFlashBaseRate               = fresh.blackFlashBaseRate;
        this.blackFlashZoneBonus              = fresh.blackFlashZoneBonus;
        this.xpMultiplierGradeDiff            = fresh.xpMultiplierGradeDiff;
        this.allowCharacterReselect           = fresh.allowCharacterReselect;
        this.rikaLifetimeTicks                = fresh.rikaLifetimeTicks;
        this.maharagaThreshold                = fresh.maharagaThreshold;
        this.sealDurationTicks                = fresh.sealDurationTicks;
        this.zoneDurationTicks                = fresh.zoneDurationTicks;
        this.ceRegenOutOfCombat               = fresh.ceRegenOutOfCombat;
        this.ceRegenInCombat                  = fresh.ceRegenInCombat;
    }

    // Backward-compatible no-arg load: uses Minecraft server working directory
    public static JjkConfig load() {
        return load(Path.of("config/jjk/config.json"));
    }

    public int getJackpotDurationTicks() { return jackpotDurationTicks; }
    public float getTrialSuccessRate()   { return trialSuccessRate; }
}
