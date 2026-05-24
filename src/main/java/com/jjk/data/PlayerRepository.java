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

public class PlayerRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-repo");
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>(){}.getType();
    private static final Type COOLDOWNS_TYPE = new TypeToken<Map<String, Long>>(){}.getType();

    private Connection conn;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final Migrator migrator = new Migrator();

    // Called from SERVER_STARTING ??path is world/jjk/player_data.db
    public void init(Path dbPath) {
        try {
            Files.createDirectories(dbPath.getParent());
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA foreign_keys=ON");
                st.execute(DDL_PLAYER_DATA);
            }
            migrator.migrate(conn);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize player_data.db", e);
        }
    }

    public void close() {
        flushAll();
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            LOGGER.error("Failed to close DB connection", e);
        }
    }

    // Checks cache first; queries DB on miss. Always sets data.uuid.
    public PlayerData load(UUID uuid) {
        Objects.requireNonNull(uuid);
        PlayerData cached = cache.get(uuid);
        if (cached != null) return cached;

        PlayerData data = loadFromDb(uuid);
        data.uuid = uuid;
        cache.put(uuid, data);
        return data;
    }

    // Writes to cache only. Flushed on saveImmediate / evict.
    public void save(PlayerData data) {
        Objects.requireNonNull(data.uuid);
        cache.put(data.uuid, data);
    }

    // Writes to cache and immediately flushes to DB.
    public void saveImmediate(PlayerData data) {
        Objects.requireNonNull(data.uuid);
        cache.put(data.uuid, data);
        flush(data);
    }

    // Flushes to DB and evicts from cache. Call on player disconnect.
    public void evict(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) flush(data);
    }

    // ???? private ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

    private void flushAll() {
        new ArrayList<>(cache.values()).forEach(this::flush);
    }

    private void flush(PlayerData data) {
        if (conn == null) return;
        try {
            upsertPlayerData(data);
        } catch (SQLException e) {
            LOGGER.error("Failed to flush data for {}", data.uuid, e);
        }
    }

    private PlayerData loadFromDb(UUID uuid) {
        if (conn == null) return new PlayerData();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new PlayerData();
                return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load data for {}", uuid, e);
            return new PlayerData();
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
        d.schemaVersion          = rs.getInt("schema_version");
        if (d.unlockedSkills == null) d.unlockedSkills = new ArrayList<>();
        if (d.cooldowns == null)      d.cooldowns = new HashMap<>();
        if (d.deadShikigamiIds == null) d.deadShikigamiIds = new ArrayList<>();
        return d;
    }

    private void upsertPlayerData(PlayerData d) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_PLAYER)) {
            ps.setString(1,  d.uuid.toString());
            ps.setString(2,  d.characterId);
            ps.setString(3,  d.grade != null ? d.grade : "4疫?");
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
            ps.setString(20, d.trialState);
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
            ps.setInt(34,    d.schemaVersion);
            ps.executeUpdate();
        }
    }

    // ???? SQL ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

    private static final String DDL_PLAYER_DATA = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid                        TEXT    PRIMARY KEY,
                character_id                TEXT,
                grade                       TEXT    NOT NULL DEFAULT '4疫?',
                xp                          INTEGER NOT NULL DEFAULT 0,
                mastery                     INTEGER NOT NULL DEFAULT 0,
                ce_current                  REAL    NOT NULL DEFAULT 1000.0,
                ce_max                      REAL    NOT NULL DEFAULT 1000.0,
                hp_current                  REAL    NOT NULL DEFAULT 20.0,
                hp_max                      REAL    NOT NULL DEFAULT 20.0,
                attack_stat                 INTEGER NOT NULL DEFAULT 0,
                defense_stat                INTEGER NOT NULL DEFAULT 0,
                speed_stat                  INTEGER NOT NULL DEFAULT 0,
                finger_count                INTEGER NOT NULL DEFAULT 0,
                unlocked_skills             TEXT    NOT NULL DEFAULT '[]',
                cooldowns                   TEXT    NOT NULL DEFAULT '{}',
                binding_vow_declared_tick   INTEGER NOT NULL DEFAULT 0,
                domain_cooldown_until       INTEGER NOT NULL DEFAULT 0,
                jackpot_cooldown_until      INTEGER NOT NULL DEFAULT 0,
                curtain_cooldown_until      INTEGER NOT NULL DEFAULT 0,
                trial_state                 TEXT,
                burden                      INTEGER NOT NULL DEFAULT 0,
                zone_active                 INTEGER NOT NULL DEFAULT 0,
                zone_end_tick               INTEGER NOT NULL DEFAULT 0,
                last_combat_tick            INTEGER NOT NULL DEFAULT 0,
                last_known_ip               TEXT,
                awakening_active            INTEGER NOT NULL DEFAULT 0,
                awakening_end_tick          INTEGER NOT NULL DEFAULT 0,
                awakening_cooldown_until    INTEGER NOT NULL DEFAULT 0,
                burst_active                INTEGER NOT NULL DEFAULT 0,
                burst_end_tick              INTEGER NOT NULL DEFAULT 0,
                dead_shikigami_ids          TEXT    NOT NULL DEFAULT '[]',
                healing_active              INTEGER NOT NULL DEFAULT 0,
                zone_penalty_until_tick     INTEGER NOT NULL DEFAULT 0,
                schema_version              INTEGER NOT NULL DEFAULT 1
            )""";

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
                healing_active, zone_penalty_until_tick, schema_version
            ) VALUES (
                ?,?,?,?,?, ?,?,?,?, ?,?,?,?,
                ?,?, ?,?,?, ?,?,?,?, ?,?, ?,?,?, ?,?,?, ?,?,?
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
                schema_version            = excluded.schema_version
            """;
}
