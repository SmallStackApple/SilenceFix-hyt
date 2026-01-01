package dev.yalan.live.silencefix.events;

import dev.xinxin.event.api.events.Event;

public class EventLiveAuthenticationResult implements Event {
    private final boolean isSuccess;
    private final String message;

    public EventLiveAuthenticationResult(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }
}
