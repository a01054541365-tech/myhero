package com.jjk.item;

import com.jjk.JJKMod;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.OpenCharacterSelectS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;

// 캐릭터 선택 책. 우클릭 시 서버에서 OpenCharacterSelectS2CPacket 전송. 소비 없음.
public class CharacterSelectionBookItem extends Item {

    public CharacterSelectionBookItem() {
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
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        String currentCharId = data.characterId != null ? data.characterId : "";

        ServerPlayNetworking.send(player, new OpenCharacterSelectS2CPacket(
                new ArrayList<>(CharacterRegistry.ids()),
                currentCharId));

        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
