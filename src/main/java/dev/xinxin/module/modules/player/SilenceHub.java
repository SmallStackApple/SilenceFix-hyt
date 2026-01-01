package dev.xinxin.module.modules.player;

import dev.xinxin.SilenceFix;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.AutoProjectile;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.combat.SilenceCrit;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.movement.Speed;
import dev.xinxin.module.modules.world.AutoEEE;
import dev.xinxin.module.modules.world.ChestAura;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.utils.TimerUtil;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SilenceHub extends Module {
    private final TimerUtil delayTimer = new TimerUtil();
    private static final Set<Module> autoDisabledModules = new HashSet<>();

    public SilenceHub() {
        super("SilenceHub", Category.Misc, "快速逃逸");
    }

    @Override
    public void onEnable() {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            disableAllModules();
            if (delayTimer.delay(300)) {
                mc.thePlayer.sendChatMessage("/hub");
                reenableAutoProjectile();
                autoDisabledModules.clear();

                setState(false);
            }
        });
    }

    private void disableAllModules() {
        autoDisabledModules.clear();
        Module autoProjectile = SilenceFix.instance.moduleManager.getModule(AutoProjectile.class);
        if (autoProjectile != null && autoProjectile.getState()) {
            autoProjectile.setState(false);
            autoDisabledModules.add(autoProjectile);
        }
        for (Class<? extends Module> mClass : getDisableModules()) {
            if (mClass == AutoProjectile.class) continue;

            final Module module = SilenceFix.instance.moduleManager.getModule(mClass);
            if (module != null) {
                module.setState(false);
            }
        }
    }
    private void reenableAutoProjectile() {
        for (Module module : autoDisabledModules) {
            if (module != null && !module.getState()) {
                module.setState(true);
            }
        }
    }

    public static ArrayList<Class<? extends Module>> getDisableModules() {
        final ArrayList<Class<? extends Module>> list = new ArrayList<>();

        list.add(KillAura.class);
        list.add(ChestStealer.class);
//        list.add(SilenceCrit.class);
        list.add(AutoGapple.class);
        list.add(AutoEEE.class);
        list.add(InvCleaner.class);
        list.add(Speed.class);
        list.add(ChestAura.class);
        list.add(Scaffold.class);
        list.add(AutoProjectile.class); // 包含在列表中但特殊处理
        return list;
    }
}