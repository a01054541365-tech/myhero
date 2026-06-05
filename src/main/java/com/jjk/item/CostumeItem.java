package com.jjk.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CostumeItem extends Item {

    private final String costumeId;

    public CostumeItem(Settings settings, String costumeId) {
        super(settings);
        this.costumeId = costumeId;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.pass(stack);
        CostumeItemHandler.apply((ServerPlayerEntity) user, costumeId);
        return TypedActionResult.success(stack);
    }

    public String getCostumeId() { return costumeId; }
}
