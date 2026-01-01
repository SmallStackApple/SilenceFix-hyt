package dev.xinxin.event.rendering;

import dev.xinxin.event.api.events.callables.EventCancellable;

public class EventShader extends EventCancellable {
    private final boolean bloom;
    private final String source; // 👈 添加来源字段

    public EventShader(boolean bloom, String source) {
        this.bloom = bloom;
        this.source = source;
    }

    public boolean isBloom() {
        return this.bloom;
    }

    public String getSource() {
        return this.source;
    }
}


