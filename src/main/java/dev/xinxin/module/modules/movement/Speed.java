package dev.xinxin.module.modules.movement;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.misc.Teams;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.player.MoveUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.AxisAlignedBB;

public class Speed extends Module {
    public NumberValue speedOption = new NumberValue("Speed", 5, 1, 8, 1);
    public BoolValue followTargetOption = new BoolValue("Target Strafe", true);
    public BoolValue onlyJumpOption = new BoolValue("Only Jump", true, () -> followTargetOption.getValue());
    public BoolValue hurttimeCheck = new BoolValue("HurtTime Check", false);
    public BoolValue behindOption = new BoolValue("Behind Target", false);

    public Speed() {
        super("Speed", Category.Movement, "飘逸", "Allows you to move faster.", "允许你在触碰实体时加速.");
    }

    @Override
    public void onDisable() {
        mc.gameSettings.keyBindLeft.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindLeft));
    }

    @EventTarget
    public void onPre(EventMotion event) {
        setSuffix(MoveUtil.speedCalculator());
        if (getModule(Scaffold.class).state || mc.currentScreen instanceof GuiChat || mc.thePlayer.hurtTime > 6 && hurttimeCheck.getValue() || (onlyJumpOption.getValue() && mc.thePlayer.onGround)) {
            mc.gameSettings.keyBindLeft.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindLeft));
            return;
        }
        if (event.isPre()) {
            doBoost();
        }
    }

    private boolean isMovingRight = true;
    private long lastSwitchTime = 0;
    private long switchDelay = 500;

    private void doBoost() {
        AxisAlignedBB playerBox = mc.thePlayer.getEntityBoundingBox().expand(1.0, 1.0, 1.0);
        int entityCount = 0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if ((entity instanceof EntityLivingBase ||
                    entity instanceof EntityBoat ||
                    entity instanceof EntityMinecart ||
                    entity instanceof EntityFishHook) &&
                    !(entity instanceof EntityArmorStand) &&
                    entity.getEntityId() != mc.thePlayer.getEntityId() &&
                    playerBox.intersectsWith(entity.getEntityBoundingBox()) &&
                    entity.getEntityId() != -8 &&
                    entity.getEntityId() != -1337 &&
                    !getModule(Blink.class).state &&
                    !Teams.isSameTeam(entity)) {
                entityCount++;
            }
        }

        if (entityCount > 0 && MoveUtil.isMoving() && mc.thePlayer.isSprinting()) {
            double strafeOffset = (Math.min(entityCount, 3)) * (speedOption.getValue() / 100);

            float yaw = getMoveYaw();

            double mx = -Math.sin(Math.toRadians(yaw));
            double mz = Math.cos(Math.toRadians(yaw));

            if (mc.thePlayer.movementInput.moveForward == 0 && mc.thePlayer.movementInput.moveStrafe == 0) {
                if (mc.thePlayer.motionX > strafeOffset) {
                    mc.thePlayer.motionX -= strafeOffset;
                } else if (mc.thePlayer.motionX < -strafeOffset) {
                    mc.thePlayer.motionX += strafeOffset;
                } else {
                    mc.thePlayer.motionX = 0.0;
                }
                if (mc.thePlayer.motionZ > strafeOffset) {
                    mc.thePlayer.motionZ -= strafeOffset;
                } else if (mc.thePlayer.motionZ < -strafeOffset) {
                    mc.thePlayer.motionZ += strafeOffset;
                } else {
                    mc.thePlayer.motionZ = 0.0;
                }
            }

            if (mx < 0.0) {
                if (mc.thePlayer.motionX > strafeOffset) {
                    mc.thePlayer.motionX -= strafeOffset;
                } else
                    mc.thePlayer.motionX += mx * strafeOffset;
            } else if (mx > 0.0) {
                if (mc.thePlayer.motionX < -strafeOffset) {
                    mc.thePlayer.motionX += strafeOffset;
                } else
                    mc.thePlayer.motionX += mx * strafeOffset;
            }

            if (mz < 0.0) {
                if (mc.thePlayer.motionZ > strafeOffset) {
                    mc.thePlayer.motionZ -= strafeOffset;
                } else
                    mc.thePlayer.motionZ += mz * strafeOffset;
            } else if (mz > 0.0) {
                if (mc.thePlayer.motionZ < -strafeOffset) {
                    mc.thePlayer.motionZ += strafeOffset;
                } else
                    mc.thePlayer.motionZ += mz * strafeOffset;
            }

            if (mc.thePlayer.hurtTime > 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSwitchTime >= switchDelay) {
                    isMovingRight = !isMovingRight;
                    lastSwitchTime = currentTime;
                }

                if (isMovingRight) {
                    mc.gameSettings.keyBindRight.setPressed(true);
                    mc.gameSettings.keyBindLeft.setPressed(false);
                } else {
                    mc.gameSettings.keyBindLeft.setPressed(true);
                    mc.gameSettings.keyBindRight.setPressed(false);
                }
            }
            if (KillAura.target != null && behindOption.getValue()) {
                moveBehindTarget();
            }
        } else {
            mc.gameSettings.keyBindLeft.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindLeft));
            mc.gameSettings.keyBindRight.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindRight));
        }
    }

    private void moveBehindTarget() {
        if (KillAura.target != null) {
            double deltaX = KillAura.target.posX - mc.thePlayer.posX;
            double deltaZ = KillAura.target.posZ - mc.thePlayer.posZ;
            double angle = Math.atan2(deltaZ, deltaX) + Math.PI;

            double mx = -Math.sin(angle);
            double mz = Math.cos(angle);

            mc.thePlayer.motionX = mx * speedOption.getValue() / 30;
            mc.thePlayer.motionZ = mz * speedOption.getValue() / 30;
        }
    }

    private float getMoveYaw() {
        EntityPlayerSP thePlayer = mc.thePlayer;
        float moveYaw = thePlayer.rotationYaw;

        if (thePlayer.moveForward != 0 && thePlayer.moveStrafing == 0) {
            moveYaw += (thePlayer.moveForward > 0) ? 0 : 180;
        } else if (thePlayer.moveForward != 0) {
            if (thePlayer.moveForward > 0) {
                moveYaw += (thePlayer.moveStrafing > 0) ? -45 : 45;
            } else {
                moveYaw -= (thePlayer.moveStrafing > 0) ? -45 : 45;
            }
            moveYaw += (thePlayer.moveForward > 0) ? 0 : 180;
        } else if (thePlayer.moveStrafing != 0) {
            moveYaw += (thePlayer.moveStrafing > 0) ? -90 : 90;
        }

        if (KillAura.target != null && followTargetOption.getValue() && (!onlyJumpOption.getValue() || mc.gameSettings.keyBindJump.isKeyDown())) {
            moveYaw = RotationComponent.rotation.x;
        }
        return moveYaw;
    }
}
