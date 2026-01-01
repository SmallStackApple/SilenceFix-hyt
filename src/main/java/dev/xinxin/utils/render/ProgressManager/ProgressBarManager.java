package dev.xinxin.utils.render.ProgressManager;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class ProgressBarManager {

    private static final List<AbstractProgressBar> allBars = new ArrayList<>();

    public static void create(String id, Supplier<Float> progressSupplier, float max, boolean 百分比) {
        create(id, progressSupplier, max, 百分比, "", 0);
    }

    public static void create(String id, Supplier<Float> progressSupplier, float max, boolean 百分比, String extra) {
        create(id, progressSupplier, max, 百分比, extra, 0);
    }

    public static void create(String id, Supplier<Float> progressSupplier, float max, boolean 百分比, String extra, int priority) {
        if (getBar(id) == null) {
            ProgressBar bar = new ProgressBar(id, progressSupplier, max, 百分比, extra);
            bar.setPriority(priority);
            allBars.add(bar);
        }
    }

    public static void createCustom(String id, int height, DrawableProgressBar.Drawable renderer) {
        createCustom(id, height, renderer, 0);
    }

    public static void createCustom(String id, int height, DrawableProgressBar.Drawable renderer, int priority) {
        if (getBar(id) == null) {
            int actualHeight = Math.max(height, 1); // 防止传 0
            DrawableProgressBar bar = new DrawableProgressBar(id, actualHeight, renderer);
            bar.setPriority(priority);
            allBars.add(bar);
        }
    }


    public static void remove(String id) {
        AbstractProgressBar bar = getBar(id);
        if (bar != null) {
            bar.markForRemoval();
        }
    }

    private static AbstractProgressBar getBar(String id) {
        return allBars.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    @EventTarget
    public static void onRender2D(EventRender2D event) {
        final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        final float sf = Math.max(Math.min(sr.getScaledWidth() / 1920f, sr.getScaledHeight() / 1080f), 0.5f);
        final int centerX = sr.getScaledWidth() / 2;
        final int baseY = (int) (sr.getScaledHeight() / 1.3);

        final int spacing = Math.round(26 * sf);

        allBars.sort(Comparator.comparingInt(AbstractProgressBar::getPriority).reversed());


        float totalYOffset = 0f;
        for (AbstractProgressBar bar : allBars) {
            if (bar.isDead()) continue;


            if (!bar.isRemoving()) {
                bar.setTargetY(baseY - totalYOffset);
                int barHeight = bar.getPixelHeight(sf);
                totalYOffset += barHeight + spacing;
            }
        }


        for (AbstractProgressBar bar : allBars) {
            if (bar.isDead()) continue;

            bar.updatePosition();
            bar.render(centerX, sf, event.getPartialTicks());
        }


        allBars.removeIf(AbstractProgressBar::isDead);
    }
}
