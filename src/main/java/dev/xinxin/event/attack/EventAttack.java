package dev.xinxin.event.attack;

import dev.xinxin.event.api.events.Event;
import net.minecraft.entity.Entity;

public class EventAttack
implements Event {
    private final boolean pre;
    private Entity target;

    public EventAttack(Entity entity, boolean pre) {
        this.target = entity;
        this.pre = pre;
    }


    public Entity getTarget() {
        return this.target;
    }

    public void setTarget(Entity target) {
        this.target = target;
    }

    public boolean isPre() {
        return this.pre;
    }

    public boolean isPost() {
        return !this.pre;
    }
}

