package dev.xinxin.utils.client.menu;

import dev.xinxin.SilenceFix;
import dev.xinxin.gui.CustomMenuButton;
import dev.xinxin.gui.altmanager.GuiAltManager;
import dev.xinxin.utils.misc.MinecraftInstance;
import dev.xinxin.utils.render.BackgroundTextureManager;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class BetterMainMenu extends GuiScreen implements MinecraftInstance {
    private Animation displayAnimation;
    private final List<CustomMenuButton> buttons;
    private final float buttonWidth = 140;
    private final float buttonHeight = 28.5f;
    private final float buttonSpacing = 5;

    private ResourceLocation bannerTexture = new ResourceLocation("express/icon/banner.png");
    private net.minecraft.client.shader.Framebuffer stencilFramebuffer;

    public BetterMainMenu() {
        displayAnimation = new DecelerateAnimation(1000, 1);
        buttons = Arrays.asList(
                new CustomMenuButton("单人世界"),
                new CustomMenuButton("多人世界"),
                new CustomMenuButton(EnumChatFormatting.BOLD + "" + EnumChatFormatting.YELLOW + "内置进服"),
                new CustomMenuButton("游戏设置"),
                new CustomMenuButton("退出游戏")
        );
        initButtonActions();
    }

    private void initButtonActions() {
        for (CustomMenuButton button : buttons) {
            button.clickAction = () -> handleButtonClick(button.text);
        }
    }

    private void handleButtonClick(String buttonText) {
        switch (buttonText) {
            case "单人世界":
                super.mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case "多人世界":
                super.mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case "游戏设置":
                super.mc.displayGuiScreen(new GuiOptions(this, super.mc.gameSettings));
                break;
            case "§l§e内置进服":
                super.mc.displayGuiScreen(new GuiAltManager(this));
                break;
            case "退出游戏":
                super.mc.shutdown();
                break;
        }
    }

    @Override
    public void initGui() {
        displayAnimation.setDirection(Direction.FORWARDS);
        fadeOutAnimation.setDirection(Direction.BACKWARDS);
        fadeOutAnimation.setOutput(0);
        shouldTransition = false;
        buttons.forEach(CustomMenuButton::initGui);
    }

    private void drawModernButton(CustomMenuButton button, int mouseX, int mouseY) {
        boolean hovered = RenderUtil.isHovering(button.x, button.y, button.width, button.height, mouseX, mouseY);
        Color buttonColor = hovered
                ? new Color(78, 7, 246, 160)
                : new Color(161, 131, 237, 120);

        RoundedUtils.drawRound(button.x, button.y, button.width, button.height, 12f, buttonColor);
        if (hovered) {
            for (int i = 1; i <= 2; i++) {
                RoundedUtils.drawRound(button.x - i, button.y - i, button.width + i * 2, button.height + i * 2, 12f + i,
                        new Color(255, 255, 255, 20));
            }
        }
        int textColor = new Color(255, 255, 255, 240).getRGB();
        FontManager.arial20.drawCenteredString(button.text,
                button.x + button.width / 2f,
                button.y + FontManager.arial20.getMiddleOfBox(button.height) + 1.0f,
                textColor);
    }


    private Animation fadeOutAnimation = new DecelerateAnimation(900, 1.0);
    private String pendingButtonText = null;
    private boolean shouldTransition = false;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        SilenceFix.instance.wallpaperEngine.render(width, height);
        BackgroundTextureManager.renderBackground();

        RenderUtil.drawGradientRect(0, 0, width, height,
                new Color(40, 40, 40, 80).getRGB(),
                new Color(20, 20, 20, 120).getRGB());
        float cardWidth = 420;
        float cardHeight = 380;
        float midX = width / 2f - cardWidth / 2f;
        float midY = height / 2f - cardHeight / 2f;
        RoundedUtils.drawRound(midX, midY, cardWidth, cardHeight, 20f,
                new Color(255, 255, 255, 60));
        for (int i = 1; i <= 4; i++) {
            RoundedUtils.drawRound(midX - i, midY - i, cardWidth + i * 2, cardHeight + i * 2, 20f + i,
                    new Color(255, 255, 255, 8));
        }
        FontManager.arial40.drawCenteredString("SilenceFix Menu",
                width / 2f,
                midY + 40,
                new Color(255, 255, 255, 230).getRGB());
        float buttonY = midY + 100;
        float buttonHeight = 40;
        float buttonSpacing = 15;
        for (CustomMenuButton button : buttons) {
            button.x = width / 2f - buttonWidth / 2f;
            button.y = buttonY;
            button.width = buttonWidth;
            button.height = buttonHeight;

            drawModernButton(button, mouseX, mouseY);
            buttonY += buttonHeight + buttonSpacing;
        }
        if (shouldTransition) {
            float alpha = (float) fadeOutAnimation.getOutput();
            int fadeColor = new Color(0, 0, 0, (int) (alpha * 255)).getRGB();
            drawRect(0, 0, width, height, fadeColor);

            if (fadeOutAnimation.isDone()) {
                handleButtonClick(pendingButtonText);
                shouldTransition = false;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }




    @Override
    public void onGuiClosed() {
        displayAnimation.setDirection(Direction.BACKWARDS);
        if (super.mc.currentScreen == null) {
            super.mc.displayGuiScreen(new BetterMainMenu());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (CustomMenuButton button : buttons) {
            if (RenderUtil.isHovering(button.x, button.y, button.width, button.height, mouseX, mouseY)) {
                pendingButtonText = button.text;
                fadeOutAnimation.setDirection(Direction.FORWARDS);
                shouldTransition = true;
                break;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }



    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
