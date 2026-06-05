package com.jjk.client.renderer;

import com.jjk.entity.npc.SimpleNpcEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class NpcEntityRenderer extends BipedEntityRenderer<SimpleNpcEntity, BipedEntityModel<SimpleNpcEntity>> {

    private static final Identifier TEXTURE =
            Identifier.of("minecraft", "textures/entity/player/slim/alex.png");

    public NpcEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM)), 0.5f);
    }

    @Override
    public Identifier getTexture(SimpleNpcEntity entity) {
        return TEXTURE;
    }
}
