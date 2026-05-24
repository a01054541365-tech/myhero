package com.jjk;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class JjkConfig {

    public boolean mangaExpEnabled = false;
    public boolean allowDuplicateCharacter = false;
    public boolean gradePvpScaling = true;
    public double pvpDamageCapMaxHpRatio = 0.40;
    public double trialSuccessRate = 0.60;
    public boolean bindingVowBreakBySpecialGradeHit = true;
    public int bindingVowTimeoutTicks = 300;
    public int jackpotDurationTicks = 251;
    public int jackpotDurationMinTicks = 60;
    public int jackpotDurationMaxTicks = 251;
    public int respawnDelayTicks = 100;
    public String respawnLocation = "SPAWN";
    public double respawnCePercent = 0.50;
    public double respawnHpPercent = 0.50;
    public double fingerDropRate = 0.10;
    public int fingerMaxCount = 20;
    public int blackFlashBaseRate = 5;
    public int blackFlashZoneBonus = 10;
    public double xpMultiplierGradeDiff = 1.5;
    public boolean allowCharacterReselect = false;
    public int rikaLifetimeTicks = 200;
    public int maharagaThreshold = 5;
    public int sealDurationTicks = 600;
    public int zoneDurationTicks = 200;
    public double ceRegenOutOfCombat = 1.0;
    public double ceRegenInCombat = 0.2;

    private static final Gson GSON = new Gson();

    public static JjkConfig load() {
        Path path = Path.of("config/jjk/config.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, JjkConfig.class);
        } catch (IOException e) {
            return new JjkConfig();
        }
    }
}
