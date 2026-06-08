package com.jjk.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// 영역 블록 변경 이력 DAO.
// 기존 PlayerRepository SQLite 연결(conn)을 재사용 — 새 Connection 열지 않음.
// 비동기 쓰기: CompletableFuture.runAsync() (기존 PlayerRepository 패턴 동일).
public class DomainBlockHistoryDao {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-domain-history");
    private final Connection conn;

    public record BlockHistoryEntry(long id, String domainId, String worldKey,
                                     int x, int y, int z,
                                     String originalState, String changedState) {}

    public DomainBlockHistoryDao(Connection conn) {
        this.conn = conn;
    }

    // 영역 전개 시 변경 블록 일괄 insert.
    // INSERT OR IGNORE — (domain_id, world_key, pos_x, pos_y, pos_z) 중복 방지.
    public void insertBatch(String domainId, String worldKey, List<BlockHistoryEntry> entries) {
        if (entries.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR IGNORE INTO domain_block_history
                (domain_id, world_key, pos_x, pos_y, pos_z,
                 original_state, changed_state, recovered, created_at)
                VALUES (?,?,?,?,?,?,?,0,?)
                """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                for (BlockHistoryEntry e : entries) {
                    ps.setString(1, domainId);
                    ps.setString(2, worldKey);
                    ps.setInt(3, e.x());
                    ps.setInt(4, e.y());
                    ps.setInt(5, e.z());
                    ps.setString(6, e.originalState());
                    ps.setString(7, e.changedState());
                    ps.setLong(8, now);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException ex) {
                LOGGER.error("insertBatch failed for domain {}", domainId, ex);
            }
        });
    }

    // 단건 복구 완료 표시.
    public void markRecovered(long id) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE domain_block_history SET recovered=1 WHERE id=?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            } catch (SQLException ex) {
                LOGGER.error("markRecovered failed for id {}", id, ex);
            }
        });
    }

    // 도메인 전체 복구 완료 표시 (도메인 정상 종료 시 호출).
    public void markAllRecovered(String domainId) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE domain_block_history SET recovered=1 WHERE domain_id=?")) {
                ps.setString(1, domainId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                LOGGER.error("markAllRecovered failed for domain {}", domainId, ex);
            }
        });
    }

    // 서버 시작 시 미복구 항목 전체 조회 (메인 스레드, 1회).
    public List<BlockHistoryEntry> loadUnrecovered() {
        List<BlockHistoryEntry> result = new ArrayList<>();
        String sql = """
            SELECT id, domain_id, world_key, pos_x, pos_y, pos_z,
                   original_state, changed_state
            FROM domain_block_history WHERE recovered=0
            """;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new BlockHistoryEntry(
                    rs.getLong("id"),
                    rs.getString("domain_id"),
                    rs.getString("world_key"),
                    rs.getInt("pos_x"),
                    rs.getInt("pos_y"),
                    rs.getInt("pos_z"),
                    rs.getString("original_state"),
                    rs.getString("changed_state")
                ));
            }
        } catch (SQLException ex) {
            LOGGER.error("loadUnrecovered failed", ex);
        }
        return result;
    }

    // 7일 이상 지난 복구 완료 항목 정리.
    public void purgeOldRecovered(long olderThanMs) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM domain_block_history WHERE recovered=1 AND created_at < ?")) {
                ps.setLong(1, olderThanMs);
                int deleted = ps.executeUpdate();
                if (deleted > 0) LOGGER.info("[JJK] 오래된 복구 기록 {}개 정리", deleted);
            } catch (SQLException ex) {
                LOGGER.error("purgeOldRecovered failed", ex);
            }
        });
    }
}
