package dev.xinxin.module.modules.combat;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.CriticalsEvent;
import dev.xinxin.event.world.EventMoveInput;
import dev.xinxin.event.world.EventStrafe;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.movement.Speed;
import dev.xinxin.module.modules.world.Scaffold;
import dev.yalan.live.silencefix.LiveClient;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldSettings;

public class SilenceCrit extends Module {
//    private final BoolValue display = new BoolValue("Display Criticals", true);
    boolean gappleNoGround = false;

    public SilenceCrit() {
        super("Criticals", Category.Combat,"刀刀暴击","Every Attack Triggers Critical Damage.","每次攻击必定造成暴击伤害。");
    }

    public static boolean isSpectator() {
        return mc.playerController != null &&
                mc.playerController.getCurrentGameType() != null &&
                mc.playerController.getCurrentGameType() == WorldSettings.GameType.SPECTATOR;
    }

    @Override
    public void onDisable() {
        if (mc.theWorld != null) {
            mc.theWorld.skiptick = 0;
        }
    }


    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) {
            this.reset();
            return;
        }
        if (isSpectator()) {
            return; // 旁观者模式下不执行暴击逻辑
        }

        if (SilenceFix.instance.moduleManager.getModule(AutoGapple.class).state) return;
        if (SilenceFix.instance.moduleManager.getModule(Speed.class).state) return;
        if (SilenceFix.instance.moduleManager.getModule(Scaffold.class).state) return;

        if (KillAura.target == null) {
            return;
        }

        if (this.cantCrit(KillAura.target) ||
                (KillAura.target instanceof EntityPlayer && isTrustedPlayer((EntityPlayer) KillAura.target))) {
            this.reset(); // 目标无法暴击，或是自己人则不触发暴击逻辑
        } else {
            KillAura aura = SilenceFix.instance.moduleManager.getModule(KillAura.class);
            if (KillAura.target != null) {
                if (!this.isNull()
                        && mc.thePlayer.motionY < 0.0
                        && !mc.thePlayer.onGround
                        && aura.getState()
                        && mc.thePlayer.getClosestDistanceToEntity(KillAura.target) <= 2.0f) {

                    if (++mc.theWorld.skiptick > 20) {
                        mc.theWorld.skiptick = 20;
                    }
                } else if (!this.isNull() && !aura.getState()) {
                    this.reset();
                }
            }
        }
    }

    @Override
    public void onEnable() {
        if (isSpectator()) {
            this.setState(false); // 立即关闭
            return;
        }
    }


    public static boolean isTrustedPlayer(EntityPlayer player) {
        if (player == null) return false;
        String name = player.getName();
        boolean onlineTrusted = player.liveUser != null && "HeShuYou".equals(player.liveUser.getName());
        boolean offlineTrusted = LiveClient.INSTANCE != null && LiveClient.INSTANCE.getOfflineTrustedMap().containsKey(name);
        if (onlineTrusted) {
            return LiveFriendly.Instance != null && LiveFriendly.Instance.getState();
        }
        if (offlineTrusted) {
            return LiveFriendly.Instance != null && LiveFriendly.Instance.getState();
        }

        return false;
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (isSpectator()) {
            return;
        }
        if (SilenceFix.instance.moduleManager.getModule(AutoGapple.class).state)return;
        if (SilenceFix.instance.moduleManager.getModule(Speed.class).state)return;
        if (SilenceFix.instance.moduleManager.getModule(Scaffold.class).state)return;

        if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.pressed && KillAura.target != null && mc.thePlayer.getClosestDistanceToEntity(KillAura.target) <= 2.0f) {
            mc.thePlayer.jump();
        }
    }

    @EventTarget
    public void onCritical(CriticalsEvent event) {

    }

    public boolean cantCrit(EntityLivingBase targetEntity) {
        EntityPlayerSP player = mc.thePlayer;
        return player.isOnLadder() || player.isInWeb || player.isInWater() || player.isInLava() || player.ridingEntity != null || targetEntity.hurtTime > 10 || targetEntity.getHealth() <= 0.0f || this.gappleNoGround;
    }

    private void reset() {
        if (mc.theWorld != null) {
            mc.theWorld.skiptick = 0;
        }
    }  }

