package com.jjk.world.loot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class JJKLootTable {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");
    private static final Map<String, List<LootTableEntry>> TABLES = new HashMap<>();

    static {
        TABLES.put("grade_4", List.of(
            new LootTableEntry("jjk:cursed_dagger",  60f, 1, 1),
            new LootTableEntry("jjk:cursed_orb",     30f, 1, 1),
            new LootTableEntry("jjk:throwing_tool",  10f, 1, 3)
        ));
        TABLES.put("grade_3", List.of(
            new LootTableEntry("jjk:cursed_dagger",  40f, 1, 1),
            new LootTableEntry("jjk:seal_talisman",  35f, 1, 2),
            new LootTableEntry("jjk:throwing_tool",  25f, 1, 3)
        ));
        TABLES.put("grade_2", List.of(
            new LootTableEntry("jjk:ce_staff",       50f, 1, 1),
            new LootTableEntry("jjk:technique_sword",35f, 1, 1),
            new LootTableEntry("jjk:cursed_orb",     15f, 1, 1)
        ));
        TABLES.put("grade_1", List.of(
            new LootTableEntry("jjk:nanami_blunt",   50f, 1, 1),
            new LootTableEntry("jjk:technique_sword",30f, 1, 1),
            new LootTableEntry("jjk:dragon_bone",    20f, 1, 1)
        ));
        TABLES.put("special_grade", List.of(
            new LootTableEntry("jjk:playful_cloud",      40f, 1, 1),
            new LootTableEntry("jjk:chokoku",            30f, 1, 1),
            new LootTableEntry("jjk:kamutoke",           20f, 1, 1),
            new LootTableEntry("jjk:split_soul_katana",  10f, 1, 1)
        ));
        // 주술고전 건물 전용 테이블 (기존 등급 테이블 재사용)
        TABLES.put("building_chest_3grade", TABLES.get("grade_3"));
        TABLES.put("building_chest_2grade", TABLES.get("grade_2"));
    }

    private JJKLootTable() {}

    public static String selectGradeTableKey(Random random) {
        float roll = random.nextFloat() * 100f;
        if (roll < 45f) return "grade_4";
        if (roll < 75f) return "grade_3";
        if (roll < 90f) return "grade_2";
        if (roll < 99f) return "grade_1";
        return "special_grade";
    }

    public static ItemStack roll(String tableKey, Random random) {
        List<LootTableEntry> table = TABLES.get(tableKey);
        if (table == null || table.isEmpty()) return ItemStack.EMPTY;

        float totalWeight = 0f;
        for (LootTableEntry e : table) totalWeight += e.weight();

        float pick = random.nextFloat() * totalWeight;
        float cumulative = 0f;
        for (LootTableEntry e : table) {
            cumulative += e.weight();
            if (pick < cumulative) return createStack(e, random);
        }
        return createStack(table.get(table.size() - 1), random);
    }

    private static ItemStack createStack(LootTableEntry entry, Random random) {
        Identifier id = Identifier.tryParse(entry.itemId());
        if (id == null) {
            LOGGER.warn("[JJK] JJKLootTable: 잘못된 itemId={}", entry.itemId());
            return ItemStack.EMPTY;
        }
        if (!Registries.ITEM.containsId(id)) {
            LOGGER.warn("[JJK] JJKLootTable: 등록되지 않은 아이템={}", entry.itemId());
            return ItemStack.EMPTY;
        }
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        int range = entry.maxCount() - entry.minCount();
        int count = entry.minCount() + (range > 0 ? random.nextInt(range + 1) : 0);
        return new ItemStack(item, count);
    }
}
