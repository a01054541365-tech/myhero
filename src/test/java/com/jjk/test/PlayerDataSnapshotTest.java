package com.jjk.test;

import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataSnapshotTest {

    @Test
    void testSnapshotAllFields() throws Exception {
        UUID uuid = UUID.randomUUID();
        PlayerData original = PlayerData.createDefault(uuid);
        original.characterId = "gojo";
        original.xp = 9999L;
        original.zoneActive = true;
        original.unlockedSkills.add("skill_f");
        original.cooldowns.put("test_key", 12345L);

        PlayerData snap = original.snapshot();

        Field[] fields = PlayerData.class.getDeclaredFields();
        assertTrue(fields.length > 0, "PlayerData 필드가 0개일 수 없음");

        for (Field f : fields) {
            f.setAccessible(true);
            Object orig = f.get(original);
            Object copy = f.get(snap);
            // 컬렉션은 동등성, 기본형·불변 객체는 동등성
            if (orig == null) {
                assertNull(copy, "필드 " + f.getName() + " 가 null이어야 함");
            } else {
                assertEquals(orig, copy, "필드 " + f.getName() + " 값 불일치");
            }
        }
    }

    @Test
    void testSnapshotIsolation() {
        PlayerData original = PlayerData.createDefault(UUID.randomUUID());
        original.cooldowns.put("key1", 100L);

        PlayerData snap = original.snapshot();
        original.cooldowns.put("key2", 200L);

        assertFalse(snap.cooldowns.containsKey("key2"),
                "snapshot 후 원본 cooldowns 변경이 snapshot에 영향 없어야 함");
    }

    @Test
    void testSnapshotListIsolation() {
        PlayerData original = PlayerData.createDefault(UUID.randomUUID());
        original.unlockedSkills.add("skill_a");

        PlayerData snap = original.snapshot();
        original.unlockedSkills.add("skill_b");

        assertFalse(snap.unlockedSkills.contains("skill_b"),
                "snapshot 후 원본 unlockedSkills 변경이 snapshot에 영향 없어야 함");
    }

    /** Reflection으로 Collection 필드가 독립 복사(deep copy)되었는지 검증. */
    @Test
    void testCollectionFieldsAreDeepCopied() throws Exception {
        PlayerData original = PlayerData.createDefault(UUID.randomUUID());
        original.cooldowns.put("test", 999L);
        original.unlockedSkills.add("skill_x");
        original.deadShikigamiIds.add("nue");

        PlayerData snap = original.snapshot();

        for (Field f : PlayerData.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            Object orig = f.get(original);
            Object copy = f.get(snap);
            if (orig instanceof List || orig instanceof Map) {
                assertNotSame(orig, copy,
                    "Collection 필드 " + f.getName() + " 는 deep copy여야 함 (다른 레퍼런스)");
            }
        }
    }

    /** H-2-1: snapshot()이 null을 반환하지 않음. */
    @Test
    void testSnapshotNotNull() {
        PlayerData original = PlayerData.createDefault(UUID.randomUUID());
        assertNotNull(original.snapshot(), "snapshot()은 null을 반환해서는 안 됨");
    }

    /** H-2-1: 기본 타입 필드는 snapshot 후 독립적으로 변경 가능 — 원본에 영향 없음. */
    @Test
    void testPrimitiveFieldIndependence() {
        PlayerData original = PlayerData.createDefault(UUID.randomUUID());
        original.xp = 500L;
        original.ceCurrent = 80f;
        original.awakeningActive = false;

        PlayerData snap = original.snapshot();
        // snapshot 필드를 수정해도 원본이 바뀌지 않아야 함
        snap.xp = 9999L;
        snap.ceCurrent = 1f;
        snap.awakeningActive = true;

        assertEquals(500L, original.xp,
                "snapshot xp 변경이 원본 xp에 영향 없어야 함");
        assertEquals(80f, original.ceCurrent, 0.001f,
                "snapshot ceCurrent 변경이 원본에 영향 없어야 함");
        assertFalse(original.awakeningActive,
                "snapshot awakeningActive 변경이 원본에 영향 없어야 함");
    }

    /** H-2-1: questProgress Map 독립성 — snapshot 후 원본 수정이 복사본에 영향 없음. */
    @Test
    void testQuestProgressMapIndependence() {
        PlayerData original = PlayerData.createDefault(UUID.randomUUID());
        original.questProgress.put("kill_count", 3);

        PlayerData snap = original.snapshot();
        original.questProgress.put("kill_count", 99);

        assertEquals(3, snap.questProgress.get("kill_count"),
                "snapshot 후 원본 questProgress 수정이 snapshot에 영향 없어야 함");
    }
}
