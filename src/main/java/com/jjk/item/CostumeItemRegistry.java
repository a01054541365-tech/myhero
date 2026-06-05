package com.jjk.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class CostumeItemRegistry {
    private CostumeItemRegistry() {}

    public static CostumeItem GOJO_BLINDFOLD;
    public static CostumeItem GOJO_SUNGLASSES;
    public static CostumeItem SUKUNA_TATTOOED;
    public static CostumeItem ITADORI_UNIFORM;
    public static CostumeItem NANAMI_SUIT;
    public static CostumeItem CURSED_SPIRIT_RED;
    public static CostumeItem CURSED_SPIRIT_BLUE;

    public static void register() {
        GOJO_BLINDFOLD   = reg("costume_gojo_blindfold",    "gojo_blindfold");
        GOJO_SUNGLASSES  = reg("costume_gojo_sunglasses",   "gojo_sunglasses");
        SUKUNA_TATTOOED  = reg("costume_sukuna_tattooed",   "sukuna_tattooed");
        ITADORI_UNIFORM  = reg("costume_itadori_uniform",   "itadori_uniform");
        NANAMI_SUIT      = reg("costume_nanami_suit",        "nanami_suit");
        CURSED_SPIRIT_RED  = reg("costume_cursed_spirit_red",  "cursed_spirit_red");
        CURSED_SPIRIT_BLUE = reg("costume_cursed_spirit_blue", "cursed_spirit_blue");
    }

    private static CostumeItem reg(String id, String costumeId) {
        return Registry.register(
            Registries.ITEM,
            Identifier.of("jjk", id),
            new CostumeItem(new Item.Settings().maxCount(1), costumeId)
        );
    }
}
