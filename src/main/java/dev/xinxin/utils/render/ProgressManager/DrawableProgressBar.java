package dev.xinxin.utils.render.ProgressManager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;

public class DrawableProgressBar extends AbstractProgressBar{

    @Getter
    private final String id;
    private final int height;
    private final Drawable renderer;

    private float yOffset = 0f;
    @Setter
    private float targetY = 0f;
    private float alpha = 1f;
    @Getter
    private boolean removing = false;

    public interface Drawable {
        void draw(float x, float y, float alpha, float scale);
    }

    public DrawableProgressBar(String id, int height, Drawable renderer) {
        this.id = id;
        this.height = height;
        this.renderer = renderer;
    }

    public void markForRemoval() {
        removing = true;
    }

    public boolean isDead() {
        return removing && alpha <= 0f;
    }

    @Override
    public void render(float centerX, float sf,float p) {
        yOffset += (targetY - yOffset) * 0.2f;
        if (removing) {
            alpha -= 0.05f;
        } else {
            alpha += (1.0f - alpha) * 0.1f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));
        if (alpha <= 0f) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        renderer.draw(centerX, yOffset, alpha, sf);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public int getPixelHeight(float scale) {
        return Math.round(height * scale);
    }
}
