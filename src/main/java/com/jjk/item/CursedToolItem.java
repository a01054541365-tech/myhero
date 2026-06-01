package com.jjk.item;

import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager;
import net.minecraft.item.Item;

public class CursedToolItem extends Item {

    private final String toolId;

    public CursedToolItem(String toolId, Settings settings) {
        super(settings);
        this.toolId = toolId;
    }

    public String getToolId() { return toolId; }

    /** 플레이어 등급이 장착 조건을 만족하는지 확인 */
    public boolean isEligible(PlayerData data) {
        return isEligible(data, CursedToolRegistry.getEffect(toolId));
    }

    /** 순수 로직 헬퍼 — MC 컨텍스트 없이 테스트 가능 */
    public static boolean isEligible(PlayerData data, CursedToolEffect effect) {
        return GradeManager.Grade.fromLabel(data.grade).rank >= effect.requiredGrade();
    }
}
