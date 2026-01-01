package dev.xinxin.utils.render.ProgressManager;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractProgressBar {
    protected float yOffset = 0f;
    protected float targetY = 0f;
    protected float alpha = 1f;
    protected boolean removing = false;

    @Getter
    @Setter
    protected int priority = 0;

    public abstract String getId();
    public abstract int getPixelHeight(float scale);
    public abstract void render(float centerX, float scale, float partialTicks);

    public void markForRemoval() {
        removing = true;
    }

    public boolean isRemoving() {
        return removing;
    }

    public boolean isDead() {
        return removing && alpha <= 0f;
    }

    public void setTargetY(float y) {
        this.targetY = y;
    }

    public float getYOffset() {
        return yOffset;
    }

    public void updatePosition() {
        yOffset += (targetY - yOffset) * 0.2f;
        if (removing) {
            alpha -= 0.05f;
        } else {
            alpha += (1.0f - alpha) * 0.1f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));
    }

    public float getAlpha() {
        return alpha;
    }

}
