package dev.xinxin.event.world;

import dev.xinxin.event.api.events.callables.EventCancellable;
import net.minecraft.network.Packet;

public class EventHigherPacketSend
        extends EventCancellable {
    private Packet<?> packet;

    public EventHigherPacketSend(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

}
