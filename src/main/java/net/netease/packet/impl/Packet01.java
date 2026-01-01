package net.netease.packet.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.PacketBuffer;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 31/01/2024
 */
@AllArgsConstructor
@Getter
public class Packet01 implements GermPacket {
    private String key;

    public Packet01() {
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        packetBuffer.writeString(key);
    }

    @Override
    public int getPacketId() {
        return 1;
    }
}
