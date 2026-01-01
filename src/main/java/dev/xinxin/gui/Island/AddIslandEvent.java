package dev.xinxin.gui.Island;


import dev.xinxin.event.api.events.callables.EventCancellable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddIslandEvent extends EventCancellable {

    public EventState State;

    public AddIslandEvent(EventState state) {
        State = state;
    }

    public boolean isPre() {
        return State == EventState.PRE;
    }

    public boolean isPost() {
        return State == EventState.POST;
    }

    public enum EventState {
        PRE,
        POST
    }
}
