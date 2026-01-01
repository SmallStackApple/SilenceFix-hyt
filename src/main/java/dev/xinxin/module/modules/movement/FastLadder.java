package dev.xinxin.module.modules.movement;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventLivingUpdate;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.*;

import static dev.xinxin.utils.player.PlaceInfo.getBlock;

public class FastLadder extends Module {


    private List<BlockPos> blockLadder = new ArrayList<>();
    public boolean cancel = false;
    private boolean normalClimb = false;


    public FastLadder() {
        super("FastLadder", Category.Movement, "快速爬梯子");
    }


    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!mc.thePlayer.isOnLadder()) {
            normalClimb = false;
            cancel = false;
            blockLadder.clear();
        }
        if (!mc.thePlayer.isOnLadder() && !cancel) {
            return;
        }
        if (normalClimb && mc.thePlayer.isOnLadder()) {
            return;
        }
        if (mc.thePlayer.isOnLadder() && mc.gameSettings.keyBindJump.isKeyDown()) {
            blockLadder.clear();
            cancel = false;
            normalClimb = true;
            return;
        }
        for (Map.Entry<BlockPos, Block> entry : searchBlocks(4).entrySet()) {
            BlockPos block = entry.getKey();
            Block value = entry.getValue();
            if (value instanceof BlockLadder) {
                if (!blockLadder.contains(block)) {
                    blockLadder.add(block);
                }
            }
        }
        if (!blockLadder.isEmpty()) {
            for (BlockPos block : blockLadder) {
                mc.getNetHandler().getNetworkManager().sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, block, EnumFacing.DOWN));
            }
            if (mc.thePlayer.isOnLadder()) {
                cancel = true;
            }
        }
    }

    public static Block getBlock(BlockPos blockPos) {
        IBlockState blockState;
        if (mc.theWorld != null && (blockState = mc.theWorld.getBlockState(blockPos)) != null) {
            return blockState.getBlock();
        }
        return null;
    }

    @EventTarget
    public void onWorld(EventWorldLoad event) {
        blockLadder.clear();
    }

    public Map<BlockPos, Block> searchBlocks(int radius) {
        Map<BlockPos, Block> blocks = new HashMap<>();

        EntityPlayerSP thethePlayer = mc.thePlayer;
        if (thethePlayer == null) {
            return blocks;
        }

        for (int x = radius; x >= -radius + 1; x--) {
            for (int y = radius; y >= -radius + 1; y--) {
                for (int z = radius; z >= -radius + 1; z--) {
                    BlockPos blockPos = new BlockPos(thethePlayer.posX + x, thethePlayer.posY + y, thethePlayer.posZ + z);
                    Block block = getBlock(blockPos);
                    if (block != null) {
                        blocks.put(blockPos, block);
                    }
                }
            }
        }

        return blocks;
    }
}
