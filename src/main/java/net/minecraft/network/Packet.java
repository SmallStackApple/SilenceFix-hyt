package net.minecraft.network;

import java.io.IOException;

public interface Packet<T extends INetHandler> {
    public void readPacketData(PacketBuffer var1) throws IOException;

    public void writePacketData(PacketBuffer var1) throws IOException;

    public void processPacket(T var1);
}

