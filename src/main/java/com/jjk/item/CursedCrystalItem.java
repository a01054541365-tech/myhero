package com.jjk.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

// CE 결정체 — 주령 처치·흑섬 발동으로 획득하는 기본 화폐 아이템
public class CursedCrystalItem extends Item {

    public static CursedCrystalItem INSTANCE;

    public CursedCrystalItem() {
        super(new Settings().maxCount(64));
    }

    public static void register() {
        INSTANCE = Registry.register(
            Registries.ITEM,
            Identifier.of("jjk", "cursed_crystal"),
            new CursedCrystalItem());
    }
}
