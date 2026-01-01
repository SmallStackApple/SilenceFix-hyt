package dev.xinxin.event.world;


import dev.xinxin.event.api.events.Event;
import net.minecraft.entity.Entity;

public class CriticalsEvent
implements Event {
    private Entity entity;

    public Entity getEntity() {
        return this.entity;
    }

    public CriticalsEvent(Entity entity) {
        this.entity = entity;
    }
}

