package com.jjk.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/** 로컬 플레이어의 서버 사이드 상태 캐시. */
@Environment(EnvType.CLIENT)
public final class JjkClientState {
    private JjkClientState() {}

    // ── 캐릭터 / CE ──────────────────────────────────────────────────────────
    private static String characterId = null;
    private static String grade = null;
    private static float ceMax = 100f;
    private static float ceCurrent = 100f;

    // ── HP (BossBarUpdateS2CPacket) ───────────────────────────────────────────
    private static int hp = 20;
    private static int hpMax = 20;

    // ── 스킬 쿨타임 (SkillCooldownSyncS2CPacket) ──────────────────────────────
    // keyId → [startTick, totalTicks]
    private static final Map<Integer, Long>    cooldownStart = new HashMap<>();
    private static final Map<Integer, Integer> cooldownTotal = new HashMap<>();

    // ── 각성 상태 ─────────────────────────────────────────────────────────────
    private static boolean awakeningActive = false;

    // ── HudSyncS2CPacket 기반 추가 상태 ───────────────────────────────────────
    private static boolean inCombat = false;
    private static int gradeInt = 5; // 0=特級, 1=準1級, 2=1級, 3=2級, 4=3級, 5=4級
    private static final Map<String, Integer> hudCooldowns = new HashMap<>();

    // ── 영역(Zone) 상태 ───────────────────────────────────────────────────────
    private static String activeDomainId = null;
    private static long   domainEnteredTick = 0L;
    private static int    activeDomainCount = 0;

    // ── 단일-스킬 봉인 (SealedSkillSyncS2CPacket, Phase I-2) ─────────────────
    private static String sealedSkillId = null;
    private static long   sealExpireAtTick = 0L;

    // ── Update methods ────────────────────────────────────────────────────────

    public static void update(String charId, String gr, float max, float current) {
        characterId = charId;
        grade = gr;
        ceMax = max;
        ceCurrent = current;
    }

    public static void updateHp(int newHp, int newHpMax) {
        hp = newHp;
        hpMax = newHpMax;
    }

    public static void updateCe(float current, float max) {
        ceCurrent = current;
        ceMax = max;
    }

    public static void setCharacterId(String charId) {
        characterId = charId;
    }

    public static void onSkillCooldown(int keyId, int totalTicks, long worldTick) {
        cooldownStart.put(keyId, worldTick);
        cooldownTotal.put(keyId, totalTicks);
    }

    /** 0.0 = 쿨타임 완료, 1.0 = 방금 사용. */
    public static float getCooldownRatio(int keyId, long currentTick) {
        Long start = cooldownStart.get(keyId);
        Integer total = cooldownTotal.get(keyId);
        if (start == null || total == null || total <= 0) return 0f;
        long elapsed = currentTick - start;
        if (elapsed >= total) return 0f;
        return 1f - (float) elapsed / total;
    }

    /** 남은 쿨타임 (틱). */
    public static int getCooldownRemaining(int keyId, long currentTick) {
        Long start = cooldownStart.get(keyId);
        Integer total = cooldownTotal.get(keyId);
        if (start == null || total == null) return 0;
        return (int) Math.max(0, total - (currentTick - start));
    }

    public static void onZoneEnter(String domainId, long worldTick) {
        activeDomainId = domainId;
        domainEnteredTick = worldTick;
        activeDomainCount++;
    }

    public static void onZoneExit() {
        activeDomainId = null;
        activeDomainCount = Math.max(0, activeDomainCount - 1);
    }

    /** sealedSkillId가 빈 문자열이면 봉인 해제로 간주. */
    public static void onSealedSkillSync(String skillId, long expireAtTick) {
        if (skillId == null || skillId.isEmpty()) {
            sealedSkillId = null;
            sealExpireAtTick = 0L;
        } else {
            sealedSkillId = skillId;
            sealExpireAtTick = expireAtTick;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static String getCharacterId() { return characterId; }
    public static String getGrade()       { return grade; }
    public static float  getCeMax()       { return ceMax; }
    public static float  getCeCurrent()   { return ceCurrent; }
    public static int    getHp()          { return hp; }
    public static int    getHpMax()       { return hpMax; }
    public static String getActiveDomainId()    { return activeDomainId; }
    public static long   getDomainEnteredTick()  { return domainEnteredTick; }
    public static int    getActiveDomainCount()  { return activeDomainCount; }
    public static boolean isInDomain()     { return activeDomainId != null; }

    public static void setAwakening(boolean active) { awakeningActive = active; }
    public static boolean isAwakening()             { return awakeningActive; }

    public static String getSealedSkillId()       { return sealedSkillId; }
    public static long   getSealExpireAtTick()    { return sealExpireAtTick; }
    public static boolean isSkillSealed(long currentTick) {
        return sealedSkillId != null && currentTick < sealExpireAtTick;
    }

    // ── HudSync 갱신 (HudSyncS2CPacket) ──────────────────────────────────────

    public static void updateHudSync(float cePercent, float hpPercent, int newGradeInt,
                                      boolean newInCombat, Map<String, Integer> cooldowns) {
        ceCurrent = cePercent * ceMax;
        gradeInt = newGradeInt;
        inCombat = newInCombat;
        hudCooldowns.clear();
        if (cooldowns != null) hudCooldowns.putAll(cooldowns);
    }

    public static boolean isInCombat()                         { return inCombat; }
    public static int getGradeInt()                            { return gradeInt; }
    public static int getHudCooldownRemaining(String key)      { return hudCooldowns.getOrDefault(key, 0); }
    public static Map<String, Integer> getHudCooldowns()       { return Collections.unmodifiableMap(hudCooldowns); }
}
