package dev.xinxin.module.modules.player;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.world.ChestAura;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.HelperUtil;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.client.TimeUtil;
import dev.xinxin.utils.player.BlockUtil;
import dev.xinxin.utils.player.RotationNew;
import dev.xinxin.utils.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

import java.util.List;
import java.util.Map;

import static dev.xinxin.module.modules.world.Scaffold.getVec3;

public class Helper extends Module {

    public Helper() {
        super("Helper", Category.Player, "助手");
    }

    private static final BoolValue tnt = new BoolValue("Anti TNT", false);
    private static final BoolValue fire = new BoolValue("Anti Fire", true);
    private boolean isMine;
    private final TimeUtil timeUtil = new TimeUtil();

    @EventTarget
    public void onTick(EventTick e) {
        if (tnt.getValue()) {
            EntityTNTPrimed incoming = HelperUtil.findThreateningTnt(45.0, 0.18, 80, 28.0);

            if (incoming != null) {
                if(HelperUtil.isTntWithin(incoming,4)){
                    mc.gameSettings.keyBindUseItem.setPressed(true);
                }
                HelperUtil.TntIncomingInfo info = HelperUtil.computeTntIncomingAndShield(incoming);
                if (info != null) {
                    mc.thePlayer.inventory.currentItem = HelperUtil.getBlockSlot();
//                    List<BlockPos> placeable = HelperUtil.getFourDirectionBlocks();
                    float reversedYaw = info.reversedYaw;      // 你要的“反转后的 yaw”
                    List<BlockPos> placeable = info.shieldPositions; // 优先把第一个点尝试放方块
                    if (!placeable.isEmpty()) {
                        for (BlockPos pos : placeable) {
                            RotationNew block = RotationUtil.getRotationBlock(pos, 0.3f);
                            mc.gameSettings.keyBindForward.setPressed(true);

                            mc.thePlayer.rotationYaw = block.getYaw();
                            RotationComponent.setRotation(block, 180, true);

                            Vec3 baseVec = mc.thePlayer.getPositionEyes(2F);
                            checkBlock(baseVec, pos);
                            Vec3 hitvec = getVec3(blockPos, enumFacing);

                            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), blockPos, enumFacing, hitvec);
                            if (timeUtil.delay(150)) return;
                            mc.gameSettings.keyBindForward.setPressed(false);
                        }
                    }
                    if (placeable.isEmpty()) {
                        mc.thePlayer.rotationYaw = -reversedYaw;
                    }
                }
            }
        }
    }


    private BlockPos blockPos;
    private EnumFacing enumFacing;

    private void checkBlock(Vec3 baseVec, BlockPos pos) {
        if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) return;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (EnumFacing face : EnumFacing.values()) {
            Vec3 hit = center.add(new Vec3(face.getDirectionVec()).scale(0.5));
            Vec3i baseBlock = pos.add(face.getDirectionVec());
            if (!mc.theWorld.getBlockState(new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ())).getBlock().isSca())
                continue;
            Vec3 relevant = hit.subtract(baseVec);
            if (relevant.lengthSquared() <= 4.5 * 4.5 && relevant.dotProduct(
                    new Vec3(face.getDirectionVec())) >= 0) {
                blockPos = new BlockPos(baseBlock);
                enumFacing = face.getOpposite();
                return;
            }
        }
    }

    @EventTarget
    public void onUpdate(EventMotion e) {
        if (fire.getValue()) {
            Map<BlockPos, Block> searchBlock = BlockUtil.searchBlocks(4);
            for (Map.Entry<BlockPos, Block> block : searchBlock.entrySet()) {
                if (mc.theWorld.getBlockState(block.getKey()).getBlock() == Blocks.fire) {
                    RotationNew rotation = RotationUtil.getRotationBlock(block.getKey(), 0.4f);
                    if (getModule(ChestAura.class).needRot != null || getModule(Scaffold.class).state || KillAura.target != null || isMine)
                        return;
                    RotationComponent.setRotation(rotation, 666666666, true);
                    PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, block.getKey(), EnumFacing.DOWN));
                    PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, block.getKey(), EnumFacing.DOWN));
                }
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
