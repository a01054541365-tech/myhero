package com.jjk.client.renderer;

import com.jjk.entity.CursedSpiritEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CursedSpiritEntityRenderer extends BipedEntityRenderer<CursedSpiritEntity, BipedEntityModel<CursedSpiritEntity>> {

    private static final Identifier TEXTURE =
            Identifier.of("minecraft", "textures/entity/zombie/zombie.png");

    public CursedSpiritEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public Identifier getTexture(CursedSpiritEntity entity) {
        return TEXTURE;
    }
}
