package dev.xinxin.module.modules.world;

import javax.vecmath.Vector2f;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.client.PacketUtil;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;


public class Stuck extends Module {
    private static Stuck INSTANCE;
    public BoolValue antiSB = new BoolValue("OffGround Can't Disable Module", true);
    private double x;
    private double y;
    private double z;
    private double motionX;
    private double motionY;
    private double motionZ;
    private boolean onGround = false;
    private Vector2f rotation;

    public Stuck() {
        super("Stuck", Category.Movement ,"滞空");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) {
            return;
        }
        this.onGround = mc.thePlayer.onGround;
        this.x = mc.thePlayer.posX;
        this.y = mc.thePlayer.posY;
        this.z = mc.thePlayer.posZ;
        this.motionX = mc.thePlayer.motionX;
        this.motionY = mc.thePlayer.motionY;
        this.motionZ = mc.thePlayer.motionZ;
        this.rotation = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        float f = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 1.2f;
        this.rotation.x -= this.rotation.x % gcd;
        this.rotation.y -= this.rotation.y % gcd;
    }

    @EventTarget
    public void onPacket(EventPacketSend event) {
        if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            Vector2f current = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            float f = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            float gcd = f * f * f * 1.2f;
            current.x -= current.x % gcd;
            current.y -= current.y % gcd;
            if (this.rotation.equals(current)) {
                return;
            }
            this.rotation = current;
            event.setCancelled(true);
            PacketUtil.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(current.x, current.y, this.onGround));
            PacketUtil.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
        if (event.getPacket() instanceof C03PacketPlayer) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onPacketR(EventPacketReceive event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.setStateSilent(false);
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        mc.thePlayer.motionX = 0.0;
        mc.thePlayer.motionY = 0.0;
        mc.thePlayer.motionZ = 0.0;
        mc.thePlayer.setPosition(this.x, this.y, this.z);
    }

    public static boolean isStuck() {
        return INSTANCE.getState();
    }

    public void throwPearl(Vector2f current) {
        if (!INSTANCE.getState()) {
            return;
        }
        mc.thePlayer.rotationYaw = current.x;
        mc.thePlayer.rotationPitch = current.y;
        float f = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 1.2f;
        current.x -= current.x % gcd;
        current.y -= current.y % gcd;
        if (!Stuck.INSTANCE.rotation.equals(current)) {
            PacketUtil.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(current.x, current.y, Stuck.INSTANCE.onGround));
        }
        Stuck.INSTANCE.rotation = current;
        PacketUtil.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));

    }
    private boolean closing = false;

    public static void onS08() {
        Stuck.INSTANCE.closing = true;
        INSTANCE.setState(false);
        Stuck.INSTANCE.closing = false;
    }
}

