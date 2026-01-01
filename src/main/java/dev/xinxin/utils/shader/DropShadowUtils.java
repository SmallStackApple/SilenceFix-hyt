package dev.xinxin.utils.shader;

import dev.xinxin.utils.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

public class DropShadowUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // 复用你已有的 ShaderUtils，使用其中的 "dropShadow" 片元着色器
    private static final ShaderUtils shader = new ShaderUtils("dropShadow");

    // 用作 ping-pong 的中间 FBO（水平卷积输出）
    private static Framebuffer pingFbo = new Framebuffer(1, 1, false);

    /**
     * @param sourceTexture  原始遮罩/形状纹理（alpha = 1 为内容，其余 0）。注意：应是“未模糊”的版本。
     * @param radius         高斯半径（建议 10~20）
     * @param offset         采样步长（通常 1~2，表示以 texel 为单位沿 direction 移动）
     * @param fresh          是否确保/重建 FBO（通常传 true；若你自己管理尺寸可传 false）
     */
    public static void renderDropShadow(int sourceTexture, int radius, int offset, boolean fresh) {
        if (!ShaderUtils.isSupportGLSL()) return;

        // 可选：你也可以把 pingFbo 的尺寸对齐为窗口大小，避免反复重建
        if (fresh) {
            pingFbo = RenderUtil.createFrameBuffer(pingFbo);
        }

        // --- 全局混合/Alpha 状态 ---
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        // 关键：Pass 期间放宽/关闭 Alpha Test，避免低透明度被阈值裁掉
        GlStateManager.disableAlpha();
        // 或者使用：GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0f);

        // --- 构建并归一化权重 ---
        final FloatBuffer weights = buildNormalizedGaussian(radius);

        // =============== Pass 1: 水平卷积 => pingFbo ===============
        RenderUtil.setAlphaLimit(0.0F);                // 放宽 alpha 限制
        pingFbo.framebufferClear();                    // 必须清为 (0,0,0,0)
        pingFbo.bindFramebuffer(true);                 // true 以设置正确 viewport

        shader.init();
        setupUniforms(radius, /*dirX*/offset, /*dirY*/0, weights,
                pingFbo.framebufferWidth, pingFbo.framebufferHeight);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sourceTexture);
        ShaderUtils.drawQuads();                       // 全屏四边形
        shader.unload();

        pingFbo.unbindFramebuffer();
        mc.getFramebuffer().bindFramebuffer(true);     // 还原到屏幕并设置 viewport

        // =============== Pass 2: 垂直卷积 => 屏幕 ===============
        shader.init();
        setupUniforms(radius, /*dirX*/0, /*dirY*/offset, weights,
                mc.displayWidth, mc.displayHeight);

        // 每帧都绑定检查纹理到 unit16（用于竖直 pass 的 discard）
        GL13.glActiveTexture(GL13.GL_TEXTURE16);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sourceTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        // 输入纹理 = Pass1 输出
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, pingFbo.framebufferTexture);
        ShaderUtils.drawQuads();
        shader.unload();

        // --- 收尾：恢复 Alpha Test ---
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableAlpha();
        GlStateManager.bindTexture(0);
    }

    /**
     * 统一设置 shader uniform。注意按目标纹理尺寸传递 texelSize。
     */
    private static void setupUniforms(int radius, int dirX, int dirY,
                                      FloatBuffer weights, float targetW, float targetH) {
        shader.setUniformi("inTexture", 0);
        shader.setUniformi("textureToCheck", 16);
        shader.setUniformf("radius", radius);
        shader.setUniformf("texelSize", 1.0F / targetW, 1.0F / targetH);
        shader.setUniformf("direction", dirX, dirY);
        OpenGlHelper.glUniform1(shader.getUniform("weights"), weights);
    }

    /**
     * 构建对称高斯核并归一化（sum = 1）。
     * 这里给出一个简单的权重函数实现；如你已有 MathUtils，可替换。
     */
    private static FloatBuffer buildNormalizedGaussian(int radius) {
        // 生成半径为 radius 的 1D 高斯核（sigma 取 radius 的 1/2，按需调整）
        final FloatBuffer buf = BufferUtils.createFloatBuffer(256);
        final float sigma = Math.max(1f, radius * 0.5f);
        final float twoSigma2 = 2f * sigma * sigma;

        float sum = 0f;
        for (int i = 0; i <= radius; i++) {
            float w = (float) Math.exp(-(i * i) / twoSigma2);
            buf.put(w);
            sum += (i == 0 ? w : 2f * w); // 对称核：中心一次，其他左右各一次
        }
        buf.rewind();
        for (int i = 0; i <= radius; i++) {
            buf.put(i, buf.get(i) / sum);
        }
        buf.rewind();
        return buf;
    }
}
