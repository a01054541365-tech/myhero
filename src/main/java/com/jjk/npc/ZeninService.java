package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.economy.CursedStoneManager;
import com.jjk.grade.GradeManager.Grade;
import com.jjk.grade.MasterySystem;
import com.jjk.item.CursedCrystalItem;
import com.jjk.item.CursedToolItem;
import com.jjk.item.CursedToolRegistry;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ZeninService {

    private static final Map<String, Long> PRICES = Map.of(
        "cursed_dagger",    300L,
        "potion_ce",        150L,
        "potion_hp",        100L,
        "thousand_spear",  1200L,
        "playful_cloud",   3500L,
        "inverted_spear",  8000L,
        "split_soul_blade",6000L
    );

    private static final Map<String, Integer> GRADE_REQS = Map.of(
        "thousand_spear",   Grade.GRADE_1.rank,
        "playful_cloud",    Grade.SEMI_SPECIAL.rank,
        "inverted_spear",   Grade.SPECIAL.rank,
        "split_soul_blade", Grade.SPECIAL.rank
    );

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick) {
        return handle(packet, data, player, tick, JJKMod.getCursedStoneManager());
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick,
                                      CursedStoneManager csm) {
        if (csm == null) return SkillResult.FAIL;
        return switch (packet.action()) {
            case "buy"          -> buy(packet.param(), data, player, csm);
            case "appraise"     -> appraise(data, player, csm);
            case "crystal_buy"  -> crystalBuy(packet.param(), data, player);
            case "enhance"      -> enhanceTool(data, player);
            default             -> SkillResult.FAIL;
        };
    }

    private static SkillResult buy(String itemId, PlayerData data,
                                    ServerPlayerEntity player, CursedStoneManager csm) {
        Long price = PRICES.get(itemId);
        if (price == null) return SkillResult.FAIL;

        Integer gradeReq = GRADE_REQS.get(itemId);
        if (gradeReq != null && Grade.fromLabel(data.grade).rank < gradeReq) {
            return SkillResult.FAIL;
        }

        if (!csm.spend(data, price, "buy_" + itemId, player)) {
            return SkillResult.CE_INSUFFICIENT;
        }

        // 아이템 생성은 실제 플레이어가 있을 때만 시도
        if (player != null) {
            ItemStack item = createItem(itemId);
            if (!item.isEmpty() && !player.getInventory().insertStack(item)) {
                player.dropItem(item, false);
            }
        }
        return SkillResult.SUCCESS;
    }

    private static SkillResult appraise(PlayerData data, ServerPlayerEntity player,
                                         CursedStoneManager csm) {
        if (!csm.spend(data, 200L, "appraise", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        if (ThreadLocalRandom.current().nextFloat() < 0.02f) {
            String[] pool = {"cursed_dagger", "thousand_spear", "playful_cloud"};
            String won = pool[ThreadLocalRandom.current().nextInt(pool.length)];
            ItemStack item = createItem(won);
            if (player != null && !player.getInventory().insertStack(item)) {
                player.dropItem(item, false);
            }
            return SkillResult.SUCCESS;
        }
        return SkillResult.FAIL;
    }

    // ─── 결정체 교환 (P3-1) ───────────────────────────────────────────────────

    private static final Map<String, Integer> CRYSTAL_PRICES = Map.of(
        "mastery_xp",      50,
        "grade_xp",        80,
        "finger_tracker", 150
    );

    private static SkillResult crystalBuy(String itemId, PlayerData data,
                                           ServerPlayerEntity player) {
        if (itemId == null) return SkillResult.FAIL;
        Integer cost = CRYSTAL_PRICES.get(itemId);
        if (cost == null) return SkillResult.FAIL;

        if (countCrystals(player) < cost) {
            if (player != null) {
                player.sendMessage(
                    Text.literal("[JJK] CE 결정체가 부족합니다. (필요: " + cost + "개)"), true);
            }
            return SkillResult.CE_INSUFFICIENT;
        }
        consumeCrystals(player, cost);

        return switch (itemId) {
            case "mastery_xp" -> {
                MasterySystem.awardMastery(data, 100, player);
                if (player != null) player.sendMessage(
                    Text.literal("[JJK] 숙련도 +100 지급."), true);
                yield SkillResult.SUCCESS;
            }
            case "grade_xp" -> {
                if (JJKMod.getGradeManager() != null) {
                    JJKMod.getGradeManager().addXp(data, 200, player);
                }
                if (player != null) player.sendMessage(
                    Text.literal("[JJK] 등급 경험치 +200 지급."), true);
                yield SkillResult.SUCCESS;
            }
            case "finger_tracker" -> {
                ItemStack tracker = createFingerTracker();
                if (player != null && !player.getInventory().insertStack(tracker)) {
                    player.dropItem(tracker, false);
                }
                yield SkillResult.SUCCESS;
            }
            default -> SkillResult.FAIL;
        };
    }

    private static ItemStack createFingerTracker() {
        ItemStack stack = new ItemStack(Items.COMPASS);
        NbtCompound nbt = new NbtCompound();
        nbt.putString("jjk_type", "finger_tracker");
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("스쿠나 손가락 추적기").styled(s -> s.withColor(0xAA00AA).withItalic(false)));
        return stack;
    }

    // ─── 주구 강화 (P3-3) ────────────────────────────────────────────────────

    private static final int ENHANCE_CRYSTAL_COST = 20;
    private static final int ENHANCE_MAX_LEVEL    = 3;

    private static SkillResult enhanceTool(PlayerData data, ServerPlayerEntity player) {
        if (player == null) return SkillResult.FAIL;
        ItemStack held = player.getMainHandStack();
        if (!(held.getItem() instanceof CursedToolItem tool)) {
            player.sendMessage(Text.literal("[JJK] 주구를 손에 들어야 합니다."), true);
            return SkillResult.FAIL;
        }

        NbtComponent customData = held.get(DataComponentTypes.CUSTOM_DATA);
        int level = customData != null ? customData.copyNbt().getInt("enhance_level") : 0;
        if (level >= ENHANCE_MAX_LEVEL) {
            player.sendMessage(Text.literal("[JJK] 이미 최대 강화 단계(+" + ENHANCE_MAX_LEVEL + ")입니다."), true);
            return SkillResult.FAIL;
        }
        if (countCrystals(player) < ENHANCE_CRYSTAL_COST) {
            player.sendMessage(
                Text.literal("[JJK] CE 결정체 " + ENHANCE_CRYSTAL_COST + "개가 필요합니다."), true);
            return SkillResult.CE_INSUFFICIENT;
        }
        consumeCrystals(player, ENHANCE_CRYSTAL_COST);

        int newLevel = level + 1;
        NbtCompound nbt = customData != null ? customData.copyNbt() : new NbtCompound();
        nbt.putInt("enhance_level", newLevel);
        held.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        held.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("+" + newLevel + " " + getToolDisplayName(tool.getToolId()))
                .styled(s -> s.withColor(0xFFAA00).withItalic(false)));

        player.sendMessage(Text.literal("[JJK] 주구 강화 완료! +" + newLevel), false);
        return SkillResult.SUCCESS;
    }

    private static String getToolDisplayName(String toolId) {
        return switch (toolId) {
            case "cursed_dagger"    -> "저주받은 단검";
            case "thousand_spear"   -> "千본 선";
            case "playful_cloud"    -> "遊雲";
            case "inverted_spear"   -> "천역모의 창";
            case "split_soul_blade" -> "魂裂 도";
            default                 -> toolId;
        };
    }

    // ─── 결정체 인벤토리 유틸 ────────────────────────────────────────────────

    private static int countCrystals(ServerPlayerEntity player) {
        if (player == null || CursedCrystalItem.INSTANCE == null) return 0;
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == CursedCrystalItem.INSTANCE) total += stack.getCount();
        }
        return total;
    }

    private static void consumeCrystals(ServerPlayerEntity player, int amount) {
        if (player == null || CursedCrystalItem.INSTANCE == null) return;
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == CursedCrystalItem.INSTANCE) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
    }

    static ItemStack createItem(String itemId) {
        return switch (itemId) {
            case "cursed_dagger"    -> CursedToolRegistry.CURSED_DAGGER    != null
                ? new ItemStack(CursedToolRegistry.CURSED_DAGGER)    : ItemStack.EMPTY;
            case "thousand_spear"   -> CursedToolRegistry.THOUSAND_SPEAR   != null
                ? new ItemStack(CursedToolRegistry.THOUSAND_SPEAR)   : ItemStack.EMPTY;
            case "playful_cloud"    -> CursedToolRegistry.PLAYFUL_CLOUD    != null
                ? new ItemStack(CursedToolRegistry.PLAYFUL_CLOUD)    : ItemStack.EMPTY;
            case "inverted_spear"   -> CursedToolRegistry.INVERTED_SPEAR   != null
                ? new ItemStack(CursedToolRegistry.INVERTED_SPEAR)   : ItemStack.EMPTY;
            case "split_soul_blade" -> CursedToolRegistry.SPLIT_SOUL_BLADE != null
                ? new ItemStack(CursedToolRegistry.SPLIT_SOUL_BLADE) : ItemStack.EMPTY;
            case "potion_ce"        -> createCePotion();
            case "potion_hp"        -> createHpPotion();
            default                 -> ItemStack.EMPTY;
        };
    }

    static ItemStack createCePotion() {
        ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
        NbtCompound nbt = new NbtCompound();
        nbt.putString("jjk_type", "ce_potion");
        nbt.putInt("jjk_ce_restore", 200);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("주력 회복제").styled(s -> s.withColor(0x55FFFF).withItalic(false)));
        return stack;
    }

    static ItemStack createHpPotion() {
        ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
        NbtCompound nbt = new NbtCompound();
        nbt.putString("jjk_type", "hp_potion");
        nbt.putFloat("jjk_hp_restore", 5.0f);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("응급 치료제").styled(s -> s.withColor(0xFF5555).withItalic(false)));
        return stack;
    }

    private ZeninService() {}
}
