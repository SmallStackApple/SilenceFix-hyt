package dev.xinxin.utils.component;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.*;
import net.minecraft.network.GetC03StatusUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;


public final class Class18 extends SilenceFix {
    public static Boolean pre = false;
    public static boolean cancelMove = false;
    private static double motionX = 0.0;
    private static double motionY = 0.0;
    private static double motionZ = 0.0;
    private static float fallDistance = 0.0f;
    private static int moveTicks = 0;

    public static void Method6() {
        if (mc.thePlayer == null) {
            return;
        }
        if (cancelMove) {
            return;
        }
        cancelMove = true;
        motionX = mc.thePlayer.motionX;
        motionY = mc.thePlayer.motionY;
        motionZ = mc.thePlayer.motionZ;
        fallDistance = mc.thePlayer.fallDistance;
    }

    public static void Method7() {
        cancelMove = false;
        moveTicks = 0;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.isPost()) {
            pre = false;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (cancelMove) {
            if (moveTicks > 0) {
                return;
            }
            mc.thePlayer.motionX = motionX;
            mc.thePlayer.motionZ = motionZ;
            mc.thePlayer.motionY = motionY;
            mc.thePlayer.fallDistance = fallDistance;
        }
    }

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
            if (event.getPacket() instanceof C03PacketPlayer && cancelMove && moveTicks > 0) {
                motionX = Class18.mc.thePlayer.motionX;
                motionZ = Class18.mc.thePlayer.motionZ;
                motionY = Class18.mc.thePlayer.motionY;
                fallDistance = Class18.mc.thePlayer.fallDistance;
                --moveTicks;
            }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.thePlayer == null) {
            Method7();
            return;
        }
        pre = true;
        if (cancelMove) {
            if (GetC03StatusUtil.noMovePackets >= 19) {
                mc.thePlayer.motionX = motionX;
                mc.thePlayer.motionY = motionY;
                mc.thePlayer.motionZ = motionZ;
                mc.thePlayer.fallDistance = fallDistance;
                ++moveTicks;
            }
            if (moveTicks > 0) {
                return;
            }
            mc.thePlayer.motionX = motionX;
            mc.thePlayer.motionZ = motionZ;
            mc.thePlayer.motionY = motionY;
            mc.thePlayer.fallDistance = fallDistance;
        }
    }

    @EventTarget
    public void onMove(EventMove event) {
        if (cancelMove) {
            if (moveTicks > 0) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onPacket(EventPacketReceive event) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId() && cancelMove) {
                mc.thePlayer.motionX = motionX;
                mc.thePlayer.motionY = motionY;
                mc.thePlayer.motionZ = motionZ;
                mc.thePlayer.fallDistance = fallDistance;
                ++moveTicks;
            }
    }
}

