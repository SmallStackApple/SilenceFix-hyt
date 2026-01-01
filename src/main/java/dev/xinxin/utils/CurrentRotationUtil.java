package dev.xinxin.utils;

import dev.xinxin.utils.misc.MinecraftInstance;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.lwjgl.util.vector.Vector2f;

public class CurrentRotationUtil
        implements MinecraftInstance {
    public static Vector2f currentRotation = new Vector2f(0.0f, 0.0f);

    public static void getCurrentRotation(C03PacketPlayer rotationPacket) {
        if (rotationPacket.rotating) {
            currentRotation = new Vector2f(rotationPacket.getYaw(), rotationPacket.getPitch());
        }
    }
}
