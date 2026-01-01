package dev.xinxin.event.attack;

import dev.xinxin.event.api.events.callables.EventCancellable;
import net.minecraft.entity.Entity;

public class AttackEvent
extends EventCancellable {
    private final Entity target;

    public AttackEvent(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return this.target;
    }
}

