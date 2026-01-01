package dev.xinxin.module.modules.render;

import dev.xinxin.event.EventManager;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.StencilUtil;
import dev.xinxin.utils.render.shader.KawaseBloom;
import dev.xinxin.utils.render.shader.KawaseBlur;
import dev.xinxin.utils.shader.DropShadowUtils;
import dev.xinxin.utils.shader.DualBlurUtils;
import net.minecraft.client.shader.Framebuffer;

public class PostProcessing
extends Module {
    public final BoolValue blur = new BoolValue("Blur", true);
    private final NumberValue iterations = new NumberValue("Blur Iterations", 2.0, 1.0, 8.0, 1.0);
    private final NumberValue offset = new NumberValue("Blur Offset", 3.0, 1.0, 10.0, 1.0);
    private final BoolValue bloom = new BoolValue("Bloom", true);
    private final NumberValue shadowRadius = new NumberValue("Bloom Iterations", 3.0, 1.0, 8.0, 1.0);
    private final NumberValue shadowOffset = new NumberValue("Bloom Offset", 1.0, 1.0, 10.0, 1.0);
    private String currentMode;
    private Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public PostProcessing() {
        super("PostProcessing", Category.Render,"阴影模糊");
    }
    public static PostProcessing INSTANCE = new PostProcessing();


    public void blurScreen() {

        EventShader eventShader;
        if (!this.getState()) {
            return;
        }
        if (this.blur.getValue()) {
            // 1) 写模板：把“需要被模糊”的形状画进模板缓冲，不真正上色
            StencilUtil.write(false);

            eventShader = new EventShader(false, "PostProcessing");
            EventManager.call(eventShader);

            RenderUtil.resetColor();
            this.drawBlur();                         // 这里画出要模糊的几何/区域（只写模板）

            for (Runnable r : ShaderElement.getTasks()) r.run();
            ShaderElement.getTasks().clear();

            // 2) 开启模板测试，让后续全屏模糊只在模板区生效
            StencilUtil.erase(true);

            DualBlurUtils.renderBlur(
                    iterations.getValue().intValue(),
                    offset.getValue().intValue(),
                    true
            );

            // 3) 收尾，恢复 GL 状态
            StencilUtil.dispose();
        }

        if (this.bloom.getValue().booleanValue()) {
            this.stencilFramebuffer = RenderUtil.createFrameBuffer(this.stencilFramebuffer);
            this.stencilFramebuffer.framebufferClear();
            this.stencilFramebuffer.bindFramebuffer(false);
            eventShader = new EventShader(true, "PostProcessing");
            EventManager.call(eventShader);
            RenderUtil.resetColor();
            this.drawBloom();
            for (Runnable runnable : ShaderElement.getBloomTasks()) {
                runnable.run();
            }
            ShaderElement.getBloomTasks().clear();
            RenderUtil.resetColor();
            this.stencilFramebuffer.unbindFramebuffer();
//            KawaseBloom.renderBlur(this.stencilFramebuffer.framebufferTexture, ((Double)this.shadowRadius.getValue()).intValue(), ((Double)this.shadowOffset.getValue()).intValue());
            DropShadowUtils.renderDropShadow(stencilFramebuffer.framebufferTexture,shadowRadius.getValue().intValue(),shadowOffset.getValue().intValue(),true);
        }
    }

    private void drawBloom() {
    }

    private void drawBlur() {
    }
}

