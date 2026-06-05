package com.jjk.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.RawFilteredPair;

/** 서버 입문 가이드북. 최초 접속 시 1회 지급. /jj guide 로 재지급 가능. */
public final class GuideBookItem {
    private GuideBookItem() {}

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT,
            new WrittenBookContentComponent(
                RawFilteredPair.of("주술사 안내서"),
                "주술고전",
                0,
                GuideBookContent.getPages(),
                true
            )
        );
        return book;
    }
}
