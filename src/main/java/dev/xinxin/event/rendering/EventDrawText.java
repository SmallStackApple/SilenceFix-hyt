package dev.xinxin.event.rendering;

import dev.xinxin.event.api.events.Event;

public class EventDrawText
implements Event {
    public String text;

    public EventDrawText(String text) {
        this.text = text;
    }
}

