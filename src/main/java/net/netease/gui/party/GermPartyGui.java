package net.netease.gui.party;

import dev.xinxin.gui.CustomMenuButton;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.netease.gui.DragComponent;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ByteBreaker
 * create 30/01/2024
 */
@Getter
@Setter
public class GermPartyGui extends GuiScreen {
    public static GermPartyGui INSTANCE = new GermPartyGui();
    private final Map<Type, List<CustomMenuButton>> buttons = new HashMap<>();
    private final Map<SubType, GermPartyData> dataMap = new HashMap<>();
    private float x, y, width, height;
    private final DragComponent dragComponent = new DragComponent();
    private static ScaledResolution scaledResolution;
    private Type currentType;
    private SubType currentSubType;
    private GermPartyWindow currentWindow;

    @Override
    public void initGui() {
        currentWindow = null;
        scaledResolution = new ScaledResolution(mc);
        if (currentType == Type.CREATE) {
            this.width = 320f;
            this.height = 150f;
        } else {
            this.width = 530f;
            this.height = 310f;
        }
        this.x = scaledResolution.getScaledWidth() / 2f - width / 2f;
        this.y = scaledResolution.getScaledHeight() / 2f - height / 2f;
        for (CustomMenuButton button : buttons.get(currentType)) {
            button.initGui();
        }

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        dragComponent.setX(x);
        dragComponent.setY(y);
        dragComponent.setWidth(width);
        dragComponent.setHeight(height);
        dragComponent.setLimitHeight(height);
        dragComponent.handleDrag(mouseX, mouseY, 0, false);
        x = dragComponent.getX();
        y = dragComponent.getY();

        RoundedUtils.drawRound(x, y, width, height, 7f, new Color(0, 0, 0, 120));


        switch (currentType) {
            case CREATE:
                float offsetY = 0;
                for (CustomMenuButton button : buttons.get(currentType)) {
                    button.setWidth(100);
                    button.setHeight(35);
                    button.setX(x + width / 2f - button.getWidth() / 2f);
                    button.setY(y + 30 + offsetY);
                    button.drawScreen(mouseX, mouseY, partialTicks);

                    offsetY += button.getHeight() + 15;
                }
                break;
            case MAIN:
                float offsetX = 0;
                for (CustomMenuButton button : buttons.get(currentType)) {
                    button.setWidth(70);
                    button.setHeight(20);
                    button.setX(x + 30 + offsetX);
                    button.setY(y + height - button.getHeight() - 10);
                    button.drawScreen(mouseX, mouseY, partialTicks);

                    offsetX += button.getWidth() + 10;
                }
                break;
        }

        if (currentWindow != null) {
            currentWindow.drawScreen(mouseX, mouseY, partialTicks);
        }
    }


    public void setCurrentType(Type currentType) {
        this.currentType = currentType;
        this.currentWindow = null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        dragComponent.handleDrag(mouseX, mouseY, mouseButton, true);

        for (CustomMenuButton button : buttons.get(currentType)) {
            button.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (currentWindow != null) {
            currentWindow.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public enum Type {
        CREATE,
        MAIN,
        LIST
    }

    public enum SubType {
        LIST,
        INPUT,
        INVITE,
        REQUEST,
        KICK;
        @Getter
        private final Animation animation = new DecelerateAnimation(270, 1).setDirection(Direction.BACKWARDS);

        public void setCurrent() {
            if (GermPartyGui.INSTANCE.getCurrentSubType() != null) {
                GermPartyGui.INSTANCE.getCurrentSubType().animation.setState(false);
            }
            GermPartyGui.INSTANCE.setCurrentSubType(this);
            animation.setState(true);
            GermPartyData data = GermPartyGui.INSTANCE.getDataMap().get(this);
            GermPartyWindow window = new GermPartyWindow(data.getText(), this, data.getButtons(),GermPartyGui.INSTANCE);

            window.setX(50);
            window.setY(50);
            window.setWidth(100f);
            window.setHeight(Math.min(150, 40f + data.getButtons().size() * 18f));

            GermPartyGui.INSTANCE.setCurrentWindow(window);
        }
    }
}
