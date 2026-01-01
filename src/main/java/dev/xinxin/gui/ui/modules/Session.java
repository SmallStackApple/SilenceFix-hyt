package dev.xinxin.gui.ui.modules;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.module.modules.world.PlayerWarn;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.entity.AbstractClientPlayer;

import java.awt.*;

public class Session extends UiModule {
   private long startTime;

   public Session() {
      super("Session", 10.0, 50.0, 190.0, 68.0);
      this.resetTimer();
   }

   @EventTarget
   public void onShader(EventShader e) {
      double x = this.getPosX();
      double y = this.getPosY();
      double width = 190.0;
      double height = 68.0;

      RoundedUtils.drawRound((float)x, (float)y, (float)width, (float)height, 8.0F, new Color(20, 20, 20, 210));
      RoundedUtils.drawRoundOutline((float)x, (float)y, (float)width, (float)height,
              8.0F, 1.5F, new Color(0, 0, 0, 0), new Color(60, 60, 60, 150));
      RoundedUtils.drawRoundOutline((float)x+1, (float)y+1, (float)width-2, (float)height-2,
              7.0F, 0.8F, new Color(0, 0, 0, 0), new Color(80, 80, 80, 80));
      this.drawLine(x, y + 10.0, 2.0, 10.0, HUD.color(0));
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      double x = this.getPosX();
      double y = this.getPosY();
      double width = 190.0;
      double height = 68.0;
      AbstractClientPlayer player2 = mc.thePlayer;
            RoundedUtils.drawRound((float)x, (float)y, (float)width, (float)height, 8.0F, new Color(20, 20, 20, 210));
            this.drawLine(x, y + 10.0, 2.0, 10.0, HUD.color(0));
            if (HUD.langModeValue.is("English")) {
               FontManager.bold22.drawStringDynamic("Session", x + 22.0, y + 10.0, 1, 6);
            } else {
               FontManager.chineseFont20.drawStringDynamic("信息显示", x + 20.0, y + 8.0, 1, 6);
            }
            FontManager.icon22.drawStringDynamic("s", x + 8.0, y + 10.0, 1, 6);
            RoundedUtils.drawRound((float)x + 12, (float)y + 25, 38.0F, 38.0F, 19.0F, new Color(30, 30, 30, 220));
            RenderUtil.drawPlayerHead(player2.getLocationSkin(), (int)x + 13, (int)y + 26, 36, 36);
            FontManager.chineseFont20.drawStringWithShadow(player2.getName(), x + 56.0, y + 30.0, Color.WHITE.getRGB());
            RoundedUtils.drawRound((float)x + 55, (float)y + 45, 130.0F, 18.0F, 4.0F, new Color(30, 30, 30, 180));
            String timeAndBans = this.getTime() + "  •  " + PlayerWarn.banned +
                    (HUD.langModeValue.is("English") ? " Bans" : " 封禁");
            if (HUD.langModeValue.is("English")) {
               FontManager.chineseFont18.drawStringWithShadow(
                       timeAndBans,
                       x + 58.0,
                       y + 48.0,
                       new Color(200, 200, 200).getRGB()
               );
            } else {
               FontManager.chineseFont18.drawStringWithShadow(
                       timeAndBans,
                       x + 58.0,
                       y + 48.0,
                       new Color(200, 200, 200).getRGB()
               );
            }
   }

   private void resetTimer() {
      this.startTime = System.currentTimeMillis();
   }

   private String getTime() {
      long currentTime = System.currentTimeMillis();
      long elapsed = currentTime - this.startTime;
      int seconds = (int)(elapsed / 1000L) % 60;
      int minutes = (int)(elapsed / 60000L) % 60;
      int hours = (int)(elapsed / 3600000L);

      if (hours > 0) {
         return String.format("%dh %02dm", hours, minutes);
      } else {
         return String.format("%02dm %02ds", minutes, seconds);
      }
   }

   public void drawLine(double x, double y, double width, double height, Color color) {
      RoundedUtils.drawRound((float)x, (float)y, (float)width, (float)height,
              Math.min((float)width, (float)height)/2, color);
   }
}