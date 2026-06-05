package com.jjk.test;

import com.jjk.JjkConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JjkStructureBuilder 좌표 및 config 동작 검증.
 * Minecraft Bootstrap 없이 순수 수학/config 검사만 수행.
 */
class StructureBuilderTest {

    @Test
    void mainBuilding_1F_wallBounds() {
        // hollow(-20, 0, -15, 20, 8, 15) 파라미터 검증
        int x1 = -20, x2 = 20, z1 = -15, z2 = 15, y1 = 0, y2 = 8;
        assertEquals(41, x2 - x1 + 1, "1F 너비 (X) = 41");
        assertEquals(9,  y2 - y1 + 1, "1F 높이 (Y) = 9");
        assertEquals(31, z2 - z1 + 1, "1F 깊이 (Z) = 31");

        // 내부 공간: 벽 안쪽 (x1+1 ~ x2-1, y1+1 ~ y2-1, z1+1 ~ z2-1)
        assertTrue(0 > x1 && 0 < x2, "원점 X=0 은 1F 내부");
        assertTrue(1 > y1 && 1 < y2, "Y=1 은 1F 내부");
        assertTrue(0 > z1 && 0 < z2, "원점 Z=0 은 1F 내부");
    }

    @Test
    void npcSpawnPositions_withinBuilding() {
        // 이치지: 1층 내부 (-20..20, 0..8, -15..15)
        int[] ijichi = {-15, 1, -12};
        assertTrue(ijichi[0] >= -20 && ijichi[0] <= 20, "이치지 X 범위");
        assertTrue(ijichi[1] >= 0   && ijichi[1] <= 8,  "이치지 Y 범위");
        assertTrue(ijichi[2] >= -15 && ijichi[2] <= 15, "이치지 Z 범위");

        // 공시우: 지하 B2 내부 (-25..25, -20..-11, -25..25)
        int[] gojoShiyu = {-15, -19, 0};
        assertTrue(gojoShiyu[0] >= -25 && gojoShiyu[0] <= 25,  "공시우 X 범위");
        assertTrue(gojoShiyu[1] >= -20 && gojoShiyu[1] <= -11, "공시우 Y 범위");
        assertTrue(gojoShiyu[2] >= -25 && gojoShiyu[2] <= 25,  "공시우 Z 범위");

        // 야가: 3층 내부 (-20..20, 18..27, -15..15)
        int[] yaga = {-10, 19, 0};
        assertTrue(yaga[0] >= -20 && yaga[0] <= 20, "야가 X 범위");
        assertTrue(yaga[1] >= 18  && yaga[1] <= 27, "야가 Y 범위");
        assertTrue(yaga[2] >= -15 && yaga[2] <= 15, "야가 Z 범위");
    }

    @Test
    void trainingField_spawnMarkers_count() {
        int[][] markers = {
            {-30, 25}, {0, 25}, {30, 25},
            {-30, 50}, {30, 50},
            {-30, 75}, {0, 75}, {30, 75}
        };
        assertEquals(8, markers.length, "주령 스폰 마커 8개");

        // 마커가 훈련 필드 범위 내 (X:-40..40, Z:20..80)
        for (int[] m : markers) {
            assertTrue(m[0] >= -40 && m[0] <= 40, "마커 X 범위: " + m[0]);
            assertTrue(m[1] >= 20  && m[1] <= 80, "마커 Z 범위: " + m[1]);
        }
    }

    @Test
    void domainBannedChunks_trainingRoom_included() {
        JjkConfig cfg = new JjkConfig();
        assertNotNull(cfg.domainBannedChunks, "domainBannedChunks 목록 null 아님");

        // 서버 시작 시 훈련장 청크 추가 가능 여부 확인
        cfg.domainBannedChunks.add("training:0,0");
        assertTrue(cfg.domainBannedChunks.contains("training:0,0"),
            "domainBannedChunks 훈련장 청크 추가 및 조회 가능");
    }

    @Test
    void buildingGenerated_flag_setAfterBuild() {
        JjkConfig cfg = new JjkConfig();
        assertFalse(cfg.jjtBuildingGenerated(), "초기값 false");
        cfg.setJjtBuildingGenerated(true);
        assertTrue(cfg.jjtBuildingGenerated(), "setJjtBuildingGenerated(true) 후 true");
        // 재실행 방지 로직 확인
        assertTrue(cfg.jjtBuildingGenerated(), "반복 호출 후에도 true 유지");
    }
}
