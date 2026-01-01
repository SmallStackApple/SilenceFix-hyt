package dev.xinxin.module.modules.movement;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.*;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.player.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.Map;


public class NoLiquid extends Module {

    public NoLiquid() {
        super("NoLiquid", Category.Movement, "无液体");
    }
    boolean isMine;
    @EventTarget
    public void onUpdate(EventMotion e) {
        if (getModule(Blink.class).getState()) return;
        if (mc.thePlayer == null) return;
        if (e.isPost()) return;
        if (isMine) return;
        if (KillAura.target !=null) return;
        if (mc.thePlayer.isInWater()) return;

        Map<BlockPos, Block> searchBlock = BlockUtil.searchBlocks(8);
        for (Map.Entry<BlockPos, Block> block : searchBlock.entrySet()) {
            if (mc.theWorld.getBlockState(block.getKey()).getBlock() instanceof BlockLiquid) {
                PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, block.getKey(), EnumFacing.DOWN));
                PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, block.getKey(), EnumFacing.DOWN));
                mc.theWorld.setBlockToAir(block.getKey());
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacketSend event){
        if (event.getPacket() instanceof C07PacketPlayerDigging packet){
            if(packet.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) isMine = true;
            else if (packet.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK || packet.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK){
                isMine = false;
            }
        }
    }
}

