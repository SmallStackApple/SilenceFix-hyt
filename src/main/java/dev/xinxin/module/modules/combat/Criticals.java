package dev.xinxin.module.modules.combat;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.attack.EventAttack;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventStep;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.client.MathUtil;
import dev.xinxin.utils.client.TimeUtil;
import dev.xinxin.utils.player.PlayerUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.util.BlockPos;
public class Criticals extends Module {
    private final TimeUtil timer = new TimeUtil();
    private final TimeUtil prevent = new TimeUtil();
    private final ModeValue<modeEnums> modeValue = new ModeValue("Mode", (Enum[])modeEnums.values(), (Enum)modeEnums.Hypixel);
    private final NumberValue hurtTimeValue = new NumberValue("HurtTime", 15.0, 0.0, 20.0, 1.0);
    private final NumberValue delayValue = new NumberValue("Delay", 3.0, 0.0, 10.0, 0.5);
    private final BoolValue debug = new BoolValue("DeBug",false);
    private int groundTicks;

    private int target = 0;
    private String string = "";

    public Criticals() {
        super("Criticals", Category.Combat,"刀刀暴击");
    }
    @Override
    public void onEnable() {
        this.timer.reset();
        this.prevent.reset();
        this.groundTicks = 0;
        target = 0;
        string = "";
    }

    @EventTarget
    void onUpdate(EventMotion event) {
        this.setSuffix(((modeEnums)((Object)this.modeValue.getValue())).toString());
        this.groundTicks = PlayerUtil.isOnGround(0.01) ? ++this.groundTicks : 0;
        if (this.groundTicks > 20) {
            this.groundTicks = 20;
        }
        if (this.modeValue.getValue() == modeEnums.NoGround) {
            event.setOnGround(false);
        }
    }

    @EventTarget
    void onStep(EventStep event) {
        if (!event.isPre()) {
            this.prevent.reset();
        }
    }

    @EventTarget
    void onAttack(EventAttack event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        boolean canCrit;
        boolean bl = canCrit = this.groundTicks > 3 && mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)).getBlock().isFullBlock() && !PlayerUtil.isInLiquid() && !PlayerUtil.isOnLiquid() && !mc.thePlayer.isOnLadder() && mc.thePlayer.ridingEntity == null && mc.thePlayer.onGround;

        EntityLivingBase entity = (EntityLivingBase) event.getTarget();

        if (entity != null) {
            target = entity.getEntityId();
            string = event.getTarget().getName();
        }

        if (event.isPre() && canCrit && event.getTarget().hurtResistantTime <= ((Double)this.hurtTimeValue.getValue()).intValue() && this.prevent.hasPassed(300L) && this.timer.hasPassed((long)((Double)this.delayValue.getValue()).intValue() * 100L)) {
            switch (((modeEnums)((Object)this.modeValue.getValue())).toString().toLowerCase()) {
                case "hypixel": {
                    double[] values;
                    for (double value : values = new double[]{0.0625 + Math.random() / 100.0, 0.03125 + Math.random() / 100.0}) {
                        mc.getNetHandler().getNetworkManager().sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + value, mc.thePlayer.posZ, false));
                    }
                    break;
                }
                case "hvh": {
                    for (double offset : new double[]{0.06253453, 0.02253453, 0.001253453, 1.135346E-4}) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ, false));
                    }
                    break;
                }
                case "packet": {
                    break;
                }
                case "grim": {
                    break;
                }
                case "packets": {
                    double[] values = new double[]{0.0425, 0.0015, MathUtil.getRandom().nextBoolean() ? 0.012 : 0.014};
                    if (mc.thePlayer.ticksExisted % 2 != 0) break;
                    for (double value : values) {
                        double random = MathUtil.getRandom().nextBoolean() ? MathUtil.getRandom(-1.0E-8, -1.0E-7) : MathUtil.getRandom(1.0E-7, 1.0E-8);
                        mc.getNetHandler().getNetworkManager().sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + value + random, mc.thePlayer.posZ, false));
                    }
                    break;
                }
                case "visual": {
                    mc.thePlayer.onCriticalHit(event.getTarget());
                    break;
                }
                case "jump": {
                    mc.thePlayer.jump();
                    break;
                }
                case "hop": {
                    mc.thePlayer.motionY = 0.1;
                    mc.thePlayer.fallDistance = 0.1f;
                    mc.thePlayer.onGround = false;
                }
            }
        }
    }

    @EventTarget
    private void onPacketReceive(EventPacketReceive event) {
        if (mc.theWorld == null) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof S0BPacketAnimation) {
            S0BPacketAnimation animation = (S0BPacketAnimation) packet;
            if (animation.getAnimationType() == 4 && animation.getEntityID() == target) {
                if (debug.getValue()) {
                    sendCritAlert();
                }
            }
        }
    }

    private void sendCritAlert() {
        String message = String.format("§f§l暴击提示>§c§l%s", string);
        HelperUtil.sendMessage(message);
    }

    private static enum modeEnums {
        Packet,
        Packets,
        Hypixel,
        HvH,
        Hop,
        Jump,
        Grim,
        Visual,
        NoGround;

    }
}

