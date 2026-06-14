package com.jjk.item;

import com.jjk.JJKMod;
import com.jjk.character.CharacterRegistry;
import com.jjk.network.s2c.CharacterSelectS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;

public class CharacterSelectBookItem extends Item {

    public CharacterSelectBookItem() {
        super(new Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
        if (world.isClient()) {
            return TypedActionResult.success(user.getStackInHand(hand));
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ItemStack stack = user.getStackInHand(hand);

        if (!JJKMod.getConfig().allowCharacterReselect) {
            player.sendMessage(Text.literal("§c캐릭터 재선택이 비활성화되어 있습니다."), true);
            return TypedActionResult.fail(stack);
        }

        stack.decrement(1);
        ServerPlayNetworking.send(player,
            new CharacterSelectS2CPacket(new ArrayList<>(CharacterRegistry.ids())));
        return TypedActionResult.success(stack);
    }
}
