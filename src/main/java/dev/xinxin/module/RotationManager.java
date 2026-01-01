package dev.xinxin.module;

import dev.xinxin.event.EventPriority;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.*;
import dev.xinxin.utils.Rotation;
import dev.xinxin.utils.player.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

public class RotationManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    public Vector2f rotation = new Vector2f(0.0f, 0.0f);
    public Vector2f lastRotation;
    public Vector2f targetRotation;
    public Vector2f lastServerRotation;
    private float rotationSpeed;
    private boolean modify;
    private boolean smoothed;
    private boolean movementFix;
    private boolean strict;

    public Vector2f getRotation() {
        return this.rotation;
    }

    public void setRotation(Vector2f rotation, float rotationSpeed, boolean movementFix, boolean strict) {
        this.targetRotation = rotation;
        this.rotationSpeed = rotationSpeed;
        this.movementFix = movementFix;
        this.modify = true;
        this.strict = strict;
        this.smoothRotation();
    }

    public void setRotation(Vector2f rotation, float rotationSpeed, boolean movementFix) {
        this.targetRotation = rotation;
        this.rotationSpeed = rotationSpeed;
        this.movementFix = movementFix;
        this.modify = true;
        this.strict = false;
        this.smoothRotation();
    }

    public void setRotation(RotationUtil.Rotation rotation, float rotationSpeed, boolean movementFix) {
        this.targetRotation = rotation.toVec2f();
        this.rotationSpeed = rotationSpeed;
        this.movementFix = movementFix;
        this.modify = true;
        this.strict = false;
        this.smoothRotation();
    }

    public double getRotationDifference(Rotation rotation) {
        return this.lastServerRotation == null ? 0.0 : this.getRotationDifference(rotation, this.lastServerRotation);
    }

    public float getAngleDifference(float a, float b) {
        return ((a - b) % 360.0f + 540.0f) % 360.0f - 180.0f;
    }

    public double getRotationDifference(Rotation a, Vector2f b) {
        return Math.hypot(this.getAngleDifference(a.getYaw(), b.getX()), a.getPitch() - b.getY());
    }

    @EventTarget
    @EventPriority(value=8888)
    public void onMotion(EventUpdate event) {
        if (!this.modify || this.rotation == null || this.lastRotation == null || this.targetRotation == null) {
            this.lastServerRotation = this.targetRotation = new Vector2f(this.mc.thePlayer.rotationYaw, this.mc.thePlayer.rotationPitch);
            this.lastRotation = this.targetRotation;
            this.rotation = this.targetRotation;
        }
        if (this.modify) {
            this.smoothRotation();
        }
    }

    @EventTarget
    @EventPriority(value=8888)
    public void onMovementInput(EventMoveInput event) {
        if (this.modify && this.movementFix && !this.strict) {
            float yaw = this.rotation.getX();
            float forward = event.getForward();
            float strafe = event.getStrafe();
            double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(RotationManager.getDirection(this.mc.thePlayer.rotationYaw, forward, strafe)));
            if (forward == 0.0f && strafe == 0.0f) {
                return;
            }
            float closestForward = 0.0f;
            float closestStrafe = 0.0f;
            float closestDifference = Float.MAX_VALUE;
            for (float predictedForward = -1.0f; predictedForward <= 1.0f; predictedForward += 1.0f) {
                for (float predictedStrafe = -1.0f; predictedStrafe <= 1.0f; predictedStrafe += 1.0f) {
                    double predictedAngle;
                    double difference;
                    if (predictedStrafe == 0.0f && predictedForward == 0.0f || !((difference = Math.abs(angle - (predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(RotationManager.getDirection(yaw, predictedForward, predictedStrafe)))))) < (double)closestDifference)) continue;
                    closestDifference = (float)difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
            event.setForward(closestForward);
            event.setStrafe(closestStrafe);
        }
    }

    public static double getDirection(float rotationYaw, double moveForward, double moveStrafing) {
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
    @EventPriority(value=8888)
    public void onLook(EventLook event) {
        if (this.modify) {
            event.setRotation(this.rotation);
        }
    }

    @EventTarget
    @EventPriority(value=8888)
    public void onStrafe(EventStrafe event) {
        if (this.modify && this.movementFix) {
            event.setYaw(this.rotation.getX());
        }
    }

    @EventTarget
    @EventPriority(value=8888)
    public void onJump(EventJump event) {
        if (this.modify && this.movementFix) {
            event.setYaw(this.rotation.getX());
        }
    }

    @EventTarget
    @EventPriority(value=8888)
    public void onUpdate(EventMotion event) {
        if (event.isPre()) {
            if (this.modify) {
                event.setYaw(this.rotation.getX());
                event.setPitch(this.rotation.getY());
                this.mc.thePlayer.renderYawOffset = this.rotation.getX();
                this.mc.thePlayer.rotationYawHead = this.rotation.getX();
                this.mc.thePlayer.renderPitchHead = this.rotation.getY();
                this.lastServerRotation = new Vector2f(this.rotation.getX(), this.rotation.getY());
                if (Math.abs((this.rotation.getX() - this.mc.thePlayer.rotationYaw) % 360.0f) < 1.0f && Math.abs(this.rotation.getY() - this.mc.thePlayer.rotationPitch) < 1.0f) {
                    this.modify = false;
                    this.correctDisabledRotations();
                }
                this.lastRotation = this.rotation;
            } else {
                this.lastRotation = new Vector2f(this.mc.thePlayer.rotationYaw, this.mc.thePlayer.rotationPitch);
            }
            this.targetRotation = new Vector2f(this.mc.thePlayer.rotationYaw, this.mc.thePlayer.rotationPitch);
            this.smoothed = false;
        }
    }

    private void correctDisabledRotations() {
        Vector2f rotations = new Vector2f(this.mc.thePlayer.rotationYaw, this.mc.thePlayer.rotationPitch);
        Vector2f fixedRotations = this.resetRotation(this.applySensitivityPatch(rotations, this.lastRotation));
        this.mc.thePlayer.rotationYaw = fixedRotations.getX();
        this.mc.thePlayer.rotationPitch = fixedRotations.getY();
    }

    public Vector2f resetRotation(Vector2f rotation) {
        if (rotation == null) {
            return null;
        }
        float yaw = rotation.getX() + MathHelper.wrapAngleTo180_float(this.mc.thePlayer.rotationYaw - rotation.getX());
        float pitch = this.mc.thePlayer.rotationPitch;
        return new Vector2f(yaw, pitch);
    }

    public Vector2f applySensitivityPatch(Vector2f rotation, Vector2f previousRotation) {
        float mouseSensitivity = (float)((double)this.mc.gameSettings.mouseSensitivity * (1.0 + Math.random() / 1.0E7) * (double)0.6f + (double)0.2f);
        double multiplier = (double)(mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0f) * 0.15;
        float yaw = previousRotation.getX() + (float)((double)Math.round((double)(rotation.getX() - previousRotation.getX()) / multiplier) * multiplier);
        float pitch = previousRotation.getY() + (float)((double)Math.round((double)(rotation.getY() - previousRotation.getY()) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }

    private void smoothRotation() {
        if (!this.smoothed) {
            float lastYaw = this.lastRotation.getX();
            float lastPitch = this.lastRotation.getY();
            float targetYaw = this.targetRotation.getX();
            float targetPitch = this.targetRotation.getY();
            this.rotation = this.getSmoothRotation(new Vector2f(lastYaw, lastPitch), new Vector2f(targetYaw, targetPitch), (double)this.rotationSpeed + Math.random());
            if (this.movementFix) {
                this.mc.thePlayer.movementYaw = this.rotation.getX();
            }
            this.mc.thePlayer.velocityYaw = this.rotation.getX();
        }
        this.smoothed = true;
        this.mc.entityRenderer.getMouseOver(1.0f);
    }

    public Vector2f getSmoothRotation(Vector2f lastRotation, Vector2f targetRotation, double speed) {
        float yaw = targetRotation.getX();
        float pitch = targetRotation.getY();
        float lastYaw = lastRotation.getX();
        float lastPitch = lastRotation.getY();
        if (speed != 0.0) {
            float rotationSpeed = (float)speed;
            double deltaYaw = MathHelper.wrapAngleTo180_float(targetRotation.getX() - lastRotation.getX());
            double deltaPitch = pitch - lastPitch;
            double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            double distributionYaw = Math.abs(deltaYaw / distance);
            double distributionPitch = Math.abs(deltaPitch / distance);
            double maxYaw = (double)rotationSpeed * distributionYaw;
            double maxPitch = (double)rotationSpeed * distributionPitch;
            float moveYaw = (float)Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            float movePitch = (float)Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);
            yaw = lastYaw + moveYaw;
            pitch = lastPitch + movePitch;
        }
        boolean randomise = Math.random() > 0.8;
        for (int i = 1; i <= (int)(2.0 + Math.random() * 2.0); ++i) {
            if (randomise) {
                yaw += (float)((Math.random() - 0.5) / 1.0E8);
                pitch -= (float)(Math.random() / 2.0E8);
            }
            Vector2f rotations = new Vector2f(yaw, pitch);
            Vector2f fixedRotations = this.applySensitivityPatch(rotations);
            yaw = fixedRotations.getX();
            pitch = Math.max(-90.0f, Math.min(90.0f, fixedRotations.getY()));
        }
        return new Vector2f(yaw, pitch);
    }

    public Vector2f applySensitivityPatch(Vector2f rotation) {
        Vector2f previousRotation = new Vector2f(this.mc.thePlayer.lastReportedYaw, this.mc.thePlayer.lastReportedPitch);
        float mouseSensitivity = (float)((double)this.mc.gameSettings.mouseSensitivity * (1.0 + Math.random() / 1.0E7) * (double)0.6f + (double)0.2f);
        double multiplier = (double)(mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0f) * 0.15;
        float yaw = previousRotation.getX() + (float)((double)Math.round((double)(rotation.getX() - previousRotation.getX()) / multiplier) * multiplier);
        float pitch = previousRotation.getY() + (float)((double)Math.round((double)(rotation.getY() - previousRotation.getY()) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }
}

