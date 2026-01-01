package dev.xinxin.module.modules.movement;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.TimerUtil;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.player.MoveUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.impl.SmoothStepAnimation;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.util.ChatComponentText;

import java.awt.*;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Timer extends Module {
    public final BoolValue render = new BoolValue("Render", false);
    final ConcurrentLinkedQueue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final NumberValue amount = new NumberValue("Amount", 5, 0.1, 10, 0.1);
    private final BoolValue balance = new BoolValue("Balance", false);
    private final Animation anim = new SmoothStepAnimation(250, 1);
    private final LinkedList<Packet<NetHandlerPlayClient>> inBus = new LinkedList<>();
    private final TimerUtil timerUtil = new TimerUtil();
    private int count = 0;



    public Timer() {
        super("Timer", Category.Player, "时间管理大师");
    }

    @EventTarget
    private void onMotion(EventMotion e) {
        if (e.isPre()) {
            if (!balance.getValue()) {
                mc.timer.timerSpeed = amount.getValue().floatValue();
            } else {
                PacketUtil.sendPacketNoEvent(new C0FPacketConfirmTransaction(0, (short) 0, true));
                if (count >= 0) {
                    mc.timer.timerSpeed = MoveUtil.isMoving() ? amount.getValue().floatValue() : 1f;
                } else {
                    toggle();
                }
            }
        }
        if (timerUtil.hasTimeElapsed(800, true)) {
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("§e[SilenceTimer-Packets]§e:" + count));
        }
    }

    @EventTarget
    private void onPacket(EventPacketReceive event) {
        if (event.getPacket() instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity) event.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
            toggle();
        }
        if (event.getPacket() instanceof S18PacketEntityTeleport && ((S18PacketEntityTeleport) event.getPacket()).getEntityId() == mc.thePlayer.getEntityId()) {
            toggle();
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            toggle();
        }
        if (event.getPacket() instanceof S32PacketConfirmTransaction && mc.thePlayer.hurtTime == 0) {
            event.setCancelled(true);
            inBus.add((Packet<NetHandlerPlayClient>) event.getPacket());
            mc.getNetHandler().addToSendQueue(new C0FPacketConfirmTransaction(0, (short) 514, true));
        }
    }

    @EventTarget
    private void onPacket(EventPacketSend event) {
        if (event.getPacket() instanceof C03PacketPlayer) {
            if (!((C03PacketPlayer) event.getPacket()).isMoving()) {
                count += 30;
                event.setCancelled(true);
            } else {
                count -= 30;
            }
        }
        if (event.getPacket() instanceof C0FPacketConfirmTransaction) {
            event.setCancelled(true);
            packets.add(event.getPacket());
        }
        if (event.getPacket() instanceof C02PacketUseEntity && ((C02PacketUseEntity) event.getPacket()).getAction() == C02PacketUseEntity.Action.ATTACK) {
            toggle();
        }
    }

    @Override
    public void onDisable() {
        if (!packets.isEmpty()) {
            packets.forEach(PacketUtil::sendPacketNoEvent);
            packets.clear();
        }
        try {
            while (!inBus.isEmpty()) {
                inBus.poll().processPacket(mc.getNetHandler());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        count = 0;
        mc.timer.timerSpeed = 1;
        super.onDisable();
    }


    private float smoothProgress = 0;
    private long lastUpdateTime = System.currentTimeMillis();
    int barHeight = 6;

    @EventTarget
    public void onRender2D(EventRender2D event) {
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;
        int barWidth = 140;
        int barHeight = 6;
        int barX = centerX - barWidth / 2;
        int barY = centerY + 30;
        float cornerRadius = 4.0f;
        int maxPackets = 6000;
        float progress = Math.min(1.0f, count / (float) maxPackets);
        int currentWidth = (int) (progress * barWidth);

        GlStateManager.disableAlpha();
        drawBackgroundShadow(barX - 2, barY - 2, barWidth + 4, barHeight + 4, cornerRadius + 2);
        drawProgressBarBackground(barX, barY, barWidth, barHeight, cornerRadius);
        if (currentWidth > 0) {
            Color hudColor = HUD.color(0);
            Color gradientStart = new Color(hudColor.getRed(), hudColor.getGreen(), hudColor.getBlue(), 220);
            Color gradientEnd = new Color(hudColor.getRed(), hudColor.getGreen(), hudColor.getBlue(), 180);

            drawProgressBar(barX, barY, currentWidth, barHeight, cornerRadius, gradientStart, gradientEnd);
            Color highlightColor = new Color(255, 255, 255, 50);
            drawHighlightEffect(barX, barY, currentWidth, (int) (barHeight / 2f), cornerRadius, highlightColor);
        }
        String tipText = "正在攒包中...";
        float tipTextX = centerX - FontManager.chineseFont16.getStringWidth(tipText) / 2f;
        float tipTextY = barY - 12; // 位置在进度条上方
        FontManager.chineseFont16.drawString(tipText, tipTextX + 0.6f, tipTextY + 0.6f,
                new Color(0, 0, 0, 150).getRGB());
        FontManager.chineseFont16.drawString(tipText, tipTextX, tipTextY,
                new Color(230, 230, 230, 240).getRGB());
        drawPacketCountText(centerX, barY);
        drawProgressBarOutline(barX, barY, barWidth, barHeight, cornerRadius);

        GlStateManager.enableAlpha();
    }
    private void drawBackgroundShadow(int barX, int barY, int barWidth, int barHeight, float cornerRadius) {
        RoundedUtils.drawGradientRound(barX - 2, barY - 2, barWidth + 4, barHeight + 4, cornerRadius + 1,
                new Color(0, 0, 0, 100), new Color(0, 0, 0, 50),
                new Color(0, 0, 0, 50), new Color(0, 0, 0, 100));
    }
    private void drawProgressBarBackground(int barX, int barY, int barWidth, int barHeight, float cornerRadius) {
        RoundedUtils.drawGradientRound(barX, barY, barWidth, barHeight, cornerRadius,
                new Color(25, 25, 25, 220), new Color(15, 15, 15, 200),
                new Color(15, 15, 15, 200), new Color(25, 25, 25, 220));
    }
    private void drawProgressBar(int barX, int barY, int currentWidth, int barHeight, float cornerRadius, Color startColor, Color endColor) {
        RoundedUtils.drawGradientRound(
                barX,
                barY,
                currentWidth,
                barHeight,
                cornerRadius,
                startColor, // 左上
                endColor,   // 右上
                endColor,   // 右下
                startColor  // 左下
        );
    }
    private void drawHighlightEffect(int barX, int barY, int currentWidth, int barHeight, float cornerRadius, Color hudColor) {
        Color highlightColor = new Color(hudColor.getRed(), hudColor.getGreen(), hudColor.getBlue(), 80);
        RoundedUtils.drawGradientRound(barX, barY, currentWidth, barHeight / 2, cornerRadius, highlightColor, new Color(255, 255, 255, 30),
                new Color(255, 255, 255, 30), new Color(255, 255, 255, 80));
    }
    private void drawPacketCountText(int centerX, int barY) {
        String text = String.format("%,d packets", count);
        FontManager.chineseFont18.drawStringWithShadow(text,
                centerX - FontManager.chineseFont18.getStringWidth(text) / 2f,
                barY + barHeight + 10,
                new Color(240, 240, 255, 255).getRGB());
    }
    private void drawProgressBarOutline(int barX, int barY, int barWidth, int barHeight, float cornerRadius) {
        RoundedUtils.drawRoundOutline(barX, barY, barWidth, barHeight, cornerRadius, 1.0f,
                new Color(40, 40, 40, 0), new Color(120, 90, 90, 116));
    }

}
