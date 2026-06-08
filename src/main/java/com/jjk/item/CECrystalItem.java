package com.jjk.item;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

// CE 결정체 — 소모 시 플레이어 CE +50 회복. NPC 교환 재료로도 사용.
public class CECrystalItem extends Item {

    public static CECrystalItem INSTANCE;

    public CECrystalItem() {
        super(new Settings().maxCount(64));
    }

    public static void register() {
        INSTANCE = Registry.register(
            Registries.ITEM,
            Identifier.of("jjk", "ce_crystal"),
            new CECrystalItem());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.pass(stack);
        if (!(user instanceof ServerPlayerEntity sp)) return TypedActionResult.pass(stack);
        if (JJKMod.getInstance() == null) return TypedActionResult.pass(stack);

        PlayerData data = JJKMod.getPlayerRepository().load(sp.getUuid());
        data.ceCurrent = Math.min(data.ceCurrent + 50f, data.ceMax);
        JJKMod.getPlayerRepository().save(data);

        stack.decrement(1);
        return TypedActionResult.success(stack);
    }
}
