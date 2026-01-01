package dev.xinxin.event.world;

import dev.xinxin.event.api.events.callables.EventCancellable;
import net.minecraft.entity.player.EntityPlayer;

public class EventPlayerDead extends EventCancellable {
    private final EntityPlayer player;

    public EventPlayerDead(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
