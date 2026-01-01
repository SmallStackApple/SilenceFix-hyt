package dev.xinxin.gui.ui.modules;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.attack.EventAttack;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

import java.awt.*;

public class BlockRote extends UiModule {
   private static final long FADE_DURATION = 500;
   private static final long PROGRESS_ANIM_DURATION = 500;
   private static final int MAX_ALPHA = 180;
   private static final float PROGRESS_BAR_RADIUS = 2.0f;
   private static final float PROGRESS_BAR_HEIGHT = 5.0f;
   private int targetBlockV = 0;
   private float animatedBlockV = 0;
   private int slot = -1;
   private long lastActiveTime = 0;
   private long animationStartTime = 0;
   private float lastAlpha = 0;

   public BlockRote() {
      super("BlockRate", 100.0, 100.0, 120.0, 40.0);
   }

   @EventTarget
   public void onShader(EventShader e) {
      float alpha = calculateCurrentAlpha();
      if (alpha <= 0.01f) return;

      double x = this.getPosX();
      double y = this.getPosY();
      RoundedUtils.drawRound(
              (float)x, (float)y, (float)this.getWidth(), (float)this.getHeight(),
              6.0F, new Color(20, 20, 20, (int)alpha)
      );
      float barX = (float)x + 5;
      float barY = (float)(y + this.getHeight() - 10);
      float barWidth = (float)this.getWidth() - 10;
      RoundedUtils.drawRound(
              barX, barY, barWidth, PROGRESS_BAR_HEIGHT,
              PROGRESS_BAR_RADIUS, new Color(50, 50, 50, (int)alpha)
      );
      this.drawLine(x, y + 10.0, 2.0, 10.0, HUD.color(0));
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      updateAnimationValues();
      float alpha = calculateCurrentAlpha();
      if (alpha <= 0.01f && animatedBlockV <= 0.01f) {
         return;
      }

      double x = this.getPosX();
      double y = this.getPosY();

      String string = targetBlockV > 0 ? "BlockIng: " + (targetBlockV * 10) + "%" : "NoBlockIng";
      if (HUD.langModeValue.is("Chinese")) {
         string = targetBlockV > 0 ? "防砍率: " + (targetBlockV * 10) + "%" : "无防砍";
      }
      RoundedUtils.drawRound((float)x, (float)y, (float)this.getWidth(), (float)this.getHeight(),
              6.0F, new Color(20, 20, 20, (int)alpha));
      float barX = (float)x + 5;
      float barY = (float)(y + this.getHeight() - 10);
      float barWidth = (float)this.getWidth() - 10;

      RoundedUtils.drawRound(barX, barY, barWidth, PROGRESS_BAR_HEIGHT,
              PROGRESS_BAR_RADIUS, new Color(50, 50, 50, (int)alpha));
      if (animatedBlockV > 0) {
         float progress = animatedBlockV * 10.0f;
         float progressWidth = progress * barWidth / 100.0f;

         RoundedUtils.drawGradientRound(barX, barY, progressWidth, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_RADIUS,
                 new Color(80, 230, 220, (int)alpha), new Color(0, 200, 180, (int)alpha),
                 new Color(0, 190, 200, (int)alpha), new Color(0, 200, 190, (int)alpha));
      }
      this.drawLine(x, y + 10.0, 2.0, 10.0, HUD.color(0));
      String swordIcon = "s";
      FontManager.icon22.drawStringDynamic(
              swordIcon,
              x + 8,
              y + 10,
              0,
              10
      );
      FontManager.chineseFont18.drawStringWithShadow(
              string,
              x + 30,
              y + 10,
              new Color(255, 255, 255, (int)alpha).getRGB()
      );
   }

   private long lastBlockTime = 0;
   private boolean wasBlocking = false;
   private static final int BLOCK_INCREASE_STEP = 1;
   private static final int BLOCK_DECREASE_STEP = 1;
   @EventTarget
   public void onUpdate(EventUpdate eventUpdate) {
      if (Minecraft.getMinecraft().thePlayer == null) return;
      boolean isBlocking = KillAura.isBlocking ||
              (Minecraft.getMinecraft().thePlayer.isBlocking() &&
                      Minecraft.getMinecraft().thePlayer.getHeldItem() != null &&
                      Minecraft.getMinecraft().thePlayer.getHeldItem().getItem() instanceof ItemSword);

      boolean isEatingGapple = AutoGapple.eating;
      boolean justAteGapple = isEatingGapple &&
              System.currentTimeMillis() - lastGappleTime < 300;
      if (isBlocking && !justAteGapple) {
         if (targetBlockV < 10) {
            targetBlockV = Math.min(10, targetBlockV + BLOCK_INCREASE_STEP);
            animationStartTime = System.currentTimeMillis();
            lastActiveTime = System.currentTimeMillis();
         }
      } else {
         if (targetBlockV > 0) {
            targetBlockV = Math.max(0, targetBlockV - BLOCK_DECREASE_STEP);
            animationStartTime = System.currentTimeMillis();
            if (targetBlockV == 0) {
               lastActiveTime = System.currentTimeMillis();
            }
         }
      }
   }
   private void updateAnimationValues() {
      long currentTime = System.currentTimeMillis();
      long elapsed = currentTime - animationStartTime;

      if (elapsed < PROGRESS_ANIM_DURATION) {
         float progress = (float)elapsed / PROGRESS_ANIM_DURATION;
         animatedBlockV = lerp(animatedBlockV, targetBlockV, progress);
      } else {
         animatedBlockV = targetBlockV;
      }
   }
   private long lastGappleTime = 0;

   @EventTarget
   public void onPacketSend(EventPacketSend e) {
      if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && AutoGapple.eating) {
         lastGappleTime = System.currentTimeMillis();
      }
   }
   private long lastAttackTime = 0;

   @EventTarget
   public void onAttack(EventAttack e) {
      lastAttackTime = System.currentTimeMillis();
   }
   private float calculateCurrentAlpha() {
      if (targetBlockV > 0) {
         lastAlpha = MAX_ALPHA;
         return MAX_ALPHA;
      }

      long elapsed = System.currentTimeMillis() - lastActiveTime;
      if (elapsed >= FADE_DURATION) {
         return 0;
      }

      float progress = (float)elapsed / FADE_DURATION;
      progress = 1 - (1 - progress) * (1 - progress);
      lastAlpha = MAX_ALPHA * (1 - progress);
      return lastAlpha;
   }
   private float lerp(float start, float end, float progress) {
      return start + (end - start) * progress;
   }
   public void drawLine(double x, double y, double width, double height, Color color) {
      RoundedUtils.drawRound((float)x, (float)y, (float)width, (float)height,
              Math.min((float)width, (float)height)/2, color);
   }

}