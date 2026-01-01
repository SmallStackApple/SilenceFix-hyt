package net.netease.packet.impl;

import lombok.AllArgsConstructor;
import net.minecraft.network.PacketBuffer;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 28/12/2023
 */

@AllArgsConstructor
public class Packet16 implements GermPacket {
    private String version;
    private String message;


    public Packet16() {
    }

    @Override
    public void process() {
        if (Packet731.flag) {
            
        }
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.version);
        packetBuffer.writeString(this.message);
    }

    public int getPacketId() {
        return 16;
    }
}
