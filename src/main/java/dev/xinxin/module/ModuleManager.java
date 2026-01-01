package dev.xinxin.module;

import cn.dev.annotations.JNICExclude;
import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.misc.EventKey;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.module.modules.combat.*;
import dev.xinxin.module.modules.config.*;
import dev.xinxin.module.modules.misc.*;
import dev.xinxin.module.modules.movement.*;
import dev.xinxin.module.modules.player.*;
import dev.xinxin.module.modules.render.*;
import dev.xinxin.module.modules.world.*;
import dev.xinxin.module.values.Value;
import dev.yalan.live.silencefix.IRCModule;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<Module>();
    private boolean enabledNeededMod = true;



    private void addModule(Module module) {
        for (Field field : module.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object obj = field.get(module);
                if (!(obj instanceof Value)) continue;
                module.getValues().add((Value)obj);
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        module.onRegister();
        this.modules.add(module);
    }

    public static List<Module> getModulesInType(Category t) {
        ArrayList<Module> output = new ArrayList<Module>();
        for (Module m : modules) {
            if (m.getCategory() != t) continue;
            output.add(m);
        }
        return output;
    }
    public List<Module> getModsByPage(Category.Pages m) {
        ArrayList<Module> output = new ArrayList<Module>();
        for (Module module : modules) {
            if (module.getCategory().pages != m) continue;
            output.add(module);
        }
        return output;
    }
    @JNICExclude
    public List<Module> getModules() {
        return this.modules;
    }
    @JNICExclude
    public static <T extends Module> T getModule(Class<T> cls) {
        for (Module m : modules) {
            if (m.getClass() != cls) continue;
            return (T)m;
        }
        return null;
    }
    @JNICExclude
    public Module getModule(String name) {
        for (Module m : this.modules) {
            if (!m.getName().equalsIgnoreCase(name)) continue;
            return m;
        }
        return null;
    }
    @JNICExclude
    public boolean haveModules(Category category, String key) {
        ArrayList<Module> array = new ArrayList<Module>(this.modules);
        array.removeIf(module -> module.getCategory() != category);
        array.removeIf(module -> !module.getName().toLowerCase().replaceAll(" ", "").contains(key));
        return array.size() == 0;
    }
    @JNICExclude
    @EventTarget
    public void onKey(EventKey e) {
        for (Module m : this.modules) {
            if (m.getKey() != e.getKey() || e.getKey() == -1) continue;
            m.toggle();
        }
    }

    public List<Module> getModsByCategory(Category m) {
        ArrayList<Module> findList = new ArrayList<Module>();
        for (Module mod : this.modules) {
            if (mod.getCategory() != m) continue;
            findList.add(mod);
        }
        return findList;
    }

    @EventTarget
    private void on2DRender(EventRender2D e) {
        if (this.enabledNeededMod) {
            this.enabledNeededMod = false;
            for (Module m : this.modules) {
                if (!m.isDefaultOn()) continue;
                m.setState(true);
            }
        }
    }


    public void init() {
        EventManager.register(this);
        System.out.println("Init Modules...");
        this.addModule(new AutoClicker());
        this.addModule(new AntiBot());
        this.addModule(new AntiFireBall());
        this.addModule(new AutoSoup());
        this.addModule(new AutoWeapon());
        this.addModule(new SilenceCrit());
        this.addModule(new MCF());
        this.addModule(new KillAura());
        this.addModule(new Reach());
        this.addModule(new SuperKnockback());
        this.addModule(new FastLadder());
        this.addModule(new Velocity());
        this.addModule(new BowAimbot());
        this.addModule(new NoSlow());
        this.addModule(new Speed());
        this.addModule(new AntiVoid());
//        this.addModule(new AutoEEE());
        this.addModule(new Sprint());
        this.addModule(new Dead());
        this.addModule(new LiveFriendly());
        this.addModule(new AutoGapple());
        this.addModule(new Fly());
//        this.addModule(new Strafe());
        this.addModule(new Gapple());
//        this.addModule(new Timer());
//        this.addModule(new TargetStrafe());
        addModule(new BalancedTimer());
        this.addModule(new GuiMove());
        this.addModule(new BlockESP());
        this.addModule(new BlockHit());
        this.addModule(new Camera());
        this.addModule(new DamageParticle());
        this.addModule(new SilenceHub());
        this.addModule(new Chams());
        this.addModule(new ClickGui());
//        this.addModule(new ChinaHat());
        this.addModule(new ESP());
        this.addModule(new GlowESP());
        this.addModule(new NameTags());
        this.addModule(new FullBright());
        this.addModule(new Health());
        this.addModule(new HUD());
        this.addModule(new ItemPhysics());
        this.addModule(new Skeletal());
        this.addModule(new KillEffect());
        this.addModule(new PostProcessing());
        this.addModule(new MotionBlur());
//        this.addModule(new Trail());
        this.addModule(new XRay());
        this.addModule(new Projectile());
        this.addModule(new MoBendsMod());
        this.addModule(new Ambience());
        this.addModule(new AutoPlay());
        this.addModule(new PacketFix());
        this.addModule(new Disabler());
        this.addModule(new Protocol());
        this.addModule(new ConfigSW());
        this.addModule(new ConfigSWBEST());
        this.addModule(new ConfigBW());
        this.addModule(new ConfigBWBEST());
        this.addModule(new ConfigPVPBEST());
        this.addModule(new ConfigPVP());
        this.addModule(new NoRotateSet());
        this.addModule(new Teams());
        this.addModule(new NoLiquid());
        this.addModule(new AutoArmor());
        this.addModule(new AutoTool());
        this.addModule(new Blink());
        this.addModule(new AutoProjectile());
       // this.addModule(new SilenceCrit());
        this.addModule(new FastPlace());
        this.addModule(new Helper());
        this.addModule(new AdminPanel());
        this.addModule(new InvCleaner());
        this.addModule(new AutoLFix());
        this.addModule(new FastEat());
        this.addModule(new Breaker());
        this.addModule(new NoJumpDelay());
        this.addModule(new SpeedMine());
        this.addModule(new ChestStealer());
        this.addModule(new NoWeb());
        this.addModule(new Scaffold());
        this.addModule(new Eagle());
        this.addModule(new PlayerWarn());
        this.addModule(new Spammer());
        this.addModule(new ChestAura());
        this.addModule(new Stuck());
        this.addModule(new IRCModule());
//        this.addModule(new AutoKit());
        this.addModule(new AutoPotion());
        this.addModule(new NoHurtCam());
//        this.addModule(new KeepFov());
        this.addModule(new AutoReport());
        modules.sort(Comparator.comparing((Function<? super Module, ? extends Comparable>)Module::getName));
    }
}

