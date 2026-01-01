package net.minecraft.network;

import dev.xinxin.utils.misc.MinecraftInstance;
import net.minecraft.network.play.client.C03PacketPlayer;

public class GetC03StatusUtil
implements MinecraftInstance {
    public static final GetC03StatusUtil INSTANCE = new GetC03StatusUtil();
    public static int noMovePackets = 0;

    public static void packetEvent(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            noMovePackets = ((C03PacketPlayer)packet).isMoving() ? 0 : (noMovePackets = noMovePackets + 1);
        }
    }
}

