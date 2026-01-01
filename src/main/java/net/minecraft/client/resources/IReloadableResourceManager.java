package net.minecraft.client.resources;

import java.util.List;

public interface IReloadableResourceManager
extends IResourceManager {
    public void reloadResources(List<IResourcePack> var1);

    public void registerReloadListener(IResourceManagerReloadListener var1);
}

