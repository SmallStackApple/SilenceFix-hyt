package dev.xinxin.utils.player;

import com.google.common.base.Predicates;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.utils.Location;
import dev.xinxin.utils.RayCastUtil;
import dev.xinxin.utils.Rotation;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.misc.MathUtils;
import dev.xinxin.utils.vec.Vector3d;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Setter
@Getter
public class RotationUtil {
    public static Minecraft mc = Minecraft.getMinecraft();
    private float yaw;
    private float pitch;

    private static final List<Double> xzPercents = Arrays.asList(0.5, 0.4, 0.3, 0.2, 0.1, 0.0, -0.1, -0.2, -0.3, -0.4, -0.5);

    public RotationUtil(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static boolean isRotationAligned(Vector2f target, Vector2f current, float threshold) {
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(target.x - current.x));
        float pitchDiff = Math.abs(target.y - current.y);
        return yawDiff < threshold && pitchDiff < threshold;
    }

    public static float[] getRotations(double x, double y, double z, EnumFacing facing) {
        Vec3 hitVec = new Vec3(
                x + facing.getFrontOffsetX() * 0.5,
                y + facing.getFrontOffsetY() * 0.5,
                z + facing.getFrontOffsetZ() * 0.5
        );
        return getRotationFromVec(hitVec);
    }


    private static float[] getRotationFromVec(Vec3 vec) {
        Vec3 eyesPos = new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ
        );

        double diffX = vec.xCoord - eyesPos.xCoord;
        double diffY = vec.yCoord - eyesPos.yCoord;
        double diffZ = vec.zCoord - eyesPos.zCoord;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float)-Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{ yaw, pitch };
    }
    public static float[] getBlockPosRotation(BlockPos pos) {
        return RotationUtil.getRotationFromPosition(pos.getX(), pos.getZ(), pos.getY());
    }
    public static Vector2f getRotationFromEyeToPoint(Vector3d point3d) {
        return getRotation(new Vector3d(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), point3d);
    }
    public static Vector2f getPlayerRotation() {
        return new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }
    public static float[] getRotationFromPosition(double x, double z, double y) {
        double xDiff = x - Minecraft.getMinecraft().thePlayer.posX;
        double zDiff = z - Minecraft.getMinecraft().thePlayer.posZ;
        double yDiff = y - Minecraft.getMinecraft().thePlayer.posY - 1.2;
        double dist = (double)MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
        float yaw = (float)(Math.atan2(zDiff, xDiff) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float)(-(Math.atan2(yDiff, dist) * 180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    public static boolean isPlayerLookingAtEntity(Entity entity) {
        Vec3 lookVec = getVectorForRotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw);
        if (RotationComponent.rotation != null) {
            lookVec = getVectorForRotation(RotationComponent.rotation.y, RotationComponent.rotation.x);
        }

        Vec3 startPos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 endPos = startPos.add(lookVec.scale(5.0));

        return entity.getEntityBoundingBox().intersects(startPos, endPos);
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw)
    {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }


    public static float[] getRotationsNeeded(Entity target) {
        double yDist = target.posY - RotationUtil.mc.thePlayer.posY;
        Vec3 pos = yDist >= 1.7 ? new Vec3(target.posX, target.posY, target.posZ) : (yDist <= -1.7 ? new Vec3(target.posX, target.posY + (double)target.getEyeHeight(), target.posZ) : new Vec3(target.posX, target.posY + (double)(target.getEyeHeight() / 2.0f), target.posZ));
        Vec3 vec = new Vec3(RotationUtil.mc.thePlayer.posX, RotationUtil.mc.thePlayer.getEntityBoundingBox().minY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), RotationUtil.mc.thePlayer.posZ);
        double x2 = pos.xCoord - vec.xCoord;
        double y2 = pos.yCoord - vec.yCoord;
        double z = pos.zCoord - vec.zCoord;
        double sqrt = Math.sqrt(x2 * x2 + z * z);
        float yaw = (float)Math.toDegrees(Math.atan2(z, x2)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(y2, sqrt)));
        return new float[]{yaw, Math.min(Math.max(pitch, -90.0f), 90.0f)};
    }
    public static float[] getRotationsNeededBall(final Entity entity) {
        if (entity == null) {
            return null;
        }

        Minecraft mc = Minecraft.getMinecraft();
        double xSize = entity.posX - mc.thePlayer.posX;
        double ySize = (entity.posY + entity.getEyeHeight() / 2) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double zSize = entity.posZ - mc.thePlayer.posZ;

        double theta = MathHelper.sqrt_double(xSize * xSize + zSize * zSize);
        float yaw = (float) Math.toDegrees(Math.atan2(zSize, xSize)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(ySize, theta));

        float playerYaw = mc.thePlayer.rotationYaw;
        float playerPitch = mc.thePlayer.rotationPitch;

        float deltaYaw = MathHelper.wrapAngleTo180_float(yaw - playerYaw);
        float deltaPitch = MathHelper.wrapAngleTo180_float(pitch - playerPitch);

        float newYaw = playerYaw + deltaYaw;
        float newPitch = playerPitch + deltaPitch;

        return new float[]{ newYaw % 360.0f, newPitch % 360.0f };
    }

    public static float[] getBlockRotations(double x2, double y2, double z) {
        double var4 = x2 - RotationUtil.mc.thePlayer.posX + 0.5;
        double var6 = z - RotationUtil.mc.thePlayer.posZ + 0.5;
        double var8 = y2 - (RotationUtil.mc.thePlayer.posY + (double)RotationUtil.mc.thePlayer.getEyeHeight() - 1.0);
        double var14 = MathHelper.sqrt_double(var4 * var4 + var6 * var6);
        float var12 = (float)(Math.atan2(var6, var4) * 180.0 / Math.PI) - 90.0f;
        return new float[]{var12, (float)(-Math.atan2(var8, var14) * 180.0 / Math.PI)};
    }

    public static float[] positionRotation(double posX, double posY, double posZ, float[] lastRots, float yawSpeed, float pitchSpeed, boolean random) {
        double x2 = posX - RotationUtil.mc.thePlayer.posX;
        double y2 = posY - (RotationUtil.mc.thePlayer.posY + (double)RotationUtil.mc.thePlayer.getEyeHeight());
        double z = posZ - RotationUtil.mc.thePlayer.posZ;
        float calcYaw = (float)(MathHelper.atan2(z, x2) * 180.0 / Math.PI - 90.0);
        float calcPitch = (float)(-(MathHelper.atan2(y2, MathHelper.sqrt_double(x2 * x2 + z * z)) * 180.0 / Math.PI));
        float yaw = RotationUtil.updateRotation(lastRots[0], calcYaw, yawSpeed);
        float pitch = RotationUtil.updateRotation(lastRots[1], calcPitch, pitchSpeed);
        if (random) {
            yaw += (float)ThreadLocalRandom.current().nextGaussian();
            pitch += (float)ThreadLocalRandom.current().nextGaussian();
        }
        return new float[]{yaw, pitch};
    }

    public static int wrapAngleToDirection(float yaw, int zones) {
        int angle = (int)((double)(yaw + (float)(360 / (2 * zones))) + 0.5) % 360;
        if (angle < 0) {
            angle += 360;
        }
        return angle / (360 / zones);
    }

    public static float getGCD() {
        return (float)(Math.pow((double)RotationUtil.mc.gameSettings.mouseSensitivity * 0.6 + 0.2, 3.0) * 1.2);
    }

    public static Vector2f getRotationFromEyeToPointOffset(Vec3 position, EnumFacing enumFacing) {
        double x2 = position.xCoord + 0.5;
        double y2 = position.yCoord + 0.5;
        double z = position.zCoord + 0.5;
        return RotationUtil.getRot(new Vec3(x2 + (double) enumFacing.getDirectionVec().getX() * 0.5, y2 + (double) enumFacing.getDirectionVec().getY() * 0.5, z + (double) enumFacing.getDirectionVec().getZ() * 0.5));
    }

    public static Vector2f getRot(Vec3 pos) {
        Vec3 vec = new Vec3(RotationUtil.mc.thePlayer.posX, RotationUtil.mc.thePlayer.getEntityBoundingBox().minY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), RotationUtil.mc.thePlayer.posZ);
        double x2 = pos.xCoord - vec.xCoord;
        double y2 = pos.yCoord - vec.yCoord;
        double z = pos.zCoord - vec.zCoord;
        double sqrt = Math.sqrt(x2 * x2 + z * z);
        float yaw = (float)Math.toDegrees(Math.atan2(z, x2)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(y2, sqrt)));
        return new Vector2f(yaw, Math.min(Math.max(pitch, -90.0f), 90.0f));
    }

    public static float[] getRotationsToPosition(double x2, double y2, double z) {
        double deltaX = x2 - RotationUtil.mc.thePlayer.posX;
        double deltaY = y2 - RotationUtil.mc.thePlayer.posY - (double)RotationUtil.mc.thePlayer.getEyeHeight();
        double deltaZ = z - RotationUtil.mc.thePlayer.posZ;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float)Math.toDegrees(-Math.atan2(deltaX, deltaZ));
        float pitch = (float)Math.toDegrees(-Math.atan2(deltaY, horizontalDistance));
        return new float[]{yaw, pitch};
    }

    public static float[] getRotationsToPosition(double x2, double y2, double z, double targetX, double targetY, double targetZ) {
        double dx = targetX - x2;
        double dy = targetY - y2;
        double dz = targetZ - z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(-Math.atan2(dx, dz));
        float pitch = (float)Math.toDegrees(-Math.atan2(dy, horizontalDistance));
        return new float[]{yaw, pitch};
    }

    public static float[] scaffoldRots(double bx, double by, double bz, float lastYaw, float lastPitch, float yawSpeed, float pitchSpeed, boolean random) {
        double x2 = bx - RotationUtil.mc.thePlayer.posX;
        double y2 = by - (RotationUtil.mc.thePlayer.posY + (double)RotationUtil.mc.thePlayer.getEyeHeight());
        double z = bz - RotationUtil.mc.thePlayer.posZ;
        float calcYaw = (float)(Math.toDegrees(MathHelper.atan2(z, x2)) - 90.0);
        float calcPitch = (float)(-(MathHelper.atan2(y2, MathHelper.sqrt_double(x2 * x2 + z * z)) * 180.0 / Math.PI));
        float pitch = RotationUtil.updateRotation(lastPitch, calcPitch, pitchSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
        float yaw = RotationUtil.updateRotation(lastYaw, calcYaw, yawSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
        if (random) {
            yaw += (float)ThreadLocalRandom.current().nextDouble(-2.0, 2.0);
            pitch += (float)ThreadLocalRandom.current().nextDouble(-0.2, 0.2);
        }
        return new float[]{yaw, pitch};
    }

    public static float[] mouseSens(float yaw, float pitch, float lastYaw, float lastPitch) {
        if ((double)RotationUtil.mc.gameSettings.mouseSensitivity == 0.5) {
            RotationUtil.mc.gameSettings.mouseSensitivity = 0.47887325f;
        }
        if (yaw == lastYaw && pitch == lastPitch) {
            return new float[]{yaw, pitch};
        }
        float f1 = RotationUtil.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float f2 = f1 * f1 * f1 * 8.0f;
        int deltaX = (int)((6.667 * (double)yaw - 6.667 * (double)lastYaw) / (double)f2);
        int deltaY = (int)((6.667 * (double)pitch - 6.667 * (double)lastPitch) / (double)f2) * -1;
        float f3 = (float)deltaX * f2;
        float f4 = (float)deltaY * f2;
        yaw = (float)((double)lastYaw + (double)f3 * 0.15);
        float f5 = (float)((double)lastPitch - (double)f4 * 0.15);
        pitch = MathHelper.clamp_float(f5, -90.0f, 90.0f);
        return new float[]{yaw, pitch};
    }

    public static float rotateToYaw(float yawSpeed, float currentYaw, float calcYaw) {
        float yaw = RotationUtil.updateRotation(currentYaw, calcYaw, yawSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
        double diffYaw = MathHelper.wrapAngleTo180_float(calcYaw - currentYaw);
        if ((double)(-yawSpeed) > diffYaw || diffYaw > (double)yawSpeed) {
            yaw += (float)((double)RandomUtils.nextFloat(1.0f, 2.0f) * Math.sin((double)RotationUtil.mc.thePlayer.rotationPitch * Math.PI));
        }
        if (yaw == currentYaw) {
            return currentYaw;
        }
        if ((double)RotationUtil.mc.gameSettings.mouseSensitivity == 0.5) {
            RotationUtil.mc.gameSettings.mouseSensitivity = 0.47887325f;
        }
        float f1 = RotationUtil.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float f2 = f1 * f1 * f1 * 8.0f;
        int deltaX = (int)((6.667 * (double)yaw - 6.666666666666667 * (double)currentYaw) / (double)f2);
        float f3 = (float)deltaX * f2;
        yaw = (float)((double)currentYaw + (double)f3 * 0.15);
        return yaw;
    }

    public static float updateRotation(float current, float calc, float maxDelta) {
        float f = MathHelper.wrapAngleTo180_float(calc - current);
        if (f > maxDelta) {
            f = maxDelta;
        }
        if (f < -maxDelta) {
            f = -maxDelta;
        }
        return current + f;
    }

    public static float rotateToPitch(float pitchSpeed, float currentPitch, float calcPitch) {
        float pitch = RotationUtil.updateRotation(currentPitch, calcPitch, pitchSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
        if (pitch != calcPitch) {
            pitch += (float)((double)RandomUtils.nextFloat(1.0f, 2.0f) * Math.sin((double)RotationUtil.mc.thePlayer.rotationYaw * Math.PI));
        }
        if ((double)RotationUtil.mc.gameSettings.mouseSensitivity == 0.5) {
            RotationUtil.mc.gameSettings.mouseSensitivity = 0.47887325f;
        }
        float f1 = RotationUtil.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float f2 = f1 * f1 * f1 * 8.0f;
        int deltaY = (int)((6.667 * (double)pitch - 6.666667 * (double)currentPitch) / (double)f2) * -1;
        float f3 = (float)deltaY * f2;
        float f4 = (float)((double)currentPitch - (double)f3 * 0.15);
        pitch = MathHelper.clamp_float(f4, -90.0f, 90.0f);
        return pitch;
    }

    public static double getRotationDifference(Entity entity) {
        Vector2f rotation = RotationUtil.toRotation(RotationUtil.getCenter(entity.getEntityBoundingBox()), true);
        return RotationUtil.getRotationDifference(rotation, new Vector2f(RotationUtil.mc.thePlayer.rotationYaw, RotationUtil.mc.thePlayer.rotationPitch));
    }

    public static double getRotationDifference(Vector2f a, Vector2f b2) {
        return Math.hypot(RotationComponent.getAngleDifference(a.getX(), b2.getX()), a.getY() - b2.getY());
    }

    public static Vec3 getCenter(AxisAlignedBB bb) {
        return new Vec3(bb.minX + (bb.maxX - bb.minX) * 0.5, bb.minY + (bb.maxY - bb.minY) * 0.5, bb.minZ + (bb.maxZ - bb.minZ) * 0.5);
    }

    public static Vector2f toRotation(Vec3 vec, boolean predict) {
        Vec3 eyesPos = new Vec3(RotationUtil.mc.thePlayer.posX, RotationUtil.mc.thePlayer.getEntityBoundingBox().minY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), RotationUtil.mc.thePlayer.posZ);
        if (predict) {
            eyesPos.addVector(RotationUtil.mc.thePlayer.motionX, RotationUtil.mc.thePlayer.motionY, RotationUtil.mc.thePlayer.motionZ);
        }
        double diffX = vec.xCoord - eyesPos.xCoord;
        double diffY = vec.yCoord - eyesPos.yCoord;
        double diffZ = vec.zCoord - eyesPos.zCoord;
        return new Vector2f(MathHelper.wrapAngleTo180_float((float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f), MathHelper.wrapAngleTo180_float((float)(-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))));
    }

    public static float[] getXinXinRotation(Entity target, double range) { //dwgx
        double yDist = target.posY - RotationUtil.mc.thePlayer.posY;
        Vec3 pos = yDist >= 1.7 ? new Vec3(target.posX, target.posY, target.posZ) :
                (yDist <= -1.7 ? new Vec3(target.posX, target.posY + (double)target.getEyeHeight(), target.posZ) :
                        new Vec3(target.posX, target.posY + (double)(target.getEyeHeight() / 2.0f), target.posZ));

        Vec3 vec = new Vec3(RotationUtil.mc.thePlayer.posX, RotationUtil.mc.thePlayer.getEntityBoundingBox().minY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), RotationUtil.mc.thePlayer.posZ);
        double xDist = pos.xCoord - vec.xCoord;
        double yDist2 = pos.yCoord - vec.yCoord;
        double zDist = pos.zCoord - vec.zCoord;
        float yaw = (float)Math.toDegrees(Math.atan2(zDist, xDist)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(yDist2, Math.sqrt(xDist * xDist + zDist * zDist))));

        return new float[]{yaw, Math.min(Math.max(pitch, -90.0f), 90.0f)};
    }


    public static float[] getHVHRotation(Entity entity, double maxRange) {
        if (entity == null) {
            return null;
        }
        double diffX = entity.posX - RotationUtil.mc.thePlayer.posX;
        double diffZ = entity.posZ - RotationUtil.mc.thePlayer.posZ;
        Vec3 BestPos = RotationUtil.getNearestPointBB(RotationUtil.mc.thePlayer.getPositionEyes(1.0f), entity.getEntityBoundingBox());
        Location myEyePos = new Location(Minecraft.getMinecraft().thePlayer.posX, Minecraft.getMinecraft().thePlayer.posY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), Minecraft.getMinecraft().thePlayer.posZ);
        double diffY = BestPos.yCoord - myEyePos.getY();
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    public static float[] getRotationsNeededBlock(double x2, double y2, double z) {
        double diffX = x2 + 0.5 - Minecraft.getMinecraft().thePlayer.posX;
        double diffZ = z + 0.5 - Minecraft.getMinecraft().thePlayer.posZ;
        double diffY = y2 + 0.5 - (Minecraft.getMinecraft().thePlayer.posY + (double)Minecraft.getMinecraft().thePlayer.getEyeHeight());
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-Math.atan2(diffY, dist) * 180.0 / Math.PI);
        return new float[]{Minecraft.getMinecraft().thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - Minecraft.getMinecraft().thePlayer.rotationYaw), Minecraft.getMinecraft().thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - Minecraft.getMinecraft().thePlayer.rotationPitch)};
    }

    public static Vector2f getRotations(double posX, double posY, double posZ) {
        EntityPlayerSP player = RotationUtil.mc.thePlayer;
        double x2 = posX - player.posX;
        double y2 = posY - (player.posY + (double)player.getEyeHeight());
        double z = posZ - player.posZ;
        double dist = MathHelper.sqrt_double(x2 * x2 + z * z);
        float yaw = (float)(Math.atan2(z, x2) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(y2, dist) * 180.0 / Math.PI));
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f getRotations(BlockPos block, EnumFacing face) {
        double x2 = (double)block.getX() + 0.5 - RotationUtil.mc.thePlayer.posX + (double)face.getFrontOffsetX() / 2.0;
        double z = (double)block.getZ() + 0.5 - RotationUtil.mc.thePlayer.posZ + (double)face.getFrontOffsetZ() / 2.0;
        double y2 = (double)block.getY() + 0.5;
        double d1 = RotationUtil.mc.thePlayer.posY + (double)RotationUtil.mc.thePlayer.getEyeHeight() - y2;
        double d3 = MathHelper.sqrt_double(x2 * x2 + z * z);
        float yaw = (float)(Math.atan2(z, x2) * 180.0 / Math.PI) - 82.0f;
        float pitch = (float)(Math.atan2(d1, d3) * 180.0 / Math.PI);
        if (yaw < 0.0f) {
            yaw += 360.0f;
        }
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f getRotationsNonLivingEntity(Entity entity) {
        return RotationUtil.getRotations(entity.posX, entity.posY + (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * 0.5, entity.posZ);
    }

    public static void setVisualRotations(float yaw, float pitch) {
        RotationUtil.mc.thePlayer.rotationYawHead = RotationUtil.mc.thePlayer.renderYawOffset = yaw;
        RotationUtil.mc.thePlayer.rotationPitchHead = pitch;
    }

    public static Vec3 getVectorForRotation(Vector2f rotation) {
        float yawCos = MathHelper.cos(-rotation.getX() * ((float)Math.PI / 180) - (float)Math.PI);
        float yawSin = MathHelper.sin(-rotation.getX() * ((float)Math.PI / 180) - (float)Math.PI);
        float pitchCos = -MathHelper.cos(-rotation.getY() * ((float)Math.PI / 180));
        float pitchSin = MathHelper.sin(-rotation.getY() * ((float)Math.PI / 180));
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    public static float[] getRotations(Entity entity) {
        double pX = Minecraft.getMinecraft().thePlayer.posX;
        double pY = Minecraft.getMinecraft().thePlayer.posY + (double)Minecraft.getMinecraft().thePlayer.getEyeHeight();
        double pZ = Minecraft.getMinecraft().thePlayer.posZ;
        double eX = entity.posX;
        double eY = entity.posY + (double)(entity.height / 2.0f);
        double eZ = entity.posZ;
        double dX = pX - eX;
        double dY = pY - eY;
        double dZ = pZ - eZ;
        double dH = Math.sqrt(Math.pow(dX, 2.0) + Math.pow(dZ, 2.0));
        double yaw = Math.toDegrees(Math.atan2(dZ, dX)) + 90.0;
        double pitch = Math.toDegrees(Math.atan2(dH, dY));
        return new float[]{(float)yaw, (float)(90.0 - pitch)};
    }

    public static Vec3 getNearestPointBB(Vec3 eye, AxisAlignedBB box) {
        double[] origin = new double[]{eye.xCoord, eye.yCoord, eye.zCoord};
        double[] destMins = new double[]{box.minX, box.minY, box.minZ};
        double[] destMaxs = new double[]{box.maxX, box.maxY, box.maxZ};
        for (int i = 0; i < 3; ++i) {
            if (origin[i] > destMaxs[i]) {
                origin[i] = destMaxs[i];
                continue;
            }
            if (!(origin[i] < destMins[i])) continue;
            origin[i] = destMins[i];
        }
        return new Vec3(origin[0], origin[1], origin[2]);
    }

    public static Vector2f toRotationMisc(Vec3 vec, boolean predict) {
        Vec3 eyesPos = new Vec3(RotationUtil.mc.thePlayer.posX, RotationUtil.mc.thePlayer.getEntityBoundingBox().minY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), RotationUtil.mc.thePlayer.posZ);
        if (predict) {
            eyesPos.addVector(RotationUtil.mc.thePlayer.motionX, RotationUtil.mc.thePlayer.motionY, RotationUtil.mc.thePlayer.motionZ);
        }
        double diffX = vec.xCoord - eyesPos.xCoord;
        double diffY = vec.yCoord - eyesPos.yCoord;
        double diffZ = vec.zCoord - eyesPos.zCoord;
        return new Vector2f(MathHelper.wrapAngleTo180_float((float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f), MathHelper.wrapAngleTo180_float((float)(-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))));
    }

    public static float getTrajAngleSolutionLow(float d3, float d1, float velocity) {
        float g2 = 0.006f;
        float sqrt = velocity * velocity * velocity * velocity - g2 * (g2 * (d3 * d3) + 2.0f * d1 * (velocity * velocity));
        return (float)Math.toDegrees(Math.atan(((double)(velocity * velocity) - Math.sqrt(sqrt)) / (double)(g2 * d3)));
    }

    public static float getBowRot(Entity entity) {
        double diffY;
        double diffX = entity.posX - RotationUtil.mc.thePlayer.posX;
        double diffZ = entity.posZ - RotationUtil.mc.thePlayer.posZ;
        Location BestPos = new Location(entity.posX, entity.posY, entity.posZ);
        Location myEyePos = new Location(Minecraft.getMinecraft().thePlayer.posX, Minecraft.getMinecraft().thePlayer.posY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), Minecraft.getMinecraft().thePlayer.posZ);
        for (diffY = entity.boundingBox.minY + 0.7; diffY < entity.boundingBox.maxY - 0.1; diffY += 0.1) {
            Location location = new Location(entity.posX, diffY, entity.posZ);
            if (!(myEyePos.distanceTo(location) < myEyePos.distanceTo(BestPos))) continue;
            BestPos = new Location(entity.posX, diffY, entity.posZ);
        }
        diffY = BestPos.getY() - (Minecraft.getMinecraft().thePlayer.posY + (double)Minecraft.getMinecraft().thePlayer.getEyeHeight());
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
        return yaw;
    }
    public static Vector2f calculate(Vector3d from, Vector3d to) {
        Vector3d diff = to.subtract(from);
        double distance = Math.hypot(diff.getX(), diff.getZ());
        float yaw = (float)(MathHelper.atan2(diff.getZ(), diff.getX()) * (double)MathHelper.TO_DEGREES) - 90.0f;
        float pitch = (float)(-(MathHelper.atan2(diff.getY(), distance) * (double)MathHelper.TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f calculate(Vec3 to) {
        return RotationUtil.calculate(RotationUtil.mc.thePlayer.getCustomPositionVector().add(0.0, RotationUtil.mc.thePlayer.getEyeHeight(), 0.0), new Vector3d(to.xCoord, to.yCoord, to.zCoord));
    }

    public static Vector2f calculate(Vector3d to) {
        return RotationUtil.calculate(RotationUtil.mc.thePlayer.getCustomPositionVector().add(0.0, RotationUtil.mc.thePlayer.getEyeHeight(), 0.0), to);
    }

    public static Vector2f calculate(Vector3d position, EnumFacing enumFacing) {
        double x2 = position.getX() + 0.5;
        double y2 = position.getY() + 0.5;
        double z = position.getZ() + 0.5;
        return RotationUtil.calculate(new Vector3d(x2 + (double) enumFacing.getDirectionVec().getX() * 0.5, y2 + (double) enumFacing.getDirectionVec().getY() * 0.5, z + (double) enumFacing.getDirectionVec().getZ() * 0.5));
    }

    public static Vector2f calculate(Entity entity) {
        return RotationUtil.calculate(entity.getCustomPositionVector().add(0.0, Math.max(0.0, Math.min(RotationUtil.mc.thePlayer.posY - entity.posY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * 0.9)), 0.0));
    }

    public static Vector2f calculate(Entity entity, boolean adaptive, double range) {
        Vector2f normalRotations = RotationUtil.calculate(entity);
        if (!adaptive || RayCastUtil.rayCast(normalRotations, range).typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return normalRotations;
        }
        for (double yPercent = 1.0; yPercent >= 0.0; yPercent -= 0.25) {
            for (double xPercent = 1.0; xPercent >= -0.5; xPercent -= 0.5) {
                for (double zPercent = 1.0; zPercent >= -0.5; zPercent -= 0.5) {
                    Vector2f adaptiveRotations = RotationUtil.calculate(entity.getCustomPositionVector().add((entity.getEntityBoundingBox().maxX - entity.getEntityBoundingBox().minX) * xPercent, (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * yPercent, (entity.getEntityBoundingBox().maxZ - entity.getEntityBoundingBox().minZ) * zPercent));
                    if (RayCastUtil.rayCast(adaptiveRotations, range).typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) continue;
                    return adaptiveRotations;
                }
            }
        }
        return normalRotations;
    }

    public static Vector2f calculateSimple(Entity entity, double range, double wallRange) {
        AxisAlignedBB aabb = entity.getEntityBoundingBox().contract(-0.05, -0.05, -0.05).contract(0.05, 0.05, 0.05);
        range += 0.05;
        wallRange += 0.05;
        Vec3 eyePos = RotationUtil.mc.thePlayer.getPositionEyes(1.0f);
        Vec3 nearest = new Vec3(MathUtils.clamp(eyePos.xCoord, aabb.minX, aabb.maxX), MathUtils.clamp(eyePos.yCoord, aabb.minY, aabb.maxY), MathUtils.clamp(eyePos.zCoord, aabb.minZ, aabb.maxZ));
        Vector2f rotation = RotationUtil.toRotation(nearest, false);
        if (nearest.subtract(eyePos).lengthSquared() <= wallRange * wallRange) {
            return rotation;
        }
        MovingObjectPosition result = RaytraceUtil.rayCast(rotation, range, 0.0f, false);
        double maxRange = Math.max(wallRange, range);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && result.entityHit == entity && result.hitVec.subtract(eyePos).lengthSquared() <= maxRange * maxRange) {
            return rotation;
        }
        return null;
    }

    public static Vector2f calculate(Entity entity, boolean adaptive, double range, double wallRange, boolean predict, boolean randomCenter) {
        MovingObjectPosition normalResult;
        if (RotationUtil.mc.thePlayer == null) {
            return null;
        }
        double rangeSq = range * range;
        double wallRangeSq = wallRange * wallRange;
        Vector2f simpleRotation = RotationUtil.calculateSimple(entity, range, wallRange);
        if (simpleRotation != null) {
            return simpleRotation;
        }
        Vector2f normalRotations = RotationUtil.toRotation(RotationUtil.getVec(entity), predict);
        if (!randomCenter && (normalResult = RaytraceUtil.rayCast(normalRotations, range, 0.0f, false)) != null && normalResult.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return normalRotations;
        }
        double yStart = 1.0;
        double yEnd = 0.0;
        double yStep = -0.5;
        if (randomCenter && MathUtils.secureRandom.nextBoolean()) {
            yStart = 0.0;
            yEnd = 1.0;
            yStep = 0.5;
        }
        double yPercent = yStart;
        while (Math.abs(yEnd - yPercent) > 0.001) {
            double xzStart = 0.5;
            double xzEnd = -0.5;
            double xzStep = -0.1;
            if (randomCenter) {
                Collections.shuffle(xzPercents);
            }
            for (double xzPercent : xzPercents) {
                for (int side = 0; side <= 3; ++side) {
                    MovingObjectPosition result;
                    double xPercent = 0.0;
                    double zPercent = 0.0;
                    switch (side) {
                        case 0: {
                            xPercent = xzPercent;
                            zPercent = 0.5;
                            break;
                        }
                        case 1: {
                            xPercent = xzPercent;
                            zPercent = -0.5;
                            break;
                        }
                        case 2: {
                            xPercent = 0.5;
                            zPercent = xzPercent;
                            break;
                        }
                        case 3: {
                            xPercent = -0.5;
                            zPercent = xzPercent;
                        }
                    }
                    Vec3 Vec32 = RotationUtil.getVec(entity).add(new Vec3((entity.getEntityBoundingBox().maxX - entity.getEntityBoundingBox().minX) * xPercent, (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * yPercent, (entity.getEntityBoundingBox().maxZ - entity.getEntityBoundingBox().minZ) * zPercent));
                    double distanceSq = Vec32.squareDistanceTo(RotationUtil.mc.thePlayer.getPositionEyes(1.0f));
                    Rotation rotation = RotationUtil.toRotationRot(Vec32, predict);
                    rotation.fixedSensitivity(Float.valueOf(RotationUtil.mc.gameSettings.mouseSensitivity));
                    rotation.distanceSq = distanceSq;
                    if (distanceSq <= wallRangeSq && (result = RaytraceUtil.rayCast(rotation.toVec2f(), wallRange, 0.0f, true)) != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        return rotation.toVec2f();
                    }
                    if (!(distanceSq <= rangeSq) || (result = RaytraceUtil.rayCast(rotation.toVec2f(), range, 0.0f, false)) == null || result.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) continue;
                    return rotation.toVec2f();
                }
            }
            yPercent += yStep;
        }
        return null;
    }

    public static RotationNew calculate(Entity entity, final double range, final double wallRange, float predict, float predictPlayer) {
        if (mc.thePlayer == null) return null;

        RotationNew normalRotations = toRotation(new Vec3(
                entity.posX,
                getClosestPoint(mc.thePlayer.getPositionEyes(1f), entity.getEntityBoundingBox()).yCoord,
                entity.posZ
        ), 0F);

        MovingObjectPosition normalResult = rayCast(normalRotations, range, 0.05F, mc.thePlayer, false, predict, predictPlayer);
        if (normalResult != null && normalResult.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return normalRotations;
        }

        RotationNew simpleRotation = calculateSimple(entity, range, wallRange, predict, predictPlayer);
        if (simpleRotation != null) return simpleRotation;

        return normalRotations;
    }

    public static RotationNew calculateSimple(final Entity entity, double range, double wallRange, float predict, float predictPlayer) {
        AxisAlignedBB aabb = entity.getEntityBoundingBox();
        if (predict != 0) {
            aabb = aabb.offset(
                    (entity.posX - entity.prevPosX) * predict,
                    (entity.posY - entity.prevPosY) * predict,
                    (entity.posZ - entity.prevPosZ) * predict
            );
        }
        Vec3 eyePos = mc.thePlayer.getPositionEyes(predictPlayer);
        Vec3 nearest = new Vec3(
                MathHelper.clamp_double(eyePos.xCoord, aabb.minX, aabb.maxX),
                MathHelper.clamp_double(eyePos.yCoord, aabb.minY, aabb.maxY),
                MathHelper.clamp_double(eyePos.zCoord, aabb.minZ, aabb.maxZ)
        );
        RotationNew rotation = toRotation(nearest, predictPlayer);
        if (nearest.subtract(eyePos).lengthSquared() <= wallRange * wallRange) {
            return rotation;
        }

        MovingObjectPosition result = rayCast(rotation, range, 0.05F, mc.thePlayer, false, predict, predictPlayer);
        final double maxRange = Math.max(wallRange, range);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && result.entityHit == entity && result.hitVec.subtract(eyePos).lengthSquared() <= maxRange * maxRange) {
            return rotation;
        }

        return null;
    }

    public static RotationNew toRotation(final Vec3 vec, float partialTicks) {
        final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY +
                mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ).addVector(mc.thePlayer.motionX * partialTicks, mc.thePlayer.motionY * partialTicks, mc.thePlayer.motionZ * partialTicks);
        return new RotationNew(eyesPos, vec);
    }

    /**
     * Get the closest point on a boundingBox from start
     *
     * @param start       Src
     * @param boundingBox boundingBox to calculate closest point from start
     * @return The closest point on boundingBox as a hit vec
     */
    public static Vec3 getClosestPoint(final Vec3 start,
                                final AxisAlignedBB boundingBox) {
        final double closestX = start.xCoord >= boundingBox.maxX ? boundingBox.maxX :
                start.xCoord <= boundingBox.minX ? boundingBox.minX :
                        boundingBox.minX + (start.xCoord - boundingBox.minX);

        final double closestY = start.yCoord >= boundingBox.maxY ? boundingBox.maxY :
                start.yCoord <= boundingBox.minY ? boundingBox.minY :
                        boundingBox.minY + (start.yCoord - boundingBox.minY);

        final double closestZ = start.zCoord >= boundingBox.maxZ ? boundingBox.maxZ :
                start.zCoord <= boundingBox.minZ ? boundingBox.minZ :
                        boundingBox.minZ + (start.zCoord - boundingBox.minZ);

        return new Vec3(closestX, closestY, closestZ);
    }

    public static MovingObjectPosition rayCast(final RotationNew rotation, final double range) {
        return rayCast(rotation, range, 0);
    }

    public static MovingObjectPosition rayCast(final RotationNew rotation, final double range, final float expand) {
        return rayCast(rotation, range, expand, mc.thePlayer);
    }

    public static MovingObjectPosition rayCast(final RotationNew rotation, final double range, final float expand, Entity entity) {
        final float partialTicks = mc.timer.renderPartialTicks;
        MovingObjectPosition objectMouseOver;

        if (entity != null && mc.theWorld != null) {
            objectMouseOver = entity.rayTraceCustom(range, rotation.getYaw(), rotation.getPitch());
            double d1 = range;
            final Vec3 vec3 = entity.getPositionEyes(partialTicks);

            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = mc.thePlayer.getVectorForRotation(rotation.getYaw(), rotation.getPitch());
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range);
            Entity pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize() + expand;
                final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    final double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }

            return objectMouseOver;
        }

        return null;
    }

    public static MovingObjectPosition rayCast(final RotationNew rotation, final double range, final float expand, Entity entity, boolean throughWall, float predict, float predictPlayer) {
        MovingObjectPosition objectMouseOver;
        if (entity != null && mc.theWorld != null) {
            objectMouseOver = entity.rayTrace(range, rotation.getYaw(), rotation.getPitch(), predictPlayer);
            double d1 = range;
            final Vec3 vec3 = entity.getPositionEyes(predictPlayer);

            if (objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !throughWall) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = mc.thePlayer.getVectorForRotation(rotation.getPitch(), rotation.getYaw());
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range);
            Entity pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;

            for (final Entity entity1 : list) {
                if (entity1.getUniqueID().equals(mc.thePlayer.getUniqueID())) continue;

                final float f1 = entity1.getCollisionBorderSize() + expand;
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);

                if (predict != 0) {
                    axisalignedbb = axisalignedbb.offset(
                            (entity1.posX - entity1.prevPosX) * predict,
                            (entity1.posY - entity1.prevPosY) * predict,
                            (entity1.posZ - entity1.prevPosZ) * predict
                    );
                }

                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    final double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }

            return objectMouseOver;
        }

        return null;
    }

    public static Rotation toRotationRot(Vec3 vec, boolean predict) {
        Vec3 eyesPos = new Vec3(RotationUtil.mc.thePlayer.posX, RotationUtil.mc.thePlayer.getEntityBoundingBox().minY + (double)RotationUtil.mc.thePlayer.getEyeHeight(), RotationUtil.mc.thePlayer.posZ);
        if (predict) {
            eyesPos.addVector(RotationUtil.mc.thePlayer.motionX, RotationUtil.mc.thePlayer.motionY, RotationUtil.mc.thePlayer.motionZ);
        }
        double diffX = vec.xCoord - eyesPos.xCoord;
        double diffY = vec.yCoord - eyesPos.yCoord;
        double diffZ = vec.zCoord - eyesPos.zCoord;
        return new Rotation(MathHelper.wrapAngleTo180_float((float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f), MathHelper.wrapAngleTo180_float((float)(-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))));
    }

    public static Vec3 getVec(Entity entity) {
        return new Vec3(entity.posX, entity.posY, entity.posZ);
    }

    public static void setVisualRotations(float[] rotations) {
        RotationUtil.setVisualRotations(rotations[0], rotations[1]);
    }

    public static void setVisualRotations(EventMotion e) {
        RotationUtil.setVisualRotations(e.getYaw(), e.getPitch());
    }

    public static float getRotation(float currentRotation, float targetRotation, float maxIncrement) {
        float deltaAngle = MathHelper.wrapAngleTo180_float(targetRotation - currentRotation);
        if (deltaAngle > maxIncrement) {
            deltaAngle = maxIncrement;
        }
        if (deltaAngle < -maxIncrement) {
            deltaAngle = -maxIncrement;
        }
        return currentRotation + deltaAngle / 2.0f;
    }
    public static Vector2f getRotation(Vector3d from, Vector3d to) {

        final double x = to.getX() - from.getX();
        final double y = to.getY() - from.getY();
        final double z = to.getZ() - from.getZ();

        final double sqrt = Math.sqrt(x * x + z * z);

        final float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90F;
        final float pitch = (float) (-Math.toDegrees(Math.atan2(y, sqrt)));

        return new Vector2f(yaw, Math.min(Math.max(pitch, -90), 90));
    }
    public static Vector2f resetRotation(Vector2f rotation) {
        if (rotation == null) {
            return null;
        }
        float yaw = RotationUtil.mc.thePlayer.rotationYaw;
        float pitch = RotationUtil.mc.thePlayer.rotationPitch;
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f applySensitivityPatch(Vector2f rotation, Vector2f previousRotation) {
        float mouseSensitivity = (float)((double)RotationUtil.mc.gameSettings.mouseSensitivity * (1.0 + Math.random() / 1.0E7) * (double)0.6f + (double)0.2f);
        double multiplier = (double)(mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0f) * 0.15;
        float yaw = previousRotation.x + (float)((double)Math.round((double)(rotation.x - previousRotation.x) / multiplier) * multiplier);
        float pitch = previousRotation.y + (float)((double)Math.round((double)(rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }

    public static Vector2f smooth(Vector2f targetRotation) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f smooth(Vector2f lastRotation, Vector2f targetRotation, double speed) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        return new Vector2f(yaw, pitch);
    }


    private static float[] getRotationsByVec(Vec3 origin, Vec3 position) {
        Vec3 difference = position.subtract(origin);
        double distance = difference.flat().lengthVector();
        float yaw = (float)Math.toDegrees(Math.atan2(difference.zCoord, difference.xCoord)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(difference.yCoord, distance)));
        return new float[]{yaw, pitch};
    }

    public static float[] getRotationBlock(BlockPos pos) {
        return RotationUtil.getRotationsByVec(RotationUtil.mc.thePlayer.getPositionVector().addVector(0.0, RotationUtil.mc.thePlayer.getEyeHeight(), 0.0), new Vec3((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5));
    }

    public static RotationNew getRotationBlock(final BlockPos pos, float predict) {
        return new RotationNew(mc.thePlayer.getPositionEyes(predict), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public static float getYawDirection(float yaw, float strafe, float moveForward) {
        float rotationYaw = yaw;
        if (moveForward < 0.0f) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (moveForward < 0.0f) {
            forward = -0.5f;
        } else if (moveForward > 0.0f) {
            forward = 0.5f;
        }
        if (strafe > 0.0f) {
            rotationYaw -= 90.0f * forward;
        }
        if (strafe < 0.0f) {
            rotationYaw += 90.0f * forward;
        }
        return rotationYaw;
    }

    public static float getClampRotation() {
        float rotationYaw = Minecraft.getMinecraft().thePlayer.rotationYaw;
        float n = 1.0f;
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward < 0.0f) {
            rotationYaw += 180.0f;
            n = -0.5f;
        } else if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward > 0.0f) {
            n = 0.5f;
        }
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe > 0.0f) {
            rotationYaw -= 90.0f * n;
        }
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe < 0.0f) {
            rotationYaw += 90.0f * n;
        }
        return rotationYaw * ((float)Math.PI / 180);
    }

    public Vector2f toVec2f() {
        return new Vector2f(this.yaw, this.pitch);
    }

    @Setter
    @Getter
    public static class Rotation {
        public double distanceSq;
        float yaw;
        float pitch;

        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Rotation(Vec3 from, Vec3 to) {
            final Vec3 diff = to.subtract(from);

            this.yaw = MathHelper.wrapDegrees(
                    (float) Math.toDegrees(Math.atan2(diff.zCoord, diff.xCoord)) - 90F
            );
            this.pitch = MathHelper.wrapDegrees(
                    (float) (-Math.toDegrees(Math.atan2(diff.yCoord, Math.sqrt(diff.xCoord * diff.xCoord + diff.zCoord * diff.zCoord))))
            );
        }

        public void toPlayer(EntityPlayer player) {
            if (Float.isNaN(this.yaw) || Float.isNaN(this.pitch)) {
                return;
            }
            this.fixedSensitivity(RotationUtil.mc.gameSettings.mouseSensitivity);
            player.rotationYaw = this.yaw;
            player.rotationPitch = this.pitch;
        }

        public void fixedSensitivity(Float sensitivity) {
            float f = sensitivity * 0.6f + 0.2f;
            float gcd = f * f * f * 1.2f;
            this.yaw -= this.yaw % gcd;
            this.pitch -= this.pitch % gcd;
        }

        public static float updateRotation(float current, float calc, float maxDelta) {
            float f = MathHelper.wrapAngleTo180_float(calc - current);
            if (f > maxDelta) {
                f = maxDelta;
            }
            if (f < -maxDelta) {
                f = -maxDelta;
            }
            return current + f;
        }

        public float rotateToYaw(float yawSpeed, float currentYaw, float calcYaw) {
            float yaw = Rotation.updateRotation(currentYaw, calcYaw, yawSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
            double diffYaw = MathHelper.wrapAngleTo180_float(calcYaw - currentYaw);
            if ((double)(-yawSpeed) > diffYaw || diffYaw > (double)yawSpeed) {
                yaw += (float)((double)RandomUtils.nextFloat(1.0f, 2.0f) * Math.sin((double)RotationUtil.mc.thePlayer.rotationPitch * Math.PI));
            }
            if (yaw == currentYaw) {
                return currentYaw;
            }
            if ((double)RotationUtil.mc.gameSettings.mouseSensitivity == 0.5) {
                RotationUtil.mc.gameSettings.mouseSensitivity = 0.47887325f;
            }
            float f1 = RotationUtil.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            float f2 = f1 * f1 * f1 * 8.0f;
            int deltaX = (int)((6.667 * (double)yaw - 6.666666666666667 * (double)currentYaw) / (double)f2);
            float f3 = (float)deltaX * f2;
            yaw = (float)((double)currentYaw + (double)f3 * 0.15);
            return yaw;
        }

        public float rotateToYaw(float yawSpeed, float[] currentRots, float calcYaw) {
            float yaw = Rotation.updateRotation(currentRots[0], calcYaw, yawSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
            if (yaw != calcYaw) {
                yaw += (float)((double)RandomUtils.nextFloat(1.0f, 2.0f) * Math.sin((double)currentRots[1] * Math.PI));
            }
            if (yaw == currentRots[0]) {
                return currentRots[0];
            }
            yaw += (float)(ThreadLocalRandom.current().nextGaussian() * 0.2);
            if ((double)RotationUtil.mc.gameSettings.mouseSensitivity == 0.5) {
                RotationUtil.mc.gameSettings.mouseSensitivity = 0.47887325f;
            }
            float f1 = RotationUtil.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            float f2 = f1 * f1 * f1 * 8.0f;
            int deltaX = (int)((6.667 * (double)yaw - 6.6666667 * (double)currentRots[0]) / (double)f2);
            float f3 = (float)deltaX * f2;
            yaw = (float)((double)currentRots[0] + (double)f3 * 0.15);
            return yaw;
        }

        public float rotateToPitch(float pitchSpeed, float currentPitch, float calcPitch) {
            float pitch = Rotation.updateRotation(currentPitch, calcPitch, pitchSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
            if (pitch != calcPitch) {
                pitch += (float)((double)RandomUtils.nextFloat(1.0f, 2.0f) * Math.sin((double)RotationUtil.mc.thePlayer.rotationYaw * Math.PI));
            }
            if ((double)RotationUtil.mc.gameSettings.mouseSensitivity == 0.5) {
                RotationUtil.mc.gameSettings.mouseSensitivity = 0.47887325f;
            }
            float f1 = RotationUtil.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            float f2 = f1 * f1 * f1 * 8.0f;
            int deltaY = (int)((6.667 * (double)pitch - 6.666667 * (double)currentPitch) / (double)f2) * -1;
            float f3 = (float)deltaY * f2;
            float f4 = (float)((double)currentPitch - (double)f3 * 0.15);
            pitch = MathHelper.clamp_float(f4, -90.0f, 90.0f);
            return pitch;
        }

        public float rotateToPitch(float pitchSpeed, float[] currentRots, float calcPitch) {
            float pitch = Rotation.updateRotation(currentRots[1], calcPitch, pitchSpeed + RandomUtils.nextFloat(0.0f, 15.0f));
            if (pitch != calcPitch) {
                pitch += (float)((double)RandomUtils.nextFloat(1.0f, 2.0f) * Math.sin((double)currentRots[0] * Math.PI));
            }
            if ((double)RotationUtil.mc.gameSettings.mouseSensitivity == 0.5) {
                RotationUtil.mc.gameSettings.mouseSensitivity = 0.47887325f;
            }
            float f1 = RotationUtil.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            float f2 = f1 * f1 * f1 * 8.0f;
            int deltaY = (int)((6.667 * (double)pitch - 6.666667 * (double)currentRots[1]) / (double)f2) * -1;
            float f3 = (float)deltaY * f2;
            float f4 = (float)((double)currentRots[1] - (double)f3 * 0.15);
            pitch = MathHelper.clamp_float(f4, -90.0f, 90.0f);
            return pitch;
        }

        public Vector2f toVec2f() {
            return new Vector2f(this.yaw, this.pitch);
        }
    }
    public static float[] getRotationToBlock(BlockPos pos) {
        double x = pos.getX() - Minecraft.getMinecraft().thePlayer.posX;
        double y = pos.getY() - Minecraft.getMinecraft().thePlayer.posY + 1.7;
        double z = pos.getZ() - Minecraft.getMinecraft().thePlayer.posZ;
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
        return new float[]{yaw, pitch};
    }
}

