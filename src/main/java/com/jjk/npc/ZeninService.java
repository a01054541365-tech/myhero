package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.economy.CursedStoneManager;
import com.jjk.grade.GradeManager.Grade;
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
            case "buy"      -> buy(packet.param(), data, player, csm);
            case "appraise" -> appraise(data, player, csm);
            default         -> SkillResult.FAIL;
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
