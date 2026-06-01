package com.jjk.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.util.List;

/** 서버 입문 가이드북. 최초 접속 시 1회 지급. */
public final class GuideBookItem {
    private GuideBookItem() {}

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<RawFilteredPair<Text>> pages = List.of(
            RawFilteredPair.of(Text.literal(
                "=== JJK 서버 ===\n\n" +
                "/jj char select <이름>\n으로 캐릭터 선택\n\n" +
                "스킬: F / Shift+F\nR / Shift+R / V"
            )),
            RawFilteredPair.of(Text.literal(
                "=== 전투 ===\n\n" +
                "흑섬: 8~12틱 내\n연속 스킬 타이밍\n\n" +
                "Zone: 흑섬 발동 후\n10초간 전투력↑\n\n" +
                "각성: HP 5% 이하\n8초간 1.5배"
            )),
            RawFilteredPair.of(Text.literal(
                "=== 등급 성장 ===\n\n" +
                "전투 승리 +50XP\n흑섬 +15XP\n퍼펙트 +30XP\n\n" +
                "4급 → 특급\n등급별 스킬 해금"
            ))
        );
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT,
            new WrittenBookContentComponent(
                RawFilteredPair.of("주술사 안내서"),
                "주술고전",
                0,
                pages,
                true
            )
        );
        return book;
    }
}
