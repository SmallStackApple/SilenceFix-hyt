package dev.xinxin.utils;

import dev.xinxin.SilenceFix;
import dev.xinxin.utils.client.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

public class ESPUtil {
    private static final Frustum frustum = new Frustum();
    private static final FloatBuffer windPos = BufferUtils.createFloatBuffer((int)4);
    private static final IntBuffer intBuffer = GLAllocation.createDirectIntBuffer(16);
    private static final FloatBuffer floatBuffer1 = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer floatBuffer2 = GLAllocation.createDirectFloatBuffer(16);

    public static boolean isInView(Entity ent) {
        frustum.setPosition(SilenceFix.mc.getRenderViewEntity().posX, SilenceFix.mc.getRenderViewEntity().posY, SilenceFix.mc.getRenderViewEntity().posZ);
        return frustum.isBoundingBoxInFrustum(ent.getEntityBoundingBox()) || ent.ignoreFrustumCheck;
    }

    public static Vector3f projectOn2D(float x2, float y2, float z, int scaleFactor) {
        GL11.glGetFloat((int)2982, (FloatBuffer)floatBuffer1);
        GL11.glGetFloat((int)2983, (FloatBuffer)floatBuffer2);
        GL11.glGetInteger((int)2978, (IntBuffer)intBuffer);
        if (GLU.gluProject((float)x2, (float)y2, (float)z, (FloatBuffer)floatBuffer1, (FloatBuffer)floatBuffer2, (IntBuffer)intBuffer, (FloatBuffer)windPos)) {
            return new Vector3f(windPos.get(0) / (float)scaleFactor, ((float) SilenceFix.mc.displayHeight - windPos.get(1)) / (float)scaleFactor, windPos.get(2));
        }
        return null;
    }

    public static double[] getInterpolatedPos(Entity entity) {
        float ticks = SilenceFix.mc.timer.renderPartialTicks;
        return new double[]{MathUtil.interpolate(entity.lastTickPosX, entity.posX, ticks) - SilenceFix.mc.getRenderManager().viewerPosX, MathUtil.interpolate(entity.lastTickPosY, entity.posY, ticks) - SilenceFix.mc.getRenderManager().viewerPosY, MathUtil.interpolate(entity.lastTickPosZ, entity.posZ, ticks) - SilenceFix.mc.getRenderManager().viewerPosZ};
    }

    public static AxisAlignedBB getInterpolatedBoundingBox(Entity entity) {
        double[] renderingEntityPos = ESPUtil.getInterpolatedPos(entity);
        double entityRenderWidth = (double)entity.width / 1.5;
        return new AxisAlignedBB(renderingEntityPos[0] - entityRenderWidth, renderingEntityPos[1], renderingEntityPos[2] - entityRenderWidth, renderingEntityPos[0] + entityRenderWidth, renderingEntityPos[1] + (double)entity.height + (entity.isSneaking() ? -0.3 : 0.18), renderingEntityPos[2] + entityRenderWidth).expand(0.15, 0.15, 0.15);
    }

    public static Vector4f getEntityPositionsOn2D(Entity entity) {
        AxisAlignedBB bb = ESPUtil.getInterpolatedBoundingBox(entity);
        float yOffset = 0.0f;
        List<Vector3f> vectors = Arrays.asList(new Vector3f((float)bb.minX, (float)bb.minY, (float)bb.minZ), new Vector3f((float)bb.minX, (float)bb.maxY - yOffset, (float)bb.minZ), new Vector3f((float)bb.maxX, (float)bb.minY, (float)bb.minZ), new Vector3f((float)bb.maxX, (float)bb.maxY - yOffset, (float)bb.minZ), new Vector3f((float)bb.minX, (float)bb.minY, (float)bb.maxZ), new Vector3f((float)bb.minX, (float)bb.maxY - yOffset, (float)bb.maxZ), new Vector3f((float)bb.maxX, (float)bb.minY, (float)bb.maxZ), new Vector3f((float)bb.maxX, (float)bb.maxY - yOffset, (float)bb.maxZ));
        Vector4f entityPos = new Vector4f(Float.MAX_VALUE, Float.MAX_VALUE, -1.0f, -1.0f);
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        for (Vector3f vector3f : vectors) {
            vector3f = ESPUtil.projectOn2D(vector3f.x, vector3f.y, vector3f.z, sr.getScaleFactor());
            if (vector3f == null || !((double)vector3f.z >= 0.0) || !((double)vector3f.z < 1.0)) continue;
            entityPos.x = Math.min(vector3f.x, entityPos.x);
            entityPos.y = Math.min(vector3f.y, entityPos.y);
            entityPos.z = Math.max(vector3f.x, entityPos.z);
            entityPos.w = Math.max(vector3f.y, entityPos.w);
        }
        return entityPos;
    }
}

