package dev.xinxin.event.misc;

import dev.xinxin.event.api.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EventMouseOver extends EventCancellable {

    private double range;
    private float expand;

}
