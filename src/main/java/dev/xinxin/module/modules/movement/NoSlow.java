package dev.xinxin.module.modules.movement;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.*;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.world.Stuck;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.render.ProgressManager.ProgressBarManager;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.awt.*;



public class NoSlow extends Module {
        boolean isSlow, startedEat;
        private final ModeValue<NoSlowMode> mode = new ModeValue<>("Mode", NoSlowMode.values(), NoSlowMode.Vanilla);
        private final ModeValue<foodmod> foodMode = new ModeValue<>("FoodMode", foodmod.values(), foodmod.SW);
        private int eatTicks;
        private int tempAppleCount;

        public static boolean blocked;

        public NoSlow() {
                super("NoSlow", Category.Movement, "无减速","None Speed with you","修改你的移动速度");
        }




        public static boolean xinXin = false;

        private static final int GAPPLE_TICKS = 32;      // 32 tick 满

        @EventTarget
        public void onRender2D(EventRender2D event) {
                boolean notEatingNow =
                        !startedEat ||
                                (mc.thePlayer.getHeldItem() != null &&
                                        !(mc.thePlayer.getHeldItem().getItem() instanceof ItemAppleGold));

                if (notEatingNow) {
                        ProgressBarManager.remove("GappleEat");
                        return;
                }

                // 到达 32 tick：满条 → 触发淡出
                if (eatTicks >= GAPPLE_TICKS) {
                        startedEat = false;
                        // 可选：若想先显示满条 100% 再淡出，可先更新到 100
                        // ProgressBarManager.update("GappleEat", 100f, 100f);
                        ProgressBarManager.remove("GappleEat");
                        return;
                }

                // 以百分比为单位驱动（0–100）
                final float maxPercent = 100f;
                ProgressBarManager.create(
                        "GappleEat",
                        () -> Math.min(maxPercent, (eatTicks / (float) GAPPLE_TICKS) * 100f),
                        maxPercent,
                        true,                 // 显示百分比文本
                        "吃金苹果",            // 附加文字（可设为 ""）
                        10                    // 层级/排序（根据你的 HUD 需要调整）
                );

                // 若你已有全局转发到 ProgressBarManager，这行请删除
                ProgressBarManager.onRender2D(event);
        }



        @EventTarget
        public void onSlowDown(EventSlowDown e) {
                if (e.getType() == EventSlowDown.Type.Item) {
                        if (SilenceFix.instance.moduleManager.getModule(Stuck.class).getState()) {
                                return;
                        }

                        ItemStack itemStack = mc.thePlayer.getHeldItem();
                        if (itemStack == null) return;

                        e.setCancelled(!isSlow || mode.getValue() == NoSlowMode.Vanilla);
                        if (mc.thePlayer.isUsingItem() && mc.thePlayer.moveForward >= 0.0f) {
                                mc.thePlayer.setSprinting(true);
                        }
                }
        }

        @EventTarget
        public void onPacketReceiveFood(EventPacketSend event) {
                if (SilenceFix.instance.moduleManager.getModule(Stuck.class).getState()) {
                        return;
                }

                ItemStack stack = mc.thePlayer.getHeldItem();
                if (stack == null) return;

                Packet<?> packet = event.getPacket();
                if (mode.getValue() == NoSlowMode.Grim) {
                        switch (foodMode.getValue()) {
                                case SW -> {
                                        if (stack.getItem() instanceof ItemAppleGold && stack.stackSize - 1 >= 2) {
                                                if (packet instanceof C08PacketPlayerBlockPlacement blockPlacement && blockPlacement.getPosition().getY() == -1 && !isSlow) {
                                                        PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                                                        eatTicks = 0;
                                                        tempAppleCount = blockPlacement.getStack().stackSize;
                                                        isSlow = true;
                                                        startedEat = true;
                                                }

                                                if (packet instanceof C07PacketPlayerDigging c07 && c07.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM && startedEat) {
                                                        event.setCancelled(true);
                                                }
                                        } else {
                                                if (!(stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow) && packet instanceof C08PacketPlayerBlockPlacement blockPlacement && blockPlacement.getPosition().getY() == -1 && !isSlow) {
                                                        isSlow = true;
                                                }
                                        }

                                        if (packet instanceof C09PacketHeldItemChange) {
                                                eatTicks = 0;
                                                startedEat = false;
                                        }
                                }
                                case BW -> {
                                        if (stack.getItem() instanceof ItemFood || stack.getItem() instanceof ItemPotion) {
                                                if (packet instanceof C08PacketPlayerBlockPlacement blockPlacement && blockPlacement.getPosition().getY() == -1 && !isSlow) {
                                                        mc.thePlayer.sendChatMessage("/lizi open");
                                                        isSlow = true;
                                                }

                                        }
                                }
                        }
                }
        }

        @EventTarget
        public void onTick(EventTick event) {
                if (startedEat && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemAppleGold) {
                        eatTicks++;
                        int stackSize = mc.thePlayer.getHeldItem().stackSize;
                        if (stackSize <= 0) {

                                eatTicks = 0;
                                startedEat = false;
                        }
                        if (stackSize < tempAppleCount - 1) {
                                eatTicks = 0;
                                startedEat = false;
                        }
                } else {
                        eatTicks = 0;
                }
                if (eatTicks >= 1){
                        mc.gameSettings.keyBindUseItem.pressed = false; 
                }
        }

        @EventTarget
        public void onPacket(EventPacketReceive eventPacketReceive) {
                if (isSlow){
                        if (eventPacketReceive.getPacket() instanceof S2DPacketOpenWindow openWindow){
                                if (openWindow.getWindowTitle() != null && openWindow.getWindowTitle().getFormattedText().equals("§c§l          [- 特效系统- ]§r")){
                                        eventPacketReceive.setCancelled();
                                }
                        }
                }
                if (SilenceFix.instance.moduleManager.getModule(Stuck.class).getState()) {
                        return;
                }

                ItemStack stack = mc.thePlayer.getHeldItem();
                if (stack == null) return;

                Packet<?> packet = eventPacketReceive.getPacket();

                if (mode.getValue() == NoSlowMode.Grim && foodMode.getValue() == foodmod.BW) {
                        if (stack.getItem() instanceof ItemPotion ||
                                (stack.getItem() instanceof ItemAppleGold)) {
                                if (packet instanceof S2DPacketOpenWindow) {
                                        eventPacketReceive.setCancelled();
                                        xinXin = true;
                                        isSlow = false;
                                }
                        }
                }
        }

        @EventTarget
        public void onUpdate(EventUpdate eventUpdate) {
                this.setSuffix(this.mode.getValue().toString());

                if (SilenceFix.instance.moduleManager.getModule(Stuck.class).getState()) {
                        return;
                }

                ItemStack stack = mc.thePlayer.getHeldItem();
                if (mc.thePlayer == null || stack == null) return;

                if (stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow) {
                        isSlow = false;
                }

                if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                        isSlow = false;

                        if (foodMode.getValue() == foodmod.BW && xinXin) {
                                mc.thePlayer.closeScreen();
                                xinXin = false;
                        }
                }
        }

        @EventTarget
        public void onUpdateGrimPre(EventMotion e) {
                if (mc.thePlayer == null) return;

                ItemStack stack = mc.thePlayer.getHeldItem();
                if (stack == null) return;

                if (getModule(AutoGapple.class).state) return;

                if (mode.getValue() == NoSlowMode.Grim) {
                        if (e.isPre()) {
                                if ((stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow) && (mc.thePlayer.isUsingItem() || KillAura.isBlocking)) {
                                        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem + 1));
                                        mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload("sbhyt", new PacketBuffer(Unpooled.buffer())));
                                        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                                }
                        }
                        if (e.isPost()) {
                                if ((stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow) && (mc.thePlayer.isUsingItem() || KillAura.isBlocking)) {
                                        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                                }
                        }
                }
        }

        enum NoSlowMode {
                Vanilla,
                Grim
        }

        enum foodmod {
                BW,
                SW
        }
}
