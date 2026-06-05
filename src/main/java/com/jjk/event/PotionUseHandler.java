package com.jjk.event;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class PotionUseHandler {

    public static void register() {
        UseItemCallback.EVENT.register(PotionUseHandler::onUseItem);
    }

    private static TypedActionResult<ItemStack> onUseItem(
            PlayerEntity player, World world, Hand hand) {
        if (world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
        if (!(player instanceof ServerPlayerEntity sp)) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        ItemStack stack = sp.getStackInHand(hand);
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return TypedActionResult.pass(stack);

        NbtCompound nbt = customData.copyNbt();
        String jjkType = nbt.getString("jjk_type");
        if (jjkType.isEmpty()) return TypedActionResult.pass(stack);

        PlayerData data = JJKMod.getPlayerRepository().load(sp.getUuid());
        boolean consumed = false;

        if ("ce_potion".equals(jjkType)) {
            int restore = nbt.getInt("jjk_ce_restore");
            data.ceCurrent = Math.min(data.ceCurrent + restore, data.ceMax);
            consumed = true;
        } else if ("hp_potion".equals(jjkType)) {
            float restore = nbt.getFloat("jjk_hp_restore");
            data.hpCurrent = Math.min(data.hpCurrent + restore, data.hpMax);
            consumed = true;
        }

        if (consumed) {
            stack.decrement(1);
            JJKMod.getPlayerRepository().saveImmediate(data);
            return TypedActionResult.success(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        return TypedActionResult.pass(stack);
    }

    private PotionUseHandler() {}
}
