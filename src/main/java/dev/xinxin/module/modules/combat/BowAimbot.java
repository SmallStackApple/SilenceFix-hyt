package dev.xinxin.module.modules.combat;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.misc.Teams;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.render.RenderUtil;
import dev.yalan.live.silencefix.LiveComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BowAimbot extends Module {
    public static NumberValue minRange = new NumberValue("Min Range", 15, 0, 16, 1);
    public static NumberValue maxRange = new NumberValue("Max Range", 60, 3, 128, 1);
    public static NumberValue rotationCount = new NumberValue("Rotation Count", 3, 1, 20, 1);
    public static BoolValue autoRelease = new BoolValue("Auto Release", true);
    public static NumberValue releaseCount = new NumberValue("Release Count", 20, 1, 20, 1);
    public static NumberValue predictValue = new NumberValue("Predict", 2.0, 0, 2, 0.1);
    public static BoolValue markValue = new BoolValue("Mark Target", true);
    public static BoolValue rayCastValue = new BoolValue("RayCast", false);
    public static ModeValue<PRIORITY> priority = new ModeValue<>("Priority", PRIORITY.values(), PRIORITY.Distance);
    public static EntityLivingBase target;

    public BowAimbot() {
        super("BowAimbot", Category.Combat, "自动弓箭瞄准");
    }

    private boolean isFriend(EntityPlayer player) {
        return player != null && SilenceFix.instance.getFriendManager().isFriend(player.getName());
    }

    private float[] rotations;

    @EventTarget
    public void onUpdatePre(EventMotion event) {
        if (event.isPost()) return;
        if (mc.thePlayer.getHeldItem() == null ||
                !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) ||
                !mc.thePlayer.isUsingItem()) {
            target = null;
            return;
        }
        int use = mc.thePlayer.getItemInUseDuration();
        if (use >= rotationCount.getValue().intValue()) {
            getTarget(minRange.getValue().floatValue(), maxRange.getValue().floatValue()).ifPresent(entity -> {
                target = entity;
                rotations = getBowRotations(entity, predictValue.getValue().floatValue());
                RotationComponent.setRotations(new Vector2f(rotations[0], rotations[1]), 10, true);
                boolean blocked = false;
                if (rayCastValue.getValue()) blocked = isHardBlockedLineOfSight(entity);
                if (autoRelease.getValue() && use > releaseCount.getValue().intValue() && !blocked) {
                    mc.getNetHandler().addToSendQueue(
                            new C07PacketPlayerDigging(
                                    Action.RELEASE_USE_ITEM,
                                    BlockPos.ORIGIN,
                                    EnumFacing.DOWN
                            )
                    );
                    mc.playerController.onStoppedUsingItem(mc.thePlayer);
                }
            });
        } else {
            target = null;
        }
    }

    @EventTarget
    public void onRender3D() {
        if (target != null && markValue.getValue()) {
            int alpha = target.hurtTime * 5;
            Color color = new Color(135, 206, 250, 100 + alpha);
            RenderUtil.drawEntityBox(target, color, true);
        }
    }

    @Override
    public void onDisable() {
        target = null;
    }

    private float[] getBowRotations(Entity tgt, float predict) {
        double tx = tgt.posX + (tgt.posX - tgt.prevPosX) * predict;
        double ty = tgt.posY + (tgt.posY - tgt.prevPosY) * predict + tgt.getEyeHeight() - 0.15;
        double tz = tgt.posZ + (tgt.posZ - tgt.prevPosZ) * predict;
        double px = mc.thePlayer.posX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * predict;
        double py = mc.thePlayer.posY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * predict + mc.thePlayer.getEyeHeight();
        double pz = mc.thePlayer.posZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * predict;
        double dx = tx - px;
        double dy = ty - py;
        double dz = tz - pz;
        double dHoriz = Math.sqrt(dx * dx + dz * dz);
        float f = mc.thePlayer.getItemInUseDuration() / 20F;
        f = (f * f + 2F * f) / 3F;
        if (f > 1F) f = 1F;
        float v = f * 3.0f;
        float g = 0.006f;
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch;
        double v2 = v * v;
        double underSqrt = v2 * v2 - g * (g * dHoriz * dHoriz + 2.0 * dy * v2);
        if (underSqrt >= 0.0) {
            double sqrt = Math.sqrt(underSqrt);
            double low = Math.atan((v2 - sqrt) / (g * dHoriz));
            double high = Math.atan((v2 + sqrt) / (g * dHoriz));
            double chosen = Math.abs(low) < Math.abs(high) ? low : high;
            pitch = (float) -Math.toDegrees(chosen);
        } else {
            pitch = mc.thePlayer.rotationPitch;
        }
        return new float[]{wrapYaw(yaw), clampPitch(pitch)};
    }

    private Optional<EntityLivingBase> getTarget(float min, float max) {
        double minRangeSq = min * min;
        double maxRangeSq = max * max;
        Stream<EntityPlayer> stream = mc.theWorld.playerEntities.stream()
                .filter(e -> e != mc.thePlayer)
                .filter(e -> !Teams.isSameTeam(e))
                .filter(Entity::isEntityAlive)
                .filter(e -> !isFriend(e))
                .filter(e -> LiveComponent.isAttackable(e.liveUser))
                .filter(e -> {
                    double distSq = mc.thePlayer.getDistanceSqToEntity(e);
                    return distSq >= minRangeSq && distSq <= maxRangeSq;
                });
        if (priority.getValue() == PRIORITY.Distance) {
            stream = stream.sorted(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceSqToEntity(e)));
        } else if (priority.getValue() == PRIORITY.Angle) {
            stream = stream.sorted(Comparator.comparingDouble(e -> Math.abs(RotationComponent.getAngleDifference(mc.thePlayer.rotationYaw,
                    (float) (MathHelper.atan2(e.posZ - mc.thePlayer.posZ, e.posX - mc.thePlayer.posX) * 180 / Math.PI - 90)))));
        }
        List<EntityPlayer> targets = stream.collect(Collectors.toList());
        return targets.isEmpty() ? Optional.empty() : Optional.of(targets.get(0));
    }

    private boolean isHardBlockedLineOfSight(EntityLivingBase e) {
        Vec3 start = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 end = new Vec3(e.posX, e.posY + e.getEyeHeight(), e.posZ);
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(start, end);
        return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private float wrapYaw(float yaw) {
        yaw %= 360.0f;
        if (yaw >= 180.0f) yaw -= 360.0f;
        if (yaw < -180.0f) yaw += 360.0f;
        return yaw;
    }

    private float clampPitch(float pitch) {
        if (pitch > 90.0f) pitch = 90.0f;
        if (pitch < -90.0f) pitch = -90.0f;
        return pitch;
    }

    private enum PRIORITY {
        Distance,
        Angle
    }
}
