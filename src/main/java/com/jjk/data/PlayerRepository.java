package com.jjk.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-repo");
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>(){}.getType();
    private static final Type COOLDOWNS_TYPE = new TypeToken<Map<String, Long>>(){}.getType();

    private Connection conn;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    // Production constructor: no-arg, init(Path) sets up connection + migration
    public PlayerRepository() {}

    // Test constructor: accepts existing connection (Migrator called externally)
    public PlayerRepository(Connection conn) {
        this.conn = conn;
    }

    public void init(Path dbPath) {
        try {
            Files.createDirectories(dbPath.getParent());
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA foreign_keys=ON");
            }
            new Migrator().migrate(conn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize player_data.db", e);
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public void close() {
        flushAll();
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            LOGGER.error("Failed to close DB connection", e);
        }
    }

    public PlayerData load(UUID uuid) {
        Objects.requireNonNull(uuid);
        PlayerData cached = cache.get(uuid);
        if (cached != null) return cached;

        PlayerData data = loadFromDb(uuid);
        data.uuid = uuid;
        cache.put(uuid, data);
        return data;
    }

    public void save(PlayerData data) {
        Objects.requireNonNull(data.uuid);
        cache.put(data.uuid, data);
    }

    // snapshot() 후 저장. 원본을 DB에 직접 전달하지 않음.
    public void saveImmediate(PlayerData data) {
        Objects.requireNonNull(data.uuid);
        cache.put(data.uuid, data);
        PlayerData snap = data.snapshot();
        try {
            upsertPlayerData(snap);
        } catch (SQLException e) {
            LOGGER.error("saveImmediate failed for {}", data.uuid, e);
            throw new RuntimeException("saveImmediate failed", e);
        }
    }

    // snapshot() 후 비동기 저장. 실패 시 조용히 로그만.
    public CompletableFuture<Void> saveAsync(PlayerData data) {
        Objects.requireNonNull(data.uuid);
        cache.put(data.uuid, data);
        PlayerData snap = data.snapshot();
        return CompletableFuture.runAsync(() -> {
            try {
                upsertPlayerData(snap);
            } catch (SQLException e) {
                LOGGER.error("saveAsync failed for {}", snap.uuid, e);
            }
        });
    }

    public void evict(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            PlayerData snap = data.snapshot();
            try {
                upsertPlayerData(snap);
            } catch (SQLException e) {
                LOGGER.error("evict flush failed for {}", uuid, e);
            }
        }
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private void flushAll() {
        new ArrayList<>(cache.values()).forEach(data -> {
            PlayerData snap = data.snapshot();
            try { upsertPlayerData(snap); }
            catch (SQLException e) { LOGGER.error("flushAll failed for {}", snap.uuid, e); }
        });
    }

    private PlayerData loadFromDb(UUID uuid) {
        if (conn == null) return PlayerData.createDefault(uuid);
        try (PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return PlayerData.createDefault(uuid);
                return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load data for {}", uuid, e);
            return PlayerData.createDefault(uuid);
        }
    }

    private PlayerData mapRow(ResultSet rs) throws SQLException {
        PlayerData d = new PlayerData();
        d.characterId            = rs.getString("character_id");
        d.grade                  = rs.getString("grade");
        d.xp                     = rs.getLong("xp");
        d.mastery                = rs.getInt("mastery");
        d.ceCurrent              = rs.getFloat("ce_current");
        d.ceMax                  = rs.getFloat("ce_max");
        d.hpCurrent              = rs.getFloat("hp_current");
        d.hpMax                  = rs.getFloat("hp_max");
        d.attackStat             = rs.getInt("attack_stat");
        d.defenseStat            = rs.getInt("defense_stat");
        d.speedStat              = rs.getInt("speed_stat");
        d.fingerCount            = rs.getInt("finger_count");
        d.unlockedSkills         = GSON.fromJson(rs.getString("unlocked_skills"), LIST_TYPE);
        d.cooldowns              = GSON.fromJson(rs.getString("cooldowns"), COOLDOWNS_TYPE);
        d.bindingVowDeclaredTick = rs.getLong("binding_vow_declared_tick");
        d.domainCooldownUntil    = rs.getLong("domain_cooldown_until");
        d.jackpotCooldownUntil   = rs.getLong("jackpot_cooldown_until");
        d.curtainCooldownUntil   = rs.getLong("curtain_cooldown_until");
        d.trialState             = rs.getString("trial_state");
        d.burden                 = rs.getInt("burden");
        d.zoneActive             = rs.getInt("zone_active") != 0;
        d.zoneEndTick            = rs.getLong("zone_end_tick");
        d.lastCombatTick         = rs.getLong("last_combat_tick");
        d.lastKnownIp            = rs.getString("last_known_ip");
        d.awakeningActive        = rs.getInt("awakening_active") != 0;
        d.awakeningEndTick       = rs.getLong("awakening_end_tick");
        d.awakeningCooldownUntil = rs.getLong("awakening_cooldown_until");
        d.burstActive            = rs.getInt("burst_active") != 0;
        d.burstEndTick           = rs.getLong("burst_end_tick");
        d.deadShikigamiIds       = GSON.fromJson(rs.getString("dead_shikigami_ids"), LIST_TYPE);
        d.healingActive          = rs.getInt("healing_active") != 0;
        d.zonePenaltyUntilTick   = rs.getLong("zone_penalty_until_tick");
        d.hasExecutionSword      = rs.getInt("has_execution_sword") != 0;
        d.jackpotActive          = rs.getInt("jackpot_active") != 0;
        d.jackpotEndTick         = rs.getLong("jackpot_end_tick");
        d.lastJackpotAttemptTick = rs.getLong("last_jackpot_attempt_tick");
        d.infinityActive         = rs.getInt("infinity_active") != 0;
        d.curtainActive          = rs.getInt("curtain_active") != 0;
        d.overtimeWork           = rs.getInt("overtime_work") != 0;
        d.fallingBlossomActive   = rs.getInt("falling_blossom_active") != 0;
        d.fallingBlossomUntil    = rs.getLong("falling_blossom_until");
        d.simpleBarrierActive    = rs.getInt("simple_barrier_active") != 0;
        d.shikigamiDmgBoost      = rs.getFloat("shikigami_dmg_boost");
        d.schemaVersion          = rs.getInt("schema_version");
        try { d.zoneEntryTick  = rs.getLong("zone_entry_tick"); }  catch (SQLException ignored) {}
        try { d.lastAttackTick = rs.getLong("last_attack_tick"); } catch (SQLException ignored) {}
        try { d.lastReceivedSkillId       = rs.getString("last_received_skill_id"); }       catch (SQLException ignored) {}
        try { d.lastReceivedBaseDamage    = rs.getInt("last_received_base_damage"); }        catch (SQLException ignored) {}
        try { d.lastReceivedCeCost        = rs.getInt("last_received_ce_cost"); }            catch (SQLException ignored) {}
        try { d.lastReceivedCooldownTicks = rs.getInt("last_received_cooldown_ticks"); }     catch (SQLException ignored) {}
        try { d.lastReceivedIsDomain      = rs.getInt("last_received_is_domain") != 0; }    catch (SQLException ignored) {}
        try { d.cursedStones      = rs.getLong("cursed_stones"); }       catch (SQLException ignored) {}
        try { d.masteryResetCount = rs.getInt("mastery_reset_count"); }  catch (SQLException ignored) {}
        try { d.bounty            = rs.getLong("bounty"); }               catch (SQLException ignored) {}
        try { d.weeklyQuestDone   = rs.getLong("weekly_quest_done"); }    catch (SQLException ignored) {}
        try { d.costumeId         = rs.getString("costume_id"); if (d.costumeId == null) d.costumeId = "default"; } catch (SQLException ignored) {}
        if (d.unlockedSkills == null)   d.unlockedSkills = new ArrayList<>();
        if (d.cooldowns == null)        d.cooldowns = new HashMap<>();
        if (d.deadShikigamiIds == null) d.deadShikigamiIds = new ArrayList<>();
        return d;
    }

    private void upsertPlayerData(PlayerData d) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_PLAYER)) {
            ps.setString(1,  d.uuid.toString());
            ps.setString(2,  d.characterId);
            ps.setString(3,  d.grade != null ? d.grade : "4급");
            ps.setLong(4,    d.xp);
            ps.setInt(5,     d.mastery);
            ps.setFloat(6,   d.ceCurrent);
            ps.setFloat(7,   d.ceMax);
            ps.setFloat(8,   d.hpCurrent);
            ps.setFloat(9,   d.hpMax);
            ps.setInt(10,    d.attackStat);
            ps.setInt(11,    d.defenseStat);
            ps.setInt(12,    d.speedStat);
            ps.setInt(13,    d.fingerCount);
            ps.setString(14, GSON.toJson(d.unlockedSkills));
            ps.setString(15, GSON.toJson(d.cooldowns));
            ps.setLong(16,   d.bindingVowDeclaredTick);
            ps.setLong(17,   d.domainCooldownUntil);
            ps.setLong(18,   d.jackpotCooldownUntil);
            ps.setLong(19,   d.curtainCooldownUntil);
            ps.setString(20, d.trialState != null ? d.trialState : "IDLE");
            ps.setInt(21,    d.burden);
            ps.setInt(22,    d.zoneActive ? 1 : 0);
            ps.setLong(23,   d.zoneEndTick);
            ps.setLong(24,   d.lastCombatTick);
            ps.setString(25, d.lastKnownIp);
            ps.setInt(26,    d.awakeningActive ? 1 : 0);
            ps.setLong(27,   d.awakeningEndTick);
            ps.setLong(28,   d.awakeningCooldownUntil);
            ps.setInt(29,    d.burstActive ? 1 : 0);
            ps.setLong(30,   d.burstEndTick);
            ps.setString(31, GSON.toJson(d.deadShikigamiIds));
            ps.setInt(32,    d.healingActive ? 1 : 0);
            ps.setLong(33,   d.zonePenaltyUntilTick);
            ps.setInt(34,    d.hasExecutionSword ? 1 : 0);
            ps.setInt(35,    d.jackpotActive ? 1 : 0);
            ps.setLong(36,   d.jackpotEndTick);
            ps.setLong(37,   d.lastJackpotAttemptTick);
            ps.setInt(38,    d.infinityActive ? 1 : 0);
            ps.setInt(39,    d.curtainActive ? 1 : 0);
            ps.setInt(40,    d.overtimeWork ? 1 : 0);
            ps.setInt(41,    d.fallingBlossomActive ? 1 : 0);
            ps.setLong(42,   d.fallingBlossomUntil);
            ps.setInt(43,    d.simpleBarrierActive ? 1 : 0);
            ps.setFloat(44,  d.shikigamiDmgBoost);
            ps.setInt(45,    d.schemaVersion);
            ps.setLong(46,   d.zoneEntryTick);
            ps.setLong(47,   d.lastAttackTick);
            ps.setString(48, d.lastReceivedSkillId);
            ps.setInt(49,    d.lastReceivedBaseDamage);
            ps.setInt(50,    d.lastReceivedCeCost);
            ps.setInt(51,    d.lastReceivedCooldownTicks);
            ps.setInt(52,    d.lastReceivedIsDomain ? 1 : 0);
            ps.setLong(53,   d.cursedStones);
            ps.setInt(54,    d.masteryResetCount);
            ps.setLong(55,   d.bounty);
            ps.setLong(56,   d.weeklyQuestDone);
            ps.setString(57, d.costumeId != null ? d.costumeId : "default");
            ps.executeUpdate();
        }
    }

    /** 등급 상위 N명 반환. 각 항목은 [displayName, grade] 배열. */
    public List<String[]> findTopPlayers(int limit) {
        if (conn == null) return List.of();
        String sql = """
            SELECT uuid, grade FROM player_data
            WHERE grade IS NOT NULL
            ORDER BY CASE grade
                WHEN '특급'   THEN 6
                WHEN '준특급'  THEN 5
                WHEN '1급'    THEN 4
                WHEN '2급'    THEN 3
                WHEN '3급'    THEN 2
                ELSE 1
            END DESC, xp DESC
            LIMIT ?
            """;
        List<String[]> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String grade   = rs.getString("grade");
                    String display = uuidStr;
                    if (com.jjk.JJKMod.getServer() != null) {
                        try {
                            UUID id = UUID.fromString(uuidStr);
                            var p = com.jjk.JJKMod.getServer().getPlayerManager().getPlayer(id);
                            display = p != null ? p.getName().getString()
                                               : (uuidStr.length() >= 8 ? uuidStr.substring(0, 8) + "…" : uuidStr);
                        } catch (Exception ignored) {}
                    }
                    result.add(new String[]{display, grade != null ? grade : "4급"});
                }
            }
        } catch (SQLException e) {
            LOGGER.error("findTopPlayers failed", e);
        }
        return result;
    }

    /** 최고 등급 주술사 플레이어 이름 반환 (GojoShiyuService 정보 구매용). */
    public String findTopGradeSorcerer() {
        if (conn == null) return "알 수 없음";
        String sql = """
            SELECT uuid FROM player_data
            WHERE character_id IN ('gojo','itadori','megumi','okkotsu','nanami','inumaki','hakari','higuruma')
            ORDER BY CASE grade
                WHEN '특급'  THEN 6
                WHEN '준특급' THEN 5
                WHEN '1급'   THEN 4
                WHEN '2급'   THEN 3
                WHEN '3급'   THEN 2
                ELSE 1
            END DESC, xp DESC
            LIMIT 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return "알 수 없음";
            String uuidStr = rs.getString("uuid");
            if (com.jjk.JJKMod.getServer() != null) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                    var p = com.jjk.JJKMod.getServer().getPlayerManager().getPlayer(uuid);
                    if (p != null) return p.getName().getString();
                } catch (Exception ignored) {}
            }
            return uuidStr.length() >= 8 ? uuidStr.substring(0, 8) + "..." : uuidStr;
        } catch (SQLException e) {
            return "알 수 없음";
        }
    }

    // ─── SQL ──────────────────────────────────────────────────────────────────

    private static final String SELECT_PLAYER =
            "SELECT * FROM player_data WHERE uuid = ?";

    private static final String UPSERT_PLAYER = """
            INSERT INTO player_data (
                uuid, character_id, grade, xp, mastery,
                ce_current, ce_max, hp_current, hp_max,
                attack_stat, defense_stat, speed_stat, finger_count,
                unlocked_skills, cooldowns,
                binding_vow_declared_tick, domain_cooldown_until,
                jackpot_cooldown_until, curtain_cooldown_until,
                trial_state, burden, zone_active, zone_end_tick,
                last_combat_tick, last_known_ip,
                awakening_active, awakening_end_tick, awakening_cooldown_until,
                burst_active, burst_end_tick, dead_shikigami_ids,
                healing_active, zone_penalty_until_tick,
                has_execution_sword, jackpot_active, jackpot_end_tick,
                last_jackpot_attempt_tick, infinity_active, curtain_active,
                overtime_work, falling_blossom_active, falling_blossom_until,
                simple_barrier_active, shikigami_dmg_boost, schema_version,
                zone_entry_tick, last_attack_tick,
                last_received_skill_id, last_received_base_damage,
                last_received_ce_cost, last_received_cooldown_ticks,
                last_received_is_domain,
                cursed_stones, mastery_reset_count, bounty,
                weekly_quest_done,
                costume_id
            ) VALUES (
                ?,?,?,?,?, ?,?,?,?, ?,?,?,?,
                ?,?, ?,?,?,?,
                ?,?,?,?, ?,?,
                ?,?,?, ?,?,?,
                ?,?, ?,?,?,
                ?,?,?, ?,?,?,
                ?,?,?, ?,?,
                ?,?,?,?,?,
                ?,?,?,?,
                ?
            )
            ON CONFLICT(uuid) DO UPDATE SET
                character_id              = excluded.character_id,
                grade                     = excluded.grade,
                xp                        = excluded.xp,
                mastery                   = excluded.mastery,
                ce_current                = excluded.ce_current,
                ce_max                    = excluded.ce_max,
                hp_current                = excluded.hp_current,
                hp_max                    = excluded.hp_max,
                attack_stat               = excluded.attack_stat,
                defense_stat              = excluded.defense_stat,
                speed_stat                = excluded.speed_stat,
                finger_count              = excluded.finger_count,
                unlocked_skills           = excluded.unlocked_skills,
                cooldowns                 = excluded.cooldowns,
                binding_vow_declared_tick = excluded.binding_vow_declared_tick,
                domain_cooldown_until     = excluded.domain_cooldown_until,
                jackpot_cooldown_until    = excluded.jackpot_cooldown_until,
                curtain_cooldown_until    = excluded.curtain_cooldown_until,
                trial_state               = excluded.trial_state,
                burden                    = excluded.burden,
                zone_active               = excluded.zone_active,
                zone_end_tick             = excluded.zone_end_tick,
                last_combat_tick          = excluded.last_combat_tick,
                last_known_ip             = excluded.last_known_ip,
                awakening_active          = excluded.awakening_active,
                awakening_end_tick        = excluded.awakening_end_tick,
                awakening_cooldown_until  = excluded.awakening_cooldown_until,
                burst_active              = excluded.burst_active,
                burst_end_tick            = excluded.burst_end_tick,
                dead_shikigami_ids        = excluded.dead_shikigami_ids,
                healing_active            = excluded.healing_active,
                zone_penalty_until_tick   = excluded.zone_penalty_until_tick,
                has_execution_sword       = excluded.has_execution_sword,
                jackpot_active            = excluded.jackpot_active,
                jackpot_end_tick          = excluded.jackpot_end_tick,
                last_jackpot_attempt_tick = excluded.last_jackpot_attempt_tick,
                infinity_active           = excluded.infinity_active,
                curtain_active            = excluded.curtain_active,
                overtime_work             = excluded.overtime_work,
                falling_blossom_active    = excluded.falling_blossom_active,
                falling_blossom_until     = excluded.falling_blossom_until,
                simple_barrier_active     = excluded.simple_barrier_active,
                shikigami_dmg_boost       = excluded.shikigami_dmg_boost,
                schema_version            = excluded.schema_version,
                zone_entry_tick                = excluded.zone_entry_tick,
                last_attack_tick               = excluded.last_attack_tick,
                last_received_skill_id         = excluded.last_received_skill_id,
                last_received_base_damage      = excluded.last_received_base_damage,
                last_received_ce_cost          = excluded.last_received_ce_cost,
                last_received_cooldown_ticks   = excluded.last_received_cooldown_ticks,
                last_received_is_domain        = excluded.last_received_is_domain,
                cursed_stones                  = excluded.cursed_stones,
                mastery_reset_count            = excluded.mastery_reset_count,
                bounty                         = excluded.bounty,
                weekly_quest_done              = excluded.weekly_quest_done,
                costume_id                     = excluded.costume_id
            """;
}
