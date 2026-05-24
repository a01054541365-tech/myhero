package com.jjk.client.renderer;

import com.jjk.entity.RikaEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class RikaEntityRenderer extends GeoEntityRenderer<RikaEntity> {

    public RikaEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new RikaEntityModel());
    }

    private static class RikaEntityModel extends GeoModel<RikaEntity> {
        private static final Identifier MODEL      = Identifier.of("jjk", "geo/rika.geo.json");
        private static final Identifier TEXTURE    = Identifier.of("jjk", "textures/entity/rika.png");
        private static final Identifier ANIMATIONS = Identifier.of("jjk", "animations/rika.animation.json");

        @Override public Identifier getModelResource(RikaEntity e)     { return MODEL; }
        @Override public Identifier getTextureResource(RikaEntity e)   { return TEXTURE; }
        @Override public Identifier getAnimationResource(RikaEntity e) { return ANIMATIONS; }
    }
}
