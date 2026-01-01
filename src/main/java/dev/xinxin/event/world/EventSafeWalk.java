package dev.xinxin.event.world;

import dev.xinxin.event.api.events.callables.EventCancellable;

public class EventSafeWalk
extends EventCancellable {
    public EventSafeWalk(boolean safeWalking) {
        this.setCancelled(safeWalking);
    }
}

