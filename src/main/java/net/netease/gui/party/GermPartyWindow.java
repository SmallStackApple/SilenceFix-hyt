package net.netease.gui.party;

import dev.xinxin.gui.CustomMenuButton;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.netease.gui.DragComponent;
import net.netease.gui.Scroll;

import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * @author ByteBreaker
 * create 02/02/2024
 */
@Getter
@Setter
public class GermPartyWindow {
    private final String text;
    private final GermPartyGui.SubType type;
    private final List<CustomMenuButton> buttons;
    private float x, y, width, height;
    private final DragComponent dragComponent = new DragComponent();
    private final Scroll scroll = new Scroll();
    private GuiScreen prevGui;

    public GermPartyWindow(String text, GermPartyGui.SubType type, List<CustomMenuButton> buttons,GuiScreen prevGui) {
        this.text = text;
        this.type = type;
        this.buttons = buttons;
        this.prevGui = prevGui;
    }


    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RapeMasterFontManager font16 = FontManager.arial16;
        RapeMasterFontManager bold18 = FontManager.arial18;
        dragComponent.setX(x);
        dragComponent.setY(y);
        dragComponent.setWidth(width);
        dragComponent.setHeight(height);
        dragComponent.setLimitHeight(height);
        dragComponent.handleDrag(mouseX, mouseY, 0, false);
        x = dragComponent.getX();
        y = dragComponent.getY();
        RoundedUtils.drawRound(x, y, width, height, 7f, new Color(0, 0, 0, 120));


        bold18.drawString(text, x + 6, y + 8, -1);
        bold18.drawString("●", x + width - 14, y + 8, new Color(233, 30, 99).getRGB());

        RenderUtil.startGlScissor((int) x, (int) y + 22, (int) width, (int) height - 22);
        float offsetY = 0;
        for (CustomMenuButton button : buttons) {
            button.setFont(font16);
            button.setWidth(font16.getStringWidth(button.getText()) + 15);
            button.setHeight(font16.getHeight() + 10);
            button.setX(x + width / 2 - button.getWidth() / 2);
            button.setY(y + 22 + offsetY + scroll.getAnimationTarget());
            button.drawScreen(mouseX, mouseY, partialTicks);
            offsetY += button.getHeight() + 5;
        }

        scroll.setMaxTarget(offsetY - height);
        if (RenderUtil.isHovering(x, y, width, height, mouseX, mouseY)) {
            scroll.use();
        }
        scroll.animate();
        scroll.use();
        RenderUtil.stopGlScissor();
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        dragComponent.handleDrag(mouseX, mouseY, mouseButton, true);
        if (RenderUtil.isHovering(x + width - 14, y + 8, 12, 12, mouseX, mouseY)) {
            Minecraft.getMinecraft().displayGuiScreen(prevGui);
        }
        if (!RenderUtil.isHovering(x, y + 22, width, height, mouseX, mouseY)) return;
        for (CustomMenuButton button : buttons) {
            button.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
}
