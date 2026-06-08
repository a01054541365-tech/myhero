package com.jjk.item;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class JJKItems {

    public static CharacterSelectionBookItem CHARACTER_SELECTION_BOOK;
    public static CECrystalItem CE_CRYSTAL;

    private JJKItems() {}

    public static void register() {
        CHARACTER_SELECTION_BOOK = Registry.register(
                Registries.ITEM,
                Identifier.of("jjk", "character_selection_book"),
                new CharacterSelectionBookItem());

        CECrystalItem.register();
        CE_CRYSTAL = CECrystalItem.INSTANCE;
    }
}
