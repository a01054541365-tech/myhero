package com.jjk.client.renderer;

import com.jjk.entity.CeProjectileEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CeProjectileEntityRenderer extends EntityRenderer<CeProjectileEntity> {

    private static final Identifier TEXTURE =
            Identifier.of("minecraft", "textures/item/snowball.png");

    public CeProjectileEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(CeProjectileEntity entity) {
        return TEXTURE;
    }
}
