package com.jjk.item.cursedtool;

import net.minecraft.item.Item;

public class CursedToolItem extends Item {

    private final CursedToolGrade  grade;
    private final CursedToolEffect effect;
    private final String           toolId;

    public CursedToolItem(CursedToolGrade grade, CursedToolEffect effect, String toolId) {
        super(new Settings().maxCount(1));
        this.grade  = grade;
        this.effect = effect;
        this.toolId = toolId;
    }

    public CursedToolGrade  getCursedToolGrade()  { return grade;  }
    public CursedToolEffect getCursedToolEffect() { return effect; }
    public String           getToolId()           { return toolId; }
}
