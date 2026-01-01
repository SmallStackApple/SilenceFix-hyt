package dev.xinxin.event.api.events.callables;

import dev.xinxin.event.api.events.Cancellable;
import dev.xinxin.event.api.events.Event;

public abstract class EventCancellable
        implements Event,
        Cancellable {
    private boolean cancelled;

    protected EventCancellable() {
    }


    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public void setCancelled() {
        this.cancelled = true;
    }
}
