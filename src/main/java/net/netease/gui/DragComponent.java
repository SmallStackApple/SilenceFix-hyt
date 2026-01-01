package net.netease.gui;

import dev.xinxin.utils.render.RenderUtil;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.input.Mouse;

/**
 * @author ByteBreaker
 * create 13/10/2023
 */
@Getter
@Setter
public class DragComponent {
    private float x, y, width, height, dragX, dragY;
    private float limitHeight;
    private boolean dragging, dragged;

    public void handleDrag(int mouseX, int mouseY, int mouseButton, boolean onMouse) {
        if (onMouse) {
            if ((RenderUtil.isHovering(x, y, width, limitHeight, mouseX, mouseY)) && mouseButton == 0) {
                dragging = true;
                dragged = true;
                this.dragX = mouseX - x;
                this.dragY = mouseY - y;
            }
        } else {
            if (dragging) {
                if (!Mouse.isButtonDown(0)) this.dragging = false;
                x = mouseX - dragX;
                y = mouseY - dragY;
            }
        }
    }
}
