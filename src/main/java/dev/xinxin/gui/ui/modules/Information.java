package dev.xinxin.gui.ui.modules;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.ArrayList;


public class Information
extends UiModule {
    public Information() {
        super("Information", 50.0, 20.0, 100.0, 20.0);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        RapeMasterFontManager f18 = FontManager.harmonybold18;
        double positionX = 2; // 从左边缘的偏移量
        double positionY = event.getScaledResolution().getScaledHeight() - 10;
        ArrayList<Integer> positionLongList = new ArrayList<Integer>();
        positionLongList.add(f18.getStringWidth("X: " + (int)Information.mc.thePlayer.posX + " "));
        positionLongList.add(f18.getStringWidth("Y: " + (int)Information.mc.thePlayer.posY + " "));
        positionLongList.add(f18.getStringWidth("Z: " + (int)Information.mc.thePlayer.posZ + " "));
        int mainTextColor = HUD.color(1).getRGB();
        f18.drawStringWithShadow("X", positionX, positionY, mainTextColor);
        f18.drawStringWithShadow(": " + (int)Information.mc.thePlayer.posX, positionX + (double)f18.getStringWidth("X"), positionY, Color.white.getRGB());
        f18.drawStringWithShadow("Y", positionX + (double)((Integer)positionLongList.get(0)).intValue(), positionY, mainTextColor);
        f18.drawStringWithShadow(": " + (int)Information.mc.thePlayer.posY, positionX + (double)f18.getStringWidth("Y") + (double)((Integer)positionLongList.get(0)).intValue(), positionY, Color.white.getRGB());
        f18.drawStringWithShadow("Z", positionX + (double)((Integer)positionLongList.get(1)).intValue() + (double)((Integer)positionLongList.get(0)).intValue(), positionY, mainTextColor);
        f18.drawStringWithShadow(": " + (int)Information.mc.thePlayer.posZ, positionX + (double)f18.getStringWidth("Z") + (double)((Integer)positionLongList.get(1)).intValue() + (double)((Integer)positionLongList.get(0)).intValue(), positionY, Color.white.getRGB());
        f18.drawStringWithShadow("Fps", positionX, positionY -= f18.getStringHeight() + 2.0, mainTextColor);
        f18.drawStringWithShadow(": " + Minecraft.getDebugFPS(), positionX + (double)f18.getStringWidth("Fps"), positionY, Color.white.getRGB());
    }
}

