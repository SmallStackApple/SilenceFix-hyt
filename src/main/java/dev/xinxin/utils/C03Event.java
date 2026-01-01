package dev.xinxin.utils;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;

public class C03Event {
    public static final C03Event INSTANCE = new C03Event();
    public static int noMovePackets = 0;

    public static void packetEvent(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            noMovePackets = ((C03PacketPlayer)packet).isMoving() ? 0 : (noMovePackets = noMovePackets + 1);
        }

    }
}
