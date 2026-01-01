package dev.xinxin.module.modules.world;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.utils.client.PacketUtil;
import net.minecraft.block.BlockGlass;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class AutoEEE extends Module {

    public AutoEEE() {
        super("AutoPhase", Category.Movement,"空岛出笼");
    }
    private int i;
    private boolean sb;
    private boolean c07;

    @Override
    public void onEnable() {
        i = 0;
        sb = false;
        c07 = false;
    }

    @Override
    public void onDisable() {
        if (SilenceFix.instance.moduleManager.getModule(Blink.class).state)
            SilenceFix.instance.moduleManager.getModule(Blink.class).setState(false);
    }

    @EventTarget
    public void onUpdate(EventUpdate eventUpdate) {
        BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);

        if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockGlass) {
            if (!SilenceFix.instance.moduleManager.getModule(Blink.class).state) SilenceFix.instance.moduleManager.getModule(Blink.class).setState(true);

            if (!c07) {
                PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, EnumFacing.DOWN));
                PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.DOWN));
                mc.theWorld.setBlockToAir(pos);
                c07 = true;
            }
        }
    }

    @EventTarget
    public void onTick(EventTick eventTick) {
        if (sb && c07) {
            i++;
        }

        if (i >= 23) {
            setState(false);
        }
    }

    @EventTarget
    public void onPacket(EventPacketReceive eventPacketReceive) {
        if (eventPacketReceive.getPacket() instanceof S02PacketChat) {
            S02PacketChat s02PacketChat = (S02PacketChat) eventPacketReceive.getPacket();

            if (s02PacketChat.getChatComponent().getUnformattedText().contains("开始倒计时: 1 秒")) {
                sb = true;
            }
        }
    }
}
