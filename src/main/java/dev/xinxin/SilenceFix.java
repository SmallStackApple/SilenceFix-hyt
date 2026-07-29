package dev.xinxin;

import cn.dev.annotations.JNICInclude;
import dev.xinxin.command.CommandManager;
import dev.xinxin.config.ConfigManager;
import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.gui.Island.Island;
import dev.xinxin.gui.altmanager.AltManager;
import dev.xinxin.gui.ui.UiManager;
import dev.xinxin.module.Module;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.RotationManager;
import dev.xinxin.module.modules.combat.AutoProjectile;
import dev.xinxin.module.values.Value;
import dev.xinxin.utils.*;
import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.component.*;
import dev.xinxin.utils.novoshader.BackgroundShader;
import dev.xinxin.utils.render.BackgroundTextureManager;
import dev.xinxin.utils.render.WallpaperEngine;
import dev.xinxin.utils.wings.FriendManager;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.netease.PacketProcessor;
import net.netease.chunk.WorldLoader;
import org.apache.commons.compress.utils.IOUtils;
import org.lwjgl.opengl.Display;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@JNICInclude
public class SilenceFix {
    public static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static Minecraft mc = Minecraft.getMinecraft();
    public static SilenceFix instance;
    public static String name;
    public static String sbfantong;

    public static String BanBen;
    public static String pvp;
    public static String NAME;
    public static String VERSION = "41.50";


    public static ResourceLocation[] capes;
    @Getter
    public  FriendManager friendManager;

    public String USER = "";
    private static boolean logged;
    public String commandPrefix = ".";
    @Getter
    public SilenceFixSoundManager soundManager;
    @Getter
    public ConfigManager configManager;
    @Getter
    public RotationManager rotationManager;
    public MovementFixer movementFixer;
    public AltManager altManager;
    public ModuleManager moduleManager;
    public CommandManager commandManager;
    public FallDistanceManager fallDistanceManager;
    public UiManager uiManager;
    @Getter
    public SlotSpoofManager slotSpoofManager;
    @Getter
    public YawPitchHelper yawPitchHelper;
    public HashSet<String> ircFriends = new HashSet<>();
    public WallpaperEngine wallpaperEngine;
    public List<Float> cGUIPosX = new ArrayList<>();
    public List<Float> cGUIPosY = new ArrayList<>();
    public List<Module> cGUIInSetting = new ArrayList<>();
    public List<Value<?>> cGUIInMode = new ArrayList<>();
    public static Unsafe theUnsafe;
    public BackgroundShader blobShader;
    public Island island;


    public String getUser() {
        return this.USER;
    }

    public String getVersion() {
        return VERSION;
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean state) {
        logged = state;
    }

    public SilenceFix() {
        logged = false;
    }

    public void init() {
        SilenceFix.logged = true;
        SilenceFix.instance = this;
        this.soundManager = new SilenceFixSoundManager();
        this.fallDistanceManager = new FallDistanceManager();
        this.altManager = new AltManager();
        this.commandManager = new CommandManager();
        this.configManager = new ConfigManager();
        this.movementFixer = new MovementFixer();
        this.island = new Island();
        friendManager = new FriendManager();
        sbfantong = GuiOptions.setSuffix();

        this.rotationManager = new RotationManager();
        this.uiManager = new UiManager();
        this.slotSpoofManager = new SlotSpoofManager();
        this.yawPitchHelper = new YawPitchHelper();
        this.setWindowIcon();

        this.setLogged(true);
        this.moduleManager = new ModuleManager();
        EventManager.register(this);
        EventManager.register(new Class18());
        EventManager.register(new Class19());
        EventManager.register(new RotationComponent());
        EventManager.register(island);
        EventManager.register(new PacketStoringComponent());
        EventManager.register(MovementComponent.INSTANCE);
        EventManager.register(new FallDistanceComponent());
        EventManager.register(new InventoryClickFixComponent());
        EventManager.register(new PingSpoofComponent());
        EventManager.register(new BadPacketsComponent());
        EventManager.register(PacketProcessor.INSTANCE);
        EventManager.register(new WorldLoader());
        EventManager.register(new TickDispatcher());
        this.soundManager.init();
        this.moduleManager.init();
        this.commandManager.init();
        this.uiManager.init();

        try {
            this.configManager.loadAllConfig();
        } catch (Exception ex) {
            System.out.println("Error loading config: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

        BackgroundTextureManager.loadTextureFromApi();

        this.wallpaperEngine = new WallpaperEngine();
        String gameDirPath = SilenceFix.mc.mcDataDir.getAbsolutePath();
        String videoFilePath = gameDirPath + File.separator + "background.mp4";
        File videoFile = new File(videoFilePath);

        try {
            if (!videoFile.exists()) {
                InputStream inputStream = SilenceFix.class.getResourceAsStream("/assets/minecraft/express/background.mp4");
                if (inputStream != null) {
                    try {
                        Files.copy(inputStream, Paths.get(videoFilePath), REPLACE_EXISTING);
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                    }
                } else {
                    System.err.println("InputStream is null. The resource may not exist.");
                }
            } else {
                System.out.println("File already exists, skipping copy.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.wallpaperEngine.setup(new File(videoFilePath), 30);

        capes = new ResourceLocation[]{
                new ResourceLocation("silencefix/cape1.png"),
                new ResourceLocation("silencefix/cape2.png"),
                new ResourceLocation("silencefix/cape3.png"),
                new ResourceLocation("silencefix/danzai.png"),
                new ResourceLocation("silencefix/danzai1.png")
        };
    }

    public static void displayGuiScreen(GuiScreen guiScreenIn) {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void setWindowIcon() {
        Util.EnumOS util$enumos = Util.getOSType();
        if (util$enumos != Util.EnumOS.OSX) {
            InputStream inputstream1;
            InputStream inputstream;
            block5: {
                inputstream = null;
                inputstream1 = null;
                try {
                    inputstream = SilenceFix.mc.mcDefaultResourcePack.getInputStreamAssets(new ResourceLocation("/assets/minecraft/express/icon/bh16.png"));
                    inputstream1 = SilenceFix.mc.mcDefaultResourcePack.getInputStreamAssets(new ResourceLocation("/assets/minecraft/express/icon/bh32.png"));
                    if (inputstream == null || inputstream1 == null) break block5;
                    Display.setIcon(new ByteBuffer[]{mc.readImageToBuffer(inputstream), mc.readImageToBuffer(inputstream1)});
                }
                catch (IOException ioexception) {
                    try {
                        Minecraft.logger.error("Couldn't set icon", ioexception);
                    }
                    catch (Throwable throwable) {
                        IOUtils.closeQuietly(inputstream);
                        IOUtils.closeQuietly(inputstream1);
                        throw throwable;
                    }
                    IOUtils.closeQuietly(inputstream);
                    IOUtils.closeQuietly(inputstream1);
                }
            }
            IOUtils.closeQuietly(inputstream);
            IOUtils.closeQuietly(inputstream1);
        }
    }


    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        AutoProjectile autoProjectile = ModuleManager.getModule(dev.xinxin.module.modules.combat.AutoProjectile.class);
        if (autoProjectile != null && !autoProjectile.getState() && !autoProjectile.manualClose) {
            boolean hasSharpXAxe = false;
            boolean hasSharp2GoldSword = false;

            for (int i = 0; i < dev.xinxin.utils.InventoryUtil.END; i++) {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (stack != null && stack.getItem() != null) {
                    if (stack.getItem() instanceof ItemAxe) {
                        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
                        if (sharpness >= 10) {
                            hasSharpXAxe = true;
                        }
                    } else if (stack.getItem() == Items.golden_sword) {
                        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
                        if (sharpness >= 2) {
                            hasSharp2GoldSword = true;
                        }
                    }
                }
            }

            if (!hasSharpXAxe && !hasSharp2GoldSword) {
               HelperUtil.sendMessage("§f破甲失效，自动开启雪球");
                autoProjectile.setState(true);
            }
        }
    }



}

