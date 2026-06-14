package com.jjk.world;

import com.jjk.world.loot.JJKLootTable;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public final class ChestPlacer {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");
    private static final int CHEST_SLOTS = 27;

    public void placeChest(ServerWorld world, BlockPos pos, String tableKey, Random random) {
        world.setBlockState(pos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity chest)) {
            LOGGER.warn("[JJK] ChestPlacer: BlockEntity가 ChestBlockEntity가 아님 @ {}", pos);
            return;
        }
        int itemCount = 3 + random.nextInt(4); // 3~6개
        int slot = 0;
        for (int i = 0; i < itemCount && slot < CHEST_SLOTS; i++) {
            ItemStack stack = JJKLootTable.roll(tableKey, random);
            if (!stack.isEmpty()) {
                chest.setStack(slot, stack);
                slot++;
            }
        }
        chest.markDirty();
    }
}
