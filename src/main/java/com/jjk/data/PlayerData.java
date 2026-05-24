package com.jjk.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 吏?3-1: 34揶???? ?袁⑤굡
public class PlayerData {

    public UUID uuid;
    public String characterId;
    public String grade;
    public long xp;
    public int mastery;
    public float ceCurrent;
    public float ceMax;
    public float hpCurrent;
    public float hpMax;
    public int attackStat;
    public int defenseStat;
    public int speedStat;
    public int fingerCount;
    public List<String> unlockedSkills = new ArrayList<>();
    public Map<String, Long> cooldowns = new HashMap<>();   // 吏?3-3: JSON TEXT ?뚎됱쓥??곗쨮 筌욊낮???
    public long bindingVowDeclaredTick;
    public long domainCooldownUntil;
    public long jackpotCooldownUntil;
    public long curtainCooldownUntil;
    public String trialState;
    public int burden;
    public boolean zoneActive;
    public long zoneEndTick;
    public long lastCombatTick;
    public String lastKnownIp;
    public boolean awakeningActive;
    public long awakeningEndTick;
    public long awakeningCooldownUntil;
    public boolean burstActive;
    public long burstEndTick;
    public List<String> deadShikigamiIds = new ArrayList<>();
    public boolean healingActive;
    public long zonePenaltyUntilTick;
    public int schemaVersion = 1;

    public PlayerData snapshot() {
        PlayerData copy = new PlayerData();
        copy.uuid = this.uuid;
        copy.characterId = this.characterId;
        copy.grade = this.grade;
        copy.xp = this.xp;
        copy.mastery = this.mastery;
        copy.ceCurrent = this.ceCurrent;
        copy.ceMax = this.ceMax;
        copy.hpCurrent = this.hpCurrent;
        copy.hpMax = this.hpMax;
        copy.attackStat = this.attackStat;
        copy.defenseStat = this.defenseStat;
        copy.speedStat = this.speedStat;
        copy.fingerCount = this.fingerCount;
        copy.unlockedSkills = new ArrayList<>(this.unlockedSkills);
        copy.cooldowns = new HashMap<>(this.cooldowns);
        copy.bindingVowDeclaredTick = this.bindingVowDeclaredTick;
        copy.domainCooldownUntil = this.domainCooldownUntil;
        copy.jackpotCooldownUntil = this.jackpotCooldownUntil;
        copy.curtainCooldownUntil = this.curtainCooldownUntil;
        copy.trialState = this.trialState;
        copy.burden = this.burden;
        copy.zoneActive = this.zoneActive;
        copy.zoneEndTick = this.zoneEndTick;
        copy.lastCombatTick = this.lastCombatTick;
        copy.lastKnownIp = this.lastKnownIp;
        copy.awakeningActive = this.awakeningActive;
        copy.awakeningEndTick = this.awakeningEndTick;
        copy.awakeningCooldownUntil = this.awakeningCooldownUntil;
        copy.burstActive = this.burstActive;
        copy.burstEndTick = this.burstEndTick;
        copy.deadShikigamiIds = new ArrayList<>(this.deadShikigamiIds);
        copy.healingActive = this.healingActive;
        copy.zonePenaltyUntilTick = this.zonePenaltyUntilTick;
        copy.schemaVersion = this.schemaVersion;
        return copy;
    }
}
