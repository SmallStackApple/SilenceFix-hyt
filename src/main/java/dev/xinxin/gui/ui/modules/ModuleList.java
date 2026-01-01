package dev.xinxin.gui.ui.modules;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.render.ColorUtil;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.xinxin.module.modules.render.HUD.*;
import static java.lang.Long.compare;

public class ModuleList extends UiModule {
    public List<Module> modules;
    private final Map<Module, Boolean> lastModuleStates = new HashMap<>();
    private final Map<Module, Long> animationStartTimes = new HashMap<>();

    public ModuleList() {
        super("ModuleList", RenderUtil.width(), 0.0, 100.0, 0.0);
        this.modules = new ArrayList<>();
    }

    private String formatModule(Module module) {
        String name;
        if (HUD.langModeValue.is("English")) {
            name = module.getName();
        } else {
            name = module.getCnName();
        }
        name = name.replace(" ", "");
        String suffix = module.getSuffix();
        if (suffix == null || suffix.isEmpty()) {
            return name;
        }
        return name + " " + EnumChatFormatting.WHITE + suffix;
    }


    public List<Module> getModules() {
        if (SilenceFix.instance == null || SilenceFix.instance.moduleManager == null) {
            return new ArrayList<>();
        }
        List<Module> mods = new ArrayList<>(SilenceFix.instance.moduleManager.getModules());
        mods.sort((a, b) -> {
            boolean aIsTop = a.getName().contains("--") || a.getCnName().contains("--");
            boolean bIsTop = b.getName().contains("--") || b.getCnName().contains("--");
            if (aIsTop && !bIsTop) return -1;
            if (!aIsTop && bIsTop) return 1;
            return 0;
        });
        mods.sort((m1, m2) -> compare(
                getStringWidthCached(formatModule(m2)),
                getStringWidthCached(formatModule(m1))
        ));
        return mods;
    }


    private final Map<String, Integer> widthCache = new HashMap<>();
    private double maxWidthLerp = 0.0;
    private double lineSpacing = 4.0;
    private int getStringWidthCached(String s) {
        Integer w = widthCache.get(s);
        if (w != null) return w;
        int width = (int) FontManager.navenRegular16.getStringWidth(s);
        widthCache.put(s, width);
        return width;
    }

    public void invalidateTextMetrics() {
        widthCache.clear();
        maxWidthLerp = 0.0;
    }


    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!arraylist.getValue()) return;
        long now = System.currentTimeMillis();
        List<Module> ordered = getModules();
        ScaledResolution sr = new ScaledResolution(mc);
        double fontH = FontManager.navenRegular16.getHeight();
        double rowH = fontH + lineSpacing;
        int cap = ordered.size();
        List<Module> visMods = new ArrayList<>(cap);
        List<String> visText = new ArrayList<>(cap);
        List<Integer> visWidth = new ArrayList<>(cap);
        List<Float> visOut = new ArrayList<>(cap);
        for (int i = 0; i < ordered.size(); i++) {
            Module m = ordered.get(i);
            if (HUD.importantModules.getValue() && m.getCategory() == Category.Render) {
                continue;
            }
            boolean cur = m.getState();
            Boolean last = lastModuleStates.get(m);
            if (last == null || last != cur) {
                m.getAnimation().reset();
                m.getAnimation().setDirection(cur ? Direction.FORWARDS : Direction.BACKWARDS);
                animationStartTimes.put(m, now);
            }
            lastModuleStates.put(m, cur);
            long start = animationStartTimes.getOrDefault(m, now);
            float t = Math.min(1.0f, (float) (now - start) / 300.0f);
            t = applyEasing(t);
            float out = m.getAnimation().getDirection() == Direction.FORWARDS ? t : 1.0f - t;
            m.getAnimation().setOutput(out);
            if (!cur && m.getAnimation().finished(Direction.BACKWARDS)) {
                continue;
            }
            if (out <= 0f) continue;
            String disp = formatModule(m);
            int w = getStringWidthCached(disp);
            visMods.add(m);
            visText.add(disp);
            visWidth.add(w);
            visOut.add(out);
        }
        double targetMax = 0.0;
        for (int i = 0; i < visWidth.size(); i++) {
            int w = visWidth.get(i);
            if (w > targetMax) targetMax = w;
        }
        double tauMs = 120.0;
        double dt = 16.0;
        maxWidthLerp += (targetMax - maxWidthLerp) * (1.0 - Math.exp(-dt / Math.max(1.0, tauMs)));
        double yOff = 0.0;
        for (int i = 0; i < visMods.size(); i++) {
            String txt = visText.get(i);
            int tw = visWidth.get(i);
            float a = visOut.get(i);
            double xr = sr.getScaledWidth() - (maxWidthLerp + 7.0);
            double x = xr + (maxWidthLerp - tw);
            double y = yOff + 8.0;
            String animName = HUD.animation.getValue().name();
            if ("MoveIn".equals(animName)) {
                x += Math.abs((a - 1.0) * (2.0 + tw));
            } else if ("ScaleIn".equals(animName)) {
                RenderUtil.scaleStart(
                        (float) (x + tw / 2.0f),
                        (float) (y + rowH / 2.0 - fontH / 2.0f),
                        a
                );
            }
            int base = HUD.color(i).getRGB();
            int txtCol = ColorUtil.applyOpacity(base, a);

            // === 改成和灵动岛一致的背景透明度 ===
            RenderUtil.drawRound((float) (x - 2.0), (float) (y - 3.0), (float) (tw + 5.0), (float) fontH + 2.0f, 6.0f, new Color(0, 0, 0, 80));

            double fx = x;
            ShaderElement.addBlurTask(() -> {
                RenderUtil.scaleStart(
                        (float) (fx + tw / 2.0f),
                        (float) (y + rowH / 2.0 - fontH / 2.0f),
                        a
                );
                RenderUtil.drawRound((float) (fx - 2.0), (float) (y - 3.0), (float) (tw + 5.0), (float) fontH + 2.0f, 6.0f, new Color(0, 0, 0, 200));
                RenderUtil.scaleEnd();
            });
            double fx2 = x;
            ShaderElement.addBloomTask(() -> {
                RenderUtil.scaleStart(
                        (float) (fx2 + tw / 2.0f),
                        (float) (y + rowH / 2.0 - fontH / 2.0f),
                        a
                );
                RenderUtil.drawRound((float) (fx2 - 2.0), (float) (y - 3.0), (float) (tw + 4.0), (float) fontH + 1.0f, 6.0f, new Color(0, 0, 0, 200));
                RenderUtil.scaleEnd();
            });
            FontManager.navenRegular16.drawStringWithShadow(
                    txt,
                    (float) x + 0.5f,
                    (float) (y - 2.0 + FontManager.navenRegular16.getMiddleOfBox((float) rowH)),
                    txtCol
            );
            if ("ScaleIn".equals(animName)) {
                RenderUtil.scaleEnd();
            }
            yOff += a * rowH;
        }
    }





    private float applyEasing(float progress) {
        if (progress < 0.5f) {
            return 2 * progress * progress;
        } else {
            progress = 2 * progress - 1;
            return 1 - (1 - progress) * (1 - progress) / 2;
        }
    }

    public enum ANIM {
        MoveIn,
        ScaleIn
    }
}