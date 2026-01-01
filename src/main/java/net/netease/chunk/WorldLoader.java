package net.netease.chunk;

import com.google.common.collect.Sets;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventWorldLoad;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class WorldLoader {
    @Setter
    private volatile String worldDirectoryName;
    @Getter
    private boolean isLoaded;
    @Getter
    private ChunkManager chunkManager;
    private static WorldLoader INSTANCE;

    public WorldLoader(){
        INSTANCE = this;
    }
    public void loadWorldData(WorldClient worldClient) {
        if (this.isLoaded) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("respath", "worlds/" + this.worldDirectoryName);
        this.setupChunkManager(worldClient, properties);
        this.isLoaded = true;
    }
    public static WorldLoader getInstance() {
        return INSTANCE;
    }
    @EventTarget
    private void onWorld(EventWorldLoad event) {
        chunkManager = null;
        isLoaded = false;
        RegionFileManager.clearRegionFileCache();
    }
    @EventTarget
    public void onClientTick(EventTick clientTickEvent) {
        if (!this.isLoaded()) {
            return;
        }
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if (entity == null) {
            return;
        }
        if (!(entity.worldObj instanceof WorldClient)) {
            return;
        }
        if (entity.ticksExisted % 40 != 0) {
            return;
        }
        HashSet<Long> loadedChunks = Sets.newHashSet();
        int renderDistance = Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
        double posX = entity.posX;
        double posZ = entity.posZ;
        int chunkX = MathHelper.floor_double(posX / 16.0);
        int chunkZ = MathHelper.floor_double(posZ / 16.0);
        for (int xOffset = -renderDistance; xOffset <= renderDistance; ++xOffset) {
            for (int zOffset = -renderDistance; zOffset <= renderDistance; ++zOffset) {
                int chunkPosX = chunkX + xOffset;
                int chunkPosZ = chunkZ + zOffset;
                loadedChunks.add(ChunkCoordIntPair.chunkXZ2Int(chunkPosX,chunkPosZ));
                if (this.isChunkLoaded(chunkPosX, chunkPosZ)) continue;
                this.loadChunk(chunkPosX, chunkPosZ);
            }
        }
        Set<Long> unloadedChunks = new HashSet<>(this.chunkManager.getLoadedChunks());
        unloadedChunks.removeAll(loadedChunks);
        for (Long l : unloadedChunks) {
            Chunk chunk = this.chunkManager.getLoadedChunk(l);
            this.unloadChunk(chunk.xPosition, chunk.zPosition);
        }
    }
    public void unloadChunk(int x, int z) {
        Minecraft.getMinecraft().theWorld.doPreChunk(x, z,false);
    }
    public boolean loadChunk(int x, int z) {
        WorldClient worldClient = Minecraft.getMinecraft().theWorld;
        worldClient.doPreChunk(x, z, true);
        if (this.isChunkLoaded(x, z)) {
            worldClient.markBlockRangeForRenderUpdate(x << 4, 0, z << 4, (x << 4) + 15, 256, (z << 4) + 15);
            return true;
        }
        return false;
    }
    public boolean isChunkLoaded(int x, int z) {
        return this.chunkManager.isChunkLoaded(x, z);
    }
    private void setupChunkManager(WorldClient worldClient, Properties properties) {
        this.chunkManager = new ChunkManager(worldClient, properties);
        Minecraft.getMinecraft().theWorld.clientChunkProvider = this.chunkManager;
        Minecraft.getMinecraft().theWorld.chunkProvider = this.chunkManager;
    }
}