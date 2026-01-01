package net.minecraft.client.resources;

import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;

public interface IResource {
    public ResourceLocation getResourceLocation();

    public InputStream getInputStream();

    public boolean hasMetadata();

    public <T extends IMetadataSection> T getMetadata(String var1);

    public String getResourcePackName();
}

