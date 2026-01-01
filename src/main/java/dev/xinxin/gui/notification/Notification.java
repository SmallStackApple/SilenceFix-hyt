package dev.xinxin.gui.notification;

import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.SmoothAnimationTimer;
import dev.xinxin.utils.misc.MinecraftInstance;
import dev.xinxin.utils.render.ColorUtil;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.AnimTimeUtil;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import dev.xinxin.utils.render.fontRender.FontManager;

import java.awt.*;

public class Notification implements MinecraftInstance {

    private final NotificationType notificationType;
    private final String title;
    private final String description;
    private final float time;
    private final AnimTimeUtil timerUtil;
    private final Animation animation;
    private final SmoothAnimationTimer smooth = new SmoothAnimationTimer(1.0f, 0.4f);

    public Notification(final NotificationType type, final String title, final String description) {
        this(type, title, description, NotificationManager.getToggleTime());
    }

    public Notification(final NotificationType type, final String title, final String description, final float time) {
        this.title = title;
        this.description = description;
        this.time = (float) (long) (time * 1000.0f);
        this.timerUtil = new AnimTimeUtil();
        this.notificationType = type;
        this.animation = new DecelerateAnimation(300, 1.0);
        this.timerUtil.reset();
    }

    public void tick() {
        boolean showing = this.timerUtil.getTime() < this.time;
        this.smooth.update(showing);
    }

    public void drawLettuce(final float x, final float y, final float width, final float height) {
        final float p = Math.max(0f, Math.min(1f, this.smooth.value));
        final float slide = (1.0f - p) * 12.0f;

        Color base = notificationType.getColor();
        int bgA = (int) (235 * p);
        int textA = (int) (255 * p);

        Color bg = new Color(base.getRed(), base.getGreen(), base.getBlue(), clampA(bgA));
        Color textCol = new Color(255, 255, 255, clampA(textA));
        Color barCol = new Color(base.getRed(), base.getGreen(), base.getBlue(), clampA((int)(190 * p)));

        float X = x + slide;

        RoundedUtils.drawRound(X, y, width, height, height / 2f, bg);

        float progress = 1.0f - Math.min(this.timerUtil.getTime() / this.time, 1.0f);
        float barH = Math.max(2.0f, Math.min(4.0f, height * 0.12f));
        RoundedUtils.drawRound(X + 6.0f, y + height - barH - 4.0f, (width - 12.0f) * progress, barH, barH / 2f, barCol);

        float textX = X + 10.0f;
        float textY = y + FontManager.harmonybold18.getMiddleOfBox(height) + 0.5f;
        FontManager.harmonybold18.drawString(description, textX, textY, textCol.getRGB());
    }

    public void blurLettuce(final float x, final float y, final float width, final float height, final boolean glow) {
        final float p = Math.max(0f, Math.min(1f, this.smooth.value));
        final float slide = (1.0f - p) * 12.0f;

        Color base = notificationType.getColor();
        Color overlay = ColorUtil.applyOpacity(base, 40f * p);
        RoundedUtils.drawRound(x + slide, y, width, height, height / 2f, overlay);
        RoundedUtils.drawRound(x + slide + 6.0f, y + height - 4.0f, width - 12.0f, 2.0f, 1.0f, HUD.color(0));
        RenderUtil.resetColor();
    }

    public NotificationType getNotificationType() {
        return this.notificationType;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public float getTime() {
        return this.time;
    }

    public AnimTimeUtil getTimerUtil() {
        return this.timerUtil;
    }

    public Animation getAnimation() {
        return this.animation;
    }

    public boolean isDead() {
        return this.timerUtil.getTime() >= this.time && this.smooth.isAnimationDone(false);
    }

    private static int clampA(int a) {
        return Math.min(255, Math.max(0, a));
    }
}
