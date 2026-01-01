package net.minecraft.client.resources;

import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface IResourceManager {
    public Set<String> getResourceDomains();

    public IResource getResource(ResourceLocation var1) throws IOException;

    public List<IResource> getAllResources(ResourceLocation var1) throws IOException;
}

