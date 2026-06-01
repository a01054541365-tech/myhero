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
        "ceRegenOutOfCombat", "ceRegenInCombat",
        "zoneEntryBlackFlashCount", "zoneStackable",
        "maharagaTimeoutTicks", "shadowMarkerLifetimeTicks", "pveGradeMultiplier",
        "xpGrade4to3", "xpGrade3to2", "xpGrade2to1", "xpGrade1toSemi", "xpGradeSemiToSpecial",
        "xpOnKill", "xpOnDamagePerHit", "xpOnDamageCapPerCombat",
        "xpOnBlackFlash", "xpOnPerfect", "xpOnDailyLogin",
        "chantMaxTicks", "chantMaxMultiplier", "chantCeDrainRatio",
        "curtainBasicCeCost", "curtainBasicCePerTick", "curtainBasicDurationTicks",
        "curtainBasicRadius", "curtainBasicCooldownTicks",
        "curtainSpecialCeCost", "curtainSpecialCePerTick", "curtainSpecialDurationTicks",
        "curtainSpecialRadius", "curtainSpecialCooldownTicks",
        "blackFlashLowHpBonus", "blackFlashZoneAtkBonus", "blackFlashZoneSkillBonus",
        "fingerStatBonusPercent", "awakeningHpThreshold", "awakeningMultiplier",
        "shieldCeDrainRatio", "shieldDamageReduction",
        "simpleBarrierCostActivate", "simpleBarrierCostPerSecond",
        "simpleBarrierSureHitNegate", "simpleBarrierAllyBonus",
        "fallingBlossomSureHitBlock", "fallingBlossomCeDrain",
        "reverseHealSelfPerTick", "reverseCeDrainSelfRatio",
        "reverseHealOtherPerTick", "reverseCeDrainOtherRatio",
        "tenShadowsBodyBonus",
        "burdenDecayOutOfCombat", "burdenDecayInCombat", "burdenSealThreshold",
        "cursedToolAttackBonus_dagger", "cursedToolAttackBonus_spear",
        "cursedToolAttackBonus_cloud", "cursedToolAttackBonus_inverted",
        "cursedToolAttackBonus_soul", "cursedToolCeReduction_spear",
        "cursedToolRangeBonus_cloud", "cursedToolDefPenetration_inverted",
        "cursedToolBlackFlashBonus_soul", "cursedToolSealCooldown_inverted"
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
    public int     zoneEntryBlackFlashCount        = 1;      // 흑섬 1회 즉시 진입
    public boolean zoneStackable                   = false;  // 존 중 추가 흑섬 발동 시 연장 없음
    public int     maharagaTimeoutTicks            = 200;
    public int     shadowMarkerLifetimeTicks       = 200;
    public float   pveGradeMultiplier              = 1.0f;
    public int     blackFlashLowHpBonus            = 7;     // 체력 10% 미만 추가 %
    public int     blackFlashZoneAtkBonus          = 15;    // Zone 내 공격력 %
    public int     blackFlashZoneSkillBonus        = 10;    // Zone 내 스킬 데미지 %
    public int     fingerStatBonusPercent          = 5;     // 스쿠나 손가락 1개당 %
    public float   awakeningHpThreshold            = 0.05f; // 각성 발동 HP 비율
    public float   awakeningMultiplier             = 1.5f;  // 각성 배율
    public float   shieldCeDrainRatio              = 0.005f;
    public float   shieldDamageReduction           = 0.02f;
    public float   simpleBarrierCostActivate       = 0.03f;
    public float   simpleBarrierCostPerSecond      = 0.002f;
    public float   simpleBarrierSureHitNegate      = 0.70f;
    public float   simpleBarrierAllyBonus          = 0.08f;
    public float   fallingBlossomSureHitBlock      = 0.80f;
    public float   fallingBlossomCeDrain           = 0.02f;
    public float   reverseHealSelfPerTick          = 0.3f;
    public float   reverseCeDrainSelfRatio         = 0.008f;
    public float   reverseHealOtherPerTick         = 0.4f;
    public float   reverseCeDrainOtherRatio        = 0.012f;
    public float   tenShadowsBodyBonus             = 0.20f;
    public double  burdenDecayOutOfCombat          = 0.25;   // 이누마키 부담 감소/틱 (전투 외)
    public double  burdenDecayInCombat             = 0.10;   // 이누마키 부담 감소/틱 (전투 중)
    public int     burdenSealThreshold             = 100;    // 봉인 임계치
    // 주구 (Cursed Tool) 수치
    public float   cursedToolAttackBonus_dagger    = 0.08f;
    public float   cursedToolAttackBonus_spear     = 0.14f;
    public float   cursedToolAttackBonus_cloud     = 0.20f;
    public float   cursedToolAttackBonus_inverted  = 0.28f;
    public float   cursedToolAttackBonus_soul      = 0.18f;
    public float   cursedToolCeReduction_spear     = 0.08f;
    public float   cursedToolRangeBonus_cloud      = 0.12f;
    public float   cursedToolDefPenetration_inverted = 0.15f;
    public int     cursedToolBlackFlashBonus_soul  = 5;
    public int     cursedToolSealCooldown_inverted = 120;
    public int     xpGrade4to3                     = 500;
    public int     xpGrade3to2                     = 1200;
    public int     xpGrade2to1                     = 2500;
    public int     xpGrade1toSemi                  = 5000;
    public int     xpGradeSemiToSpecial             = 12000;
    public int     xpOnKill                        = 50;
    public int     xpOnDamagePerHit                = 1;
    public int     xpOnDamageCapPerCombat          = 20;
    public int     xpOnBlackFlash                  = 15;
    public int     xpOnPerfect                     = 30;
    public int     xpOnDailyLogin                  = 30;
    public int     chantMaxTicks                   = 60;
    public float   chantMaxMultiplier              = 2.0f;
    public float   chantCeDrainRatio               = 0.20f;
    public int     curtainBasicCeCost              = 800;
    public float   curtainBasicCePerTick           = 0.8f;
    public int     curtainBasicDurationTicks       = 4000;
    public int     curtainBasicRadius              = 25;
    public int     curtainBasicCooldownTicks       = 600;
    public int     curtainSpecialCeCost            = 2000;
    public float   curtainSpecialCePerTick         = 1.5f;
    public int     curtainSpecialDurationTicks     = 8000;
    public int     curtainSpecialRadius            = 45;
    public int     curtainSpecialCooldownTicks     = 1200;

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
        this.zoneEntryBlackFlashCount        = fresh.zoneEntryBlackFlashCount;
        this.zoneStackable                   = fresh.zoneStackable;
        this.maharagaTimeoutTicks            = fresh.maharagaTimeoutTicks;
        this.shadowMarkerLifetimeTicks       = fresh.shadowMarkerLifetimeTicks;
        this.pveGradeMultiplier              = fresh.pveGradeMultiplier;
        this.blackFlashLowHpBonus            = fresh.blackFlashLowHpBonus;
        this.blackFlashZoneAtkBonus          = fresh.blackFlashZoneAtkBonus;
        this.blackFlashZoneSkillBonus        = fresh.blackFlashZoneSkillBonus;
        this.fingerStatBonusPercent          = fresh.fingerStatBonusPercent;
        this.awakeningHpThreshold            = fresh.awakeningHpThreshold;
        this.awakeningMultiplier             = fresh.awakeningMultiplier;
        this.shieldCeDrainRatio              = fresh.shieldCeDrainRatio;
        this.shieldDamageReduction           = fresh.shieldDamageReduction;
        this.simpleBarrierCostActivate       = fresh.simpleBarrierCostActivate;
        this.simpleBarrierCostPerSecond      = fresh.simpleBarrierCostPerSecond;
        this.simpleBarrierSureHitNegate      = fresh.simpleBarrierSureHitNegate;
        this.simpleBarrierAllyBonus          = fresh.simpleBarrierAllyBonus;
        this.fallingBlossomSureHitBlock      = fresh.fallingBlossomSureHitBlock;
        this.fallingBlossomCeDrain           = fresh.fallingBlossomCeDrain;
        this.reverseHealSelfPerTick          = fresh.reverseHealSelfPerTick;
        this.reverseCeDrainSelfRatio         = fresh.reverseCeDrainSelfRatio;
        this.reverseHealOtherPerTick         = fresh.reverseHealOtherPerTick;
        this.reverseCeDrainOtherRatio        = fresh.reverseCeDrainOtherRatio;
        this.tenShadowsBodyBonus             = fresh.tenShadowsBodyBonus;
        this.xpGrade4to3                     = fresh.xpGrade4to3;
        this.xpGrade3to2                     = fresh.xpGrade3to2;
        this.xpGrade2to1                     = fresh.xpGrade2to1;
        this.xpGrade1toSemi                  = fresh.xpGrade1toSemi;
        this.xpGradeSemiToSpecial            = fresh.xpGradeSemiToSpecial;
        this.xpOnKill                        = fresh.xpOnKill;
        this.xpOnDamagePerHit                = fresh.xpOnDamagePerHit;
        this.xpOnDamageCapPerCombat          = fresh.xpOnDamageCapPerCombat;
        this.xpOnBlackFlash                  = fresh.xpOnBlackFlash;
        this.xpOnPerfect                     = fresh.xpOnPerfect;
        this.xpOnDailyLogin                  = fresh.xpOnDailyLogin;
        this.chantMaxTicks                   = fresh.chantMaxTicks;
        this.chantMaxMultiplier              = fresh.chantMaxMultiplier;
        this.chantCeDrainRatio               = fresh.chantCeDrainRatio;
        this.curtainBasicCeCost              = fresh.curtainBasicCeCost;
        this.curtainBasicCePerTick           = fresh.curtainBasicCePerTick;
        this.curtainBasicDurationTicks       = fresh.curtainBasicDurationTicks;
        this.curtainBasicRadius              = fresh.curtainBasicRadius;
        this.curtainBasicCooldownTicks       = fresh.curtainBasicCooldownTicks;
        this.curtainSpecialCeCost            = fresh.curtainSpecialCeCost;
        this.curtainSpecialCePerTick         = fresh.curtainSpecialCePerTick;
        this.curtainSpecialDurationTicks     = fresh.curtainSpecialDurationTicks;
        this.curtainSpecialRadius            = fresh.curtainSpecialRadius;
        this.curtainSpecialCooldownTicks     = fresh.curtainSpecialCooldownTicks;
        this.burdenDecayOutOfCombat              = fresh.burdenDecayOutOfCombat;
        this.burdenDecayInCombat                 = fresh.burdenDecayInCombat;
        this.burdenSealThreshold                 = fresh.burdenSealThreshold;
        this.cursedToolAttackBonus_dagger        = fresh.cursedToolAttackBonus_dagger;
        this.cursedToolAttackBonus_spear         = fresh.cursedToolAttackBonus_spear;
        this.cursedToolAttackBonus_cloud         = fresh.cursedToolAttackBonus_cloud;
        this.cursedToolAttackBonus_inverted      = fresh.cursedToolAttackBonus_inverted;
        this.cursedToolAttackBonus_soul          = fresh.cursedToolAttackBonus_soul;
        this.cursedToolCeReduction_spear         = fresh.cursedToolCeReduction_spear;
        this.cursedToolRangeBonus_cloud          = fresh.cursedToolRangeBonus_cloud;
        this.cursedToolDefPenetration_inverted   = fresh.cursedToolDefPenetration_inverted;
        this.cursedToolBlackFlashBonus_soul      = fresh.cursedToolBlackFlashBonus_soul;
        this.cursedToolSealCooldown_inverted     = fresh.cursedToolSealCooldown_inverted;
    }

    // Backward-compatible no-arg load: uses Minecraft server working directory
    public static JjkConfig load() {
        return load(Path.of("config/jjk/config.json"));
    }

    public int   getJackpotDurationTicks()     { return jackpotDurationTicks; }
    public float getTrialSuccessRate()         { return trialSuccessRate; }
    public int   getMaharagaTimeoutTicks()       { return maharagaTimeoutTicks; }
    public int   getShadowMarkerLifetimeTicks()  { return shadowMarkerLifetimeTicks; }
    public float getPveGradeMultiplier()         { return pveGradeMultiplier; }
    public int   blackFlashBaseRate()            { return blackFlashBaseRate; }
    public int   blackFlashLowHpBonus()          { return blackFlashLowHpBonus; }
    public int   blackFlashZoneBonus()           { return blackFlashZoneBonus; }
    public int   blackFlashZoneAtkBonus()        { return blackFlashZoneAtkBonus; }
    public int   blackFlashZoneSkillBonus()      { return blackFlashZoneSkillBonus; }
    public int   fingerStatBonusPercent()        { return fingerStatBonusPercent; }
    public float awakeningHpThreshold()          { return awakeningHpThreshold; }
    public float awakeningMultiplier()           { return awakeningMultiplier; }
    public float shieldCeDrainRatio()            { return shieldCeDrainRatio; }
    public float shieldDamageReduction()         { return shieldDamageReduction; }
    public float simpleBarrierCostActivate()     { return simpleBarrierCostActivate; }
    public float simpleBarrierCostPerSecond()    { return simpleBarrierCostPerSecond; }
    public float simpleBarrierSureHitNegate()    { return simpleBarrierSureHitNegate; }
    public float simpleBarrierAllyBonus()        { return simpleBarrierAllyBonus; }
    public float fallingBlossomSureHitBlock()    { return fallingBlossomSureHitBlock; }
    public float fallingBlossomCeDrain()         { return fallingBlossomCeDrain; }
    public float reverseHealSelfPerTick()        { return reverseHealSelfPerTick; }
    public float reverseCeDrainSelfRatio()       { return reverseCeDrainSelfRatio; }
    public float reverseHealOtherPerTick()       { return reverseHealOtherPerTick; }
    public float reverseCeDrainOtherRatio()      { return reverseCeDrainOtherRatio; }
    public float tenShadowsBodyBonus()           { return tenShadowsBodyBonus; }
    public boolean gradePvpScaling()             { return gradePvpScaling; }
    public int     xpGrade4to3()                 { return xpGrade4to3; }
    public int     xpGrade3to2()                 { return xpGrade3to2; }
    public int     xpGrade2to1()                 { return xpGrade2to1; }
    public int     xpGrade1toSemi()              { return xpGrade1toSemi; }
    public int     xpGradeSemiToSpecial()        { return xpGradeSemiToSpecial; }
    public int     xpOnKill()                    { return xpOnKill; }
    public int     xpOnDamagePerHit()            { return xpOnDamagePerHit; }
    public int     xpOnDamageCapPerCombat()      { return xpOnDamageCapPerCombat; }
    public int     xpOnBlackFlash()              { return xpOnBlackFlash; }
    public int     xpOnPerfect()                 { return xpOnPerfect; }
    public int     xpOnDailyLogin()              { return xpOnDailyLogin; }
    public int     chantMaxTicks()               { return chantMaxTicks; }
    public float   chantMaxMultiplier()          { return chantMaxMultiplier; }
    public float   chantCeDrainRatio()           { return chantCeDrainRatio; }
    public int     curtainBasicCeCost()          { return curtainBasicCeCost; }
    public float   curtainBasicCePerTick()       { return curtainBasicCePerTick; }
    public int     curtainBasicDurationTicks()   { return curtainBasicDurationTicks; }
    public int     curtainBasicRadius()          { return curtainBasicRadius; }
    public int     curtainBasicCooldownTicks()   { return curtainBasicCooldownTicks; }
    public int     curtainSpecialCeCost()        { return curtainSpecialCeCost; }
    public float   curtainSpecialCePerTick()     { return curtainSpecialCePerTick; }
    public int     curtainSpecialDurationTicks() { return curtainSpecialDurationTicks; }
    public int     curtainSpecialRadius()        { return curtainSpecialRadius; }
    public int     curtainSpecialCooldownTicks() { return curtainSpecialCooldownTicks; }
    public double  burdenDecayOutOfCombat()             { return burdenDecayOutOfCombat; }
    public double  burdenDecayInCombat()                { return burdenDecayInCombat; }
    public int     burdenSealThreshold()                { return burdenSealThreshold; }
    public float   cursedToolAttackBonus_dagger()       { return cursedToolAttackBonus_dagger; }
    public float   cursedToolAttackBonus_spear()        { return cursedToolAttackBonus_spear; }
    public float   cursedToolAttackBonus_cloud()        { return cursedToolAttackBonus_cloud; }
    public float   cursedToolAttackBonus_inverted()     { return cursedToolAttackBonus_inverted; }
    public float   cursedToolAttackBonus_soul()         { return cursedToolAttackBonus_soul; }
    public float   cursedToolCeReduction_spear()        { return cursedToolCeReduction_spear; }
    public float   cursedToolRangeBonus_cloud()         { return cursedToolRangeBonus_cloud; }
    public float   cursedToolDefPenetration_inverted()  { return cursedToolDefPenetration_inverted; }
    public int     cursedToolBlackFlashBonus_soul()     { return cursedToolBlackFlashBonus_soul; }
    public int     cursedToolSealCooldown_inverted()    { return cursedToolSealCooldown_inverted; }
}
