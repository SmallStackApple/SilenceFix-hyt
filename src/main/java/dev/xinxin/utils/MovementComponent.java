package dev.xinxin.utils;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.*;
import dev.xinxin.utils.misc.MinecraftInstance;
import net.minecraft.network.GetC03StatusUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public final class MovementComponent
        implements MinecraftInstance {
    public static final MovementComponent INSTANCE = new MovementComponent();
    public static Boolean pre = false;
    public static boolean cancelMove = false;
    private static double motionX = 0.0;
    private static double motionY = 0.0;
    private static double motionZ = 0.0;
    private static float fallDistance = 0.0f;
    private static int moveTicks = 0;

    public static float getSpeed() {
        return (float)Math.sqrt(MovementComponent.mc.thePlayer.motionX * MovementComponent.mc.thePlayer.motionX + MovementComponent.mc.thePlayer.motionZ * MovementComponent.mc.thePlayer.motionZ);
    }

    public static void strafe() {
        MovementComponent.strafe(MovementComponent.getSpeed());
    }

    public static boolean isMove() {
        return MovementComponent.mc.thePlayer != null && (MovementComponent.mc.thePlayer.movementInput.moveForward != 0.0f || MovementComponent.mc.thePlayer.movementInput.moveStrafe != 0.0f);
    }

    public static void strafe(float speed) {
        if (!MovementComponent.isMove()) {
            return;
        }
        double yaw = MovementComponent.getDirection();
        MovementComponent.mc.thePlayer.motionX = -Math.sin(yaw) * (double)speed;
        MovementComponent.mc.thePlayer.motionZ = Math.cos(yaw) * (double)speed;
    }

    public static void forward(double length) {
        double yaw = Math.toRadians(MovementComponent.mc.thePlayer.rotationYaw);
        MovementComponent.mc.thePlayer.setPosition(MovementComponent.mc.thePlayer.posX + -Math.sin(yaw) * length, MovementComponent.mc.thePlayer.posY, MovementComponent.mc.thePlayer.posZ + Math.cos(yaw) * length);
    }

    public static double getDirection() {
        float rotationYaw = MovementComponent.mc.thePlayer.rotationYaw;
        if (MovementComponent.mc.thePlayer.moveForward < 0.0f) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (MovementComponent.mc.thePlayer.moveForward < 0.0f) {
            forward = -0.5f;
        } else if (MovementComponent.mc.thePlayer.moveForward > 0.0f) {
            forward = 0.5f;
        }
        if (MovementComponent.mc.thePlayer.moveStrafing > 0.0f) {
            rotationYaw -= 90.0f * forward;
        }
        if (MovementComponent.mc.thePlayer.moveStrafing < 0.0f) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    public static void cancelMove() {
        if (MovementComponent.mc.thePlayer == null) {
            return;
        }
        if (cancelMove) {
            return;
        }
        cancelMove = true;
        motionX = MovementComponent.mc.thePlayer.motionX;
        motionY = MovementComponent.mc.thePlayer.motionY;
        motionZ = MovementComponent.mc.thePlayer.motionZ;
        fallDistance = MovementComponent.mc.thePlayer.fallDistance;
    }

    public static void resetMove() {
        cancelMove = false;
        moveTicks = 0;
    }

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
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
        pre = false;
    }

     @EventTarget
    public void onUpdate(EventUpdate event) {
        if (cancelMove) {
            if (moveTicks > 0) {
                return;
            }
            MovementComponent.mc.thePlayer.motionX = motionX;
            MovementComponent.mc.thePlayer.motionZ = motionZ;
            MovementComponent.mc.thePlayer.motionY = motionY;
            MovementComponent.mc.thePlayer.fallDistance = fallDistance;
        }
    }

     @EventTarget
    public void onPacket(EventPacketSend event) {
        if (event.getPacket() instanceof C03PacketPlayer && cancelMove && moveTicks > 0) {
            motionX = MovementComponent.mc.thePlayer.motionX;
            motionZ = MovementComponent.mc.thePlayer.motionZ;
            motionY = MovementComponent.mc.thePlayer.motionY;
            fallDistance = MovementComponent.mc.thePlayer.fallDistance;
            --moveTicks;
        }
    }

     @EventTarget
    public void onTick(EventTick event) {
        if (MovementComponent.mc.thePlayer == null) {
            MovementComponent.resetMove();
            return;
        }
        pre = true;
        if (cancelMove) {
            if (GetC03StatusUtil.noMovePackets >= 20) {
                MovementComponent.mc.thePlayer.motionX = motionX;
                MovementComponent.mc.thePlayer.motionY = motionY;
                MovementComponent.mc.thePlayer.motionZ = motionZ;
                MovementComponent.mc.thePlayer.fallDistance = fallDistance;
                ++moveTicks;
            }
            if (moveTicks > 0) {
                return;
            }
            MovementComponent.mc.thePlayer.motionX = motionX;
            MovementComponent.mc.thePlayer.motionZ = motionZ;
            MovementComponent.mc.thePlayer.motionY = motionY;
            MovementComponent.mc.thePlayer.fallDistance = fallDistance;
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
        if (packet instanceof S12PacketEntityVelocity && (s12 = (S12PacketEntityVelocity)packet).getEntityID() == MovementComponent.mc.thePlayer.getEntityId() && cancelMove) {
            MovementComponent.mc.thePlayer.motionX = motionX;
            MovementComponent.mc.thePlayer.motionY = motionY;
            MovementComponent.mc.thePlayer.motionZ = motionZ;
            MovementComponent.mc.thePlayer.fallDistance = fallDistance;
            ++moveTicks;
        }
    }
}

