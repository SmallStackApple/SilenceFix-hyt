
package dev.xinxin.module.modules.movement;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventMove;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.player.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.Map;

public class NoWeb extends Module {

    public NoWeb() {
        super("NoWeb", Category.Movement, "无蜘蛛网");
    }

    @EventTarget
    public void onUpdate(EventMotion e) {
        if (getModule(Blink.class).getState()) return;
        if (mc.thePlayer == null) return;
        if (e.isPost()) return;
        Map<BlockPos, Block> searchBlock = BlockUtil.searchBlocks(3);
        for (Map.Entry<BlockPos, Block> block : searchBlock.entrySet()) {
            if (mc.theWorld.getBlockState(block.getKey()).getBlock() instanceof BlockWeb) {
                PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, block.getKey(), EnumFacing.DOWN));
                PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, block.getKey(), EnumFacing.DOWN));
                mc.thePlayer.isInWeb = false;
            }
        }
    }
}
