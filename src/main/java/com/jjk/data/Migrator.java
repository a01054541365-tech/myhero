package com.jjk.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Migrator {

    public static final int CURRENT_VERSION = 22;

    private static final String DDL_PLAYER_DATA = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid                        TEXT    PRIMARY KEY,
                character_id                TEXT,
                grade                       TEXT    NOT NULL DEFAULT '4급',
                xp                          INTEGER NOT NULL DEFAULT 0,
                mastery                     INTEGER NOT NULL DEFAULT 0,
                ce_current                  REAL    NOT NULL DEFAULT 100.0,
                ce_max                      REAL    NOT NULL DEFAULT 100.0,
                hp_current                  REAL    NOT NULL DEFAULT 20.0,
                hp_max                      REAL    NOT NULL DEFAULT 20.0,
                attack_stat                 INTEGER NOT NULL DEFAULT 10,
                defense_stat                INTEGER NOT NULL DEFAULT 10,
                speed_stat                  INTEGER NOT NULL DEFAULT 10,
                finger_count                INTEGER NOT NULL DEFAULT 0,
                unlocked_skills             TEXT    NOT NULL DEFAULT '[]',
                cooldowns                   TEXT    NOT NULL DEFAULT '{}',
                binding_vow_declared_tick   INTEGER NOT NULL DEFAULT -1,
                domain_cooldown_until       INTEGER NOT NULL DEFAULT 0,
                jackpot_cooldown_until      INTEGER NOT NULL DEFAULT 0,
                curtain_cooldown_until      INTEGER NOT NULL DEFAULT 0,
                trial_state                 TEXT    NOT NULL DEFAULT 'IDLE',
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
                has_execution_sword         INTEGER NOT NULL DEFAULT 0,
                jackpot_active              INTEGER NOT NULL DEFAULT 0,
                jackpot_end_tick            INTEGER NOT NULL DEFAULT 0,
                last_jackpot_attempt_tick   INTEGER NOT NULL DEFAULT 0,
                infinity_active             INTEGER NOT NULL DEFAULT 0,
                curtain_active              INTEGER NOT NULL DEFAULT 0,
                overtime_work               INTEGER NOT NULL DEFAULT 0,
                falling_blossom_active      INTEGER NOT NULL DEFAULT 0,
                falling_blossom_until       INTEGER NOT NULL DEFAULT 0,
                simple_barrier_active       INTEGER NOT NULL DEFAULT 0,
                shikigami_dmg_boost         REAL    NOT NULL DEFAULT 1.0,
                schema_version              INTEGER NOT NULL DEFAULT 1,
                zone_entry_tick             INTEGER NOT NULL DEFAULT -1,
                last_attack_tick            INTEGER NOT NULL DEFAULT -1,
                last_received_skill_id      TEXT,
                last_received_base_damage   INTEGER NOT NULL DEFAULT 0,
                last_received_ce_cost       INTEGER NOT NULL DEFAULT 0,
                last_received_cooldown_ticks INTEGER NOT NULL DEFAULT 0,
                last_received_is_domain     INTEGER NOT NULL DEFAULT 0,
                mahoraga_counter            INTEGER NOT NULL DEFAULT 0,
                black_flash_focus_end_tick  INTEGER NOT NULL DEFAULT 0,
                ten_shadows_active          INTEGER NOT NULL DEFAULT 0,
                shield_active               INTEGER NOT NULL DEFAULT 0,
                combo_count                 INTEGER NOT NULL DEFAULT 0,
                combo_last_hit_tick         INTEGER NOT NULL DEFAULT 0,
                combo_target_uuid           TEXT,
                last_login_day              INTEGER NOT NULL DEFAULT 0,
                last_damage_taken_tick      INTEGER NOT NULL DEFAULT 0,
                chanting                    INTEGER NOT NULL DEFAULT 0,
                chant_start_tick            INTEGER NOT NULL DEFAULT 0,
                received_guide_book         INTEGER NOT NULL DEFAULT 0,
                trial_target_uuid           TEXT,
                cursed_stones               INTEGER NOT NULL DEFAULT 0,
                mastery_reset_count         INTEGER NOT NULL DEFAULT 0,
                bounty                      INTEGER NOT NULL DEFAULT 0,
                weekly_quest_done           INTEGER NOT NULL DEFAULT -1,
                costume_id                  TEXT    NOT NULL DEFAULT 'default',
                blood_resource              INTEGER NOT NULL DEFAULT 0,
                character_reset_count       INTEGER NOT NULL DEFAULT 0,
                last_character_reset_timestamp INTEGER NOT NULL DEFAULT 0,
                has_received_selection_book INTEGER NOT NULL DEFAULT 0,
                captured_spirit_count       INTEGER NOT NULL DEFAULT 0,
                pending_binding_vow_skill_id TEXT,
                pending_binding_vow_start_tick INTEGER NOT NULL DEFAULT 0,
                vow_skill_used_this_vow     INTEGER NOT NULL DEFAULT 0,
                has_completed_tutorial      INTEGER NOT NULL DEFAULT 0
            )""";

    private static final String DDL_AUDIT_LOG = """
            CREATE TABLE IF NOT EXISTS audit_log (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp   INTEGER NOT NULL,
                event_type  TEXT    NOT NULL,
                player_uuid TEXT    NOT NULL,
                detail      TEXT
            )""";

    public void migrate(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(DDL_PLAYER_DATA);
            st.execute(DDL_AUDIT_LOG);
        }

        int version = getDbVersion(conn);
        while (version < CURRENT_VERSION) {
            applyMigration(conn, ++version);
            setDbVersion(conn, version);
        }
    }

    private int getDbVersion(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.getInt(1);
        }
    }

    private void setDbVersion(Connection conn, int version) throws SQLException {
        conn.createStatement().execute("PRAGMA user_version = " + version);
    }

    private void applyMigration(Connection conn, int targetVersion) throws SQLException {
        switch (targetVersion) {
            case 1 -> { /* initial schema created above via CREATE TABLE IF NOT EXISTS */ }
            case 2 -> {
                // 기존 DB 업그레이드용 — 신규 DB는 DDL에 이미 포함되어 있으므로 무시
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN zone_entry_tick  INTEGER NOT NULL DEFAULT -1"); }
                    catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_attack_tick INTEGER NOT NULL DEFAULT -1"); }
                    catch (SQLException ignored) {}
                }
            }
            case 3 -> {
                // §13-1 + §6-5 옷코츠 복사 술식 컬럼 추가
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_received_skill_id       TEXT"); }
                    catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_received_base_damage    INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_received_ce_cost        INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_received_cooldown_ticks INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_received_is_domain      INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                }
            }
            case 4 -> {
                // 메구미 마허라가 의식 카운터 + 이타도리 흑섬 집중 버프 만료 틱
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN mahoraga_counter           INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN black_flash_focus_end_tick INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                }
            }
            case 5 -> {
                // 메구미 식신 ID "silkworm" → "nue" 데이터 수정
                try (Statement st = conn.createStatement()) {
                    try {
                        st.execute("UPDATE player_data SET dead_shikigami_ids = REPLACE(dead_shikigami_ids, '\"silkworm\"', '\"nue\"') WHERE dead_shikigami_ids LIKE '%silkworm%'");
                    }
                    catch (SQLException ignored) {}
                }
            }
            case 6 -> {
                // 천여주박 CE 0 버프 필드 추가
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN ten_shadows_active INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                }
            }
            case 7 -> {
                // 콤보 시스템 + XP/등급 + Perfect 판정 필드
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN combo_count            INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN combo_last_hit_tick    INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN combo_target_uuid      TEXT"); }                      catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_login_day         INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_damage_taken_tick INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 8 -> {
                // 수동 방어 필드
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN shield_active INTEGER NOT NULL DEFAULT 0"); }
                    catch (SQLException ignored) {}
                }
            }
            case 9 -> {
                // 영창 시스템 필드
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN chanting         INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN chant_start_tick INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 10 -> {
                // 가이드북 수령 여부 (TASK-32)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN received_guide_book INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 11 -> {
                // 히구루마 재판 대상 UUID (TASK-34)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN trial_target_uuid TEXT"); } catch (SQLException ignored) {}
                }
            }
            case 12 -> {
                // 주력석 경제 시스템 (TASK-68)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN cursed_stones       INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN mastery_reset_count INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN bounty              INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 13 -> {
                // 주간 퀘스트 완료 epoch week (TASK-99)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN weekly_quest_done INTEGER NOT NULL DEFAULT -1"); } catch (SQLException ignored) {}
                }
            }
            case 14 -> {
                // 의상 시스템 (TASK-F)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN costume_id TEXT NOT NULL DEFAULT 'default'"); } catch (SQLException ignored) {}
                }
            }
            case 15 -> {
                // 쵸소 혈액 자원 (P1)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN blood_resource INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 16 -> {
                // CE 조작 성장 수치 (P3-2)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN ce_control REAL NOT NULL DEFAULT 1.0"); } catch (SQLException ignored) {}
                }
            }
            case 17 -> {
                // 퀘스트 진행 + 시즌 XP (P4-3, P4-7)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN quest_progress          TEXT    NOT NULL DEFAULT '{}'"); }  catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN completed_daily_quests  TEXT    NOT NULL DEFAULT '[]'"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN completed_weekly_quests TEXT    NOT NULL DEFAULT '[]'"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_quest_reset_day    INTEGER NOT NULL DEFAULT 0"); }    catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN season_xp              INTEGER NOT NULL DEFAULT 0"); }     catch (SQLException ignored) {}
                }
            }
            case 18 -> {
                // 캐릭터 재선택 이력 + 선택 책 지급 이력 (TASK A-1)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN character_reset_count          INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_character_reset_timestamp INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN has_received_selection_book    INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 19 -> {
                // 주령 포획 + 속박 서약 패널티 + 튜토리얼 완료 (PHASE G: G-2, G-4, G-5-2)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN captured_spirit_count         INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN pending_binding_vow_skill_id  TEXT"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN pending_binding_vow_start_tick INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN vow_skill_used_this_vow       INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN has_completed_tutorial        INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 20 -> {
                // 비술사(천여주박) 신체능력 강화 + 불굴 (PHASE H: H-6)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN ns_burst_expire_tick      INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN attack_boost_multiplier   REAL    NOT NULL DEFAULT 1.0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN defense_boost_multiplier  REAL    NOT NULL DEFAULT 1.0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN ns_shield_expire_tick     INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN ns_death_prevent_used     INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 21 -> {
                // 히구루마 단일-스킬 봉인 + 증거 강화 (Phase I-2)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN sealed_skills           TEXT    NOT NULL DEFAULT '[]'"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN seal_expire_tick        INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN last_used_skill_id      TEXT    NOT NULL DEFAULT ''"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN evidence_amplify_active INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
                }
            }
            case 22 -> {
                // 안티치트 — 이상행동 플래그 기록 + 격리 (TASK J-2)
                try (Statement st = conn.createStatement()) {
                    try { st.execute("ALTER TABLE player_data ADD COLUMN anti_abuse_flags TEXT    NOT NULL DEFAULT '[]'"); } catch (SQLException ignored) {}
                    try { st.execute("ALTER TABLE player_data ADD COLUMN quarantined      INTEGER NOT NULL DEFAULT 0"); }   catch (SQLException ignored) {}
                }
            }
        }
    }
}
