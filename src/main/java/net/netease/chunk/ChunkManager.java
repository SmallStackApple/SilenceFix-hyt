package net.netease.chunk;/*

Decompiled with CFR 0.152.
Could not load the following classes:
com.google.common.collect.Sets
it.unimi.dsi.fastutil.longs.Long2ObjectMap
net.minecraft.client.Minecraft
net.minecraft.client.multiplayer.ChunkProviderClient
net.minecraft.util.math.ChunkPos
net.minecraft.world.World
net.minecraft.world.chunk.Chunk
org.apache.logging.log4j.LogManager
org.apache.logging.log4j.Logger
*/

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
/*

Duplicate member names - consider using --renamedupmembers true
*/
public class ChunkManager extends ChunkProviderClient {
    @Getter
    private final World world;
    private final LongHashMap<Chunk> chunkMap;
    @Getter
    private final Set<Long> loadedChunks = new HashSet<>();
    private static final Logger LOGGER = LogManager.getLogger();
    private final ChunkLoader customCache;
    public ChunkManager(World world, Properties properties) {
        super(world);
        this.world = world;
        File file = new File(Minecraft.getMinecraft().mcDataDir,"HeShuYou/" +"XinXin/"+ "HeDaWei/" +"HeWeiLin/" + properties.getProperty("respath"));
        this.customCache = new ChunkLoader(file);
        this.chunkMap = this.chunkMapping;
    }

    public boolean isChunkLoaded(int x, int z) {
        return this.loadedChunks.contains(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }
    public Chunk getLoadedChunk(long pos) {
        return this.chunkMap.getValueByKey(pos);
    }

    public void unloadChunk(int x, int z) {
        super.unloadChunk(x, z);
        this.loadedChunks.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }

    @Override
    public Chunk loadChunk(int x, int z) {
        try {
            Chunk chunk = this.customCache.loadChunk(this.world, x, z);
            if (chunk != null) {
                chunk.setLastSaveTime(this.world.getTotalWorldTime());
                long pos = ChunkCoordIntPair.chunkXZ2Int(x, z);
                this.loadedChunks.add(pos);
                chunk.setChunkLoaded(true);
                this.chunkMap.add(pos, chunk);
                return chunk;
            }
        } catch (Exception exception) {
            LOGGER.error("Couldn't load res chunk", (Throwable) exception);
        }
        return super.loadChunk(x, z);
    }
}