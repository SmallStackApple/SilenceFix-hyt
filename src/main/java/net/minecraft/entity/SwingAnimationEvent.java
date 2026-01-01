package net.minecraft.entity;

import dev.xinxin.event.api.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class SwingAnimationEvent extends EventCancellable {

    private int animationEnd;

}
