package dev.xinxin.module.modules.render;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender3D;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Projectile extends Module {
    private float yaw;
    private float pitch;
    private float power;
    private boolean isBow;
    private boolean hitEntity;

    public static boolean overrideActive = false;
    public static float overrideYaw = 0f;
    public static float overridePitch = 0f;
    public static float overridePower = 1f;
    public static Item overrideItem = null;

    public Projectile() {
        super("Projectile", Category.Render, "抛物线预测");
    }

    public static void enableOverride(Item item, float yaw, float pitch, float power) {
        overrideActive = true;
        overrideItem = item;
        overrideYaw = yaw;
        overridePitch = pitch;
        overridePower = power;
    }

    public static void disableOverride() {
        overrideActive = false;
        overrideItem = null;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.isPost()) return;
        if (!overrideActive) {
            this.yaw = e.getYaw();
            this.pitch = e.getPitch();
        }
    }

    @EventTarget
    public void onR3D(EventRender3D e) {
        Item held = mc.thePlayer.getCurrentEquippedItem() == null ? null : mc.thePlayer.getCurrentEquippedItem().getItem();
        Item item = overrideActive ? overrideItem : held;
        if (item == null) return;

        float y = overrideActive ? overrideYaw : this.yaw;
        float p = overrideActive ? overridePitch : this.pitch;

        float size = 0.25f;
        float gravity = 0.03f;
        float motionFactor = 1.5f;
        float motionSlowdown = 0.99f;
        float pitchDifference = 0.0f;
        isBow = false;
        hitEntity = false;

        if (item instanceof ItemBow) {
            isBow = true;
            gravity = 0.05f;
            size = 0.3f;
            power = overrideActive ? overridePower : (float) mc.thePlayer.getItemInUseDuration() / 20.0f;
            power = (power * power + power * 2.0f) / 3.0f;
            if (power < 0.1f) return;
            power = Math.min(power, 1.0f);
            motionFactor = power * 3.0f;
        } else if (item instanceof ItemFishingRod) {
            gravity = 0.04f;
            motionSlowdown = 0.92f;
        } else if (mc.thePlayer.getCurrentEquippedItem() != null && ItemPotion.isSplash(mc.thePlayer.getCurrentEquippedItem().getMetadata())) {
            gravity = 0.05f;
            pitchDifference = -20.0f;
            motionFactor = 0.5f;
        } else if (!(item instanceof ItemSnowball || item instanceof ItemEnderPearl || item instanceof net.minecraft.item.ItemEgg || item.equals(Item.getItemById(46)))) {
            return;
        }

        double posX = mc.getRenderManager().renderPosX - (double) (MathHelper.cos(y / 180.0f * (float) Math.PI) * 0.16f);
        double posY = mc.getRenderManager().getRenderPosY() + (double) mc.thePlayer.getEyeHeight() - 0.1;
        double posZ = mc.getRenderManager().getRenderPosZ() - (double) (MathHelper.sin(y / 180.0f * (float) Math.PI) * 0.16f);

        double motionX = (double) (-MathHelper.sin(y / 180.0f * (float) Math.PI) * MathHelper.cos(p / 180.0f * (float) Math.PI)) * (isBow ? 1.0 : 0.4);
        double motionY = (double) (-MathHelper.sin((p + pitchDifference) / 180.0f * (float) Math.PI)) * (isBow ? 1.0 : 0.4);
        double motionZ = (double) (MathHelper.cos(y / 180.0f * (float) Math.PI) * MathHelper.cos(p / 180.0f * (float) Math.PI)) * (isBow ? 1.0 : 0.4);

        float distance = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= distance; motionY /= distance; motionZ /= distance;
        motionX *= motionFactor; motionY *= motionFactor; motionZ *= motionFactor;

        MovingObjectPosition landingPosition = null;
        boolean hasLanded = false;

        RenderUtil.enableRender3D(true);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_STRIP);

        while (!hasLanded && posY > 0.0) {
            Vec3 posBefore = new Vec3(posX, posY, posZ);
            Vec3 posAfter = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);

            landingPosition = mc.theWorld.rayTraceBlocks(posBefore, posAfter, false, true, false);
            if (landingPosition != null) {
                hasLanded = true;
                posAfter = landingPosition.hitVec;
            }

            AxisAlignedBB projectileBB = new AxisAlignedBB(posX - size, posY - size, posZ - size, posX + size, posY + size, posZ + size)
                    .addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0);

            for (Entity entity : getEntitiesWithinAABB(projectileBB)) {
                if (!entity.canBeCollidedWith() || entity == mc.thePlayer) continue;
                MovingObjectPosition entityHit = entity.getEntityBoundingBox().expand(size, size, size).calculateIntercept(posBefore, posAfter);
                if (entityHit != null) {
                    hitEntity = true;
                    hasLanded = true;
                    landingPosition = new MovingObjectPosition(entity);
                    posAfter = entityHit.hitVec;
                    break;
                }
            }

            posX += motionX; posY += motionY; posZ += motionZ;

            BlockPos blockPos = new BlockPos(posX, posY, posZ);
            Block block = mc.theWorld.getBlockState(blockPos).getBlock();
            if (block.getMaterial() == Material.water) {
                motionX *= 0.6; motionY *= 0.6; motionZ *= 0.6;
            } else {
                motionX *= motionSlowdown; motionY *= motionSlowdown; motionZ *= motionSlowdown;
            }
            motionY -= gravity;

            GL11.glVertex3d(posX - mc.getRenderManager().renderPosX, posY - mc.getRenderManager().renderPosY, posZ - mc.getRenderManager().renderPosZ);
        }
        GL11.glEnd();

        if (landingPosition != null) {
            double endX = (landingPosition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY ? landingPosition.entityHit.posX : landingPosition.hitVec.xCoord);
            double endY = (landingPosition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY ? landingPosition.entityHit.posY : landingPosition.hitVec.yCoord);
            double endZ = (landingPosition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY ? landingPosition.entityHit.posZ : landingPosition.hitVec.zCoord);

            if (landingPosition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                Entity ent = landingPosition.entityHit;
                double dx = ent.posX - ent.lastTickPosX;
                double dy = ent.posY - ent.lastTickPosY;
                double dz = ent.posZ - ent.lastTickPosZ;
                float pt = e.getPartialTicks();
                AxisAlignedBB box = ent.getEntityBoundingBox()
                        .offset(-mc.getRenderManager().renderPosX, -mc.getRenderManager().renderPosY, -mc.getRenderManager().renderPosZ)
                        .offset(-dx, -dy, -dz)
                        .offset(dx * pt, dy * pt, dz * pt)
                        .expand(0.1, 0.1, 0.1);
                GL11.glColor4f(1f, 0f, 0f, 0.5f);
                drawSolidBox(box);
                GL11.glColor4f(1f, 0f, 0f, 0.75f);
                drawOutlinedBox(box);
            }

            AxisAlignedBB bb;
            float r = 1f, g = 1f, b = 1f;
            if (landingPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                EnumFacing side = landingPosition.sideHit;
                if (side == EnumFacing.SOUTH) {
                    bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.5, 0.5, 0.1);
                } else if (side == EnumFacing.NORTH) {
                    bb = new AxisAlignedBB(0.0, 0.0, 0.4, 0.5, 0.5, 0.5);
                } else if (side == EnumFacing.EAST) {
                    bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.1, 0.5, 0.5);
                } else if (side == EnumFacing.WEST) {
                    bb = new AxisAlignedBB(0.4, 0.0, 0.0, 0.5, 0.5, 0.5);
                } else if (side == EnumFacing.UP) {
                    bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.5, 0.1, 0.5);
                    r = 0f; g = 1f; b = 0f;
                } else {
                    bb = new AxisAlignedBB(0.0, 0.4, 0.0, 0.5, 0.5, 0.5);
                }
            } else {
                bb = new AxisAlignedBB(0.15, 0.15, 0.15, 0.35, 0.35, 0.35);
            }

            double rx = endX - mc.getRenderManager().renderPosX;
            double ry = endY - mc.getRenderManager().renderPosY;
            double rz = endZ - mc.getRenderManager().renderPosZ;

            GL11.glPushMatrix();
            GL11.glTranslated(rx - 0.25, ry - 0.25, rz - 0.25);
            GL11.glColor4f(r, g, b, 0.25f);
            drawSolidBox(bb);
            GL11.glColor4f(r, g, b, 0.75f);
            drawOutlinedBox(bb);
            GL11.glPopMatrix();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderUtil.disableRender3D(true);
    }

    private void drawSolidBox(AxisAlignedBB bb) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawOutlinedBox(AxisAlignedBB bb) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static List<Entity> getEntitiesWithinAABB(AxisAlignedBB axisalignedBB) {
        ArrayList<Entity> list = new ArrayList<Entity>();
        int chunkMinX = MathHelper.floor_double((axisalignedBB.minX - 2.0) / 16.0);
        int chunkMaxX = MathHelper.floor_double((axisalignedBB.maxX + 2.0) / 16.0);
        int chunkMinZ = MathHelper.floor_double((axisalignedBB.minZ - 2.0) / 16.0);
        int chunkMaxZ = MathHelper.floor_double((axisalignedBB.maxZ + 2.0) / 16.0);

        for (int x = chunkMinX; x <= chunkMaxX; ++x) {
            for (int z = chunkMinZ; z <= chunkMaxZ; ++z) {
                if (!Projectile.mc.theWorld.getChunkProvider().chunkExists(x, z)) continue;
                Projectile.mc.theWorld.getChunkFromChunkCoords(x, z)
                        .getEntitiesWithinAABBForEntity(Projectile.mc.thePlayer, axisalignedBB, list, null);
            }
        }
        return list;
    }
}
