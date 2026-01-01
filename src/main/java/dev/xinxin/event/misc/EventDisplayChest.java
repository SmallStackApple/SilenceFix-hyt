package dev.xinxin.event.misc;

import dev.xinxin.event.api.events.callables.EventCancellable;

public class EventDisplayChest
extends EventCancellable {
    private String q;

    public EventDisplayChest(String sb) {
        this.q = sb;
    }

    public String getString() {
        return this.q;
    }
}

