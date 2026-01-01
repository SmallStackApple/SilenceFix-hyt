package dev.xinxin.gui.elements;

import dev.xinxin.gui.clickgui.book.RippleAnimation;
import dev.xinxin.utils.client.TimeUtil;
import dev.xinxin.utils.client.menu.Screen;
import dev.xinxin.utils.misc.MouseUtils;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import dev.xinxin.utils.render.fontRender.FontManager;

import java.awt.*;

public class MDUIButton
implements Screen {
    public final String text;
    public final int id;
    private Animation hoverAnimation;
    public float x;
    public float y;
    public float width;
    public float height;
    public Runnable clickAction;
    private RippleAnimation ripple = new RippleAnimation();
    public final Color color;
    private TimeUtil timer = new TimeUtil();

    public MDUIButton(String text, int id, Color color) {
        this.text = text;
        this.id = id;
        this.color = color;
    }

    @Override
    public void initGui() {
        this.hoverAnimation = new DecelerateAnimation(300, 1.0);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        boolean hovered = MouseUtils.isHovering(this.x, this.y, this.width, this.height, mouseX, mouseY);
        this.hoverAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
        RoundedUtils.drawRound(this.x, this.y, this.width, this.height, 4.0f, this.color);
        this.ripple.draw(() -> RoundedUtils.drawRound(this.x, this.y, this.width, this.height, 4.0f, new Color(255, 255, 255)));
        FontManager.arial20.drawCenteredString(this.text, this.x + this.width / 2.0f, this.y + FontManager.arial20.getMiddleOfBox(this.height) + 2.0f, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean hovered = MouseUtils.isHovering(this.x, this.y, this.width, this.height, mouseX, mouseY);
        this.ripple.mouseClicked(mouseX, mouseY);
        if (hovered && this.timer.hasReached(200.0)) {
            this.clickAction.run();
            this.timer.reset();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
    }
}

