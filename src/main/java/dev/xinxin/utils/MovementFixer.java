package dev.xinxin.utils;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;


public class MovementFixer {
    public static final MovementFixer INSTANCE = new MovementFixer();
    public static Boolean pre = false;
    public static boolean cancelMove = false;
    private static double motionX = 0.0;
    private static double motionY = 0.0;
    private static double motionZ = 0.0;
    private static float fallDistance = 0.0f;
    private static int moveTicks = 0;

    public static float Method1() {
        return (float)Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public static void Method2() {
        Method2(Method1());
    }

    public static boolean Method3() {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0.0f || mc.thePlayer.movementInput.moveStrafe != 0.0f);
    }

    public static void Method2(float speed) {
        if (!Method3()) {
            return;
        }
        double yaw = Method5();
        mc.thePlayer.motionX = -Math.sin(yaw) * (double)speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * (double)speed;
    }

    public static void Method4(double length) {
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        mc.thePlayer.setPosition(mc.thePlayer.posX + -Math.sin(yaw) * length, mc.thePlayer.posY, mc.thePlayer.posZ + Math.cos(yaw) * length);
    }

    public static double Method5() {
        float rotationYaw = mc.thePlayer.rotationYaw;
        if (mc.thePlayer.moveForward < 0.0f) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (mc.thePlayer.moveForward < 0.0f) {
            forward = -0.5f;
        } else if (mc.thePlayer.moveForward > 0.0f) {
            forward = 0.5f;
        }
        if (mc.thePlayer.moveStrafing > 0.0f) {
            rotationYaw -= 90.0f * forward;
        }
        if (mc.thePlayer.moveStrafing < 0.0f) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
    }

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

    public static double Method8(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (moveForward < 0.0) {
            forward = -0.5f;
        } else if (moveForward > 0.0) {
            forward = 0.5f;
        }
        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0f * forward;
        }
        if (moveStrafing < 0.0) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
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
    public void onPacket(EventPacketSend event) {
        if (event.getPacket() instanceof C03PacketPlayer && cancelMove && moveTicks > 0) {
            motionX = mc.thePlayer.motionX;
            motionZ = mc.thePlayer.motionZ;
            motionY = mc.thePlayer.motionY;
            fallDistance = mc.thePlayer.fallDistance;
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
            if (C03Event.noMovePackets >= 20) {
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
            event.setCancelled();
        }
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        S12PacketEntityVelocity s12;
        Packet<?> packet = event.getPacket();
        if (packet instanceof S12PacketEntityVelocity && (s12 = (S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId() && cancelMove) {
            mc.thePlayer.motionX = motionX;
            mc.thePlayer.motionY = motionY;
            mc.thePlayer.motionZ = motionZ;
            mc.thePlayer.fallDistance = fallDistance;
            ++moveTicks;
        }
    }
}
