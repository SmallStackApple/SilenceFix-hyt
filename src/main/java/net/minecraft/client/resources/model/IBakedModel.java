package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

import java.util.List;

public interface IBakedModel {
    public List<BakedQuad> getFaceQuads(EnumFacing var1);

    public List<BakedQuad> getGeneralQuads();

    public boolean isAmbientOcclusion();

    public boolean isGui3d();

    public boolean isBuiltInRenderer();

    public TextureAtlasSprite getParticleTexture();

    public ItemCameraTransforms getItemCameraTransforms();
}

