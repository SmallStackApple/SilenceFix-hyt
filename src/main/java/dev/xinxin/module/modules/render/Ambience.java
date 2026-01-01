package dev.xinxin.module.modules.render;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.client.HelperUtil;
import net.minecraft.entity.effect.EntityLightningBolt;

public class Ambience extends Module {

    private final ModeValue<WeatherMode> weatherMode = new ModeValue<>("Weather-Mode", WeatherMode.values(), WeatherMode.Clear);
    private final NumberValue rainStrength = new NumberValue("Rain-Strength", 1.0, 0.0, 1.0, 0.1);

    private float lastRain = -1;
    private float lastThunder = -1;
    private long lastLightningTime = 0;
    private int lightningStrikeCount = 0;

    public Ambience() {
        super("Ambience", Category.Render, "天气效果");
    }

    @Override
    public void onEnable() {
        lastRain = -1;
        lastThunder = -1;
        lastLightningTime = 0;
        lightningStrikeCount = 0;
        applyWeather();
        lastLightningTime = 0;
        lightningCooldown = getNextCooldown();
    }


    private long getNextCooldown() {
        return 6000 + (long)(Math.random() * 4000); // 6~10秒
    }

    @Override
    public void onDisable() {
        if (mc.theWorld != null) {
            mc.theWorld.setRainStrength(0.0f);
            mc.theWorld.setThunderStrength(0.0f);
        }
        lastRain = -1;
        lastThunder = -1;
    }
    private long lightningCooldown = 0;

    private Object lastWorld = null;

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.theWorld == null) return;
        if (mc.theWorld != lastWorld) {
            lastWorld = mc.theWorld;
            lastRain = -1;
            lastThunder = -1;
            applyWeather();
        }
        if (!mc.inGameHasFocus) return;

        if ((weatherMode.getValue() == WeatherMode.Storm || weatherMode.getValue() == WeatherMode.SnowStorm) && mc.thePlayer != null) {
            long now = System.currentTimeMillis();
            if (now - lastLightningTime >= lightningCooldown) {
                spawnLightningNearPlayer();
                lastLightningTime = now;
                lightningCooldown = getNextCooldown();

                lightningStrikeCount++;
                if (weatherMode.getValue() == WeatherMode.SnowStorm && lightningStrikeCount >= 3) {
                    HelperUtil.sendMessage("§f何树友被雷电劈掉了！");
                    lightningStrikeCount = 0;
                }
            }
        }
    }


    private void applyWeather() {
        if (mc.theWorld == null) return;

        float targetRain = 0.0f;
        float targetThunder = 0.0f;

        switch (weatherMode.getValue()) {
            case Rain:
            case Snow:
            case SnowStorm:
                targetRain = rainStrength.getValue().floatValue();
                break;
            case Storm:
                targetRain = rainStrength.getValue().floatValue();
                targetThunder = 1.0f;
                break;
            case Clear:
            default:
                targetRain = 0.0f;
                targetThunder = 0.0f;
                break;
        }

        if (weatherMode.getValue() == WeatherMode.SnowStorm) {
            targetThunder = 1.0f;
        }

        if (targetRain != lastRain) {
            mc.theWorld.setRainStrength(targetRain);
            lastRain = targetRain;
        }

        if (targetThunder != lastThunder) {
            mc.theWorld.setThunderStrength(targetThunder);
            lastThunder = targetThunder;
        }
    }

    private void spawnLightningNearPlayer() {
        double offsetX = (Math.random() - 0.5) * 10.0;
        double offsetZ = (Math.random() - 0.5) * 10.0;
        double x = mc.thePlayer.posX + offsetX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ + offsetZ;

        mc.theWorld.addWeatherEffect(new EntityLightningBolt(mc.theWorld, x, y, z));
        mc.theWorld.playSound(x, y, z, "ambient.weather.thunder", 100.0f, 1.0f, false);
    }

    public enum WeatherMode {
        Clear, Rain, Snow, Storm, SnowStorm
    }
}
