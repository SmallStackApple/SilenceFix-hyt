package dev.xinxin.gui.clickgui.MilkpowderClickgui;

import dev.xinxin.SilenceFix;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.module.values.*;
import dev.xinxin.utils.SmoothAnimationTimer;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.impl.ContinualAnimation;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class NewClickGui extends GuiScreen {
    public static NewClickGui INSTANCE = new NewClickGui();

    private enum GuiState {
        CATEGORY,
        MODULE,
        VALUE
    }

    private GuiState currentState = GuiState.CATEGORY;
    private Category selectedCategory;
    private Module selectedModule;

    private float x = 100, y = 100, width = 350, height = 250;
    private float categoryWidth = 120;
    private final float categoryHeight = 200;
    private boolean dragging, resizing;
    private float dragX, dragY;

    private boolean listeningKey = false;
    private Module keyBindingTarget = null;
    private long keyBindFeedbackTime = 0;
    private String keyBindFeedbackText = null;


    public ContinualAnimation animatedX = new ContinualAnimation();
    public ContinualAnimation animatedY = new ContinualAnimation();
    public ContinualAnimation animatedW = new ContinualAnimation();
    public ContinualAnimation animatedH = new ContinualAnimation();

    public ContinualAnimation animatedX1 = new ContinualAnimation();
    public ContinualAnimation animatedY1 = new ContinualAnimation();

    private final Map<Module, float[]> moduleColorMap = new HashMap<>();
    private final Map<Category, Float> categoryExMap = new HashMap<>();

    private float moduleBackEx = 0f;
    private float valueBackEx = 0f;


    private float moduleScrollAnimation, valueScrollAnimation;

    private NumberValue draggingNumberValue;
    private ColorValue activeColorValue;


    public NewClickGui() {
        INSTANCE = this;
        if (lastState == GuiState.VALUE && lastCategory != null && lastModule != null) {
            this.currentState = GuiState.VALUE;
            this.selectedCategory = lastCategory;
            this.selectedModule = lastModule;
        } else if (lastState == GuiState.MODULE && lastCategory != null) {
            this.currentState = GuiState.MODULE;
            this.selectedCategory = lastCategory;
        } else {
            this.currentState = GuiState.CATEGORY;
        }
        this.x = lastX;
        this.y = lastY;
        this.width = lastW;
        this.height = lastH;
    }
    SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0);
    SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (currentState == GuiState.CATEGORY) {
            float contentH = Category.values().length * 25f;
            widthTimer.target = 100f;
            heightTimer.target = Math.max(140f, 40f + contentH);
        } else {
            width = Math.max(width, 600f);
            height = Math.max(height, 380f);
            widthTimer.target  = width;
            heightTimer.target = height;
        }
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }
        if (resizing && currentState != GuiState.CATEGORY) {
            width = Math.max(600, mouseX - x);
            height = Math.max(380, mouseY - y);
            widthTimer.target  = width;
            heightTimer.target = height;
        }
        widthTimer.speed = 0.5f;
        heightTimer.speed = 0.5f;
        widthTimer.update(true);
        heightTimer.update(true);
        runToXyback(x, y);
        RoundedUtils.drawRound(animatedX1.getOutput(), animatedY1.getOutput(), widthTimer.value, heightTimer.value, 8, new Color(0, 0, 0, 80));
        ShaderElement.addBlurTask(() -> RoundedUtils.drawRound(animatedX1.getOutput(), animatedY1.getOutput(), widthTimer.value, heightTimer.value, 8, new Color(0, 0, 0, 200)));
        ShaderElement.addBloomTask(() -> RoundedUtils.drawRound(animatedX1.getOutput(), animatedY1.getOutput(), widthTimer.value, heightTimer.value, 8, new Color(0, 0, 0, 200)));
        if (currentState == GuiState.CATEGORY) {
            drawCategoryScreen(mouseX, mouseY);
        } else if (currentState == GuiState.MODULE) {
            drawModuleScreen(mouseX, mouseY);
        } else {
            drawModuleScreen(mouseX, mouseY);
            drawValueScreen(mouseX, mouseY);
        }
        if (currentState != GuiState.CATEGORY) {
            RenderUtil.drawTriangle(x + width - 10, y + height, x + width, y + height - 10, x + width, y + height, Color.WHITE.getRGB());
        }
        if (listeningSearch) {
            drawSearchOverlay();
        }
        drawBindingOverlay();
    }
    private void drawSearchOverlay() {
        if (!listeningSearch) return;
        float bx = x;
        float by = y;
        float bw = (currentState == GuiState.CATEGORY ? categoryWidth : width);
        float bh = (currentState == GuiState.CATEGORY ? categoryHeight : height);
        RoundedUtils.drawRound(bx, by, bw, bh, 8, new Color(0, 0, 0, 120));
        String l1 = "搜索模块: " + (searchText.isEmpty() ? "_" : searchText);
        float w1 = FontManager.navenRegular20.getStringWidth(l1);
        float cx = bx + bw / 2f;
        float cy = by + bh / 2f;

        if (selectAllSearch && !searchText.isEmpty()) {
            float textW = FontManager.navenRegular20.getStringWidth("搜索模块: " + searchText);
            float textH = FontManager.navenRegular20.getHeight() + 4;
            float tx = cx - w1 / 2f;
            float ty = cy - 10;
            RoundedUtils.drawRound(tx - 2, ty - 2, textW + 4, textH, 4, new Color(54, 98, 236, 180));
            FontManager.navenRegular20.drawString("搜索模块: " + searchText, tx, ty, Color.WHITE.getRGB());
        } else {
            FontManager.navenRegular20.drawString(l1, cx - w1 / 2f, cy - 10, Color.WHITE.getRGB());
        }

        String tip = "Ctrl+A 全选 Tab补全功能 回车确定搜索";
        float w2 = FontManager.navenRegular16.getStringWidth(tip);
        FontManager.navenRegular16.drawString(tip, cx - w2 / 2f, cy + 15, new Color(200, 200, 200, 200).getRGB());
    }



    public void runToXyback(float realX, float realY) {
        animatedX1.animate(realX, 2);
        animatedY1.animate(realY, 2);
    }

    public void runToXy(float realX, float realY) {
        animatedX.animate(realX, 20);
        animatedY.animate(realY, 20);
    }

    public void runToWH(float realX, float realY) {
        animatedW.animate(realX, 30);
        animatedH.animate(realY, 30);
    }

    private void drawCategoryScreen(int mouseX, int mouseY) {
        categoryWidth = 100f;
        RapeMasterFontManager titleFont = FontManager.navenbold30;
        String title = SilenceFix.NAME;
        float tw = titleFont.getStringWidth(title);
        titleFont.drawString(title, x + (categoryWidth - tw) / 2f, y + 12f   , Color.WHITE.getRGB());
        float itemGap = 25f;
        float itemH = 20f;
        float startY = y + 40f;
        int idx = 0;
        for (Category category : Category.values()) {
            categoryExMap.putIfAbsent(category, 0f);
            float rowY = startY + idx * itemGap;
            boolean hovered = RenderUtil.isHovering(x, rowY, categoryWidth, itemH, mouseX, mouseY);
            float targetEx = hovered ? 5f : 0f;
            float animatedEx = animate(categoryExMap.get(category), targetEx, 0.18f);
            categoryExMap.put(category, animatedEx);
            FontManager.navenRegular20.drawString(category.name(), x + 12f + animatedEx, rowY + 6f, Color.WHITE.getRGB());
            idx++;
        }
    }



    private boolean shouldWrapRow(float currentRowWidth, float itemWidth, float totalAvailableWidth) {
        return currentRowWidth + itemWidth + 10 > totalAvailableWidth + 1e-2f;
    }

    private static final float SCROLLBAR_W = 4f;
    private static final float SCROLLBAR_PAD = 3f;

    private SmoothAnimation moduleMotionY = new SmoothAnimation(0f);
    private SmoothAnimation valueMotionY  = new SmoothAnimation(0f);
    private static final float SCROLL_STEP = 15f;

    private void handleScrollingSmooth(int mouseX, int mouseY, float contentH, float viewH, boolean isModuleArea) {
        float currentWidth = (currentState == GuiState.CATEGORY) ? categoryWidth : width;
        float currentHeight = (currentState == GuiState.CATEGORY) ? categoryHeight : height;
        if (RenderUtil.isHovering(x, y, currentWidth, currentHeight, mouseX, mouseY)) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0) {
                float units = Math.abs(wheel) / 120f;
                float dir = wheel > 0 ? 1f : -1f;
                float boost = 2.5f;
                float step = dir * SCROLL_STEP * Math.max(1f, units) * boost;
                if (isModuleArea) {
                    float t = moduleMotionY.getValue() + step;
                    float min = -(contentH - viewH);
                    if (contentH <= viewH) t = 0f;
                    else if (t > 0f) t = 0f;
                    else if (t < min) t = min;
                    moduleMotionY.setSpeed(0.45f);
                    moduleMotionY.setTarget(t);
                    moduleScrollTouch = System.currentTimeMillis();
                } else {
                    float t = valueMotionY.getValue() + step;
                    float min = -(contentH - viewH);
                    if (contentH <= viewH) t = 0f;
                    else if (t > 0f) t = 0f;
                    else if (t < min) t = min;
                    valueMotionY.setSpeed(0.45f);
                    valueMotionY.setTarget(t);
                    valueScrollTouch = System.currentTimeMillis();
                }
            }
        }
    }


    private void drawModuleScreen(int mouseX, int mouseY) {
        if (currentState != GuiState.MODULE) return;
        String backText = "< " + selectedCategory.name();
        boolean hoveredBack = RenderUtil.isHovering(x, y, FontManager.navenRegular20.getStringWidth(backText) + 12, 30, mouseX, mouseY);
        moduleBackEx = animate(moduleBackEx, hoveredBack ? 6f : 0f, 0.2f);
        FontManager.navenRegular20.drawString(backText, x + 10 + moduleBackEx, y + 6, Color.WHITE.getRGB());

        float listX = x + 5f;
        float listY = y + 20f;
        float listW = 100f - (SCROLLBAR_W + SCROLLBAR_PAD);
        float cardH = 25f;
        float gap = 5f;

        List<Module> modules = dev.xinxin.SilenceFix.instance.moduleManager.getModsByCategory(selectedCategory);
        float contentH = Math.max(0f, modules.size() * (cardH + gap) - gap);
        float viewH = height - 25f;

        handleScrollingSmooth(mouseX, mouseY, contentH, viewH, true);
        float min = -(contentH - viewH);
        if (contentH <= viewH) {
            moduleMotionY.setTarget(0f);
        } else {
            if (moduleMotionY.getValue() < min) moduleMotionY.setTarget(min);
            if (moduleMotionY.getValue() > 0f) moduleMotionY.setTarget(0f);
        }
        moduleMotionY.update();
        moduleScrollAnimation = moduleMotionY.getValue();

        boolean show = System.currentTimeMillis() - moduleScrollTouch < 1000L;
        moduleScrollbarAlpha += ((show ? 160f : 0f) - moduleScrollbarAlpha) * 0.2f;

        RenderUtil.scissorStart(listX, y + 20f, listW, height - 25f);
        float renderY = listY + moduleScrollAnimation;

        for (Module module : modules) {
            if (!moduleColorMap.containsKey(module)) {
                moduleColorMap.put(module, new float[]{0f, 0f, 0f, 200f, 5f});
            }
            float[] anim = moduleColorMap.get(module);
            if (anim.length < 5) {
                anim = new float[]{anim[0], anim[1], anim[2], anim[3], 5f};
                moduleColorMap.put(module, anim);
            }

            boolean hovered = RenderUtil.isHovering(listX, renderY, listW, cardH, mouseX, mouseY) && !listeningKey;

            Color base = module.getState() ? new Color(54, 98, 236, 225) : new Color(25, 25, 25, 200);

            anim[0] = animate(anim[0], base.getRed(),   hovered ? 0.25f : 0.18f);
            anim[1] = animate(anim[1], base.getGreen(), hovered ? 0.25f : 0.18f);
            anim[2] = animate(anim[2], base.getBlue(),  hovered ? 0.25f : 0.18f);
            anim[3] = animate(anim[3], base.getAlpha(), hovered ? 0.25f : 0.18f);

            float targetHover = hovered ? 150f : 5f;
            anim[4] = animate(anim[4], targetHover, hovered ? 0.25f : 0.18f);
            int overlayA = Math.max(0, Math.min(255, (int)(anim[4] / 3f)));

            Color cardColor = new Color((int)anim[0], (int)anim[1], (int)anim[2], (int)anim[3]);

            RoundedUtils.drawRound(listX, renderY, listW, cardH, 5f, new Color(0, 0, 0, cardColor.getAlpha()));
            RoundedUtils.drawRound(listX, renderY, listW, cardH, 5f, cardColor);
            RoundedUtils.drawRound(listX, renderY, listW, cardH, 5f, new Color(255, 255, 255, overlayA));

            String name = displayName(module);
            FontManager.navenRegular20.drawString(name, listX + 8f, renderY + 6f, Color.WHITE.getRGB());

            renderY += cardH + gap;
        }
        RenderUtil.scissorEnd();

        drawScrollbar(listX, y + 20f, listW + SCROLLBAR_W + SCROLLBAR_PAD, height - 25f, contentH, moduleScrollAnimation, moduleScrollbarAlpha);
        drawKeyBindTip();
    }




    public class SmoothAnimation {
        private float value;
        private float target;
        private float speed;
        private long lastTime;

        public SmoothAnimation(float start) {
            this.value = start;
            this.target = start;
            this.speed = 0.25f;
            this.lastTime = System.currentTimeMillis();
        }

        public void setTarget(float target) {
            this.target = target;
        }

        public void setSpeed(float s) {
            this.speed = s;
        }

        public void update() {
            long now = System.currentTimeMillis();
            float dt = Math.max(1f, (now - lastTime)) / 16f;
            lastTime = now;
            float k = 1f - (float)Math.pow(1f - speed, dt);
            value += (target - value) * k;
        }

        public float getValue() {
            return value;
        }

        public void snapTo(float v) {
            this.value = v;
            this.target = v;
            this.lastTime = System.currentTimeMillis();
        }
    }





    private float animate(float current, float target, float speed) {
        return current + (target - current) * Math.min(speed, 1.0f);
    }

    private void drawValueScreen(int mouseX, int mouseY) {
        String backText = displayBackTextForModule(selectedModule);
        boolean hoveredBack = RenderUtil.isHovering(x, y, FontManager.navenRegular20.getStringWidth(backText) + 12, 30, mouseX, mouseY);
        valueBackEx = animate(valueBackEx, hoveredBack ? 6f : 0f, 0.2f);
        FontManager.navenRegular20.drawString(backText, x + 10 + valueBackEx, y + 6, Color.WHITE.getRGB());

        float listX = x + 5f;
        float listY = y + 20f;
        float listW = 120f - (SCROLLBAR_W + SCROLLBAR_PAD);
        float cardH = 25f;
        float gap = 5f;

        java.util.List<Module> modules = dev.xinxin.SilenceFix.instance.moduleManager.getModsByCategory(selectedCategory);
        float contentHLeft = Math.max(0f, modules.size() * (cardH + gap) - gap);
        float viewHLeft = height - 25f;

        if (RenderUtil.isHovering(listX, y + 20f, listW, height - 25f, mouseX, mouseY)) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0) {
                float units = Math.abs(wheel) / 120f;
                float dir = wheel > 0 ? 1f : -1f;
                float boost = 2.5f;
                float step = dir * SCROLL_STEP * Math.max(1f, units) * boost;
                float t = moduleMotionY.getValue() + step;
                float min = -(contentHLeft - viewHLeft);
                if (contentHLeft <= viewHLeft) t = 0f;
                else if (t > 0f) t = 0f;
                else if (t < min) t = min;
                moduleMotionY.setSpeed(0.45f);
                moduleMotionY.setTarget(t);
                moduleScrollTouch = System.currentTimeMillis();
            }
        }
        float minLeft = -(contentHLeft - viewHLeft);
        if (contentHLeft <= viewHLeft) {
            moduleMotionY.setTarget(0f);
        } else {
            if (moduleMotionY.getValue() < minLeft) moduleMotionY.setTarget(minLeft);
            if (moduleMotionY.getValue() > 0f) moduleMotionY.setTarget(0f);
        }
        moduleMotionY.update();
        moduleScrollAnimation = moduleMotionY.getValue();

        boolean showLeft = System.currentTimeMillis() - moduleScrollTouch < 1000L;
        moduleScrollbarAlpha += ((showLeft ? 160f : 0f) - moduleScrollbarAlpha) * 0.2f;

        RenderUtil.scissorStart(listX, y + 20f, listW, height - 25f);
        float renderY = listY + moduleScrollAnimation;

        for (Module module : modules) {
            if (!moduleColorMap.containsKey(module)) {
                moduleColorMap.put(module, new float[]{0f, 0f, 0f, 200f, 5f});
            }
            float[] anim = moduleColorMap.get(module);
            if (anim.length < 5) {
                anim = new float[]{anim[0], anim[1], anim[2], anim[3], 5f};
                moduleColorMap.put(module, anim);
            }

            boolean hovered = RenderUtil.isHovering(listX, renderY, listW, cardH, mouseX, mouseY) && !listeningKey;

            java.awt.Color base = module.getState() ? new java.awt.Color(54, 98, 236, 225) : new java.awt.Color(25, 25, 25, 200);

            anim[0] = animate(anim[0], base.getRed(),   hovered ? 0.25f : 0.18f);
            anim[1] = animate(anim[1], base.getGreen(), hovered ? 0.25f : 0.18f);
            anim[2] = animate(anim[2], base.getBlue(),  hovered ? 0.25f : 0.18f);
            anim[3] = animate(anim[3], base.getAlpha(), hovered ? 0.25f : 0.18f);

            float targetHover = hovered ? 150f : 5f;
            anim[4] = animate(anim[4], targetHover, hovered ? 0.25f : 0.18f);
            int overlayA = Math.max(0, Math.min(255, (int)(anim[4] / 3f)));

            java.awt.Color cardColor = new java.awt.Color((int)anim[0], (int)anim[1], (int)anim[2], (int)anim[3]);

            RoundedUtils.drawRound(listX, renderY, listW, cardH, 5f, new java.awt.Color(0, 0, 0, cardColor.getAlpha()));
            RoundedUtils.drawRound(listX, renderY, listW, cardH, 5f, cardColor);
            RoundedUtils.drawRound(listX, renderY, listW, cardH, 5f, new java.awt.Color(255, 255, 255, overlayA));

            String name = displayName(module);
            FontManager.navenRegular20.drawString(name, listX + 8f, renderY + 6f, java.awt.Color.WHITE.getRGB());

            renderY += cardH + gap;
        }
        RenderUtil.scissorEnd();
        drawScrollbar(listX, y + 20f, listW + SCROLLBAR_W + SCROLLBAR_PAD, height - 25f, contentHLeft, moduleScrollAnimation, moduleScrollbarAlpha);

        float contentLeft = x + 140f;
        float contentWidth = width - 155f;

        java.util.List<Value<?>> values = selectedModule.getValues();

        float oldX = this.x, oldW = this.width;
        this.x = contentLeft;
        this.width = contentWidth;
        float totalHeight = calculateTotalValueHeight(values);
        this.x = oldX;
        this.width = oldW;

        float viewH = height - 30f;

        this.x = contentLeft;
        this.width = contentWidth;
        if (RenderUtil.isHovering(contentLeft, y + 25f, contentWidth, height - 30f, mouseX, mouseY)) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0) {
                float units = Math.abs(wheel) / 120f;
                float dir = wheel > 0 ? 1f : -1f;
                float boost = 2.5f;
                float step = dir * SCROLL_STEP * Math.max(1f, units) * boost;
                float t = valueMotionY.getValue() + step;
                float min = -(totalHeight - viewH);
                if (totalHeight <= viewH) t = 0f;
                else if (t > 0f) t = 0f;
                else if (t < min) t = min;
                valueMotionY.setSpeed(0.45f);
                valueMotionY.setTarget(t);
                valueScrollTouch = System.currentTimeMillis();
            }
        }
        this.x = oldX;
        this.width = oldW;

        float minScroll = -(totalHeight - viewH);
        if (totalHeight <= viewH) {
            valueMotionY.setTarget(0f);
        } else {
            if (valueMotionY.getValue() < minScroll) valueMotionY.setTarget(minScroll);
            if (valueMotionY.getValue() > 0f) valueMotionY.setTarget(0f);
        }
        valueMotionY.update();
        valueScrollAnimation = valueMotionY.getValue();

        boolean show = System.currentTimeMillis() - valueScrollTouch < 1000L;
        valueScrollbarAlpha += ((show ? 160f : 0f) - valueScrollbarAlpha) * 0.2f;

        RenderUtil.scissorStart(contentLeft, y + 25f, contentWidth, height - 30f);

        oldX = this.x; oldW = this.width;
        this.x = contentLeft; this.width = contentWidth;

        float valueY = y + 30f + valueScrollAnimation;

        for (int i = 0; i < values.size();) {
            Value<?> value = values.get(i);
            if (value.isHidden()) { i++; continue; }

            if (value instanceof BoolValue) {
                java.util.List<BoolValue> boolGroup = new java.util.ArrayList<>();
                while (i < values.size() && values.get(i) instanceof BoolValue) { boolGroup.add((BoolValue) values.get(i)); i++; }
                valueY = drawBoolValueGrid(boolGroup, mouseX, mouseY, valueY);
                continue;
            }

            if (value instanceof ModeValue) {
                valueY = drawModeValue((ModeValue<?>) value, mouseX, mouseY, valueY);
                i++; continue;
            }

            if (value instanceof NumberValue) {
                drawNumberValue((NumberValue) value, mouseX, mouseY, valueY);
                valueY += 30f; i++; continue;
            }

            if (value instanceof TextValue) {
                drawTextValue((TextValue) value, valueY);
                valueY += 25f; i++; continue;
            }

            if (value instanceof ColorValue) {
                drawColorValue((ColorValue) value, mouseX, mouseY, valueY);
                valueY += 25f; i++; continue;
            }

            valueY += 25f; i++;
        }

        this.x = oldX; this.width = oldW;

        RenderUtil.scissorEnd();
        drawScrollbar(contentLeft, y + 25f, contentWidth, height - 30f, totalHeight, valueScrollAnimation, valueScrollbarAlpha);

        if (activeColorValue != null) drawColorPicker(activeColorValue, mouseX, mouseY);
    }




    private void drawBindingOverlay() {
        if (!(listeningKey && keyBindingTarget != null)) {
            titleAlpha += (0f - titleAlpha) * 0.25f;
            return;
        }
        float bx = x;
        float by = y;
        float bw = (currentState == GuiState.CATEGORY ? categoryWidth : width);
        float bh = (currentState == GuiState.CATEGORY ? categoryHeight : height);
        titleAlpha += (250f - titleAlpha) * 0.25f;
        int a = Math.max(0, Math.min(250, (int) titleAlpha));
        RoundedUtils.drawRound(bx, by, bw, bh, 8, new Color(0, 0, 0, a / 2));
        String l1 = "Press a key to bind " + keyBindingTarget.getName();
        String l2 = "(ESC cancel / Delete remove)";
        float w1 = FontManager.navenRegular20.getStringWidth(l1);
        float w2 = FontManager.navenRegular20.getStringWidth(l2);
        float cx = bx + bw / 2f;
        float cy = by + bh / 2f;
        FontManager.navenRegular20.drawString(l1, cx - w1 / 2f, cy - 10, Color.WHITE.getRGB());
        FontManager.navenRegular20.drawString(l2, cx - w2 / 2f, cy + 10, Color.WHITE.getRGB());
    }



    private Module moduleAt(int mouseX, int mouseY) {
        if (currentState != GuiState.MODULE) return null;
        List<Module> modules = dev.xinxin.SilenceFix.instance.moduleManager.getModsByCategory(selectedCategory);
        float moduleX = x + 20f;
        float moduleY = y + 30f + moduleScrollAnimation;
        float rowWidth = 0f;
        for (Module module : modules) {
            String name = HUD.langModeValue.is("English") ? module.getName() : module.getCnName();
            float w = 10 + FontManager.navenRegular16.getStringWidth(name + (module.category != Category.Config ? " >     " : ""));
            float h = 12 + FontManager.navenRegular16.getHeight();
            if (shouldWrapRow(rowWidth, w, width - 40)) {
                rowWidth = 0f;
                moduleX = x + 20f;
                moduleY += h + 10f;
            }
            if (RenderUtil.isHovering(moduleX, moduleY, w, h, mouseX, mouseY)) return module;
            moduleX += w + 13f;
            rowWidth += w + 10f;
        }
        return null;
    }



    private long categoryScrollTouch = 0L, moduleScrollTouch = 0L, valueScrollTouch = 0L;
    private float categoryScrollbarAlpha = 0f, moduleScrollbarAlpha = 0f, valueScrollbarAlpha = 0f;
    private float titleAlpha = 0f, titleHoverOffset = 0f;


    private float drawBoolValueGrid(List<BoolValue> boolGroup, int mouseX, int mouseY, float startY) {
        float boolX = x + 15f;
        float boolY = startY;
        float rowWidth = 0;
        float rowHeight = 28f; // 行高加大一点，让上下更舒服

        for (BoolValue boolValue : boolGroup) {
            if (boolValue.isHidden()) {
                continue;
            }
            float boolWidth = 25 + FontManager.navenRegular18.getStringWidth(boolValue.getName());

            if (rowWidth + boolWidth > width - 40f) {
                rowWidth = 0;
                boolX = x + 15f;
                boolY += rowHeight;
            }

            float circleX = boolX + 6;
            float circleY = boolY + 14; // 圆点位置下移，保证居中

            RenderUtil.drawRound(circleX - 4, circleY - 4, 12, 12, 3f, new Color(0, 0, 0, 200));
            RenderUtil.drawCircleCGUI(circleX + 2, circleY + 2, 9,
                    boolValue.getValue() ? new Color(54, 98, 236, 225).getRGB() : new Color(0, 0, 0, 200).getRGB());

            FontManager.navenRegular20.drawString(boolValue.getName(), circleX + 16, circleY - 6, Color.WHITE.getRGB());

            boolX += boolWidth + 14;
            rowWidth += boolWidth + 14;
        }
        return boolY + rowHeight;
    }


    private boolean listeningSearch = false;
    private String searchText = "";

    private void drawBoolValue(BoolValue boolValue, float x, float y) {
        float textY = y + 6f;
        float circleY = textY + FontManager.navenRegular20.getHeight() / 2f - 5f;

        RenderUtil.drawRound(x + 2, circleY, 12, 12, 3f, new Color(0,0,0,200));
        RenderUtil.drawCircleCGUI(x + 8, circleY + 6, 9,
                boolValue.getValue() ? new Color(54,98,236,225).getRGB() : new Color(0,0,0,200).getRGB());

        FontManager.navenRegular20.drawString(boolValue.getName(), x + 20, textY, Color.WHITE.getRGB());
    }



    private final Map<ModeValue<?>, Map<Enum<?>, ContinualAnimation>> modeAnimMap = new HashMap<>();

    private float drawModeValue(ModeValue<?> modeValue, int mouseX, int mouseY, float y) {
        String title = modeValue.getName();
        float textY = y + 6f;

        FontManager.navenRegular20.drawString(title, x + 9, textY, Color.WHITE.getRGB());

        float startX = x + 20 + FontManager.navenRegular20.getStringWidth(title);
        float innerRight = x + width - 20f;
        float innerWidth = innerRight - startX;

        float dotR = 7f;
        float chipGap = 14f;
        float textPad = 6f;

        float modeX = startX;
        float modeY = y + 26f;
        float rowW = 0f;

        for (Enum<?> m : modeValue.getModes()) {
            String label = m.name();
            float textW = FontManager.navenRegular20.getStringWidth(label);
            float chipW = (dotR * 2f) + textPad + textW;

            float need = (rowW == 0f ? chipW : rowW + chipGap + chipW);
            if (need > innerWidth + 1e-2f) {
                modeX = startX;
                modeY += 26f;
                rowW = 0f;
            } else if (rowW != 0f) {
                modeX += chipGap;
                rowW += chipGap;
            }

            boolean selected = modeValue.getValue() == m;
            RenderUtil.drawCircle(modeX + dotR, modeY, dotR, new Color(0, 0, 0, 200).getRGB());
            RenderUtil.drawCircle(modeX + dotR, modeY, dotR - 2f,
                    selected ? new Color(54, 98, 236, 225).getRGB() : new Color(69, 67, 67, 200).getRGB());

            float textY2 = modeY - FontManager.navenRegular20.getHeight() / 2f + 1f;
            FontManager.navenRegular20.drawString(label, modeX + (dotR * 2f) + textPad, textY2, Color.WHITE.getRGB());

            modeX += chipW;
            rowW += chipW;
        }
        return modeY + 20f;
    }









    private final Map<NumberValue, ContinualAnimation> numberWidthAnim = new HashMap<>();

    private void drawNumberValue(NumberValue numberValue, int mouseX, int mouseY, float y) {
        String name = numberValue.getName();
        String valueStr = fmt2(numberValue.value.doubleValue()) + " / " + fmt2(numberValue.getMax());

        float textY = y + 6f;
        FontManager.navenRegular20.drawString(name, x + 9, textY, Color.WHITE.getRGB());
        FontManager.navenRegular20.drawString(valueStr, x + width - 20 - FontManager.navenRegular20.getStringWidth(valueStr), textY, Color.WHITE.getRGB());

        float sliderX = x + 11f;
        float sliderWidth = width - 22f;
        float sliderY = y + 26f;

        double min = numberValue.getMin();
        double max = numberValue.getMax();
        double inc = numberValue.getInc();
        double range = max - min;

        double targetPercent;
        if (draggingNumberValue == numberValue) {
            double raw = ((mouseX - sliderX) / sliderWidth) * range + min;
            double snapped = Math.round(raw / inc) * inc;
            snapped = Math.max(min, Math.min(max, snapped));
            numberValue.setValue(snapped);
            targetPercent = (snapped - min) / range;
        } else {
            targetPercent = (numberValue.getValue() - min) / range;
        }
        targetPercent = Math.max(0d, Math.min(1d, targetPercent));
        float targetW = (float)(sliderWidth * targetPercent);

        ContinualAnimation anim = numberWidthAnim.get(numberValue);
        float fillW = anim == null ? targetW : anim.getOutput();
        if (anim == null) {
            ContinualAnimation a = new ContinualAnimation();
            numberWidthAnim.put(numberValue, a);
        } else {
            anim.animate(targetW, 28);
        }

        RenderUtil.drawRound(sliderX, sliderY, sliderWidth, 8f, 4f, new Color(27, 26, 26, 180));
        RenderUtil.drawRound(sliderX, sliderY, fillW, 8f, 4f, new Color(54, 98, 236, 225));
        RenderUtil.drawCircle(sliderX + fillW, sliderY + 4f, 6f, Color.WHITE.getRGB());
    }















    private void drawColorValue(ColorValue colorValue, int mouseX, int mouseY, float y) {
        FontManager.navenRegular20.drawString(colorValue.getName(), x + 11, y + 2, Color.WHITE.getRGB());
        RoundedUtils.drawRound(x + width - 45, y, 25, 12, 4, new Color(colorValue.getColor()));
    }


    private void drawColorPicker(ColorValue colorValue, int mouseX, int mouseY) {
        float pickerX = x + width + 5;
        float pickerY = y;
        float pickerWidth = 100;
        float pickerHeight = 100;

        RoundedUtils.drawRound(pickerX, pickerY, pickerWidth, pickerHeight, 6, new Color(0,0,0,30));

        ShaderElement.addBlurTask(()->{
            RoundedUtils.drawRound(pickerX, pickerY, pickerWidth, pickerHeight, 6, new Color(0,0,0,200));
        });
        ShaderElement.addBloomTask(()->{
            RoundedUtils.drawRound(pickerX, pickerY, pickerWidth, pickerHeight, 6, new Color(0,0,0,200));
        });

        float[] hsb = Color.RGBtoHSB(colorValue.getColorC().getRed(), colorValue.getColorC().getGreen(), colorValue.getColorC().getBlue(), null);
        for (int i = 0; i < pickerWidth - 10; i++) {
            for (int j = 0; j < pickerHeight - 20; j++) {
                float saturation = (float) i / (pickerWidth - 10);
                float brightness = 1.0f - ((float) j / (pickerHeight - 20));
                RenderUtil.drawRound(pickerX + 5 + i, pickerY + 5 + j, 1, 1,0,new Color(Color.HSBtoRGB(hsb[0], saturation, brightness)));
            }
        }

        for (int i = 0; i < pickerWidth - 10; i++) {
            float hue = (float) i / (pickerWidth - 10);
            RenderUtil.drawRound(pickerX + 5 + i, pickerY + pickerHeight - 10, 1, 5,0,new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f)));
        }
    }

    private void drawTextValue(TextValue textValue, float y) {
        String str = textValue.getName() + ": " + textValue.getValue();
        FontManager.navenRegular20.drawString(str, x + 11, y + 2, Color.WHITE.getRGB());
    }


    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float currentWidth = (currentState == GuiState.CATEGORY) ? categoryWidth : width;

        if (listeningKey && keyBindingTarget != null) {
            if (mouseButton == 2 || mouseButton == 3 || mouseButton == 4) {
                keyBindingTarget.setKey(-mouseButton);
                keyBindFeedbackText = keyBindingTarget.getName() + " 已绑定为 Mouse" + mouseButton;
                keyBindFeedbackTime = System.currentTimeMillis();
                listeningKey = false;
                keyBindingTarget = null;
                return;
            }
        }

        if (activeColorValue != null) {
            if (RenderUtil.isHovering(x + width + 5, y, 100, 100, mouseX, mouseY)) {
                mouseClickedColorPicker(mouseX, mouseY, mouseButton);
                return;
            }
            activeColorValue = null;
        }

        switch (currentState) {
            case CATEGORY:
                mouseClickedCategory(mouseX, mouseY, mouseButton);
                break;
            case MODULE:
                mouseClickedModule(mouseX, mouseY, mouseButton);
                break;
            case VALUE:
                mouseClickedValue(mouseX, mouseY, mouseButton);
                break;
        }

        if (RenderUtil.isHovering(x, y, currentWidth, 20, mouseX, mouseY) && activeColorValue == null) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
            return;
        }

        if (currentState != GuiState.CATEGORY && RenderUtil.isHovering(x + width - 10, y + height - 10, 10, 10, mouseX, mouseY)) {
            resizing = true;
        }
    }
    private void mouseClickedCategory(int mouseX, int mouseY, int mouseButton) {
        float itemGap = 25f;
        float itemH = 20f;
        float categoryY = y + 40f;
        for (Category category : Category.values()) {
            if (RenderUtil.isHovering(x, categoryY, 100f, itemH, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    selectedCategory = category;
                    currentState = GuiState.MODULE;
                    moduleScrollAnimation = 0f;
                    moduleMotionY.snapTo(0f);
                    lastCategory = selectedCategory;
                    lastModule = null;
                    lastState = GuiState.MODULE;
                    width = Math.max(width, 500f);
                    height = Math.max(height, 300f);
                }
                return;
            }
            categoryY += itemGap;
        }
    }

    private void mouseClickedModule(int mouseX, int mouseY, int mouseButton) {
        if (RenderUtil.isHovering(x, y, 80, 30, mouseX, mouseY)) {
            currentState = GuiState.CATEGORY;
            lastState = GuiState.CATEGORY;
            return;
        }
        if (listeningKey) return;
        float listX = x + 5f;
        float listY = y + 20f + moduleScrollAnimation;
        float listW = 120f;
        float cardH = 25f;
        float gap = 5f;
        List<Module> modules = dev.xinxin.SilenceFix.instance.moduleManager.getModsByCategory(selectedCategory);
        for (Module module : modules) {
            if (RenderUtil.isHovering(listX, listY, listW, cardH, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)) {
                        listeningKey = true;
                        keyBindingTarget = module;
                    } else {
                        module.toggle();
                    }
                } else if (mouseButton == 1) {
                    selectedModule = module;
                    currentState = GuiState.VALUE;
                    valueScrollAnimation = 0f;
                    valueMotionY.snapTo(0f);
                    lastCategory = selectedCategory;
                    lastModule = selectedModule;
                    lastState = GuiState.VALUE;
                    width = Math.max(width, 500f);
                    height = Math.max(height, 300f);
                } else if (mouseButton == 2) {
                    listeningKey = true;
                    keyBindingTarget = module;
                }
                return;
            }
            listY += cardH + gap;
        }
    }

    private void mouseClickedValue(int mouseX, int mouseY, int mouseButton) {
        String backText = displayBackTextForModule(selectedModule);
        if (RenderUtil.isHovering(x, y - 2, FontManager.navenRegular20.getStringWidth(backText), 26, mouseX, mouseY)) {
            currentState = GuiState.CATEGORY;
            lastState = GuiState.CATEGORY;
            return;
        }

        float listX = x + 5f;
        float listY = y + 20f + moduleScrollAnimation;
        float listW = 120f - (SCROLLBAR_W + SCROLLBAR_PAD);
        float cardH = 25f;
        float gap = 5f;
        java.util.List<Module> modules = dev.xinxin.SilenceFix.instance.moduleManager.getModsByCategory(selectedCategory);
        for (Module module : modules) {
            if (RenderUtil.isHovering(listX, listY, listW, cardH, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)) {
                        listeningKey = true;
                        keyBindingTarget = module;
                    } else {
                        module.toggle();
                    }
                } else if (mouseButton == 1) {
                    selectedModule = module;
                    valueScrollAnimation = 0f;
                    valueMotionY.snapTo(0f);
                    lastCategory = selectedCategory;
                    lastModule = selectedModule;
                    lastState = GuiState.VALUE;
                    width = Math.max(width, 500f);
                    height = Math.max(height, 300f);
                } else if (mouseButton == 2) {
                    listeningKey = true;
                    keyBindingTarget = module;
                }
                return;
            }
            listY += cardH + gap;
        }

        float contentLeft = x + 140f;
        float contentWidth = width - 155f;
        float oldX = this.x, oldW = this.width;
        this.x = contentLeft;
        this.width = contentWidth;

        float valueY = y + 30f + valueScrollAnimation;
        java.util.List<Value<?>> values = selectedModule.getValues();
        for (int i = 0; i < values.size();) {
            Value<?> value = values.get(i);
            if (value.isHidden()) { i++; continue; }

            if (value instanceof BoolValue) {
                java.util.List<BoolValue> group = new java.util.ArrayList<>();
                while (i < values.size() && values.get(i) instanceof BoolValue) { group.add((BoolValue) values.get(i)); i++; }
                float boolX = this.x + 15f;
                float boolY = valueY;
                float rowW = 0f;
                float rowHeight = 28f;
                for (BoolValue bv : group) {
                    float w = 25f + FontManager.navenRegular20.getStringWidth(bv.getName());
                    if (rowW + w > this.width - 40f) {
                        rowW = 0f;
                        boolX = this.x + 15f;
                        boolY += rowHeight;
                    }
                    float circleX = boolX + 6;
                    float circleY = boolY + 14;
                    if (RenderUtil.isHovering(circleX - 4, circleY - 8, w, rowHeight, mouseX, mouseY)) {
                        bv.setValue(!bv.getValue());
                        this.x = oldX;
                        this.width = oldW;
                        return;
                    }
                    boolX += w + 14;
                    rowW += w + 14;
                }
                valueY = boolY + rowHeight;
                continue;
            }

            if (value instanceof ModeValue) {
                ModeValue<?> mv = (ModeValue<?>) value;
                String title = mv.getName();
                float labelX = this.x + 9f;
                float startX = labelX + FontManager.navenRegular20.getStringWidth(title);
                float innerRight = this.x + this.width - 20f;
                float innerWidth = innerRight - startX;
                float dotR = 7f;
                float chipGap = 14f;
                float textPad = 6f;
                float modeX = startX;
                float modeY = valueY + 26f;
                float rowW = 0f;
                for (Enum<?> m : mv.getModes()) {
                    String label = m.name();
                    float textW = FontManager.navenRegular20.getStringWidth(label);
                    float chipW = (dotR * 2f) + textPad + textW;
                    float need = (rowW == 0f ? chipW : rowW + chipGap + chipW);
                    if (need > innerWidth + 1e-2f) {
                        modeX = startX;
                        modeY += 26f;
                        rowW = 0f;
                    } else if (rowW != 0f) {
                        modeX += chipGap;
                        rowW += chipGap;
                    }
                    if (RenderUtil.isHovering(modeX, modeY - 8f, chipW, 18f, mouseX, mouseY)) {
                        ((ModeValue<Enum<?>>) mv).setValue(m);
                        lastCategory = selectedCategory;
                        lastModule = selectedModule;
                        lastState = GuiState.VALUE;
                        this.x = oldX; this.width = oldW;
                        return;
                    }
                    modeX += chipW;
                    rowW += chipW;
                }
                valueY = modeY + 20f;
                i++;
                continue;
            }

            if (value instanceof NumberValue) {
                float sliderX = this.x + 11f;
                float sliderW = this.width - 22f;
                float sliderY = valueY + 26f;
                if (RenderUtil.isHovering(sliderX, sliderY, sliderW, 8f, mouseX, mouseY)) {
                    draggingNumberValue = (NumberValue) value;
                    this.x = oldX;
                    this.width = oldW;
                    return;
                }
                valueY += 30f; i++; continue;
            }

            if (value instanceof ColorValue) {
                if (RenderUtil.isHovering(this.x, valueY, this.width, 15f, mouseX, mouseY)) {
                    activeColorValue = (ColorValue) value;
                    this.x = oldX; this.width = oldW;
                    return;
                }
                valueY += 25f; i++; continue;
            }

            if (value instanceof TextValue) { valueY += 25f; i++; continue; }

            valueY += 25f; i++;
        }

        this.x = oldX;
        this.width = oldW;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingNumberValue != null) {
            float contentLeft = x + 140f;
            float contentWidth = width - 155f;
            float oldX = this.x, oldW = this.width;
            this.x = contentLeft;
            this.width = contentWidth;

            final float sliderX = this.x + 11f;
            final float sliderWidth = this.width - 22f;
            final double range = draggingNumberValue.getMax() - draggingNumberValue.getMin();
            double raw = ((mouseX - sliderX) / sliderWidth) * range + draggingNumberValue.getMin();
            double snapped = Math.round(raw / draggingNumberValue.getInc()) * draggingNumberValue.getInc();
            snapped = Math.max(draggingNumberValue.getMin(), Math.min(draggingNumberValue.getMax(), snapped));
            draggingNumberValue.setValue(snapped);

            this.x = oldX;
            this.width = oldW;
        }
        if (activeColorValue != null) {
            mouseClickedColorPicker(mouseX, mouseY, clickedMouseButton);
        }
    }






    @Override
    public void onGuiClosed() {
        lastState = currentState;
        lastCategory = selectedCategory;
        lastModule = selectedModule;
        lastX = x;
        lastY = y;
        lastW = width;
        lastH = height;
        super.onGuiClosed();
    }


    private static GuiState lastState = GuiState.CATEGORY;
    private static Category lastCategory = null;
    private static Module lastModule = null;

    private static float lastX = 100f;
    private static float lastY = 100f;
    private static float lastW = 350f;
    private static float lastH = 250f;

    private String displayName(Module m){
        return HUD.langModeValue.is("English") ? m.getName() : m.getCnName();
    }
    private String displayBackTextForModule(Module m){
        if (HUD.langModeValue.is("English")) {
            return m.desc.isEmpty() ? "< The " + m.getName() : "< " + m.getDesc();
        } else {
            return m.desc.isEmpty() ? "< " + m.getCnName() : "< " + m.getCndesc();
        }
    }
    private void drawKeyBindTip() {
        String tip;
        if (listeningKey && keyBindingTarget != null) {
            tip = "你已进入按键绑定状态，按任意键绑定；ESC退出，Delete删除。";
        } else if (keyBindFeedbackText != null && System.currentTimeMillis() - keyBindFeedbackTime < 1000) {
            tip = keyBindFeedbackText;
        } else {
            tip = "按住左Shift点击模块以绑定按键";
        }
        float w = FontManager.navenRegular20.getStringWidth(tip) + 8;
        float h = FontManager.navenRegular20.getHeight() + 6;
        runToXy(x - 4, y + height + 16);
        runToWH(w, h);
        RoundedUtils.drawRound(animatedX.getOutput(), animatedY.getOutput(), animatedW.getOutput(), animatedH.getOutput(), 8, new Color(0,0,0,80));
        ShaderElement.addBlurTask(()-> RoundedUtils.drawRound(animatedX.getOutput(), animatedY.getOutput(), animatedW.getOutput(), animatedH.getOutput(), 8, new Color(0,0,0,200)));
        ShaderElement.addBloomTask(()-> RoundedUtils.drawRound(animatedX.getOutput(), animatedY.getOutput(), animatedW.getOutput(), animatedH.getOutput(), 8, new Color(0,0,0,200)));
        FontManager.navenRegular20.drawString(tip, animatedX.getOutput()+4, animatedY.getOutput()+4, Color.WHITE.getRGB());
    }


    private void drawScrollbar(float ax, float ay, float aw, float ah, float contentH, float scroll, float alpha) {
        if (contentH <= ah) return;
        float barH = Math.max(20f, (ah * ah) / contentH);
        float progress = -scroll / (contentH - ah);
        progress = Math.max(0f, Math.min(1f, progress));
        float barY = ay + progress * (ah - barH);
        int a = Math.max(0, Math.min(160, (int) alpha));
        RoundedUtils.drawRound(ax + aw - 4, barY, 3, barH, 1.5f, new Color(54, 98, 236, a));
    }






    private void mouseClickedColorPicker(int mouseX, int mouseY, int mouseButton) {
        float pickerX = x + width + 5;
        float pickerY = y;
        float pickerWidth = 100;
        float pickerHeight = 100;

        if (RenderUtil.isHovering(pickerX + 5, pickerY + 5, pickerWidth - 10, pickerHeight - 20, mouseX, mouseY)) {
            float saturation = (mouseX - (pickerX + 5)) / (pickerWidth - 10);
            float brightness = 1.0f - ((mouseY - (pickerY + 5)) / (pickerHeight - 20));
            float[] hsb = Color.RGBtoHSB(activeColorValue.getColorC().getRed(), activeColorValue.getColorC().getGreen(), activeColorValue.getColorC().getBlue(), null);
            activeColorValue.setColor(Color.HSBtoRGB(hsb[0], saturation, brightness));
        }

        if (RenderUtil.isHovering(pickerX + 5, pickerY + pickerHeight - 10, pickerWidth - 10, 5, mouseX, mouseY)) {
            float hue = (mouseX - (pickerX + 5)) / (pickerWidth - 10);
            float[] hsb = Color.RGBtoHSB(activeColorValue.getColorC().getRed(), activeColorValue.getColorC().getGreen(), activeColorValue.getColorC().getBlue(), null);
            activeColorValue.setColor(Color.HSBtoRGB(hue, hsb[1], hsb[2]));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        resizing = false;
        draggingNumberValue = null;
    }







    private float calculateTotalValueHeight(List<Value<?>> values) {
        float total = 0f;
        final float titleH = FontManager.navenRegular18.getHeight();
        final float dotR = 6f;
        final float chipGap = 12f;
        final float textPad = 6f;
        for (int i = 0; i < values.size();) {
            Value<?> v = values.get(i);
            if (v.isHidden()) { i++; continue; }
            if (v instanceof BoolValue) {
                List<BoolValue> group = new ArrayList<>();
                while (i < values.size() && values.get(i) instanceof BoolValue) { group.add((BoolValue) values.get(i)); i++; }
                float rowW = 0f;
                int rows = 1;
                for (BoolValue b : group) {
                    float w = 25f + FontManager.navenRegular18.getStringWidth(b.getName());
                    if (rowW + w > width - 40f) { rowW = 0f; rows++; }
                    rowW += w;
                }
                total += (rows - 1) * 20f + 18f;
                continue;
            }
            if (v instanceof ModeValue) {
                ModeValue<?> mv = (ModeValue<?>) v;
                String title = mv.getName();
                float labelX = x + 10f;
                float startX = labelX + FontManager.navenRegular18.getStringWidth(title) + 15f;
                float innerRight = x + width - 20f;
                float innerWidth = innerRight - startX;
                float rowW = 0f;
                int rows = 1;
                for (Enum<?> m : mv.getModes()) {
                    String label = m.name();
                    float textW = FontManager.navenRegular18.getStringWidth(label);
                    float chipW = (dotR * 2f) + textPad + textW;
                    float need = (rowW == 0f ? chipW : rowW + chipGap + chipW);
                    if (need > innerWidth + 1e-2f) { rowW = chipW; rows++; }
                    else { rowW = need; }
                }
                total += (titleH * 0.5f) + 22f + (rows - 1) * 22f;
                i++;
                continue;
            }
            if (v instanceof NumberValue) { total += 30f; i++; continue; }
            if (v instanceof TextValue) { total += 25f; i++; continue; }
            if (v instanceof ColorValue) { total += 25f; i++; continue; }
            total += 25f; i++;
        }
        return total;
    }



    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static String fmt2(double v) { return String.format("%.2f", v); }



    private long backspaceHoldStart = 0;
    private int tabIndex = -1;
    private List<String> tabMatches = new ArrayList<>();

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (listeningSearch && org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_BACK)) {
            if (System.currentTimeMillis() - backspaceHoldStart > 200) {
                if (searchText.length() > 0) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                    backspaceHoldStart = System.currentTimeMillis() - 150;
                }
            }
        } else {
            backspaceHoldStart = 0;
        }
    }
    private boolean selectAllSearch = false;


    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningSearch) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                listeningSearch = false;
                searchText = "";
                tabMatches.clear();
                tabIndex = -1;
                selectAllSearch = false;
                return;
            }
            if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN || keyCode == org.lwjgl.input.Keyboard.KEY_NUMPADENTER) {
                String q = searchText.toLowerCase();
                Module target = null;
                for (Module m : dev.xinxin.SilenceFix.instance.moduleManager.getModules()) {
                    String en = m.getName() == null ? "" : m.getName();
                    String cn = m.getCnName() == null ? "" : m.getCnName();
                    if (en.toLowerCase().contains(q) || cn.contains(searchText)) {
                        target = m;
                        break;
                    }
                }
                if (target != null) {
                    selectedCategory = target.getCategory();
                    selectedModule = target;
                    currentState = GuiState.VALUE;
                    valueScrollAnimation = 0f;
                    valueMotionY.snapTo(0f);
                    lastCategory = selectedCategory;
                    lastModule = selectedModule;
                    lastState = GuiState.VALUE;
                    width = Math.max(width, 500f);
                    height = Math.max(height, 300f);
                }
                listeningSearch = false;
                searchText = "";
                tabMatches.clear();
                tabIndex = -1;
                selectAllSearch = false;
                return;
            }
            if (keyCode == org.lwjgl.input.Keyboard.KEY_BACK) {
                if (selectAllSearch) {
                    searchText = "";
                    selectAllSearch = false;
                } else if (searchText.length() > 0) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                if (backspaceHoldStart == 0) backspaceHoldStart = System.currentTimeMillis();
                return;
            }
            if (keyCode == org.lwjgl.input.Keyboard.KEY_TAB) {
                if (tabMatches.isEmpty()) {
                    String q = searchText.toLowerCase();
                    for (Module m : dev.xinxin.SilenceFix.instance.moduleManager.getModules()) {
                        String en = m.getName() == null ? "" : m.getName();
                        if (en.toLowerCase().startsWith(q)) tabMatches.add(en);
                    }
                }
                if (!tabMatches.isEmpty()) {
                    tabIndex = (tabIndex + 1) % tabMatches.size();
                    searchText = tabMatches.get(tabIndex);
                }
                return;
            }
            if ((keyCode == org.lwjgl.input.Keyboard.KEY_A) &&
                    (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL))) {
                selectAllSearch = !selectAllSearch;
                return;
            }
            if (Character.isLetterOrDigit(typedChar)) {
                if (selectAllSearch) {
                    searchText = "";
                    selectAllSearch = false;
                }
                searchText += typedChar;
                tabMatches.clear();
                tabIndex = -1;
            }
            return;
        }

        if ((keyCode == org.lwjgl.input.Keyboard.KEY_F)
                && (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL))) {
            listeningSearch = true;
            searchText = "";
            tabMatches.clear();
            tabIndex = -1;
            selectAllSearch = false;
            return;
        }

        if (listeningKey && keyBindingTarget != null) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                keyBindFeedbackText = "已取消绑定";
            } else if (keyCode == org.lwjgl.input.Keyboard.KEY_DELETE) {
                keyBindingTarget.setKey(-1);
                keyBindFeedbackText = keyBindingTarget.getName() + " 已绑定为 None";
            } else {
                keyBindingTarget.setKey(keyCode);
                keyBindFeedbackText = keyBindingTarget.getName() + " 已绑定为 " + org.lwjgl.input.Keyboard.getKeyName(keyCode);
            }
            keyBindFeedbackTime = System.currentTimeMillis();
            listeningKey = false;
            keyBindingTarget = null;
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }





}