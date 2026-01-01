package dev.xinxin.module.modules.render;

import dev.xinxin.SilenceFix;
import dev.xinxin.SilenceFixSoundManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.attack.EventAttack;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.utils.render.animation.impl.ContinualAnimation;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;

import java.util.Random;

import static dev.xinxin.api.utils.RandomUtils.nextFloat;

public final class KillEffect extends Module {
    private final ModeValue<killEffectRenderModes> killEffectValue = new ModeValue<>("KillEffect", killEffectRenderModes.values(), killEffectRenderModes.Squid);
    private final ModeValue<killEffectSoundModes> killSoundValue = new ModeValue<>("KillSound", killEffectSoundModes.values(), killEffectSoundModes.Squid);
    private final ModeValue<TriggerMode> triggerModeValue = new ModeValue<>(
            "TriggerMode",
            TriggerMode.values(),
            TriggerMode.Kill // 默认为击杀触发
    );

    private final BoolValue tipsKillsValue = new BoolValue("TipsKills", false);

    private int kills = 0;
    private EntityLivingBase target;
    private EntitySquid squid;
    private double percent = 0.0;
    private final ContinualAnimation anim = new ContinualAnimation();
    private static final Random random = new Random(); // 使用独立的Random实例

    public KillEffect() {
        super("KillEffect", Category.Render, "击杀特效");
    }
    public double easeInOutCirc(double x2) {
        return x2 < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * x2, 2.0))) / 2.0
                : (Math.sqrt(1.0 - Math.pow(-2.0 * x2 + 2.0, 2.0)) + 1.0) / 2.0;
    }

    @Override
    public void onDisable() {
        resetKills();
    }

    @Override
    public void onEnable() {
        resetKills();
    }

    @EventTarget
    public void onWorld(EventWorldLoad event) {
        resetKills();
    }

    private void resetKills() {
        if (tipsKillsValue.getValue()) {
            kills = 0;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (KillEffect.mc == null || KillEffect.mc.theWorld == null || KillEffect.mc.thePlayer == null) {
            return;
        }
        if (triggerModeValue.getValue() == TriggerMode.Kill) {
            if (target != null && target.getHealth() <= 0.0f && !KillEffect.mc.theWorld.loadedEntityList.contains(target)) {
                handleTargetDeath();
            }
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (event.getTarget() != null) {
            this.target = (EntityLivingBase) event.getTarget();
            if (triggerModeValue.getValue() == TriggerMode.Attack) {
                handleTargetEffect();
            }
        }
    }




    private void handleSquidEffect() {
        if (!KillEffect.mc.theWorld.loadedEntityList.contains(squid)) {
            squid = null; // 清理无效实体
            return;
        }
        if (percent < 1.0) {
            percent += Math.random() * 0.048;
        }
        if (percent >= 1.0) {
            percent = 0.0;
            spawnFlameParticles(8); // 优化：限制粒子数量
            KillEffect.mc.theWorld.removeEntity(squid);
            squid = null;
            return;
        }

        double easeInOutCirc = this.easeInOutCirc(1.0 - percent);
        anim.animate((float) easeInOutCirc, 450);
        squid.setPositionAndUpdate(squid.posX, squid.posY + (double) anim.getOutput() * 0.9, squid.posZ);
        squid.squidPitch = 0.0f;
        squid.prevSquidPitch = 0.0f;
        squid.squidYaw = 0.0f;
        squid.squidRotation = 90.0f;
    }

    private void handleTargetEffect() {
        try {
            killEffectRenderModes mode = killEffectValue.getValue();
            switch (mode) {
                case Flame:
                    spawnFlameParticles(1); // 优化：限制火焰粒子数量
                    break;
                case Smoke:
                    spawnSmokeParticles(1);
                    break;
                case Water:
                    spawnWaterParticles(1);
                    break;
                case Love:
                    spawnHeartParticles(1);
                    spawnWaterParticles(1);
                    break;
                case Blood:
                    spawnBloodParticles();
                    break;
                case LightningBolt:
                    spawnLightningBolt();
                    break;
                case Squid:
                    break;
                case Off:
                    break;
            }
            target = null; // 清理目标
        } catch (Exception e) {
        }
    }

    private void handleTargetDeath() {
        if (!tipsKillsValue.getValue()) return;

        kills++;
        NotificationManager.post(NotificationType.SUCCESS, "Kills +1", "Killed " + kills + " Players.");

        if (killSoundValue.getValue() == killEffectSoundModes.Squid) {
            SilenceFix.instance.soundManager.playSound(SilenceFixSoundManager.SoundType.KILL, 0.6f);
        }
        switch (killEffectValue.getValue()) {
            case LightningBolt:
                spawnLightningBolt();
                break;
            case Squid:
                spawnSquid();
                break;
        }
        target = null;
    }
    private void spawnFlameParticles(int count) {
        if (KillEffect.mc.effectRenderer == null) return;
        for (int i = 0; i < count; i++) {
            KillEffect.mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.FLAME);
        }
    }

    private void spawnSmokeParticles(int count) {
        if (KillEffect.mc.effectRenderer == null) return;
        for (int i = 0; i < count; i++) {
            KillEffect.mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.SMOKE_LARGE);
        }
    }

    private void spawnWaterParticles(int count) {
        if (KillEffect.mc.effectRenderer == null) return;
        for (int i = 0; i < count; i++) {
            KillEffect.mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.WATER_DROP);
        }
    }

    private void spawnHeartParticles(int count) {
        if (KillEffect.mc.effectRenderer == null) return;
        for (int i = 0; i < count; i++) {
            KillEffect.mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.HEART);
        }
    }

    private void spawnBloodParticles() {
        if (KillEffect.mc.effectRenderer == null) return;
        for (int i = 0; i < 10; i++) {
            KillEffect.mc.effectRenderer.spawnEffectParticle(
                    EnumParticleTypes.BLOCK_CRACK.getParticleID(),
                    target.posX,
                    target.posY + (double) (target.height / 2.0f),
                    target.posZ,
                    target.motionX + (double) nextFloat(-0.5f, 0.5f),
                    target.motionY + (double) nextFloat(-0.5f, 0.5f),
                    target.motionZ + (double) nextFloat(-0.5f, 0.5f),
                    Block.getStateId(Blocks.redstone_block.getDefaultState())
            );
        }
    }

    private void spawnLightningBolt() {
        try {
            EntityLightningBolt bolt = new EntityLightningBolt(KillEffect.mc.theWorld, target.posX, target.posY, target.posZ);
            KillEffect.mc.theWorld.addEntityToWorld(-Math.abs(random.nextInt()), bolt); // 使用固定负ID避免冲突
            playThunderSounds();
        } catch (Exception e) {
        }
    }

    private void playThunderSounds() {
        KillEffect.mc.theWorld.playSound(
                KillEffect.mc.thePlayer.posX,
                KillEffect.mc.thePlayer.posY,
                KillEffect.mc.thePlayer.posZ,
                "ambient.weather.thunder",
                1.0f,
                1.0f,
                false
        );
        KillEffect.mc.theWorld.playSound(
                KillEffect.mc.thePlayer.posX,
                KillEffect.mc.thePlayer.posY,
                KillEffect.mc.thePlayer.posZ,
                "random.explode",
                1.0f,
                1.0f,
                false
        );
    }

    private void spawnSquid() {
        try {
            squid = new EntitySquid(KillEffect.mc.theWorld);
            KillEffect.mc.theWorld.addEntityToWorld(-8, squid);
            squid.setPosition(target.posX, target.posY, target.posZ);
        } catch (Exception e) {
        }
    }
    public static enum TriggerMode {
        Attack,  // 每次攻击触发
        Kill     // 只有击杀时触发
    }

    public static enum killEffectSoundModes {
        Squid,
        Off
    }

    public static enum killEffectRenderModes {
        LightningBolt,
        Flame,
        Smoke,
        Water,
        Love,
        Blood,
        Squid,
        Off
    }
}