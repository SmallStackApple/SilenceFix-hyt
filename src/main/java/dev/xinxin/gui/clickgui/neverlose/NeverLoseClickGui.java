package dev.xinxin.gui.clickgui.neverlose;

import dev.xinxin.SilenceFix;
import dev.xinxin.config.ConfigManager;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.module.modules.render.PostProcessing;
import dev.xinxin.module.values.*;
import dev.xinxin.utils.HSBData;
import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.render.*;
import dev.xinxin.utils.render.Rectangle;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import dev.yalan.live.silencefix.LiveClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NeverLoseClickGui extends GuiScreen {
    public static NeverLoseClickGui INSTANCE = new NeverLoseClickGui();

    private final ThreadDownloadImageData profileImage = new ThreadDownloadImageData(null, String.format("https://q1.qlogo.cn/g?b=qq&nk=%s&s=100", LiveClient.INSTANCE.liveUser.getQQ()), null, new IImageBuffer() {
        @Override
        public BufferedImage parseUserSkin(BufferedImage image) {
            return image;
        }

        @Override
        public void skinAvailable() {

        }
    });
    private short[] date;
    public float x = 4, y = 20;
    public float width = 520;
    public final float height = 420;
    public float search = 300;
    public float visibleAnimation;
    private boolean quitting = false;

    private boolean dragging;
    private float dragX, dragY;
    public int wheel = Mouse.hasWheel() ? Mouse.getDWheel() * 2 : 0;

    private List<Module> leftModules = new CopyOnWriteArrayList<>();
    private List<Module> rightModules = new CopyOnWriteArrayList<>();

    private List[] lists = new List[]{};

    public NumberFormat nf = new DecimalFormat("0000");
    private Category.Pages current = Category.Pages.COMBAT;
    private NumberValue currentSliding = null;

    private Value<?> dropdownItem;
    private TextValue currentEditing;
    private Rectangle protectArea;

    private final InputField searchTextField = new InputField(FontManager.chineseFont16);
    private boolean searching = false;

    private final float[] moduleWheel = {0f, 0f};

    private float alphaAnimate = 10;

    private String tooltip = null;
    private float offsetY = 0;
    private boolean mouseDown = false;

    public static String author1;


    private final ArrayList<ItemStack> itemStacks = new ArrayList<>();

    public NeverLoseClickGui() {
        INSTANCE = this;
        dropdownItem = null;
        protectArea = null;

        init();
    }

    public void init() {
        font = FontManager.chineseFont18;
        for (Item item : Item.itemRegistry) {
            itemStacks.add(new ItemStack(item));
        }

        Minecraft.getMinecraft().getTextureManager().loadTexture(new ResourceLocation("silencefixProfileImage/" + LiveClient.INSTANCE.liveUser.getQQ()), profileImage);
    }

    private float scrollAni;
    private float CscrollAni;

    private static RapeMasterFontManager font;


    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        tooltip = null;


        visibleAnimation = AnimationUtil.animateSmooth(visibleAnimation, quitting ? 0 : 100, .2F);
        if (quitting) {
            currentEditing = null;
            if (Display.isActive() && !mc.inGameHasFocus) {
                mc.inGameHasFocus = true;
                mc.mouseHelper.grabMouseCursor();
            }

            if (Math.round(visibleAnimation) <= 2) mc.displayGuiScreen(null);
        }

        if (!Mouse.isButtonDown(0) && dragging) dragging = false;
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }
        if (!quitting) {
            ShaderElement.addBlurTask(() -> {
                RoundedUtils.drawRound(x, y, width, height, 8.0f, false, new Color(0,0,0));
//                if (current == Category.Pages.ENTITY) {
//                    RoundedUtils.drawRound(x + width + 8, y + (height / 2) - (height * 0.7f) / 2, 200, height * 0.7f, 6, true, getColor(217, 217, 217));
//                }
            });
            RoundedUtils.drawRound(x, y, width + 0.5f, height, 6, true, new Color(24, 24, 32, 110));
            ShaderElement.addBloomTask(()-> {
                RoundedUtils.drawRound(x, y, width, height, 8.0f, false, new Color(0,0,0));
            });
        }

        float width = Math.max(FontManager.chineseFont38.getStringWidth("SilenceFix".toUpperCase()), FontManager.chineseFont38.getStringWidth("NOVOLINE"));
        if (width > FontManager.chineseFont38.getStringWidth("NOVOLINE")) {
            this.width = AnimationUtil.animateSmooth(this.width, 520 + width - FontManager.chineseFont38.getStringWidth("NOVOLINE"), 0.5f);
        } else {
            this.width = AnimationUtil.animateSmooth(this.width, 520, 0.5f);
        }
        GlStateManager.pushMatrix();
        GradientUtil.applyGradientHorizontal(x + 10, y + 16, FontManager.harmonybold38.getStringWidth("SILENCEFIX"), FontManager.harmonybold38.getHeight(), 1.0f,
                getColor(new Color(0xFF00A7F2)), getColor(new Color(0xFFA5A5A5)), () -> {
                    FontManager.harmonybold38.drawString("SILENCEFIX", (x + 10 + width / 2) - FontManager.harmonybold38.getStringWidth("SILENCEFIX") / 2, y + 16, -1);
                });
        GlStateManager.popMatrix();
        float pageY = 44;
        MaskUtil.defineMask();
        RenderUtil.drawRectWH(x, y + pageY - 4, width + 10, 344, -1);
        MaskUtil.finishDefineMask();
        MaskUtil.drawOnMask();
        float sb = 0;
        for (Category pageManager : Category.values()) {
            sb += 12;
            for (Category.Pages ignored : pageManager.getSubPages()) {
                sb += 26;
            }
            sb += 4;
        }
        CscrollY = Math.max(CscrollY, -sb + 344);
        if (RenderUtil.isHovering(x, y + pageY, width + 10, 340, mouseX, mouseY)) {
            CscrollAni = AnimationUtil.animateSmooth(CscrollAni, CscrollY, 0.3f);
        } else {
            CscrollY = CscrollAni;
        }
        pageY += CscrollAni;

        pageY += 12;
        for (Category.Pages cate : Category.Pages.values()) {
            String text;
            boolean isChinese = HUD.langModeValue.is(HUD.LanguageMode.Chinese.name());

            switch (cate) {
                case CONFIGS:
                    text = isChinese ? "模式配置" : "Config";
                    break;
                case COMBAT:
                    text = isChinese ? "战斗模式" : "Combat Mode";
                    break;
                case MOVEMENT:
                    text = isChinese ? "移动模式" : "Movement Mode";
                    break;
                case RENDER:
                    text = isChinese ? "渲染模式" : "Render Mode";
                    break;
                case WORLD:
                    text = isChinese ? "世界模式" : "World Mode";
                    break;
                case PLAYER:
                    text = isChinese ? "玩家模式" : "Player Mode";
                    break;
                case MISC:
                    text = isChinese ? "杂项模式" : "Misc Mode";
                    break;
                default:
                    if (isChinese) {
                        text = cate.name().toLowerCase().replace("_", " ") + "模式";
                    } else {
                        String[] parts = cate.name().toLowerCase().split("_");
                        StringBuilder sbBuilder = new StringBuilder();
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                sbBuilder.append(Character.toUpperCase(part.charAt(0)))
                                        .append(part.substring(1))
                                        .append(" ");
                            }
                        }
                        text = sbBuilder.toString().trim() + " Mode";
                    }
                    break;
            }





        if (cate == current) {
                RoundedUtils.drawRound(x + 8, y + pageY, width, 16, 4, true, getColor(0,0,0,100));
                float finalPageY2 = pageY;
                ShaderElement.addBlurTask(()-> {
                    RoundedUtils.drawRound(x + 8, y + finalPageY2, width, 16, 4, true, getColor(0,0,0));
                });
                ShaderElement.addBloomTask(()-> {
                    RoundedUtils.drawRound(x + 8, y + finalPageY2, width, 16, 4, true, getColor(0,0,0));
                });
            }

            float finalPageY = pageY;
            cate.animation.draw(() -> RoundedUtils.drawRound(x + 8, y + finalPageY, width, 16, 4, true, getColor(7, 50, 74, 230)));
            try {
                RenderUtil.drawImage(new ResourceLocation("express/icon/neverlose/" + cate.name().toLowerCase() + ".png"), x + 10, y + pageY + 2, 12, 12, getColor(255,255,255).getRGB());
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            text = text.replaceAll("_", " ");
            if (cate.gradient) {
                float finalPageY1 = pageY;
                String finalText = text;
                GlStateManager.pushMatrix();
                GradientUtil.applyGradientHorizontal(x + 26, y + finalPageY1 + 4.5f, font.getStringWidth(text), font.getHeight(), Math.round((visibleAnimation / 100F)), getColor(HUD.color(1)), getColor(HUD.color(89)), () -> {
                    GlStateManager.enableAlpha();
                    GlStateManager.alphaFunc(GL11.GL_GREATER, 0);
                    font.drawString(finalText, x + 26, y + finalPageY1 + 4.5f, getColor(255, 255, 255).getRGB());

                });
                GlStateManager.popMatrix();

            } else {
                font.drawString(text, x + 26, y + pageY + 5f, getColor(255,255,255).getRGB());

            }
            pageY += 26;
        }


        MaskUtil.resetMask();



        if (current == Category.Pages.CONFIGS) {

            boolean hovered = RenderUtil.isHovering(x + width + 16 + 4, y + 15, 89, 20, mouseX, mouseY);
            RoundedUtils.drawRoundOutline(x + width + 16 + 4, y + 15, 89, 20, 4, 0.1f, getColor(0,0,0,120), hovered ? getColor(new Color(0xFF00A7F2)) : getColor(217, 217, 217));
            font.drawCenteredString("Create Config", x + width + 16 + 4 + (89 / 2f), y + 22, getColor(255,255,255).getRGB());
            boolean hovered2 = RenderUtil.isHovering(x + width + 16 + 96, y + 15, 89, 20, mouseX, mouseY);
            RoundedUtils.drawRoundOutline(x + width + 16 + 96, y + 15, 89, 20, 4, 0.1f, getColor(0,0,0,120), hovered2 ? getColor(new Color(0xFF00A7F2)) : getColor(217, 217, 217));
            font.drawCenteredString("Open directory", x + width + 16 + 96 + (89 / 2f), y + 22, getColor(255,255,255).getRGB());
        } else {

            RoundedUtils.drawRoundOutline(x + width + 16 + 10, y + 14, search, 20, 4, 0.1f,
                    getColor(0,0,0,120),
                    searching ? getColor(new Color(0xFF00A7F2)) : getColor(0,0,0,0));
            ShaderElement.addBlurTask(()-> {
                RoundedUtils.drawRoundOutline(x + width + 16 + 10, y + 14, search, 20, 4, 0.1f,
                        getColor(0,0,0,120),
                        searching ? getColor(new Color(0xFF00A7F2)) : getColor(0,0,0,0));
            });
            ShaderElement.addBloomTask(()-> {
                RoundedUtils.drawRoundOutline(x + width + 16 + 10, y + 14, search, 20, 4, 0.1f,
                        getColor(0,0,0,120),
                        searching ? getColor(new Color(0xFF00A7F2)) : getColor(0,0,0,0));
            });

            if (!searching)
                RenderUtil.drawImage(new ResourceLocation("express/icon/neverlose/search.png"), x + width + 17 + 10 + 3, y + 17, 14, 14, getColor(255,255,255, 200).getRGB());

            searchTextField.setBackgroundText("按 Ctrl+F 搜索模块....");
            searchTextField.setDrawingLine(true);
            searchTextField.setxPosition(x + width + 16 + 10 + 3 + (searching ? 0 : 16));
            searchTextField.setyPosition(y + 17);
            searchTextField.setWidth(search);
            searchTextField.setHeight(20);
            searchTextField.setDrawingBackground(false);
            searchTextField.drawTextBox(mouseX, mouseY);
        }

        StencilUtil.initStencilToWrite();

        RenderUtil.drawRectWH(x, y + 45, this.width, height - 46, new Color(24, 24, 32, 100).getRGB());

        ShaderElement.addBlurTask(()-> {
            RenderUtil.drawRectWH(x, y + 45, this.width, height - 46, getColor(0,0,0,200).getRGB());
        });
        ShaderElement.addBloomTask(()-> {
            RenderUtil.drawRectWH(x, y + 45, this.width, height - 46, getColor(0,0,0,200).getRGB());
        });

        StencilUtil.readStencilBuffer(1);
        wheel = Mouse.hasWheel() ? Mouse.getDWheel() * 12 : 0;

        if (current.module) {
            float left = render(leftModules, x + width + 24, mouseX, mouseY);
            float right = render(rightModules, x + width + 24 + 198, mouseX, mouseY);

            final float[] nextWheel = RenderUtil.getNextWheelPosition(wheel, moduleWheel, y + 10, y + 290, Math.max(left, right), 0, RenderUtil.isHovering(x + 120, y + 40, this.width - 120, this.height - 40, mouseX, mouseY));
            moduleWheel[0] = nextWheel[0];
            moduleWheel[1] = Math.max(left, right) > this.height ? Math.max(nextWheel[1], -Math.max(left, right) + this.height - 60) : nextWheel[1];

        } else {

            if (current == Category.Pages.CONFIGS) {
                SilenceFix styles = SilenceFix.instance;
                float configY = y + 40 + 8 + 14 + scrollAni;
                FontManager.chineseFont16.drawString("- My Items", x + width + 16 + 6, configY - 14, getColor(77, 77, 77).getRGB());

                float naotan = styles.configManager.getConfigs().size() * 42;

                if (naotan > this.height - 60) {
                    scrollY = Math.max(scrollY, -naotan + this.height - 60);
                    if (RenderUtil.isHovering(x + width + 16, 0, 370, 420, mouseX, mouseY)) {
                        scrollAni = AnimationUtil.animateSmooth(scrollAni, scrollY*2, 0.2f);
                    } else {
                        scrollY = scrollAni;
                    }
                }

                for (String config : styles.configManager.getConfigs()) {
                    if (!(config.equals("bw") || config.equals("bwbest") || config.equals("sw") || config.equals("swbest") || config.equals("pvp") || config.equals("pvpbest"))) {
                        continue;
                    }
                    File configF = new File(ConfigManager.dir, config + ".json");
                    String author = "null";
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(configF), StandardCharsets.UTF_8))) {
                        StringBuilder jsonString = new StringBuilder();
                        String line;

                        while ((line = br.readLine()) != null) {
                            jsonString.append(line);
                        }
                        String json = jsonString.toString();
                        author = extractAuthor(json);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                   author1 = author = switch (configF.getName()) {
                        case "bw.json" -> "起床模式";
                        case "bwbest.json" -> "高性能起床模式";
                        case "bwhvh.json" -> "起床对刀模式";
                       case "swbest.json" -> "高性能空岛模式";
                       case "hud.json" -> "不要点我  否则会崩端";
                       case "alts.json" -> "不要点我 否则会崩端";
                       case "modules.json" -> "不要点我 否则会崩端";
                       case "sw.json" -> "空岛模式";
                        case "pvp.json" -> "职业战争天坑模式";
                        case "pvpbest.json" -> "高性能职业战争天坑模式";
                       case "pvpbeta.json" -> "职业战争杀戮跟随模式";
                        default -> author;
                    };


                    long modified = configF.lastModified();
                    String modifiedTime = new SimpleDateFormat("yyyy/MM/dd").format(new Date(modified));
                    RoundedUtils.drawGradientRound(x + width + 16 + 6, configY, 370, 36, 4,
                            new Color(22, 22, 28, 200),
                            new Color(24, 24, 32, 200),
                            new Color(26, 26, 34, 200),
                            new Color(22, 22, 28, 200));
                /*    RoundedUtils.drawRoundOutline(x + width + 16 + 6, configY, 370, 36, 4, 0.5f,
                            new Color(0, 0, 0, 0),
                            new Color(0x00A7F2));*/
                    FontManager.chineseFont18.drawString(config, x + width + 16 + 12, configY + 6, getColor(255,255,255).getRGB());
                        FontManager.chineseFont18.drawString(EnumChatFormatting.GRAY + "Modified: " + EnumChatFormatting.RESET + modifiedTime + " " + EnumChatFormatting.GRAY + "Author: " + EnumChatFormatting.RESET + author, x + width + 16 + 12, configY + 8 + 12, getColor(3, 146, 214).getRGB());
                        boolean fuck = RenderUtil.isHovering(x + width + 16 + 6 + 290 - 6, configY + 8, 76, 20, mouseX, mouseY);
                        if ("modules.json".equals(config)) {
                            RoundedUtils.drawRoundOutline(x + width + 16 + 6 + 290 - 6, configY + 8, 76, 20, 4, 0.1f, getColor(217, 217, 217), fuck ? getColor(new Color(0xFF00A7F2)) : getColor(217, 217, 217));
                            RenderUtil.drawImage(new ResourceLocation("express/icon/neverlose/save.png"), x + width + 16 + 6 + 290 + 10, configY + 8 + 2, 16, 16, getColor(201, 201, 201).getRGB());
                            FontManager.chineseFont16.drawString("Save", x + width + 16 + 6 + 290 + 30, configY + 8 + 8, getColor(201, 201, 201).getRGB());
                        } else {
                            RoundedUtils.drawRound(x + width + 16 + 6 + 290 - 6, configY + 8, 76, 20, 4, false, fuck ? getColor(new Color(0xFF00A7F2)) : getColor(new Color(0x1D82AF)));
                            RenderUtil.drawImage(new ResourceLocation("express/icon/neverlose/read.png"), x + width + 16 + 6 + 290 + 10, configY + 8 + 2, 16, 16, fuck ? -1 : getColor(201, 201, 201).getRGB());
                            FontManager.chineseFont16.drawString("Load", x + width + 16 + 6 + 290 + 30, configY + 8 + 8, fuck ? -1 : getColor(201, 201, 201).getRGB());
                        }
                        configY += 42;
                }
            }
        }


        StencilUtil.endStencilBuffer();

        RenderUtil.drawRectWH(x, y + height - 30, width + 23, 0.5f, getColor(255,255,255).getRGB());
        RenderUtil.drawImageRound(profileImage.getGlTextureId(), x + 5, y + height - 27, 24, 24, -1, () -> {
            RoundedUtils.drawRound(x + 5, y + height - 27, 24, 24, 14.0f, Color.BLACK);
        });
        GlStateManager.resetColor();
        FontManager.chineseFont16.drawString(LiveClient.INSTANCE.liveUser.getName(), x + 36,y + height -22, Color.WHITE.getRGB());
        FontManager.chineseFont18.drawString(LiveClient.INSTANCE.liveUser.getQQ(), x + 36,y + height - 12, new Color(3, 168, 245).getRGB());
        GlStateManager.resetColor();

        alphaAnimate = AnimationUtil.animateSmooth(alphaAnimate, 180, 0.4f);
        if (alphaAnimate > 20) {

//         ShaderElement.addBlurTask(()->RenderUtil.drawRectWH(0, 0, new ScaledResolution(mc).getScaledWidth(), new ScaledResolution(mc).getScaledHeight(), f));
            RenderUtil.drawRectWH(0, 0, new ScaledResolution(mc).getScaledWidth(), new ScaledResolution(mc).getScaledHeight(), new Color(0, 0, 0, ((int) alphaAnimate)).getRGB());
        }

        if (dropdownItem != null && protectArea != null) {
            if (dropdownItem instanceof ModeValue) {
                final ModeValue property = (ModeValue) dropdownItem;
                property.animation = AnimationUtil.animateSmooth(property.animation, 255, 0.5f);
                RoundedUtils.drawRound((float) protectArea.getX(), (float) protectArea.getY(), (float) protectArea.getWidth(), (float) (protectArea.getHeight() + 1), 4F, getColor(RenderUtil.reAlpha(new Color(240, 240, 240), (int) property.animation)));
                int buttonY = 0;
                for (Enum s : property.getModes()) {
                    font.drawString(s.name(), (float) (protectArea.getX() + 3), (float) (protectArea.getY() + buttonY + 6), !property.is(s.name()) ? getColor(RenderUtil.reAlpha(new Color(77, 77, 77), (int) property.animation)).getRGB() : getColor(RenderUtil.reAlpha(new Color(0, 0, 0), (int) property.animation)).getRGB());

                    buttonY += 14;
                }
            }
            if (dropdownItem instanceof ColorValue) {
                ColorValue cp = (ColorValue) dropdownItem;

                final Color valColor = cp.getColorC();

                HSBData hsbData = new HSBData(valColor);

                final float[] hsba = {
                        hsbData.getHue(),
                        hsbData.getSaturation(),
                        hsbData.getBrightness(),
                        hsbData.getAlpha(),
                };

                RoundedUtils.drawRoundOutline((float) protectArea.getX(), (float) protectArea.getY(), (float) protectArea.getWidth(), (float) (protectArea.getHeight() + 1), 2F, 0.1F, getColor(5, 16, 26), getColor(217, 217, 217));
                RenderUtil.drawRectWH(protectArea.getX() + 3, protectArea.getY() + 3, 61, 61, getColor(0, 0, 0).getRGB());
                RenderUtil.drawRectWH(protectArea.getX() + 3.5, protectArea.getY() + 3.5, 60, 60, getColor(Color.getHSBColor(hsba[0], 1, 1)).getRGB());
                RenderUtil.drawHGradientRect(protectArea.getX() + 3.5, protectArea.getY() + 3.5, 60, 60, getColor(Color.getHSBColor(hsba[0], 0, 1)).getRGB(), 0x00F);
                RenderUtil.drawVGradientRect(protectArea.getX() + 3.5, protectArea.getY() + 3.5, 60, 60, 0x00F, getColor(Color.getHSBColor(hsba[0], 1, 0)).getRGB());

                RenderUtil.drawRectWH(protectArea.getX() + 3.5 + hsba[1] * 60 - .5, protectArea.getY() + 3.5 + ((1 - hsba[2]) * 60) - .5, 1.5, 1.5, getColor(0, 0, 0).getRGB());
                RenderUtil.drawRectWH(protectArea.getX() + 3.5 + hsba[1] * 60, protectArea.getY() + 3.5 + ((1 - hsba[2]) * 60), .5, .5, getColor(valColor).getRGB());

                final boolean onSB = RenderUtil.isHovering((float) (protectArea.getX() + 3), (float) (protectArea.getY() + 3), 61F, 61, mouseX, mouseY);

                if (onSB && Mouse.isButtonDown(0)) {
                    hsbData.setSaturation((float) Math.min(Math.max((mouseX - protectArea.getX() - 3) / 60F, 0), 1));
                    hsbData.setBrightness((float) (1 - Math.min(Math.max((mouseY - protectArea.getY() - 3) / 60F, 0), 1)));
                    cp.setColor(hsbData.getAsColor().getRGB());

                }

                RenderUtil.drawRectWH(protectArea.getX() + 67, protectArea.getY() + 3, 10, 61, getColor(0, 0, 0).getRGB());

                for (float f = 0F; f < 5F; f += 1F) {
                    final Color lasCol = Color.getHSBColor(f / 5F, 1F, 1F);
                    final Color tarCol = Color.getHSBColor(Math.min(f + 1F, 5F) / 5F, 1F, 1F);
                    RenderUtil.drawVGradientRect(protectArea.getX() + 67.5, protectArea.getY() + 3.5 + f * 12, 9, 12, getColor(lasCol).getRGB(), getColor(tarCol).getRGB());
                }

                RenderUtil.drawRectWH(protectArea.getX() + 67.5, protectArea.getY() + 2 + hsba[0] * 60, 9, 2, getColor(0, 0, 0).getRGB());
                RenderUtil.drawRectWH(protectArea.getX() + 67.5, protectArea.getY() + 2.5 + hsba[0] * 60, 9, 1, getColor(204, 198, 255).getRGB());

                final boolean onHue = RenderUtil.isHovering((float) (protectArea.getX() + 67), (float) (protectArea.getY() + 3), 10F, 61, mouseX, mouseY);

                if (onHue && Mouse.isButtonDown(0)) {
                    hsbData.setHue((float) Math.min(Math.max((mouseY - protectArea.getY() - 3) / 60F, 0), 1));
                    cp.setColor(hsbData.getAsColor().getRGB());
                    cp.setRainbowEnabled(false);
                }

                if (cp.isAlphaChangeable()) {

                    RenderUtil.drawRectWH(protectArea.getX() + 3, protectArea.getY() + 67, 61, 9, getColor(0, 0, 0).getRGB());

                    for (int xPosition = 0; xPosition < 30; xPosition++)
                        for (int yPosition = 0; yPosition < 4; yPosition++)
                            RenderUtil.drawRectWH(protectArea.getX() + 3.5 + (xPosition * 2), protectArea.getY() + 67.5 + (yPosition * 2), 2, 2, ((yPosition % 2 == 0) == (xPosition % 2 == 0)) ? getColor(255, 255, 255).getRGB() : getColor(190, 190, 190).getRGB());

                    RenderUtil.drawHGradientRect(protectArea.getX() + 3.5, protectArea.getY() + 67.5, 60, 8, 0x00F, getColor(Color.getHSBColor(hsba[0], 1, 1)).getRGB());

                    RenderUtil.drawRectWH(protectArea.getX() + 2.5 + hsba[3] * 60, protectArea.getY() + 67.5, 2, 8, getColor(0, 0, 0).getRGB());
                    RenderUtil.drawRectWH(protectArea.getX() + 3 + hsba[3] * 60, protectArea.getY() + 67.5, 1, 8, getColor(204, 198, 255).getRGB());

                    final boolean onAlpha = RenderUtil.isHovering((float) (protectArea.getX() + 3), (float) (protectArea.getY() + 67), 61F, 9, mouseX, mouseY);

                    if (onAlpha && Mouse.isButtonDown(0)) {
                        hsbData.setAlpha((float) Math.min(Math.max((mouseX - protectArea.getX() - 3) / 60F, 0), 1));
                    }
                }
            }
        }


        if (tooltip != null && !tooltip.isEmpty()) {

            ShaderElement.addBlurTask(() -> RoundedUtils.drawRound(mouseX + 6, mouseY + 6, font.getStringWidth(findLongestString(tooltip.split("\n"))) + 10, tooltip.split("\n").length * 14 + (tooltip.split("\n").length == 1 ? 0 : 4), 2, true, new Color(10, 19, 30, 255)));
            RoundedUtils.drawRound(mouseX + 6, mouseY + 6, font.getStringWidth(findLongestString(tooltip.split("\n"))) + 10, tooltip.split("\n").length * 14 + (tooltip.split("\n").length == 1 ? 0 : 4), 2, false, new Color(255, 255, 255, 30));

            RoundedUtils.drawRound(mouseX + 6, mouseY + 6, font.getStringWidth(findLongestString(tooltip.split("\n"))) + 10, tooltip.split("\n").length * 14 + (tooltip.split("\n").length == 1 ? 0 : 4), 2, true, new Color(10, 19, 30, 100));
            float y = 5;
            for (String s : tooltip.split("\n")) {
                font.drawString(s, mouseX + 6 + 4, mouseY + 6 + y, getColor(255, 255, 255).getRGB());
                if (tooltip.split("\n").length != 1)
                    y += 14;
            }
        }
    }

    protected boolean check(double x, double y, double x2, double y2, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x2 && mouseY >= y && mouseY <= y2;
    }

    private boolean checkClick() {
        if (!mouseDown && Mouse.isButtonDown(0)) {
            mouseDown = true;
            return true;
        }

        return false;
    }

    public static double round(final double value, final double inc) {
        if (inc == 0.0) return value;
        else if (inc == 1.0) return Math.round(value);
        else {
            final double halfOfInc = inc / 2.0;
            final double floored = Math.floor(value / inc) * inc;

            if (value >= floored + halfOfInc)
                return new BigDecimal(Math.ceil(value / inc) * inc)
                        .doubleValue();
            else return new BigDecimal(floored)
                    .doubleValue();
        }
    }

    private float render(List<Module> modules, float offset, int mouseX, int mouseY) {

        RenderUtil.drawRect(offset, y + 28, offset + 190, y + 45, new Color(30, 30, 36, 100).getRGB());
        FontManager.chineseFont14.drawCenteredString(
                "B: 表示模块绑定按键，点击右侧“更改按键 →”修改",
                offset + 95,
                y + 33,
                getColor(100, 100, 100).getRGB()
        );

        float moduleY = 0 + moduleWheel[1];

        for (Module module : modules) {
            if (HUD.langModeValue.is("English"))
                FontManager.chineseFont16.drawString(module.getName().toUpperCase(), offset + 4, y + 50 + moduleY, new Color(134, 134, 133).getRGB());
            else
                FontManager.chineseFont16.drawString(module.getCnName().toUpperCase(), offset + 4, y + 50 + moduleY, new Color(134, 134, 133).getRGB());

            int predictionHeight = 16;
            predictionHeight += module.getValues().stream().filter(Value::isAvailable).mapToInt(property -> (int) property.height).sum();

            RoundedUtils.drawRound(offset, y + 46 + FontManager.chineseFont16.getHeight() + 4 + moduleY, 190,predictionHeight, 6.0f, false, getColor(30, 30, 40, 110));


            moduleY += 10;
            font.drawString("Enabled", offset + 4, y + 50 + moduleY + 6, module.getState() ? getColor(200,200,200).getRGB() : getColor(255,255,255).getRGB());
            RoundedUtils.drawRoundOutline(offset + 162, 2f + y + 48 + moduleY + 5, 23, 12, 6, 0.1f,
                    module.getState() ? getColor(3, 168, 245) : getColor(100,100,100), getColor(0,0,0));
            String bindKey = getKeyDisplayName(module.getKey());
            String displayBind = "按键: " + bindKey;
            float bindWidth = FontManager.chineseFont16.getStringWidth(displayBind);
            float bindBoxX = offset + 120;
            float bindBoxY = y + 50 + moduleY + 4;
            float bindBoxW = bindWidth + 6;
            float bindBoxH = 12;
            RoundedUtils.drawRound(bindBoxX, bindBoxY, bindBoxW, bindBoxH, 3, new Color(50,50,50, 180));
            FontManager.chineseFont16.drawString(displayBind, bindBoxX + 3, bindBoxY + 3, getColor(200,200,200).getRGB());
            FontManager.chineseFont14.drawString("更改按键 →", bindBoxX - 55, bindBoxY + 3, new Color(200,200,200).getRGB());
            RoundedUtils.drawRound(bindBoxX, bindBoxY, bindBoxW, bindBoxH, 3, new Color(240, 240, 240, 120));
            FontManager.chineseFont16.drawString(displayBind, bindBoxX + 3, bindBoxY + 3, getColor(255,255,255).getRGB());


            module.cGUIAnimation = AnimationUtil.animateSmooth(module.cGUIAnimation, module.getState() ? 10 : 0, 0.5f);
            RenderUtil.drawImage(new ResourceLocation("express/icon/neverlose/shadow.png"),
                    offset + 160 + module.cGUIAnimation, 2f + y + 44 + moduleY + 9, 16, 16);
            RenderUtil.drawCircleCGUI(offset + 168 + module.cGUIAnimation, 2f + y + 50 + moduleY + 9, 10, getColor(255, 255, 255).getRGB());

            if (module.getValues().size() > 0)
                RenderUtil.drawRectWH(offset + 4, y + 50 + moduleY + 18, 190 - 8, .5, getColor(217, 217, 217).getRGB());

            moduleY += 18;

            for (Value<?> property : module.getValues()) {
                if (!property.isAvailable()) continue;

                if (property instanceof BoolValue) {
                    final BoolValue bp = (BoolValue) property;
                    font.drawString(property.getName(), offset + 4, y + 50 + moduleY + 6 + 2f,
                            bp.getValue() ? getColor(200,200,200).getRGB() : getColor(255,255,255).getRGB());
                    RoundedUtils.drawRoundOutline(offset + 162, 2f + y + 48 + moduleY + 5, 23, 12, 6, 0.1f,
                            bp.getValue() ?  getColor(3, 168, 245) : getColor(100,100,100), getColor(0,0,0));
                    property.animation = AnimationUtil.animateSmooth(property.animation, bp.getValue() ? 10 : 0, 0.5f);
                    RenderUtil.drawImage(new ResourceLocation("express/icon/neverlose/shadow.png"),
                            offset + 160 + property.animation, 2f + y + 44 + moduleY + 9, 16, 16);
                    RenderUtil.drawCircleCGUI(offset + 168 + property.animation, 2f + y + 50 + moduleY + 9, 10, getColor(255,255,255).getRGB());
                }

                if (property instanceof ColorValue) {
                    ColorValue cp = (ColorValue) property;
                    font.drawString(property.getName(), offset + 4, y + 50 + moduleY + 6 + 2f, getColor(255,255,255).getRGB());
                    RenderUtil.drawCircleCGUI(offset + 175, 2f + y + 50 + moduleY + 9, 11, getColor(new Color(cp.getColor())).getRGB());

                    if (dropdownItem == cp) {
                        protectArea = new Rectangle(offset + 100, 2f + y + moduleY + 50 + 24, 80, cp.isAlphaChangeable() ? 80 : 67);
                    }
                }

                if (property instanceof NumberValue) {
                    DecimalFormat df = new DecimalFormat("#.#");

                    final NumberValue dp = (NumberValue) property;
                    String display = String.valueOf(dp.getValue());
                    if (display.endsWith(".0")) display = display.substring(0, display.length() - 2);
                    else if (display.startsWith("0.")) display = "." + display.substring(2);
                    else if (display.startsWith("-0.")) display = "-" + display.substring(2);
                    font.drawString(property.getName(), offset + 4, y + 50 + moduleY + 6 + 2f,
                            dp.sliding ? getColor(200,200,200).getRGB() : getColor(255,255,255).getRGB());
                    FontManager.chineseFont16.drawCenteredString(display, offset + 190 - 13,
                            2f + y + 50 + moduleY + 6, getColor(145, 166, 179).getRGB());
                    RoundedUtils.drawGradientRound(offset + 96, 2f + y + 50 + moduleY + 8, 70, 2, 2,
                            new Color(255, 255, 255, 40),
                            new Color(255, 255, 255, 20),
                            new Color(255, 255, 255, 20),
                            new Color(255, 255, 255, 40));
                    final double ratio = (dp.getValue() - dp.getMin()) / (dp.getMax() - dp.getMin());
                    int displayLength = (int) (ratio * 70);
                    displayLength = Math.min(displayLength, 70);
                    dp.animatedPercentage = AnimationUtil.animateSmooth((float) dp.animatedPercentage, displayLength, 0.2F);
                    RoundedUtils.drawRound(offset + 92, 2f + y + 50 + moduleY + 8, (float) dp.animatedPercentage, 2, 2, true, getColor(3, 168, 245));
                    dp.animation = AnimationUtil.animateSmooth(dp.animation, dp.sliding ? 10 : 8, 0.2F);
                    RenderUtil.drawImage(new ResourceLocation("express/icon/neverlose/shadow.png"),
                            (float) (84 + offset + dp.animatedPercentage), 2f + y + 42 + moduleY + 9, 16, 16);
                    RenderUtil.drawCircleCGUI(93 + offset + dp.animatedPercentage, 2f + y + 50 + moduleY + 9,
                            dp.animation, getColor(3, 168, 245).getRGB());

                    if (dp.sliding) {
                        double num = Math.max(dp.getMin(), Math.min(dp.getMax(), round((mouseX - (offset + 92)) * (dp.getMax() - dp.getMin()) / 70 + dp.getMin(), dp.getInc())));
                        num = (double) Math.round(num * (1.0D / dp.getInc())) / (1.0D / dp.getInc());
                        dp.setValue(num);
                    }
                }

                if (property instanceof ModeValue) {
                    final ModeValue sp = (ModeValue) property;
                    sp.height = 24f;
                    font.drawString(property.getName(), offset + 4, y + 50 + moduleY + 9 + 1f, getColor(255,255,255).getRGB());
                    RoundedUtils.drawRoundOutline(offset + 100, y + 50 + moduleY + 3.5F, 80, 16, 4, 0.1f,
                            getColor(120,120,120,160), getColor(240, 240, 240, 0));
                    float finalModuleY = moduleY;
                    ShaderElement.addBlurTask(()->{RoundedUtils.drawRoundOutline(offset + 100, y + 50 + finalModuleY + 3.5F, 80, 16, 4, 0.1f,
                            getColor(120,120,120,160), getColor(240, 240, 240, 0));});
                    MaskUtil.defineMask();
                    RoundedUtils.drawRoundOutline(offset + 100, y + 50 + moduleY + 3.5F, 80, 16, 4, 0.1f,
                            getColor(60,60,60), getColor(240, 240, 240, 120));
                    MaskUtil.finishDefineMask();
                    MaskUtil.drawOnMask();
                    try {
                        font.drawString(sp.getValue().toString(), offset + 104, y + 50 + moduleY + 9, getColor(255,255,255).getRGB());
                    } catch (Exception e) {}
                    MaskUtil.resetMask();

                    float spmaxWidth = 0;
                    for (Enum s : sp.getModes()) {
                        float f = font.getStringWidth(s.name()) + 12;
                        if (f > spmaxWidth) spmaxWidth = f;
                    }

                    if (dropdownItem == sp) {
                        protectArea = new Rectangle(offset + 100, y + moduleY + 50 + 24, spmaxWidth > 80 ? spmaxWidth : 80, 14 * sp.getModes().length);
                    }
                }

                if (property instanceof TextValue) {
                    final TextValue tp = (TextValue) property;
                    boolean isMe = currentEditing == tp;
                    font.drawString(property.getName(), offset + 4, y + 50 + moduleY + 9 + 2f, getColor(255, 255, 255).getRGB());
                    RoundedUtils.drawRoundOutline(offset + 100, y + 50 + moduleY + 3.5F, 80, 18, 2, 0.1f,
                            getColor(230, 230, 230), isMe ? getColor(201, 201, 201).brighter() : getColor(217, 217, 217));
                    MaskUtil.defineMask();
                    RoundedUtils.drawRoundOutline(offset + 100, y + 50 + moduleY + 3.5F, 80, 18, 2, 0.1f,
                            getColor(230, 230, 230), isMe ? getColor(201, 201, 201).brighter() : getColor(217, 217, 217));
                    MaskUtil.finishDefineMask();
                    MaskUtil.drawOnMask();
                    font.drawString(tp.getValue() + (isMe ? "_" : ""), offset + 104, y + 50 + moduleY + 9,
                            getColor(77, 77, 77).getRGB());
                    RenderUtil.drawRectWH(offset + 104, y + 50 + moduleY + 9,
                            font.getStringWidth(tp.getSelectedString()), font.getHeight(), getColor(255, 255, 255, 100).getRGB());
                    MaskUtil.resetMask();
                }

                moduleY += property.height;

                List<Value<?>> visible = new ArrayList<>(module.getValues());
                visible.removeIf(Value::isHidden);
                if (visible.indexOf(property) != visible.size() - 1) {
                    RenderUtil.drawRectWH(offset + 4, y + 50 + moduleY, 190 - 8, .5, getColor(217, 217, 217).getRGB());
                }
            }

            moduleY += 4;
        }

        return moduleY - moduleWheel[1];
    }



    public void setQuitting(boolean quitting) {
        this.quitting = quitting;
    }

    public boolean isOpened() {
        return !quitting;
    }
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        SilenceFix.instance.configManager.saveAllConfig();
    }

    private float scrollY;
    private float CscrollY;

    @Override
    public void handleMouseInput() throws IOException {
        this.scrollY += (float) Mouse.getEventDWheel();
        if (this.scrollY >= 0.0f) {
            this.scrollY = 0.0f;
        }


        this.CscrollY += (float) Mouse.getEventDWheel();
        if (this.CscrollY >= 0.0f) {
            this.CscrollY = 0.0f;
        }

        int i = Mouse.getEventX() * new ScaledResolution(mc).getScaledWidth() / this.mc.displayWidth;
        int j = new ScaledResolution(mc).getScaledHeight() - Mouse.getEventY() * new ScaledResolution(mc).getScaledHeight() / this.mc.displayHeight - 1;
        super.handleMouseInput();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {

        if (keyCode == Keyboard.KEY_RETURN && searching)
            return;

        if (keyCode == Keyboard.KEY_ESCAPE)
            mc.displayGuiScreen(null);

        if (GuiScreen.isKeyComboCtrlF(keyCode)) {
            searching = !searching;
            if (searching) {
                lists = new List[]{leftModules, rightModules};
                searchTextField.setText("");
            } else {
                leftModules = lists[0];
                rightModules = lists[1];
                resetModuleList();
            }
            return;
        }

        if (currentEditing != null) {
            try {
                if (keyCode == Keyboard.KEY_BACK && !currentEditing.getValue().isEmpty()) {
                    currentEditing.setValue(!currentEditing.getSelectedString().isEmpty() ? "" : currentEditing.getValue().substring(0, currentEditing.getValue().length() - 1));
                    currentEditing.setSelectedString("");
                    return;
                }

                if (GuiScreen.isKeyComboCtrlA(keyCode)) {
                    currentEditing.setSelectedString(currentEditing.getValue());
                    return;
                }

                if (GuiScreen.isKeyComboCtrlC(keyCode)) {
                    GuiScreen.setClipboardString(currentEditing.getSelectedString());
                    return;
                }

                if (GuiScreen.isKeyComboCtrlV(keyCode)) {
                    if (currentEditing.getSelectedString().isEmpty() && (currentEditing.getValue() + GuiScreen.getClipboardString()).length() > 22) {
                        currentEditing.setSelectedString("");
                        return;
                    }
                    currentEditing.setValue(!currentEditing.getSelectedString().isEmpty() ? GuiScreen.getClipboardString() : currentEditing.getValue() + GuiScreen.getClipboardString());
                    currentEditing.setSelectedString("");
                    return;
                }

                if (GuiScreen.isCtrlKeyDown()) return;
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    currentEditing.setSelectedString("");
                    currentEditing = null;
                    return;
                }
                if (currentEditing.getValue().length() > 22) return;

                currentEditing.setValue(!currentEditing.getSelectedString().isEmpty() ? ChatAllowedCharacters.filterAllowedCharacters(String.valueOf(typedChar)) : currentEditing.getValue() + ChatAllowedCharacters.filterAllowedCharacters(String.valueOf(typedChar)));
                currentEditing.setSelectedString("");
                return;
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
        if (searching) {
            searchTextField.setFocused(true);
            searchTextField.keyTyped(typedChar, keyCode);
            resetModuleList();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }


    private Color getColor(int r, int g, int b) {
        return RenderUtil.reAlpha(new Color(r, g, b), Math.round((visibleAnimation / 100F) * 255F));
    }

    private Color getColor(int r, int g, int b, int a) {
        return RenderUtil.reAlpha(new Color(r, g, b), Math.round((visibleAnimation / 100F) * a));
    }

    private Color getColor(Color color) {
        return RenderUtil.reAlpha(color, Math.round((visibleAnimation / 100F) * color.getAlpha()));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (currentSliding != null) {
            currentSliding.sliding = false;
            currentSliding = null;
        }
    }

    public String findLongestString(String[] strArray) {
        String longestString = "";
        for (String str : strArray) {
            if (font.getStringWidth(str) > font.getStringWidth(longestString)) {
                longestString = str;
            }
        }
        return longestString;
    }

    @Override
    public void initGui() {
        super.initGui();
        resetModuleList();
    }

    private void click(List<Module> modules, float offset, int mouseX, int mouseY, int mouseButton) {
        float moduleY = 0 + moduleWheel[1];

        for (Module module : modules) {
            moduleY += 10;
            String bindKey = getKeyDisplayName(module.getKey());
            String displayBind = "按键: " + bindKey;
            float bindWidth = FontManager.chineseFont16.getStringWidth(displayBind);

            float bindBoxX = offset + 120;
            float bindBoxY = y + 50 + moduleY + 6;
            float bindBoxW = bindWidth + 6;
            float bindBoxH = 14;
            if (RenderUtil.isHovering(bindBoxX, bindBoxY, bindBoxW, bindBoxH, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    mc.displayGuiScreen(new BindScreen(module, this));
                    return;
                }
            }



            if (RenderUtil.isHovering(offset + 4, y + 50 + moduleY + 4, 184, 12, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    module.toggle();
                }
                return;
            }

            moduleY += 18;

            for (Value<?> property : module.getValues()) {
                if (property.isHidden()) continue;

                if (property instanceof BoolValue bp && RenderUtil.isHovering(offset + 4, y + 50 + moduleY + 5, 184, 12, mouseX, mouseY) && mouseButton == 0)
                    bp.setValue(!bp.getValue());

                if (property instanceof NumberValue dp && RenderUtil.isHovering(offset + 88, y + 50 + moduleY + 2, 78, 16, mouseX, mouseY) && mouseButton == 0) {
                    dp.sliding = true;
                    currentSliding = dp;
                }

                if (property instanceof ModeValue sp && RenderUtil.isHovering(offset + 4, y + 50 + moduleY + 6, 184, 18, mouseX, mouseY) && mouseButton == 0) {
                    if (dropdownItem != property) {
                        dropdownItem = sp;
                        sp.animation = 100;
                        protectArea = new Rectangle(offset + 100, y + moduleY + 50 + 24, 80, 14 * sp.getModes().length);
                    } else {
                        dropdownItem = null;
                        protectArea = null;
                    }
                }

                if (property instanceof TextValue tp) {
                    if (RenderUtil.isHovering(offset + 4, y + 50 + moduleY + 6, 184, 18, mouseX, mouseY) && mouseButton == 0)
                        currentEditing = tp;
                    else if (currentEditing == tp)
                        currentEditing = null;
                }

                if (property instanceof ColorValue cp && RenderUtil.isHovering(offset + 169.5f, y + 50 + moduleY + 3.5f, 11, 11, mouseX, mouseY)) {
                    if (mouseButton == 0) {
                        dropdownItem = cp;
                        protectArea = new Rectangle(offset + 100, y + moduleY + 50 + 24, 80, cp.isAlphaChangeable() ? 80 : 67);
                    } else {
                        cp.setRainbowEnabled(!cp.isEnabledRainbow());
                    }
                }

                moduleY += property.height;
            }

            moduleY += 4;
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (dropdownItem != null && protectArea != null) {
            if (dropdownItem instanceof ModeValue) {
                final ModeValue property = (ModeValue) dropdownItem;
                int buttonY = 0;
                for (Enum s : property.getModes()) {
                    final boolean isHovering = RenderUtil.isHovering((float) (protectArea.getX() + .5F), (float) (protectArea.getY() + buttonY + .5F), (float) (protectArea.getWidth() - 1), 14F, mouseX, mouseY);
                    if (isHovering && mouseButton == 0) {
                        property.setMode(s.name());
                        dropdownItem = null;
                        protectArea = null;
                        return;
                    }
                    buttonY += 14;
                }
            }

            if (dropdownItem instanceof ColorValue) {
                if (!RenderUtil.isHovering(protectArea.getX(), protectArea.getY(), protectArea.getWidth(), protectArea.getHeight() + 1, mouseX, mouseY)) {
                    dropdownItem = null;
                    protectArea = null;
                }
                return;
            }

        }

        if (!RenderUtil.isHovering(x, y, width, height, mouseX, mouseY)) return;
        float width = Math.max(FontManager.chineseFont38.getStringWidth("SilenceFix".toUpperCase()), FontManager.chineseFont38.getStringWidth("NOVOLINE"));

        if (RenderUtil.isHovering(x + width + 16 + 10, y + 10, 200, 20, mouseX, mouseY) && current != Category.Pages.CONFIGS) {
            searching = !searching;
            if (searching) {
                lists = new List[]{leftModules, rightModules};
                searchTextField.setText("");
            } else {
                leftModules = lists[0];
                rightModules = lists[1];
                resetModuleList();
            }
            searchTextField.setText("");
        }


        float pageY = 44 + CscrollAni;


        if (current == Category.Pages.CONFIGS) {
            boolean hovered = RenderUtil.isHovering(x + width + 16 + 4, y + 10, 89, 20, mouseX, mouseY);
            if (hovered) {
                mc.displayGuiScreen(new SavePresetScreen(this));
            }
            boolean hovered2 = RenderUtil.isHovering(x + width + 16 + 96, y + 10, 89, 20, mouseX, mouseY);
            if (hovered2) {
                Desktop.getDesktop().open(SilenceFix.instance.configManager.dir);
            }
        }
        if (current == Category.Pages.CONFIGS) {
            SilenceFix styles = SilenceFix.instance;
            float configY = y + 40 + 8 + 14 + scrollAni;
            for (String config : styles.configManager.getConfigs()) {
                if (!(config.equals("bw") || config.equals("bwbest") || config.equals("sw") || config.equals("swbest") || config.equals("pvp") || config.equals("pvpbest"))) {
                    continue;
                }

                boolean fuck = RenderUtil.isHovering(x + width + 16 + 6 + 290 - 6, configY + 8, 76, 20, mouseX, mouseY);
                if (fuck) {
                    if (mouseButton == 0) {
                        if ("modules.json".equals(config)) {
                            SilenceFix.instance.configManager.saveUserConfig(config + ".json");
                            NotificationManager.post(NotificationType.SUCCESS, "Config", "您的配置保存成功！");
                        } else {
                            PostProcessing post = SilenceFix.instance.moduleManager.getModule(PostProcessing.class);
                            if (post != null && post.getState()) {
                                post.setState(false);
                            }

                            SilenceFix.instance.configManager.loadUserConfig(config + ".json");
                            NotificationManager.post(NotificationType.SUCCESS, "Config", "您的配置切换成功！");
                        }

                    }
                    }

                configY += 42;
            }
        }
        if (RenderUtil.isHovering(x, y + 44, width + 10, 240, mouseX, mouseY)) {
            for (Category page : Category.values()) {
                pageY += 12;
                for (Category.Pages cate : page.getSubPages()) {
                    if (RenderUtil.isHovering(x + 8, y + pageY, width, 16, mouseX, mouseY) && mouseButton == 0) {
                        if (cate != current) cate.animation.mouseClicked(mouseX, mouseY);
                        scrollAni = 0;
                        scrollY = 0;
                        current = cate;
                        dropdownItem = null;
                        protectArea = null;
                        currentEditing = null;
                        moduleWheel[0] = 0;
                        moduleWheel[1] = 0;
                        resetModuleList();
                        return;
                    }
                    pageY += 26;
                }
                pageY += 4;
            }
        }


        if (current.module && RenderUtil.isHovering(x + width + 16 + 8, y + 40, 400, 400, mouseX, mouseY)) {
            click(leftModules, x + width + 24, mouseX, mouseY, mouseButton);
            click(rightModules, x + width + 24 + 198, mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 0 && RenderUtil.isHovering(x, y, 520, 43, mouseX, mouseY)) {
            dragX = mouseX - x;
            dragY = mouseY - y;
            dragging = true;
        }
    }

    public void setCurrent(Category.Pages current) {
        this.current = current;
    }

    public void resetModuleList() {
        leftModules.clear();
        rightModules.clear();

        final List<Module> allList = new ArrayList<>();

        if (searching) {
           for (Module module : SilenceFix.instance.moduleManager.getModules()) {
                if (module.getName().toLowerCase().replace(" ", "").contains(searchTextField.getText().toLowerCase().replace(" ", "")) || module.getCnName().toLowerCase().replace(" ", "").contains(searchTextField.getText().toLowerCase().replace(" ", "")))
                    allList.add(module);
            }
        } else {
            allList.addAll(SilenceFix.instance.moduleManager.getModsByPage(current));
        }

        allList.sort((o1, o2) -> o2.getValues().size() - o1.getValues().size());

        int updateIndex = 0;
        while (updateIndex <= allList.size() - 1) {
            leftModules.add(allList.get(updateIndex));
            updateIndex += 2;
        }

        updateIndex = 1;
        while (updateIndex <= allList.size() - 1) {
            rightModules.add(allList.get(updateIndex));
            updateIndex += 2;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }


    public class BindScreen extends GuiScreen {
        private final Module target;
        private final GuiScreen parent;

        public BindScreen(Module target, GuiScreen parent) {
            this.target = target;
            this.parent = parent;
            this.allowUserInput = true;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();

            drawCenteredString(this.fontRendererObj, "请按下任意键或鼠标键绑定模块：" + EnumChatFormatting.YELLOW + target.getName(),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFF);
            drawCenteredString(this.fontRendererObj, "按 Delete 清除绑定，按 ESC 取消操作",
                    this.width / 2, this.height / 2 + 10, 0xAAAAAA);

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.displayGuiScreen(parent);
                return;
            }

            if (keyCode == Keyboard.KEY_DELETE) {
                target.setKey(-1);
                HelperUtil.sendMessage("§f已清除模块 [§e" + target.getName() + "§f] 的绑定");
            } else {
                target.setKey(keyCode);
                HelperUtil.sendMessage("§f已将模块 [§e" + target.getName() + "§f] 绑定为 [" + Keyboard.getKeyName(keyCode) + "]");
            }

            mc.displayGuiScreen(parent);
        }

        @Override
        public void handleInput() throws IOException {
            super.handleInput();

            while (Mouse.next()) {
                if (Mouse.getEventButtonState()) {
                    int button = Mouse.getEventButton();
                    if (button >= 0 && button <= 4) {
                        target.setKey(button);
                        HelperUtil.sendMessage("§f已将模块 [§e" + target.getName() + "§f] 绑定为 [" + getMouseButtonName(button) + "]");
                        mc.displayGuiScreen(parent);
                        return;
                    }
                }
            }
        }

        private String getMouseButtonName(int button) {
            return switch (button) {
                case 0 -> "Mouse1";
                case 1 -> "Mouse2";
                case 2 -> "Mouse3";
                case 3 -> "Mouse4";
                case 4 -> "Mouse5";
                default -> "Mouse?";
            };
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }



    public static String getKeyDisplayName(int keyCode) {
        return switch (keyCode) {
            case -1 -> "空";
            case 0 -> "Mouse1";
            case 1 -> "Mouse2";
            case 2 -> "Mouse3";
            case 3 -> "Mouse4";
            case 4 -> "Mouse5";
            default -> Keyboard.getKeyName(keyCode) != null ? Keyboard.getKeyName(keyCode) : "未知";
        };
    }

    public static class SavePresetScreen extends GuiScreen {
        private final GuiScreen parent;
        private GuiTextField nameField;

        public SavePresetScreen(GuiScreen parent) {
            this.parent = parent;
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            super.keyTyped(typedChar, keyCode);

            this.nameField.textboxKeyTyped(typedChar, keyCode);

            if (keyCode == 1) {
                this.mc.displayGuiScreen(parent);
            }

            this.nameField.setText(this.nameField.getText().replace(" ", "").replace("#", "").replace("_NONE", ""));
        }

        public void initGui() {
            this.nameField = new GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, this.width / 2 - 100, this.height / 6 + 20, 200, 20);
            this.buttonList.add(new GuiButton(3, this.width / 2 - 100, this.height / 6 + 40 + 22 * 5, "Add"));
            this.buttonList.add(new GuiButton(4, this.width / 2 - 100, this.height / 6 + 40 + 22 * 6, "Cancel"));
        }

        protected void actionPerformed(GuiButton button) throws IOException {

            if (button.id == 3) {
                SilenceFix.instance.configManager.saveConfig(this.nameField.getText() + ".json");
                mc.displayGuiScreen(this.parent);
            }

            if (button.id == 4) {
                mc.displayGuiScreen(this.parent);
            }
        }


        @Override
        protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
            this.nameField.mouseClicked(mouseX, mouseY, mouseButton);
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public void updateScreen() {
            this.nameField.updateCursorCounter();
            super.updateScreen();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRendererObj, "Name", this.width / 2 - 89, this.height / 6 + 10, 0xFFFFFF);
            this.nameField.drawTextBox();

            this.drawCenteredString(this.fontRendererObj, "Adding Preset", this.width / 2, 30, 0xFFFFFF);

            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    private static String extractAuthor(String json) {
        String key = "\"author\":";
        int startIndex = json.indexOf(key);
        if (startIndex == -1) {
            return null;
        }
        startIndex += key.length();
        int endIndex = json.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = json.indexOf("}", startIndex);
        }
        return json.substring(startIndex, endIndex).replace("\"", "").trim();
    }
}
