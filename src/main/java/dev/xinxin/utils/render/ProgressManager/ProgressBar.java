package dev.xinxin.utils.render.ProgressManager;

import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.render.ColorUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import java.awt.Color;
import org.lwjgl.util.Timer;

import java.util.function.Supplier;

class ProgressBar extends AbstractProgressBar {
    @Getter
    private final String id;
    private final Supplier<Float> progressSupplier;

    private float displayPct = 0f;
    private float yOffset = 0f;
    @Setter
    private float targetY = 0f;
    private float maxNum;
    private boolean 百分比布尔值;
    private String 额外文字;
    @Getter
    private boolean removing = false;
    private float alpha = 1f;

    private final Timer fadeTimer = new Timer();

    public ProgressBar(String id , Supplier<Float> supplier , float max, boolean 百分比,String 额外) {
        this.id = id;
        maxNum = max;
        百分比布尔值 = 百分比;
        额外文字 = 额外;
        this.progressSupplier = supplier;
    }

    public void markForRemoval() {
        if (!removing) {
            removing = true;
            fadeTimer.reset();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getPixelHeight(float scale) {
        return Math.round(26 * scale);
    }




    public boolean isDead() {
        return removing && alpha <= 0f;
    }
    @Override
    public void render(float centerX, float sf, float partialTicks) {
        // 动画插值
        final float rawPct = Math.max(0f, Math.min(maxNum, progressSupplier.get()));
        displayPct += (rawPct - displayPct) * 0.15f;

        // Y 动画
        yOffset += (targetY - yOffset) * 0.2f;

        if (removing) {
            alpha -= 0.05f;
        } else {
            alpha += (1.0f - alpha) * 0.1f;
        }

        alpha = Math.max(0f, Math.min(1f, alpha));

        if (alpha <= 0f) return;

        // === 以下为复制并略作适配的渲染逻辑 ===
        final int barWidth  = Math.round(180 * sf);
        final int barHeight = Math.round(10 * sf);
        final float radius  = 5.0f * sf;

        final int barX = (int) (centerX - (float) barWidth / 2);
        final int barY = Math.round(yOffset);

        final String title = id;
        final String pctText = 百分比布尔值 ? Math.round(displayPct) + "%" + 额外文字 : progressSupplier.get() + " / " + maxNum;

        final float tickW = FontManager.harmonybold18.getStringWidth(title);
        final float pctW  = FontManager.harmonybold18.getStringWidth(pctText);
        final int titleY = barY - Math.round(14 * sf);
        final int pctTextY = barY + barHeight + Math.round(3 * sf);

        final int textShadow = new Color(0, 0, 0, (int)(120 * alpha)).getRGB();
        final int textMain = new Color(230, 230, 230, (int)(230 * alpha)).getRGB();

        final int fillWidth = Math.max(0, Math.min(barWidth, Math.round(barWidth * (displayPct / maxNum))));

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();

        FontManager.harmonybold18.drawString(title, centerX - tickW / 2f + 0.5f, titleY - 3f + 0.5f, textShadow);
        FontManager.harmonybold18.drawString(title, centerX - tickW / 2f, titleY - 3f, textMain);

        // 背板
        RoundedUtils.drawGradientRound(barX - 1, barY - 1, barWidth + 2, barHeight + 2, radius + 1,
                new Color(0, 0, 0, (int)(40 * alpha)), new Color(0, 0, 0, (int)(60 * alpha)),
                new Color(0, 0, 0, (int)(40 * alpha)), new Color(0, 0, 0, (int)(60 * alpha)));
        RoundedUtils.drawGradientRound(barX, barY, barWidth, barHeight, radius,
                new Color(18, 18, 22, (int)(190 * alpha)), new Color(24, 24, 28, (int)(210 * alpha)),
                new Color(18, 18, 22, (int)(190 * alpha)), new Color(24, 24, 28, (int)(210 * alpha)));

        // 填充
        if (fillWidth > 0) {
            RoundedUtils.drawGradientRoundLR(barX, barY, fillWidth, barHeight, radius,
                    ColorUtil.applyOpacity(HUD.color(1),255 * alpha),ColorUtil.applyOpacity(HUD.color(5),255 * alpha));

            if (fillWidth > Math.max(3, Math.round(3 * sf))) {
                RoundedUtils.drawGradientRound(barX, barY, fillWidth, Math.max(1, barHeight / 3), radius,
                        new Color(255, 255, 255, (int)(36 * alpha)), new Color(255, 255, 255, (int)(72 * alpha)),
                        new Color(255, 255, 255, (int)(36 * alpha)), new Color(255, 255, 255, (int)(72 * alpha)));
            }
        }

        FontManager.chineseFont16.drawString(pctText, centerX - pctW / 2f + 0.5f, pctTextY + 1 + 0.5f, textShadow);
        FontManager.chineseFont16.drawString(pctText, centerX - pctW / 2f, pctTextY + 1,
                textMain);

        final float outlineThickness = 0.6f * sf;
        final Color outlineBase = new Color(150, 150, 150, (int)(40 * alpha));
        final Color outlineGlow = new Color(170, 240, 255, (int)(85 * alpha));
        final Color finalOutline = (rawPct >= 99f) ? outlineGlow : outlineBase;

//        RoundedUtils.drawRoundOutline(
//                barX + 0.7f, barY + 0.7f,
//                barWidth - 1.4f, barHeight - 1.4f,
//                Math.max(0f, radius - 0.7f),
//                outlineThickness,
//                new Color(0, 0, 0, 0),
//                finalOutline
//        );

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
