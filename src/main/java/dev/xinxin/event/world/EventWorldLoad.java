package dev.xinxin.event.world;

import dev.xinxin.event.api.events.Event;
import net.minecraft.client.multiplayer.WorldClient;

public class EventWorldLoad
implements Event {
    private final WorldClient world;

    public EventWorldLoad(WorldClient world) {
        this.world = world;
    }

    public WorldClient getWorld() {
        return this.world;
    }
}

