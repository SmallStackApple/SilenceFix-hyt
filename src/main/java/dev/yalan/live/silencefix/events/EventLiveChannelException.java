package dev.yalan.live.silencefix.events;

import dev.xinxin.event.api.events.Event;

public class EventLiveChannelException implements Event {
    private final Throwable cause;

    public EventLiveChannelException(Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
