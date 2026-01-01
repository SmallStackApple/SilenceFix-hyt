package dev.xinxin.module.modules.player;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.misc.Disabler;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.render.ProgressManager.ProgressBarManager;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldSettings;

import java.awt.*;

public class SpeedMine extends Module {
    private final NumberValue speed = new NumberValue("Speed", 1.1, 1.0, 3.0, 0.1);
    private final BoolValue speedCheckBypass = new BoolValue("VanillaCheckBypass", false);
    private final BoolValue showProgress = new BoolValue("ShowProgress", true);
    private final BoolValue disableInAdventure = new BoolValue("DisableInAdventure", true);
    private EnumFacing facing;
    private BlockPos pos;
    private boolean boost = false;
    private float damage = 0.0f;

    public SpeedMine() {
        super("SpeedMine", Category.Player, "快速挖掘");
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode) {
            this.setState(false);
            NotificationManager.post(NotificationType.WARNING, "SpeedMine", "创造模式NONONO");
            return;
        }
    }

    public boolean isBoosting() {
        return this.boost && this.pos != null;
    }


    @Override
    public void onDisable() {
        if (mc.thePlayer == null || mc.thePlayer.capabilities.isCreativeMode) {
            return;
        }
        if (this.speedCheckBypass.getValue()) {
            mc.thePlayer.removePotionEffect(Potion.digSpeed.id);
        }
    }

    @EventTarget
    private void onPacket(EventPacketSend e) {
        if (mc.thePlayer == null || mc.thePlayer.capabilities.isCreativeMode) {
            return;
        }
        if (disableInAdventure.getValue() && mc.playerController.getCurrentGameType() == WorldSettings.GameType.ADVENTURE) {
            return;
        }

        if (e.packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging packet = (C07PacketPlayerDigging) e.packet;
            if (packet.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                this.boost = true;
                this.pos = packet.getPosition();
                this.facing = packet.getFacing();
                this.damage = 0.0f;
            } else if (packet.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK ||
                    packet.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                this.boost = false;
                this.pos = null;
                this.facing = null;
            }
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate e) {
        if (mc.thePlayer == null || mc.thePlayer.capabilities.isCreativeMode) {
            return;
        }
        if (disableInAdventure.getValue() && mc.playerController.getCurrentGameType() == WorldSettings.GameType.ADVENTURE) {
            return;
        }
        if (this.pos != null && this.boost) {
            if (mc.objectMouseOver == null ||
                    mc.objectMouseOver.getBlockPos() == null ||
                    !mc.objectMouseOver.getBlockPos().equals(this.pos)) {
                this.boost = false;
                this.pos = null;
                this.damage = 0.0f;
            }
        }
        if (this.speedCheckBypass.getValue()) {
            mc.thePlayer.addPotionEffect(new PotionEffect(Potion.digSpeed.id, 89640, 2));
        }
        if (mc.playerController.extendedReach()) {
            mc.playerController.blockHitDelay = 0;
        } else if (this.pos != null && this.boost) {
            IBlockState blockState = mc.theWorld.getBlockState(this.pos);
            this.damage = (float) (this.damage + blockState.getBlock().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, this.pos) * this.speed.getValue());
            if (this.damage >= 1.0f) {
                mc.theWorld.setBlockState(this.pos, Blocks.air.getDefaultState(), 11);
                Disabler disabler = this.getModule(Disabler.class);
                PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.pos, this.facing));
                PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.pos, this.facing));
                this.damage = 0.0f;
                this.boost = false;
            }
        }
    }

    // 成员字段（放到类里）
    private static final String DIG_BAR_ID = "DiggingProgress";
    private float diggingPercent = 0f;                    // 动态进度（0–100）
    private boolean diggingBarCreated = false;            // 是否已注册进度条
    private final java.util.function.Supplier<Float> diggingSupplier = () -> diggingPercent; // 持久 Supplier

//    @EventTarget
//    public void onRender2D(EventRender2D event) {
//        // 退出条件：创造/冒险(可选)/未开启显示/未 boost/无方块位置
//        if (mc.thePlayer == null || mc.thePlayer.capabilities.isCreativeMode
//                || (disableInAdventure.getValue()
//                && mc.playerController.getCurrentGameType() == WorldSettings.GameType.ADVENTURE)
//                || !showProgress.getValue() || !boost || pos == null) {
//
//            if (diggingBarCreated) {
//                ProgressBarManager.remove(DIG_BAR_ID);
//                diggingBarCreated = false;
//            }
//            return;
//        }
//
//        // 计算百分比（0–100）
//        final float percent = Math.max(0f, Math.min(100f, (float)(damage * 100f)));
//        this.diggingPercent = percent; // 更新“活体数值”，Supplier 每帧都会读到新值
//
//        // 满值：先显示到 100%，下一帧（或本帧后续逻辑处）移除
//        if (percent >= 100f) {
//            if (diggingBarCreated) {
//                ProgressBarManager.remove(DIG_BAR_ID);
//                diggingBarCreated = false;
//            }
//            return;
//        }
//
//        // 首次创建（只创建一次，保持同一个 Supplier）
//        if (!diggingBarCreated) {
//            ProgressBarManager.create(
//                    DIG_BAR_ID,
//                    diggingSupplier, // 关键：持久 Supplier 读取成员变量 diggingPercent
//                    100f,
//                    true,
//                    "正在挖掘...",
//                    10
//            );
//            diggingBarCreated = true;
//        }
//
//        // 若你的项目已有统一 HUD 转发，这行删掉避免重复
//        ProgressBarManager.onRender2D(event);
//    }



}