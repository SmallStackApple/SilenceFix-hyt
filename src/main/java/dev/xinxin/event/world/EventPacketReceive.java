package dev.xinxin.event.world;

import dev.xinxin.event.api.events.callables.EventCancellable;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;

public class EventPacketReceive
extends EventCancellable {
    private Packet<?> packet;
    private INetHandler netHandler;
    private EnumPacketDirection direction;

    public EventPacketReceive(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public INetHandler getNetHandler() {
        return this.netHandler;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public EventPacketReceive(Packet<?> packet, INetHandler netHandler, EnumPacketDirection direction) {
        this.packet = packet;
        this.netHandler = netHandler;
        this.direction = direction;
    }

    public EventPacketReceive(Packet<?> packet, INetHandler netHandler) {
        this.packet = packet;
        this.netHandler = netHandler;
}

    public EnumPacketDirection getDirection() {
        return direction;
    }
}

