package dev.xinxin.utils;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.api.types.EventType;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.*;
import dev.xinxin.module.modules.combat.AutoProjectile;
import dev.xinxin.module.modules.combat.BowAimbot;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.world.ChestAura;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.utils.client.MathUtil;
import dev.xinxin.utils.player.MoveUtil;
import dev.xinxin.utils.player.RotationNew;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

import java.util.Objects;

import static dev.xinxin.module.ModuleManager.getModule;

public final class RotationComponent {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static Vector2f rotation;
    public static Vector2f lastRotation;
    public static Vector2f targetRotation;
    public static Vector2f targetRotations;

    public static boolean active;
    public static Vector2f lastServerRotation;
    private static float rotationSpeed;
    public static boolean modify;
    private static boolean forceSilent;
    private static boolean smoothed;
    private static boolean movementFix;
    private static boolean strict;

    private static float smoothedRenderYaw;
    private static float smoothedHeadYaw;
    private static float smoothedHeadPitch;
    private static boolean viewInit = false;

    private static float pullSpeed   = 0.12f;
    private static float returnSpeed = 0.10f;

    private static float biasInitYaw = 0f;
    private static float visualBiasYaw = 0f;

    private static final float partialReturnRatio = 0.60f;
    private static final int   biasHoldTicks      = 8;
    private static int   biasHoldCounter    = 0;

    private static final int   longReleaseTicks   = 30;
    private static int   longReleaseCounter = 0;
    private static final float longReleaseDecay   = 0.04f;
    static int randomCount = 0;

    public RotationComponent() {
        rotation = new Vector2f(0.0f, 0.0f);
    }

    public Vector2f getRotation() {
        return rotation;
    }

//    @EventTarget
//    public void onLoopTick(EventRender2D e){
//        if (mc.thePlayer != null) {
//            KillAura aura =  getModule(KillAura.class);
//            Scaffold scaffold =  getModule(Scaffold.class);
//            ChestAura chestAura =  getModule(ChestAura.class);
//            BowAimbot bowAimbot =  getModule(BowAimbot.class);
//            AutoProjectile autoProjectile =  getModule(AutoProjectile.class);
//
//            if (scaffold.state && scaffold.blockPos != null) {
//                setRotation(scaffold.rotation,360f,true);
//                scaffold.place(true);
//            } else if (Objects.requireNonNull(autoProjectile).state && autoProjectile.target != null && autoProjectile.targetRotation !=null) {
//                setRotation(new Vector2f(autoProjectile.targetRotation.x, autoProjectile.targetRotation.y));
//            } else if (aura.state && aura.target != null) {
//                setRotation(new Vector2f(aura.KaRotation[0], Math.min(90.0f, aura.KaRotation[1])));
//            }  else if (chestAura.state && chestAura.needRot != null) {
//                RotationComponent.setRotation(chestAura.needRot, 180f, true);
//            }
//
//        }
//    }

//    public static void setRotation(Vector2f rotation) {
//        targetRotation = rotation;
//        RotationComponent.rotationSpeed = 360f; // 修复字段遮蔽
//        RotationComponent.movementFix = true;
//        modify = true;
//        RotationComponent.strict = false;
//        smoothRotation();
//    }

    public static void setRotation(Vector2f rotation, float rotationSpeed, boolean movementFix, boolean strict) {
        targetRotation = rotation;
        RotationComponent.rotationSpeed = rotationSpeed * 18.0f; // 修复字段遮蔽
        RotationComponent.movementFix = movementFix;
        modify = true;
        RotationComponent.strict = strict;
        smoothRotation();
    }

    public static void setRotations(Vector2f rotation, float rotationSpeed, boolean movementFix) {
        targetRotation = rotation;
        RotationComponent.rotationSpeed = rotationSpeed * 18.0f;
        RotationComponent.movementFix = movementFix;
        modify = true;
        strict = false;
        smoothRotation();
    }

    public static void setRotation(Rotation rotation, float rotationSpeed, boolean movementFix) {
        targetRotation = rotation.toVec2f();
        RotationComponent.rotationSpeed = rotationSpeed * 18.0f;
        RotationComponent.movementFix = movementFix;
        modify = true;
        strict = false;
        smoothRotation();
    }

    public static void setRotation(RotationNew rotation, float rotationSpeed, boolean movementFix) {
        targetRotation = rotation.toVec2f2();
        RotationComponent.rotationSpeed = rotationSpeed * 18.0f;
        RotationComponent.movementFix = movementFix;
        modify = true;
        strict = false;
        smoothRotation();
    }

    public static double getRotationDifference(Rotation rotation) {
        return lastServerRotation == null ? 0.0 : getRotationDifference(rotation, lastServerRotation);
    }

    public static float getAngleDifference(float a, float b2) {
        return ((a - b2) % 360.0f + 540.0f) % 360.0f - 180.0f;
    }

    public static double getRotationDifference(Rotation a, Vector2f b2) {
        return Math.hypot(getAngleDifference(a.getYaw(), b2.getX()), a.getPitch() - b2.getY());
    }

    @EventTarget(value=100)
    public void onMotion(EventUpdate event) {
        if (!modify || rotation == null || lastRotation == null || targetRotation == null) {
            lastServerRotation = targetRotation = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            lastRotation = new Vector2f(targetRotation.getX(), targetRotation.getY());
            rotation = new Vector2f(targetRotation.getX(), targetRotation.getY());
        }
        if (modify) {
            smoothRotation();
        }
    }

    @EventTarget(value=100)
    public void onMovementInput(EventMoveInput event) {
        if (modify && movementFix && !strict && (KillAura.target == null)) {
            float yaw = rotation.getX();
            float forward = event.getForward();
            float strafe = event.getStrafe();
            double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(getDirection(mc.thePlayer.rotationYaw, forward, strafe)));
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
                    if (predictedStrafe == 0.0f && predictedForward == 0.0f
                            || !((difference = Math.abs(angle - (predictedAngle = MathHelper.wrapAngleTo180_double(
                            Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)))))) < (double) closestDifference))
                        continue;
                    closestDifference = (float) difference;
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

    @EventTarget(value=100)
    public void onLook(EventLook event) {
        if (modify) {
            event.setRotation(rotation);
        }
    }

    @EventTarget(value=100)
    public void onStrafe(EventStrafe event) {
        if (modify && movementFix) {
            event.setYaw(rotation.getX());
        }
    }

    @EventTarget(value=100)
    public void onJump(EventJump event) {
        if (modify && movementFix) {
            event.setYaw(rotation.getX());
        }
    }

    @EventTarget(value=100)
    public void onUpdate(EventMotion event) {
        if (event.isPre()) {
            if (!viewInit && mc.thePlayer != null) {
                smoothedRenderYaw = mc.thePlayer.renderYawOffset;
                smoothedHeadYaw   = mc.thePlayer.rotationYawHead;
                smoothedHeadPitch = mc.thePlayer.renderPitchHead;
                viewInit = true;
            }

            if (modify) {
                event.setYaw(rotation.getX());
                event.setPitch(rotation.getY());

                lastServerRotation = new Vector2f(rotation.getX(), rotation.getY());

                float yawDiff   = Math.abs(MathHelper.wrapAngleTo180_float(rotation.getX() - mc.thePlayer.rotationYaw));
                float pitchDiff = Math.abs(rotation.getY() - mc.thePlayer.rotationPitch);
                if (yawDiff < 1.0f && pitchDiff < 1.0f) {
                    modify = false;
                    this.correctDisabledRotations();
                }

                lastRotation = new Vector2f(rotation.getX(), rotation.getY());

                float playerYaw = mc.thePlayer.rotationYaw;
                biasInitYaw     = MathHelper.wrapAngleTo180_float(rotation.getX() - playerYaw);
                visualBiasYaw   = biasInitYaw;
                biasHoldCounter = biasHoldTicks;
                longReleaseCounter = longReleaseTicks;
            } else {
                lastRotation = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

                float targetBias = biasInitYaw * partialReturnRatio;

                if (biasHoldCounter > 0) {
                    biasHoldCounter--;
                    visualBiasYaw = smoothFloat(visualBiasYaw, targetBias, 0.25f);
                } else {
                    if (longReleaseCounter > 0) {
                        longReleaseCounter--;
                        visualBiasYaw = smoothFloat(visualBiasYaw, targetBias, 0.1f);
                    } else {
                        visualBiasYaw = visualBiasYaw * (1.0f - longReleaseDecay);
                        if (Math.abs(visualBiasYaw) < 0.01f) visualBiasYaw = 0f;
                    }
                }
            }
            if(getModule(Scaffold.class).state && MoveUtil.isMoving() || (getModule(KillAura.class).state && KillAura.target != null)) {
                if(getModule(KillAura.class).state){
                    pullSpeed = 0.5f;
                    returnSpeed = 0.2f;
                } else
                {
                    pullSpeed = 0.4f;
                    returnSpeed = 0.02f;
                }
                final float playerYawNow = mc.thePlayer.rotationYaw;
                final float targetYawVis = playerYawNow + visualBiasYaw;

                float targetPitchVis;
                if(getModule(KillAura.class).state){
                    targetPitchVis = (modify ? Math.min(90,rotation.getY() + MathUtil.getRandomFloat(28,20)): mc.thePlayer.rotationPitch);
                } else {
                    targetPitchVis = (modify ? rotation.getY() : mc.thePlayer.rotationPitch);
                }
                final float speed = (modify ? pullSpeed : returnSpeed);

                smoothedRenderYaw = smoothAngleDeg(smoothedRenderYaw, targetYawVis, speed * 1.05f);
                smoothedHeadYaw = smoothAngleDeg(smoothedHeadYaw, targetYawVis, speed * 1.15f); // 头稍快
                smoothedHeadPitch = smoothFloat(smoothedHeadPitch, targetPitchVis, speed * 1.15f);

                mc.thePlayer.renderYawOffset = smoothedRenderYaw;
                mc.thePlayer.rotationYawHead = smoothedHeadYaw;

                mc.thePlayer.renderPitchHead = smoothedHeadPitch;
            } else {
                mc.thePlayer.renderYawOffset = rotation.getX();
                mc.thePlayer.rotationYawHead = rotation.getX();
                mc.thePlayer.renderPitchHead = rotation.getY();
            }

            targetRotation = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            smoothed = false;
        }
    }

    private void correctDisabledRotations() {
        Vector2f rotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        Vector2f fixedRotations = this.resetRotation(this.applySensitivityPatch(rotations, lastRotation));
        mc.thePlayer.rotationYaw = fixedRotations.getX();
        mc.thePlayer.rotationPitch = fixedRotations.getY();
    }

    public Vector2f resetRotation(Vector2f rotation) {
        if (rotation == null) {
            return null;
        }
        float yaw = rotation.getX() + MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - rotation.getX());
        float pitch = mc.thePlayer.rotationPitch;
        return new Vector2f(yaw, pitch);
    }

    public Vector2f applySensitivityPatch(Vector2f rotation, Vector2f previousRotation) {
        float mouseSensitivity = (float)((double)mc.gameSettings.mouseSensitivity * (1.0 + Math.random() / 1.0E7) * (double)0.6f + (double)0.2f);
        double multiplier = (double)(mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0f) * 0.15;
        float yaw = previousRotation.getX() + (float)((double)Math.round((double)(rotation.getX() - previousRotation.getX()) / multiplier) * multiplier);
        float pitch = previousRotation.getY() + (float)((double)Math.round((double)(rotation.getY() - previousRotation.getY()) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }

    private static void smoothRotation() {
        if (!smoothed) {
            final float lastYaw = lastRotation.getX();
            final float lastPitch = lastRotation.getY();
            final float targetYaw = targetRotation.getX();
            final float targetPitch = targetRotation.getY();

            rotation = getSmoothRotation(new Vector2f(lastYaw, lastPitch), new Vector2f(targetYaw, targetPitch),
                    rotationSpeed + Math.random());

            if (movementFix) {
                mc.thePlayer.movementYaw = rotation.getX();
            }

            mc.thePlayer.velocityYaw = rotation.getX();
        }

        smoothed = true;

        mc.entityRenderer.getMouseOver(1);
    }

    public static Vector2f getSmoothRotation(Vector2f lastRotation, Vector2f targetRotation, double speed) {
        float yaw = targetRotation.getX();
        float pitch = targetRotation.getY();
        float lastYaw = lastRotation.getX();
        float lastPitch = lastRotation.getY();
        if (speed != 0.0) {
            float rotationSpeed = (float)speed;
            double deltaYaw = MathHelper.wrapAngleTo180_float(targetRotation.getX() - lastRotation.getX());
            double deltaPitch = pitch - lastPitch;
            double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            if (distance < 1e-6) {
                return new Vector2f(lastYaw, lastPitch);
            }
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
            Vector2f fixedRotations = applySensitivityPatch(rotations);
            yaw = fixedRotations.getX();
            pitch = Math.max(-90.0f, Math.min(90.0f, fixedRotations.getY()));
        }
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f applySensitivityPatch(Vector2f rotation) {
        Vector2f previousRotation = new Vector2f(mc.thePlayer.lastReportedYaw, mc.thePlayer.lastReportedPitch);
        float mouseSensitivity = (float)((double)mc.gameSettings.mouseSensitivity * (1.0 + Math.random() / 1.0E7) * (double)0.6f + (double)0.2f);
        double multiplier = (double)(mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0f) * 0.15;
        float yaw = previousRotation.getX() + (float)((double)Math.round((double)(rotation.getX() - previousRotation.getX()) / multiplier) * multiplier);
        float pitch = previousRotation.getY() + (float)((double)Math.round((double)(rotation.getY() - previousRotation.getY()) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }

    // ====== 工具：角度与数值平滑 ======
    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static float smoothAngleDeg(float current, float target, float factor01) {
        factor01 = clamp01(factor01);
        float delta = MathHelper.wrapAngleTo180_float(target - current);
        return current + delta * factor01;
    }

    private static float smoothFloat(float current, float target, float factor01) {
        factor01 = clamp01(factor01);
        return current + (target - current) * factor01;
    }
}
