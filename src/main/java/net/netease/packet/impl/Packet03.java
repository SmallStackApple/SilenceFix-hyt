package net.netease.packet.impl;

import lombok.AllArgsConstructor;
import net.minecraft.network.PacketBuffer;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 01/02/2024
 */
@AllArgsConstructor
public class Packet03 implements GermPacket {
    private int key;
    private boolean state;

    public Packet03() {
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        packetBuffer.writeInt(key);
        packetBuffer.writeBoolean(state);
    }

    @Override
    public int getPacketId() {
        return 3;
    }
}
