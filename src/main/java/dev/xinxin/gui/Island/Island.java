package dev.xinxin.gui.Island;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.SmoothAnimationTimer;
import dev.xinxin.utils.TimerUtil;
import dev.xinxin.utils.render.AnimationUtil;
import dev.xinxin.utils.render.GradientUtil;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.StencilUtil;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.yalan.live.silencefix.LiveClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static dev.xinxin.SilenceFix.mc;
import static dev.xinxin.module.ModuleManager.getModule;
import static dev.xinxin.module.modules.render.HUD.color;
import static dev.xinxin.module.modules.render.HUD.mainColor;

import dev.xinxin.module.Module;

public class Island {
    private final List<TimedContent> timedContents;
    private final List<Content> contents;
    private float renderX, width, height;
    Color color = new Color(112, 216, 248, 239);

    SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0);
    SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0);

    public Island() {
        this.contents = new ArrayList<>();
        this.timedContents = new ArrayList<>();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!Objects.requireNonNull(getModule(HUD.class)).state) return;

        ScaledResolution sr = new ScaledResolution(mc);
        EventManager.call(new AddIslandEvent(AddIslandEvent.EventState.PRE));

        handleTimedContents();
        EventManager.call(new AddIslandEvent(AddIslandEvent.EventState.POST));

        updateSizeAndPosition(sr);
        drawIsland(sr);

        timedContents.removeIf(content -> content.timer.hasTimeElapsed(content.time));
    }


    private void handleTimedContents() {
        if (timedContents.isEmpty()) return;
        for (TimedContent content : timedContents) {
            if (!content.timer.hasTimeElapsed(content.time)) {
                contents.add(buildContentFromTimed(content));
            }
        }
    }

    private void renderTimedContent(TimedContent content) {
        switch (content.type) {
            case MODULE -> {
                if (content.timer.hasTimePassed(Math.toIntExact(content.time - 200))) {
                    content.animation.setDirection(Direction.BACKWARDS);
                }
                double animation = content.animation.getOutput();
                RenderUtil.drawRoundedRect(7, 8, 24, 24, 7, new Color(48, 117, 147, 200));
                RenderUtil.drawRoundedRect(18, 11, 2, (int) (18 * animation), 0, Color.WHITE);
                RenderUtil.drawRoundedRect(10, 19, (int) (18 * animation), 2, 0, Color.WHITE);
            }
            case SUCCESS -> renderIcon(content, new Color(95, 143, 80, 200), "v", 10.5f, 15, 20);
            case WARNING -> renderIcon(content, new Color(143, 80, 80, 200), "C", 11.3f, 13, 40);
            default -> RenderUtil.drawRoundedRect(7, 8, 24, 24, 7, new Color(200, 200, 200, 200));
        }
    }



    private Content buildContentFromTimed(TimedContent content) {
        Runnable renderer = () -> {
            renderTimedContent(content);
            renderText(content);
        };
        int width = 14 + Math.max(
                FontManager.harmonybold20.getStringWidth(content.title),
                FontManager.harmonybold18.getStringWidth(content.description)
        );
        return new Content(renderer, width, 40, PriorityUtil.LOW);
    }



    private void renderIcon(TimedContent content, Color bg, String symbol, float x, float y, int fontSize) {
        if (content.timer.hasTimePassed(Math.toIntExact(content.time - 200))) {
            content.animation.setDirection(Direction.BACKWARDS);
        }
        double animation = content.animation.getOutput();
        RenderUtil.drawRoundedRect(7, 8, 24, 24, 7, bg);
        StencilUtil.initStencilToWrite();
        RenderUtil.drawRectWH(7, 8, 24 * animation, 24, -1);
        StencilUtil.readStencilBuffer();
        if (fontSize == 20) FontManager.icontestFont20.drawString(symbol, x, y, -1);
        else FontManager.icontestFont40.drawString(symbol, x, y, -1);
        StencilUtil.endStencilBuffer();
    }



    private void renderText(TimedContent content) {
        if (content.type == IslandType.MODULE && content.module != null) {
            content.description = content.module.getName() +
                    (content.module.getState() ? " has been §2Enabled" : " has been §4Disabled");
            String title = content.title;
            String desc = content.description;
            float centerX = this.widthTimer.value / 2f;
            float titleX = centerX - FontManager.harmonybold20.getStringWidth(title) / 2f;
            float descX = centerX - FontManager.harmonybold18.getStringWidth(desc) / 2f;
            FontManager.harmonybold20.drawString(title, titleX, 9, color.getRGB());
            FontManager.harmonybold18.drawString(desc, descX, 24, -1);
        } else {
            float centerX = this.widthTimer.value / 2f;
            float descX = centerX - FontManager.harmonybold18.getStringWidth(content.description) / 2f;
            float descY = (40 - FontManager.harmonybold18.getHeight()) / 2f + 2f;
            FontManager.harmonybold18.drawString(content.description, descX, descY, -1);
        }
    }



    private void updateSizeAndPosition(ScaledResolution sr) {
        final String defaultText = " | " + LiveClient.INSTANCE.liveUser.getName() + " | " + Minecraft.getDebugFPS() + "fps";
        final boolean showLogo = HUD.islandLogo.getValue();
        final int logoW = 24, logoPadLeft = 2, logoGap = 6;
        int baseTextW = FontManager.harmonybold20.getStringWidth(SilenceFix.NAME) + FontManager.harmonybold20.getStringWidth(defaultText);
        int extraLogoW = showLogo ? (logoPadLeft + logoW + logoGap) : 0;
        int emptyContentTargetW = baseTextW + extraLogoW + 20;

        boolean tabOpen = mc.gameSettings.keyBindPlayerList.isKeyDown();

        if (tabOpen && mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            List<NetworkPlayerInfo> list = new ArrayList<>(mc.thePlayer.sendQueue.getPlayerInfoMap());
            list.sort((a, b) -> {
                String na = mc.ingameGUI.getTabList().getPlayerName(a);
                String nb = mc.ingameGUI.getTabList().getPlayerName(b);
                boolean aLive = na.contains("[Live]");
                boolean bLive = nb.contains("[Live]");
                if (aLive && !bLive) return -1;
                if (!aLive && bLive) return 1;
                return na.compareToIgnoreCase(nb);
            });
            list = list.subList(0, Math.min(list.size(), 80));

            int i = 0;
            for (NetworkPlayerInfo npi : list) {
                String name = mc.ingameGUI.getTabList().getPlayerName(npi);
                i = Math.max(i, FontManager.harmonybold18.getStringWidth(name));
            }

            int l3 = list.size();
            int i4 = l3;
            int cols;
            for (cols = 1; i4 > 20; i4 = (l3 + cols - 1) / cols) {
                ++cols;
            }

            boolean flag = mc.isIntegratedServerRunning() || mc.getNetHandler().getNetworkManager().getIsencrypted();
            int l = 0;
            int colWidth = Math.min(cols * ((flag ? 9 : 0) + i + l + 13), sr.getScaledWidth() - 50) / Math.max(1, cols);
            int totalWidth = colWidth * Math.max(1, cols) + (Math.max(1, cols) - 1) * 5;

            int rowH = Math.max(12, FontManager.harmonybold18.getHeight() + 2);
            int headerH = FontManager.harmonybold20.getHeight() + 14;
            int tabHeight = headerH + Math.max(1, i4) * rowH + 10;

            int contentsW = (int) getMaxWidth(this.contents);
            int contentsH = (int) getTotalHeight(this.contents);
            int gap = (!this.contents.isEmpty() || !this.timedContents.isEmpty()) ? 20 : 0;

            this.width = Math.max(totalWidth, contentsW);
            this.height = tabHeight + contentsH + gap;
        } else {
            this.width = this.contents.isEmpty() && this.timedContents.isEmpty() ? emptyContentTargetW : this.getMaxWidth(this.contents);
            this.height = this.contents.isEmpty() && this.timedContents.isEmpty() ? FontManager.harmonybold20.getHeight() * 2 : this.getTotalHeight(this.contents);
        }

        this.renderX = AnimationUtil.smooth(this.renderX, (sr.getScaledWidth() - this.width) / 2f, 0.1f);

        widthTimer.target = width;
        heightTimer.target = height;
        widthTimer.speed = 0.2f;
        heightTimer.speed = 0.2f;
        widthTimer.update(true);
        heightTimer.update(true);
    }





    private void drawIsland(ScaledResolution sr) {
        boolean tabOpen = mc.gameSettings.keyBindPlayerList.isKeyDown();
        boolean simple = !tabOpen && contents.isEmpty() && timedContents.isEmpty();
        if (simple) {
            drawDefaultContent();
            return;
        }

        RenderUtil.drawRoundedRect(
                (int) renderX, 15,
                (int) widthTimer.value, (int) heightTimer.value,
                11,
                new Color(0, 0, 0, 80)
        );
        ShaderElement.addBlurTask(() -> RenderUtil.drawRoundedRect(
                (int) renderX, 15,
                (int) widthTimer.value, (int) heightTimer.value,
                13,
                new Color(0, 0, 0, 200)
        ));
        ShaderElement.addBloomTask(() -> RenderUtil.drawRoundedRect(
                (int) renderX, 15,
                (int) widthTimer.value, (int) heightTimer.value,
                13,
                new Color(0, 0, 0, 200)
        ));

        RenderUtil.startGlScissor((int) renderX, 15, (int) widthTimer.value, (int) heightTimer.value);

        if (tabOpen && mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            List<NetworkPlayerInfo> list = new ArrayList<>(mc.thePlayer.sendQueue.getPlayerInfoMap());
            list.sort((a, b) -> {
                String naRaw = mc.ingameGUI.getTabList().getPlayerName(a);
                String nbRaw = mc.ingameGUI.getTabList().getPlayerName(b);
                String na = EnumChatFormatting.getTextWithoutFormattingCodes(naRaw);
                String nb = EnumChatFormatting.getTextWithoutFormattingCodes(nbRaw);
                boolean aLive = na.contains("[Live]");
                boolean bLive = nb.contains("[Live]");
                if (aLive && !bLive) return -1;
                if (!aLive && bLive) return 1;
                return na.compareToIgnoreCase(nb);
            });
            list = list.subList(0, Math.min(list.size(), 80));

            int count = list.size();
            String header = "Players | " + count;
            float headerX = (float) renderX + (widthTimer.value - FontManager.harmonybold20.getStringWidth(header)) / 2f;
            FontManager.harmonybold20.drawString(header, headerX, 23.5f, -1);

            int innerW = Math.max(0, (int) widthTimer.value - 14);
            int rowH = Math.max(12, FontManager.harmonybold18.getHeight() + 2);

            int l3 = count;
            int i4 = l3;
            int cols;
            for (cols = 1; i4 > 20; i4 = (l3 + cols - 1) / cols) {
                ++cols;
            }
            int rowsPerCol = Math.max(1, i4);
            int colW = Math.max(80, innerW / Math.max(1, cols));

            float startX = renderX + 7f;
            float startY = 23.5f + FontManager.harmonybold20.getHeight() + 4f;

            for (int i = 0; i < l3; i++) {
                int col = i / rowsPerCol;
                int row = i % rowsPerCol;
                float x = startX + col * colW;
                float y = startY + row * rowH;
                String name = mc.ingameGUI.getTabList().getPlayerName(list.get(i));
                FontManager.harmonybold18.drawString(name, x, y, -1);
            }

            if (!contents.isEmpty() || !timedContents.isEmpty()) {
                float renderY = startY + rowsPerCol * rowH + 20;
                for (Content content : this.contents) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(this.renderX, renderY, 0);
                    content.content.run();
                    GlStateManager.popMatrix();
                    renderY += content.height;
                }
                this.contents.clear();
            }
        } else {
            drawDynamicContents();
        }

        RenderUtil.stopGlScissor();
    }


    private void drawDefaultContent() {
        final String defaultText = " | " + LiveClient.INSTANCE.liveUser.getName() + " | " + Minecraft.getDebugFPS() + "fps";
        final boolean showLogo = HUD.islandLogo.getValue();
        final int logoW = 24, logoPadLeft = 2, logoGap = 6;

        int textW = FontManager.harmonybold20.getStringWidth(SilenceFix.NAME) + FontManager.harmonybold20.getStringWidth(defaultText);
        int logoExtra = showLogo ? (logoPadLeft + logoW + logoGap) : 0;
        int totalW = textW + logoExtra;

        float boxW = totalW + 20;
        float boxH = FontManager.harmonybold20.getHeight() + 12;
        float boxX = renderX + (widthTimer.value - boxW) / 2f;
        float boxY = 18;

        RenderUtil.drawRoundedRect((int) boxX, (int) boxY, (int) boxW, (int) boxH, 12, new Color(0,0,0,80));
        ShaderElement.addBlurTask(() -> RenderUtil.drawRoundedRect((int) boxX, (int) boxY, (int) boxW, (int) boxH, 12, new Color(0,0,0,200)));
        ShaderElement.addBloomTask(() -> RenderUtil.drawRoundedRect((int) boxX, (int) boxY, (int) boxW, (int) boxH, 12, new Color(0,0,0,200)));

        float textX = boxX + 10 + (showLogo ? logoW + logoGap : 0);
        float textY = boxY + (boxH - FontManager.harmonybold20.getHeight()) / 2f + 2;

        if (showLogo) {
            ResourceLocation logo = new ResourceLocation("express/SilenceLogo.png");
            RenderUtil.drawImage(logo, boxX + logoPadLeft, boxY, logoW, 24);
        }

        GradientUtil.applyGradientHorizontal(
                textX - 2, boxY, 50, FontManager.harmonybold20.getHeight() + 8, 1,
                color(1), color(4),
                () -> FontManager.harmonybold20.drawString(SilenceFix.NAME, textX, textY, -1)
        );

        FontManager.harmonybold20.drawString(
                defaultText,
                textX + FontManager.harmonybold20.getStringWidth(SilenceFix.NAME),
                textY,
                -1
        );
    }




    private static final Color BG_MAIN    = new Color(20, 20, 20, 110);
    private static final Color BG_ALT     = new Color(35, 35, 35, 100);
    private static final Color BG_FRONT   = new Color(50, 50, 50, 95);
    private static final Color BG_DEFAULT = new Color(40, 40, 40, 100);


    private void drawDynamicContents() {
        float renderY = 15;
        for (Content content : this.contents) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.renderX, renderY, 0);
            content.content.run();
            GlStateManager.popMatrix();
            renderY += content.height;
        }
        this.contents.clear();
    }

    private float getMaxWidth(List<Content> contents) {
        float max = 0;
        for (Content c : contents) {
            if (c.width > max) {
                max = c.width;
            }
        }
        return max;
    }

    private float getTotalHeight(List<Content> contents) {
        float sum = 0;
        for (Content c : contents) {
            sum += c.height;
        }
        return sum;
    }


    public void addIsland(Runnable content, int width, int height, int weight) {
        this.contents.add(new Content(content, width, height, weight));
    }

    public void addIsland(IslandType type, String title, String description, long time) {
        this.timedContents.add(new TimedContent(type, title, description, time));
    }

    public void addIsland(Module module) {
        String name = module.getName();
        if (name.equalsIgnoreCase("Blink")
                || name.equalsIgnoreCase("AutoGapple")
                || name.equalsIgnoreCase("Timer")
                || name.equalsIgnoreCase("Scaffold")) {
            return;
        }

        for (TimedContent content : this.timedContents) {
            if (content.module != null && content.module == module) {
                content.time = content.timer.getTimeElapsed() + 2000;
                return;
            }
        }
        this.timedContents.add(new TimedContent(module));
    }


    public void addIsland(Runnable icon, String title, String description, float present) {
        this.contents.add(new Content(() -> {
            if (icon != null) icon.run();

            FontManager.harmonybold20.drawString(
                    title, width / 2 - FontManager.harmonybold20.getStringWidth(title) / 2,
                    9, new Color(0, 180, 255).getRGB()
            );
            FontManager.harmonybold18.drawString(description, 7, 26, -1);

            int barX = 6;
            int barY = 42;
            int barW = (int) (this.width - 12);
            int barH = 4;

            RenderUtil.drawRoundShadow(barX, barY, barW, barH, barH / 2f, new Color(50, 50, 50, 60));
            RenderUtil.drawGradientRound(
                    barX, barY, barW, barH, barH / 2f,
                    new Color(80, 80, 80, 120),
                    new Color(60, 60, 60, 100)
            );

            int progressW = (int) Math.min(present * barW, barW);
            if (progressW > 0) {
                Color base = mainColor.getColorC();
                Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(), 220);
                RenderUtil.drawGradientHorizontal(
                        barX, barY, progressW, barH, barH / 2f,
                        c, c
                );
            }

        },
                Math.max(
                        250,
                        70 + Math.max(
                                FontManager.harmonybold20.getStringWidth(title),
                                FontManager.harmonybold18.getStringWidth(description)
                        )
                ),
                72, PriorityUtil.LOW));
    }













    public enum IslandType { MODULE, SUCCESS, WARNING, INFO }

    public static class Content {
        private final Runnable content;
        private final int width, height, weight;
        private Content(Runnable content, int width, int height, int weight) {
            this.content = content;
            this.width = width;
            this.height = height;
            this.weight = weight;
        }
    }

    private static class TimedContent {
        private final IslandType type;
        private final Module module;
        private final String title;
        private final EaseOutExpo animation;
        private final TimerUtil timer;
        private String description;
        private long time;

        TimedContent(IslandType type, String title, String description, long time) {
            this.type = type;
            this.module = null;
            this.title = title;
            this.description = description;
            this.animation = new EaseOutExpo(1000, 1);
            this.timer = new TimerUtil();
            this.time = time;
        }

        TimedContent(Module module) {
            this.type = IslandType.MODULE;
            this.module = module;
            this.title = "Module Toggled";
            this.description = module.getName() + (module.state ? " has been §2Enabled" : " has been §4Disabled");
            this.animation = new EaseOutExpo(1000, 1);
            this.timer = new TimerUtil();
            this.time = 500;
        }
    }
}
