package dev.xinxin.gui;

import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@Setter
@Getter
public class CustomMenuButton extends net.minecraft.client.gui.GuiScreen {
    public String text;
    public float x;
    public float y;
    public float width;
    public float height;
    public Runnable clickAction;
    public RapeMasterFontManager font;
    public boolean disabled;

    private Animation hoverAnimation;
    private Animation clickAnimation;
    private boolean isPressed = false;


    public void drawButton(int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;

        float scale = hovered ? 1.02f : 1.0f;

        GL11.glPushMatrix();
        GL11.glTranslatef(x + width / 2f, y + height / 2f, 0);
        GL11.glScalef(scale, scale, 1.0f);
        GL11.glTranslatef(-(x + width / 2f), -(y + height / 2f), 0);

        Color buttonColor1 = new Color(0, 78, 239);
        Color buttonColor2 = new Color(92, 139, 244);
        RoundedUtils.drawGradientRound(x, y, width, height, 8f, buttonColor1, buttonColor2, buttonColor2, buttonColor1);

        float textY = y + (height - FontManager.chineseFont18.getHeight()) / 2f + 2;
        FontManager.chineseFont18.drawCenteredStringWithShadow(text, x + width / 2f, textY, Color.WHITE.getRGB());

        GL11.glPopMatrix();
    }


    public CustomMenuButton(String text, Runnable clickAction) {
        this.text = text;
        this.font = FontManager.arial20;
        this.clickAction = clickAction;
        this.hoverAnimation = new DecelerateAnimation(300, 1.0);
        this.clickAnimation = new DecelerateAnimation(200, 1.0);
    }

    public CustomMenuButton(String text) {
        this(text, null);
    }

    @Override
    public void initGui() {
        this.hoverAnimation.setDirection(Direction.BACKWARDS);
        this.clickAnimation.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float ticks) {
        boolean hovered = RenderUtil.isHovering(this.x, this.y, this.width, this.height, mouseX, mouseY);
        hoverAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
        float clickScale = 1.0f - 0.03f * (float) clickAnimation.getOutput();
        float hoverScale = 1.0f + 0.05f * (float) hoverAnimation.getOutput();
        float finalScale = hoverScale * clickScale;

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.x + this.width / 2.0f, this.y + this.height / 2.0f, 0);
        GlStateManager.scale(finalScale, finalScale, 1.0f);
        GlStateManager.translate(-(this.x + this.width / 2.0f), -(this.y + this.height / 2.0f), 0);
        Color topColor = hovered ? new Color(255, 255, 255, 120) : new Color(255, 255, 255, 90);
        Color bottomColor = hovered ? new Color(230, 230, 230, 120) : new Color(200, 200, 200, 90);
        RoundedUtils.drawGradientRound(this.x, this.y, this.width, this.height, 18.0f, topColor, bottomColor, bottomColor, topColor);
        Color outlineColor = hovered ? new Color(255, 255, 255, 180) : new Color(255, 255, 255, 100);
        RoundedUtils.drawRoundOutline(this.x, this.y, this.width, this.height, 18.0f, 1.5f, new Color(0,0,0,0), outlineColor);
        int textColor = new Color(255, 255, 255, 230).getRGB();
        font.drawCenteredString(this.text, this.x + this.width / 2.0f, this.y + font.getMiddleOfBox(this.height) + 2.0f, textColor);

        GlStateManager.popMatrix();
    }


    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (disabled) {
            return;
        }

        boolean hovered = RenderUtil.isHovering(this.x, this.y, this.width, this.height, mouseX, mouseY);
        if (hovered) {
            isPressed = true;
            clickAnimation.setDirection(Direction.FORWARDS);
            if (this.clickAction != null) {
                this.clickAction.run();
            }
        }
    }




    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (isPressed) {
            clickAnimation.setDirection(Direction.BACKWARDS);
            isPressed = false;
        }
    }

    @Override
    public void onGuiClosed() {
        hoverAnimation.setDirection(Direction.BACKWARDS);
        clickAnimation.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {}
}
