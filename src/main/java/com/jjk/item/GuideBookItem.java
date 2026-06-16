package com.jjk.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.List;

/** 서버 입문 가이드북. 최초 접속 시 1회 지급. /jj help 로 재지급 가능. */
public final class GuideBookItem {
    private GuideBookItem() {}

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.BOOK);
        book.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6[주술사 가이드]"));
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("jjk_guide_book", true);
        book.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        book.set(DataComponentTypes.LORE, new LoreComponent(
            List.of(Text.literal("§7/jj help 로 다시 받을 수 있습니다"))
        ));
        return book;
    }

    public static boolean isGuideBook(ItemStack stack) {
        if (stack.getItem() != Items.BOOK) return false;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp != null && comp.copyNbt().getBoolean("jjk_guide_book");
    }
}
