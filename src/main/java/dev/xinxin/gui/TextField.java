package dev.xinxin.gui;

import com.viaversion.viaversion.libs.mcstructs.core.TextFormatting;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.animation.AnimationUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.MathHelper;

import java.awt.*;

@Getter
public class TextField extends GuiTextField {
    private final String title;
    private final boolean protect;

    private int focx = 0;
    private float focxAnim = 0f;

    public TextField(int componentId, String title, boolean protect) {
        super(componentId);
        this.title = title;
        this.protect = protect;
    }

    long startTime = -1;
    float fadeAlpha = 0f;
    long currentTime = 0;
    public void drawTextBox(String title) {
        boolean focused = this.isFocused();

        if (startTime == -1 && focused) {
            startTime = System.currentTimeMillis();
        }

        if (startTime != -1) {
            currentTime = System.currentTimeMillis() - startTime;
            fadeAlpha = AnimationUtils.easeInOut(currentTime, 0, 255, 400);
        }

        if (currentTime >= 1000) {
            startTime = -1;
        }

        Color borderColor = focused ? new Color(20,20,20,180) : new Color(40,40,40, 150);
        RenderUtil.drawRectWH(xPosition, yPosition, width, height, borderColor.getRGB());
        FontManager.navenbold20.drawString(title,xPosition - FontManager.navenbold20.getStringWidth(title) - 5,yPosition + 4,Color.WHITE.getRGB());
        Minecraft.getMinecraft().fontRendererObj.drawString(this.getText(), xPosition + 4, yPosition + 5, Color.WHITE.getRGB());

        fadeAlpha = Math.max(0, Math.min(255, fadeAlpha));

        if (focused) RenderUtil.drawRectWH(xPosition + 4.5 + Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.getText()), yPosition + 4, 1, height - 8, new Color(255, 255, 255, (int) fadeAlpha).darker().getRGB());

        if (this.getText().isEmpty()) {
            Minecraft.getMinecraft().fontRendererObj.drawString(title, xPosition + 4, yPosition + 5,
                    new Color(180, 180, 180, 150).getRGB());
        }
    }


    @Override
    public void mouseClicked(int p_146192_1_, int p_146192_2_, int p_146192_3_) {
        boolean flag = p_146192_1_ >= this.xPosition && p_146192_1_ < this.xPosition + this.getWidth() && p_146192_2_ >= this.yPosition && p_146192_2_ < this.yPosition + 20;

        if (canLoseFocus) {
            setFocused(flag);
        }

        if (this.isFocused() && flag && p_146192_3_ == 0) {
            int i = p_146192_1_ - this.xPosition;

            if (this.enableBackgroundDrawing) {
                i -= 4;
            }

            String s = FontManager.arial20.trimStringToWidth(this.getText().substring(this.lineScrollOffset), this.getWidth(), false);
            setCursorPosition(FontManager.arial20.trimStringToWidth(protect ? TextFormatting.BOLD + s.replaceAll("(?s).", ".") : s, i, false).length() + this.lineScrollOffset);
        }
    }

    @Override
    public void setSelectionPos(int position) {
        final int length = getText().length();

        if (position > length) position = length;
        if (position < 0) position = 0;

        this.selectionEnd = position;

        if (lineScrollOffset > length) {
            this.lineScrollOffset = length;
        }

        String s = FontManager.arial20.trimStringToWidth(this.getText().substring(this.lineScrollOffset), this.getWidth(), false);
        final int k = (protect ? TextFormatting.BOLD + s.replaceAll("(?s).", ".") : s).length() + lineScrollOffset;

        if (position == lineScrollOffset) {
            this.lineScrollOffset -= FontManager.arial20.trimStringToWidth(getText(), this.getWidth(), true).length();
        }

        if (position > k) {
            this.lineScrollOffset += position - k;
        } else if (position <= this.lineScrollOffset) {
            this.lineScrollOffset -= lineScrollOffset - position;
        }

        this.lineScrollOffset = MathHelper.clamp(lineScrollOffset, 0, length);
    }

    public void update(float x, float y, float width) {
        this.xPosition = (int) x;
        this.yPosition = (int) y;
        this.width = (int) width;
        this.height = 20;
    }
}
