package net.netease.packet.impl;

import lombok.AllArgsConstructor;
import lombok.Setter;
import net.minecraft.network.PacketBuffer;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 29/12/2023
 */

@AllArgsConstructor
@Setter
public class Packet04 implements GermPacket {
    private String message;
    private String message3;
    private int n;
    private int n2;


    public Packet04(String message) {
        this.message = message;
        this.message3 = message;
    }

    public Packet04(String message, int n) {
        this.message = message;
        this.message3 = message;
        this.n = n;
    }

    public Packet04() {
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        packetBuffer.writeInt(n);
        packetBuffer.writeInt(n2);
        packetBuffer.writeString(this.message);
        packetBuffer.writeString(this.message);
        packetBuffer.writeString(this.message3);
    }

    @Override
    public int getPacketId() {
        return 4;
    }
}
