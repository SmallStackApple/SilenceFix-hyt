package dev.xinxin.event.misc;

import dev.xinxin.event.api.events.callables.EventCancellable;
import net.minecraft.entity.player.EntityPlayer;

public class EventInventory
extends EventCancellable {
    private final EntityPlayer player;

    public EventInventory(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return this.player;
    }
}

