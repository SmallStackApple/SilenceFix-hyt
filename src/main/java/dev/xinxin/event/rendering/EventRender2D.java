package dev.xinxin.event.rendering;

import dev.xinxin.event.api.events.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
@Getter
@Setter
public class EventRender2D
implements Event {
    private float partialTicks;
    private ScaledResolution scaledResolution;
    public EventRender2D(float partialTicks, ScaledResolution resolution) {
        this.partialTicks = partialTicks;
        this.scaledResolution = resolution;
    }

}

