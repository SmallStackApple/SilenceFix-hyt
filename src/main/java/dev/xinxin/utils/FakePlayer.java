package dev.xinxin.utils;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

import static dev.xinxin.utils.player.RotationUtil.mc;

public class FakePlayer extends EntityOtherPlayerMP {
    private final EntityPlayer player;
    public static int idIndex = 0;

    public FakePlayer(EntityPlayer player) {
        super(mc.theWorld, player.getGameProfile());
        this.player = player;

        this.copyLocationAndAnglesFrom(player);
        this.setHealth(player.getHealth());
        this.setAbsorptionAmount(player.getAbsorptionAmount());

        this.setPositionAndRotation(
                player.posX,
                player.posY,
                player.posZ,
                player.rotationYaw,
                player.rotationPitch
        );

        this.rotationYaw = player.rotationYaw;
        this.rotationPitch = player.rotationPitch;
        this.rotationYawHead = player.rotationYawHead;

        mc.theWorld.addEntityToWorld(--idIndex, this);

        if (idIndex <= -100000) {
            idIndex = -1;
        }
    }

    @Override
    public boolean isInvisibleToPlayer(EntityPlayer player) {
        return this.isInvisible();
    }

    @Override
    public boolean isInvisible() {
        return mc.scheduledTasks.size() <= 3;
    }

    @Override
    public void onUpdate() {
        if (player == null || !player.isEntityAlive()) {
            mc.theWorld.removeEntity(this);
        }
        this.setSprinting(false);
        super.onUpdate();
    }
}
