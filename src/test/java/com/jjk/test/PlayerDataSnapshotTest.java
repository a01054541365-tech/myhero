package com.jjk.test;

import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
}
