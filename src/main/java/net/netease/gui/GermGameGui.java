    package net.netease.gui;

    import dev.xinxin.event.EventManager;
    import dev.xinxin.event.EventTarget;
    import dev.xinxin.event.rendering.EventShader;
    import dev.xinxin.utils.SmoothAnimationTimer;
    import dev.xinxin.utils.render.RenderUtil;
    import dev.xinxin.utils.render.RoundedUtils;
    import dev.xinxin.utils.render.fontRender.FontManager;
    import dev.xinxin.utils.render.animation.Animation;
    import dev.xinxin.utils.render.animation.Direction;
    import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
    import dev.xinxin.utils.render.animation.impl.RippleAnimation;
    import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
    import lombok.Getter;
    import lombok.Setter;
    import net.minecraft.client.gui.GuiScreen;
    import net.minecraft.client.gui.ScaledResolution;
    import net.minecraft.client.renderer.GlStateManager;
    import net.minecraft.util.ResourceLocation;

    import java.awt.*;
    import java.io.IOException;
    import java.util.*;
    import java.util.List;

    @Getter
    @Setter
    public class GermGameGui extends GuiScreen {
        public static GermGameGui INSTANCE = new GermGameGui();
        private float x, y, width, height;
        private final ResourceLocation germLogo = new ResourceLocation("quicksand/germ.png");
        private final List<GermGameElement> elements = new ArrayList<>();
        private GermGameElement currentElement;
        private String guiName;
        private final Animation switchScreenAnim = new DecelerateAnimation(0, 1).setDirection(Direction.BACKWARDS);
        private final Animation backHoverAnim = new DecelerateAnimation(0, 1).setDirection(Direction.BACKWARDS);
        private final DragComponent dragComponent = new DragComponent();
        private final Scroll scroll = new Scroll();
        private ScaledResolution scaledResolution;
        private RippleAnimation backButtonRipple = new RippleAnimation();

        private final Map<String, String> mapping = new HashMap<>();
        private final SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0);
        private final SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0);

        @Override
        public void initGui() {
            scaledResolution = new ScaledResolution(mc);
            float s = calculateGuiScale();
            this.width = 130f;
            this.height = 116f;
            heightTimer.value = 0;
            widthTimer.value = 0;
            this.x = (scaledResolution.getScaledWidth() / s) / 2f - width / 2f;
            this.y = (scaledResolution.getScaledHeight() / s) / 2f - height / 2f;
            EventManager.register(this);
        }

        @EventTarget
        public void onShader(EventShader eventShader){
            float s = calculateGuiScale();
            RenderUtil.drawRound(
                    dragComponent.getX() * s,
                    dragComponent.getY() * s,
                    widthTimer.value * s,
                    heightTimer.value * s,
                    5,
                    new Color(0, 0, 0, 100)
            );
        }


        @Override
        public void onGuiClosed() {
            switchScreenAnim.setState(false);
            backHoverAnim.setState(true);
            EventManager.unregister(this);
        }

        public void addMapping(String input, String output) {
            mapping.put(input, output);
        }

        public String map(String input) {
            return mapping.getOrDefault(input, input);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            if (pendingElement != null) {
                this.currentElement = pendingElement;
                this.pendingElement = null;
            }

            float guiScale = calculateGuiScale();

            GlStateManager.pushMatrix();
            GlStateManager.scale(guiScale, guiScale, 1.0f);

            float scaledMouseX = mouseX / guiScale;
            float scaledMouseY = mouseY / guiScale;

            updateDragComponent();
            updateTimers();

            drawBackground();
            drawElements(scaledMouseX, scaledMouseY);
            drawSubElements(scaledMouseX, scaledMouseY);

            GlStateManager.popMatrix();
        }


        private float calculateGuiScale() {
            float screenWidth = scaledResolution.getScaledWidth();
            float screenHeight = scaledResolution.getScaledHeight();
            float scaleW = screenWidth / 854f;
            float scaleH = screenHeight / 480f;
            float scale = Math.min(scaleW, scaleH);
            if (scale < 1.0f) scale = 1.0f;
            if (scale > 4.0f) scale = 4.0f;
            return scale;
        }


        private void updateDragComponent() {
            dragComponent.setX(x);
            dragComponent.setY(y);
            dragComponent.setWidth(width);
            dragComponent.setHeight(height);
            dragComponent.setLimitHeight(height);
            x = dragComponent.getX();
            y = dragComponent.getY();
        }

        private void updateTimers() {
            widthTimer.target = width;
            heightTimer.target = height;
            widthTimer.speed = 0.3f;
            heightTimer.speed = 0.3f;
            widthTimer.update(true);
            heightTimer.update(true);
        }


        private void drawBackground() {
            float s = calculateGuiScale();
            RenderUtil.drawRound(x, y, widthTimer.value, heightTimer.value, 5, new Color(0, 0, 0, 80));
            RenderUtil.startGlScissor((int) ((x - 1) * s), (int) (y * s), (int) ((widthTimer.value + 2) * s), (int) (4 * s));
            RenderUtil.drawRound(x - 1, y, widthTimer.value + 2, 6, 5, new Color(227, 17, 97, 200));
            RenderUtil.stopGlScissor();
        }


        private RapeMasterFontManager getAdaptiveFont(float s, boolean bold) {
            int baseSize = 16;
            int targetSize = Math.round(baseSize * s);

            if (bold) {
                if (targetSize <= 14) return FontManager.navenbold14;
                if (targetSize <= 16) return FontManager.navenbold16;
                if (targetSize <= 18) return FontManager.navenbold18;
                if (targetSize <= 20) return FontManager.navenbold20;
                if (targetSize <= 22) return FontManager.navenbold22;
                if (targetSize <= 24) return FontManager.navenbold24;
                if (targetSize <= 26) return FontManager.navenbold26;
                if (targetSize <= 30) return FontManager.navenbold30;
                return FontManager.navenbold38;
            } else {
                if (targetSize <= 14) return FontManager.navenRegular14;
                if (targetSize <= 16) return FontManager.navenRegular16;
                if (targetSize <= 18) return FontManager.navenRegular18;
                if (targetSize <= 20) return FontManager.navenRegular20;
                if (targetSize <= 22) return FontManager.navenRegular22;
                if (targetSize <= 24) return FontManager.navenRegular24;
                if (targetSize <= 26) return FontManager.navenRegular26;
                if (targetSize <= 30) return FontManager.navenRegular30;
                return FontManager.navenRegular38;
            }
        }

        private void drawElements(float mouseX, float mouseY) {
            float s = calculateGuiScale();
            float originalX = x;
            RenderUtil.startGlScissor((int) (originalX * s), (int) (y * s), (int) (widthTimer.value * s), (int) (heightTimer.value * s));
            float offsetY = 0f;
            float textOffsetLeft = 6f;
            float padding = 10f;

            RapeMasterFontManager font = getAdaptiveFont(s, true);

            for (GermGameElement element : elements) {
                float elementY = this.y + 6f + offsetY;

                addDefaultMappings();
                String text = map(element.getName());
                int textWidth = font.getStringWidth(text);

                float textX = this.x + 10f + textOffsetLeft;
                float boxX = textX - padding;
                float boxW = textWidth + padding * 2f;
                float eh = 18f;

                boolean isHovering = RenderUtil.isHovering(boxX, elementY, boxW, eh, (int) mouseX, (int) mouseY);
                element.setRunnable(() -> {
                    if (isHovering) element.click(guiName);
                });

                float pressScale = (mc.gameSettings.keyBindUseItem.isKeyDown() && isHovering) ? 0.95f : 1f;
                float scale = isHovering ? 1.05f * pressScale : pressScale;

                GlStateManager.pushMatrix();
                GlStateManager.translate(boxX + boxW / 2f, elementY + eh / 2f, 0);
                GlStateManager.scale(scale, scale, 1);
                GlStateManager.translate(-(boxX + boxW / 2f), -(elementY + eh / 2f), 0);

                int alpha = isHovering ? 240 : 200;
                Color baseColor = new Color(0, 200, 255, alpha);
                Color targetColor = isHovering ? new Color(0, 255, 255, 240) : new Color(0, 200, 255, 200);
                long transition = System.currentTimeMillis() % 200;
                float progress = transition / 200f;
                int r = (int) (baseColor.getRed() + (targetColor.getRed() - baseColor.getRed()) * progress);
                int g = (int) (baseColor.getGreen() + (targetColor.getGreen() - baseColor.getGreen()) * progress);
                int b = (int) (baseColor.getBlue() + (targetColor.getBlue() - baseColor.getBlue()) * progress);
                int a = (int) (baseColor.getAlpha() + (targetColor.getAlpha() - baseColor.getAlpha()) * progress);

                if (isHovering) {
                    RoundedUtils.drawRound(boxX, elementY, boxW - 15f, eh, 4f, new Color(200, 200, 200, 80));
                }

                GlStateManager.pushMatrix();
                GlStateManager.scale(1 / s, 1 / s, 1);
                font.drawString(text, (textX) * s, (elementY + 4) * s, new Color(r, g, b, a).getRGB());
                GlStateManager.popMatrix();

                backButtonRipple.draw(boxX, elementY, boxW, eh);

                GlStateManager.popMatrix();
                offsetY += eh;
            }
            RenderUtil.stopGlScissor();
        }

        private GermGameElement pendingElement;

        private void drawSubElements(float mouseX, float mouseY) {
            float s = calculateGuiScale();
            float originalX = x;
            float doubleX = x;
            float doubleY = y;
            RenderUtil.startGlScissor((int) (originalX * s), (int) (y * s), (int) (widthTimer.value * s), (int) (heightTimer.value * s));
            if (currentElement != null) {
                float startX = doubleX + 40;
                float startY = doubleY;
                float slideProgress = Math.min(1f, widthTimer.value / Math.max(1f, widthTimer.target));
                for (GermGameSubElement subElement : currentElement.getSubElements()) {
                    float subX = startX + 35f * slideProgress;
                    float subY = startY + 5f;

                    RapeMasterFontManager font = getAdaptiveFont(s, true);
                    int textWidth = font.getStringWidth(subElement.getName());
                    float padding = 10f;
                    float sw = textWidth + padding * 2;
                    float sh = 18;

                    boolean isSubHovering = RenderUtil.isHovering(subX - padding, subY, sw, sh, (int) mouseX, (int) mouseY);
                    subElement.getHoverAnim().setState(isSubHovering);
                    subElement.setRunnable(() -> {
                        if (isSubHovering) subElement.joinGame(guiName);
                    });

                    float progressW = Math.min(1f, widthTimer.value / Math.max(1f, widthTimer.target));
                    float progressH = Math.min(1f, heightTimer.value / Math.max(1f, heightTimer.target));
                    float progress = Math.min(progressW, progressH);
                    int alpha = (int) (255 * progress);

                    GlStateManager.pushMatrix();
                    GlStateManager.translate(subX + sw / 2f, subY + sh / 2f, 0);
                    GlStateManager.scale(isSubHovering ? 1.05f : 1f, isSubHovering ? 1.05f : 1f, 1);
                    GlStateManager.translate(-(subX + sw / 2f), -(subY + sh / 2f), 0);

                    if (isSubHovering) {
                        int hoverAlpha = (int) (60 * progress);
                        RoundedUtils.drawRound(subX - padding, subY, sw, sh, 4f, new Color(200, 200, 200, hoverAlpha));
                    }

                    GlStateManager.popMatrix();

                    GlStateManager.pushMatrix();
                    GlStateManager.scale(1 / s, 1 / s, 1);
                    float textX = subX - padding - 2;
                    font.drawString(
                            subElement.getName(),
                            textX * s,
                            (subY + 6) * s,
                            new Color(255, 255, 255, isSubHovering ? Math.min(255, alpha) : Math.max(0, alpha - 40)).getRGB()
                    );
                    GlStateManager.popMatrix();

                    startY += sh;
                }
            }
            RenderUtil.stopGlScissor();
            x = originalX;
        }







        private void addDefaultMappings() {
            addMapping("subject_bedwar", "起床战争");
            addMapping("subject_skywar", "空岛战争");
            addMapping("subject_leisure", "休闲模式");
            addMapping("subject_fight", "竞技游戏");
            addMapping("subject_survive", "生存模式");
            addMapping("subject_fight_team", "战争模式");
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            float s = calculateGuiScale();
            int sx = (int) (mouseX / s);
            int sy = (int) (mouseY / s);
            dragComponent.handleDrag(sx, sy, mouseButton, true);

            if (RenderUtil.isHovering(x, y, width, height, sx, sy)) {
                RapeMasterFontManager font = getAdaptiveFont(s, true);
                float padding = 10f;

                for (int i = 0; i < elements.size(); i++) {
                    GermGameElement element = elements.get(i);
                    float elementX = this.x + 10f;
                    float elementY = this.y + 6f + i * 18f;

                    int textWidth = font.getStringWidth(map(element.getName()));
                    float ew = textWidth + padding * 2;
                    float eh = 18f;

                    if (RenderUtil.isHovering(elementX, elementY, ew, eh, sx, sy)) {
                        if (element.getRunnable() != null) {
                            element.getRunnable().run();
                        }
                        this.pendingElement = element;
                        backButtonRipple.mouseClicked(sx, sy);
                    }
                }

                if (currentElement != null) {
                    for (int j = 0; j < currentElement.getSubElements().size(); j++) {
                        GermGameSubElement subElement = currentElement.getSubElements().get(j);
                        float subX = x + 40 + 35f;
                        float subY = y + j * 18f + 5f;

                        int textWidth = font.getStringWidth(subElement.getName());
                        float paddingSub = 10f;
                        float sw = textWidth + paddingSub * 2;
                        float sh = 18f;

                        if (RenderUtil.isHovering(subX - paddingSub, subY, sw, sh, sx, sy)) {
                            subElement.getAnimation().mouseClicked(sx, sy);
                            if (subElement.getRunnable() != null) {
                                subElement.getRunnable().run();
                            }
                            backButtonRipple.mouseClicked(sx, sy);
                        }
                    }
                }
            }
        }







    }
