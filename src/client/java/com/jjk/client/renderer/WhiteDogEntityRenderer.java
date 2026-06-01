package com.jjk.client.renderer;

import com.jjk.entity.ShikigamiEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class WhiteDogEntityRenderer extends GeoEntityRenderer<ShikigamiEntity> {

    public WhiteDogEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new WhiteDogGeoModel());
    }

    private static class WhiteDogGeoModel extends GeoModel<ShikigamiEntity> {
        private static final Identifier MODEL      = Identifier.of("jjk", "geo/white_dog.geo.json");
        private static final Identifier TEXTURE    = Identifier.of("jjk", "textures/entity/white_dog.png");
        private static final Identifier ANIMATIONS = Identifier.of("jjk", "animations/white_dog.animation.json");

        @Override public Identifier getModelResource(ShikigamiEntity e)     { return MODEL; }
        @Override public Identifier getTextureResource(ShikigamiEntity e)   { return TEXTURE; }
        @Override public Identifier getAnimationResource(ShikigamiEntity e) { return ANIMATIONS; }
    }
}
