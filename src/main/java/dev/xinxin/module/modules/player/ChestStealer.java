package dev.xinxin.module.modules.player;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.world.ChestAura;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.HYTUtils;
import dev.xinxin.utils.InventoryUtil;
import dev.xinxin.utils.client.StopWatch;
import dev.xinxin.utils.item.ItemUtils;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.vec.Vector3d;
import dev.xinxin.utils.vec.Vector4d;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.StencilUtils;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.inventory.Container;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

public class ChestStealer
        extends Module {
    private final BoolValue chest = new BoolValue("Chest", true);
    private final BoolValue furnace = new BoolValue("Furnace", true);
    private final BoolValue brewingStand = new BoolValue("BrewingStand", true);
    public static final StopWatch timer = new StopWatch();
    public static boolean isChest = false;
    public static StopWatch openChestTimer = new StopWatch();
    private final NumberValue delay = new NumberValue("StealDelay", 100.0, 0.0, 1000.0, 10.0);
    private final BoolValue trash = new BoolValue("PickTrash", true);
    public final BoolValue silentValue = new BoolValue("Silent", true);
    public final BoolValue ui = new BoolValue("DisplayUI",true);
    private int nextDelay = 0;
    public static BlockPos currentContainerPos;
    public static boolean working;
    public static final StopWatch stealCompleteTimer = new StopWatch();
    public static boolean isStealing = false;
    public ChestStealer() {
        super("Stealer", Category.Player,"箱子秒拿");
    }


    @Override
    public void onEnable() {
        this.working = false;
    }

    @Override
    public void onDisable() {
        this.working = false;
    }

    public Vector4d calculate(BlockPos blockPos, int factor) {
        try {
            mc.getRenderManager();
            double renderX = RenderManager.getRenderPosX();
            mc.getRenderManager();
            double renderY = RenderManager.getRenderPosY();
            mc.getRenderManager();
            double renderZ = RenderManager.getRenderPosZ();
            double x = (double)blockPos.getX() + 0.5 - renderX;
            double y = (double)blockPos.getY() + 0.5 - renderY;
            double z = (double)blockPos.getZ() + 0.5 - renderZ;
            Vector3d projectedCenter = project(x, y, z, factor);
            if (projectedCenter != null && projectedCenter.getZ() >= 0.0 && projectedCenter.getZ() < 1.0) {
                return new Vector4d(projectedCenter.getX(), projectedCenter.getY(), projectedCenter.getX(), projectedCenter.getY());
            }
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }
    private static Vector3d project(double x, double y, double z, int factor) {
        if (GLU.gluProject((float)((float)x), (float)((float)y), (float)((float)z), (FloatBuffer) ActiveRenderInfo.MODELVIEW, (FloatBuffer)ActiveRenderInfo.PROJECTION, (IntBuffer)ActiveRenderInfo.VIEWPORT, (FloatBuffer)ActiveRenderInfo.OBJECTCOORDS)) {
            return new Vector3d(ActiveRenderInfo.OBJECTCOORDS.get(0) / (float)factor, ((float) Display.getHeight() - ActiveRenderInfo.OBJECTCOORDS.get(1)) / (float)factor, ActiveRenderInfo.OBJECTCOORDS.get(2));
        }
        return null;
    }

    @EventTarget
    public void onSend(EventPacketSend event) {
        if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement) event.getPacket();
            BlockPos pos = packet.getPosition();
            if (pos != null) {
                currentContainerPos = pos;
                working = true;
                canOpenNewContainer = false;
            }
        }
    }


    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (!ui.getValue()) return;
        if (mc.currentScreen == null) return;
        if (currentContainerPos == null) return;

        net.minecraft.client.gui.ScaledResolution res = new net.minecraft.client.gui.ScaledResolution(mc);
        int factor = res.getScaleFactor();
        Vector4d pos = calculate(currentContainerPos, factor);
        if (pos == null) return;

        Container container = mc.thePlayer.openContainer;
        int slots = 0;
        boolean show = false;
        if (container instanceof ContainerChest && chest.getValue()) {
            slots = ((ContainerChest) container).getLowerChestInventory().getSizeInventory();
            show = true;
        } else if (container instanceof ContainerFurnace && furnace.getValue()) {
            slots = ((ContainerFurnace) container).tileFurnace.getSizeInventory();
            show = true;
        } else if (container instanceof ContainerBrewingStand && brewingStand.getValue()) {
            slots = ((ContainerBrewingStand) container).tileBrewingStand.getSizeInventory();
            show = true;
        }
        if (!show) return;

        float left = (float) pos.getX() - 85f;
        float top = (float) pos.getY() - 90f;

        GlStateManager.pushMatrix();
        RenderHelper.disableStandardItemLighting();

        int headerColor = new Color(150, 45, 45, 255).getRGB();
        int bodyColor = new Color(0, 0, 0, 190).getRGB();

        int radius = 5;
        int width = 170;
        int height = 62;
        int headerH = 3;

        int boxX = Math.round(left);
        int boxY = Math.round(top);
        int boxW = width;
        int boxH = height;

        RoundedUtils.drawRound(boxX, boxY, boxW, boxH, radius, new Color(bodyColor, true));
        RoundedUtils.drawRound(boxX, boxY, boxW, headerH, radius, new Color(headerColor, true));
        ShaderElement.addBlurTask(() -> {
            RoundedUtils.drawRound(boxX, boxY, boxW, boxH, radius, new Color(0, 0, 0, 200));
        });
        ShaderElement.addBloomTask(() -> {
            RoundedUtils.drawRound(boxX, boxY, boxW, boxH, radius, new Color(0, 0, 0, 200));
        });

        int count = 0;
        int ox = 0, oy = 0;

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableAlpha();
        GlStateManager.clear(256);
        mc.getRenderItem().zLevel = -150.0F;

        for (int i = 0; i < slots; i++) {
            ItemStack stack = null;
            if (container instanceof ContainerChest) {
                stack = ((ContainerChest) container).getLowerChestInventory().getStackInSlot(i);
            } else if (container instanceof ContainerFurnace) {
                stack = ((ContainerFurnace) container).tileFurnace.getStackInSlot(i);
            } else if (container instanceof ContainerBrewingStand) {
                stack = ((ContainerBrewingStand) container).tileBrewingStand.getStackInSlot(i);
            }

            if (stack != null) {
                int x1 = boxX + 5 + ox;
                int y1 = boxY + 5 + oy;

                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableBlend();
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableTexture2D();
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();

                mc.getRenderItem().renderItemIntoGUI(stack, x1, y1);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x1, y1);
            }

            ox += 18;
            if (++count >= 9) {
                count = 0;
                ox = 0;
                oy += 18;
            }
        }

        mc.getRenderItem().zLevel = 0.0F;
        GlStateManager.enableAlpha();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
    }










    @EventTarget
    public void onWorld(EventWorldLoad event) {
        this.working = false;
    }

    private void markContainerAsOpened(BlockPos pos) {
        if (pos != null) {
            ChestAura.confirmedOpenedContainers.add(pos);
        }
    }

    private boolean canOpenNewContainer = true;

    private boolean canStealAnything(ContainerChest chestContainer) {
        if (isInventoryFull()) return false;
        for (int i = 0; i < chestContainer.getLowerChestInventory().getSizeInventory(); ++i) {
            ItemStack stack = chestContainer.getLowerChestInventory().getStackInSlot(i);
            if (stack == null) continue;
            if (isItemUseful(chestContainer, i)) return true;
        }
        return false;
    }


    private boolean canMergeIntoPlayer(ItemStack stack) {
        if (!stack.isStackable()) return false;
        for (int i = 9; i < 45; i++) {
            Slot s = mc.thePlayer.inventoryContainer.getSlot(i);
            if (!s.getHasStack()) return true;
            ItemStack in = s.getStack();
            if (ItemStack.areItemsEqual(stack, in)
                    && ItemStack.areItemStackTagsEqual(stack, in)
                    && in.stackSize < in.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }


    private final Set<Integer> clickedSlots = new HashSet<>();


    @EventTarget
    public void onMotion(EventUpdate event) {
        if (!stealCompleteTimer.finished(150)) {
            working = false;
            return;
        }

        if (HYTUtils.isInLobby()) {
            working = false;
            return;
        }

        isStealing = true;

        try {
            if (mc.currentScreen != null && currentContainerPos != null) {
                working = true;
                markContainerAsOpened(currentContainerPos);
            }

            if (mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem() != null &&
                    mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                return;
            }

            if (mc.currentScreen != null) {
                Container container = mc.thePlayer.openContainer;
                BlockPos containerPos = currentContainerPos;
                markContainerAsOpened(containerPos);

                // 炉子
                if (container instanceof ContainerFurnace && furnace.getValue()) {
                    ContainerFurnace furnaceContainer = (ContainerFurnace) container;

                    if (isFurnaceEmpty(furnaceContainer) || isInventoryFull()) {
                        if (openChestTimer.finished(150) && timer.finished(150)) {
                            dropMouseItemIfNeeded(container);
                            if (!canSafelyClose()) {
                                working = true;
                                return;
                            }
                            mc.thePlayer.closeScreen();
                            working = false;
                            return;
                        }
                    }

                    for (int i = 0; i < furnaceContainer.tileFurnace.getSizeInventory(); ++i) {
                        if (furnaceContainer.tileFurnace.getStackInSlot(i) != null && !isInventoryFull() && timer.finished(nextDelay)) {
                            mc.playerController.windowClick(furnaceContainer.windowId, i, 0, 1, mc.thePlayer);
                            nextDelay = (int) (delay.getValue() * MathHelper.getRandomDoubleInRange(0.75, 1.25));
                            timer.reset();
                        }
                    }

                    // 酿造台
                } else if (container instanceof ContainerBrewingStand && brewingStand.getValue()) {
                    ContainerBrewingStand brewingStandContainer = (ContainerBrewingStand) container;

                    if (isBrewingStandEmpty(brewingStandContainer) || isInventoryFull()) {
                        if (openChestTimer.finished(150) && timer.finished(150)) {
                            dropMouseItemIfNeeded(container);
                            if (!canSafelyClose()) {
                                working = true;
                                return;
                            }
                            mc.thePlayer.closeScreen();
                            working = false;
                            return;
                        }
                    }

                    for (int i = 0; i < brewingStandContainer.tileBrewingStand.getSizeInventory(); ++i) {
                        if (brewingStandContainer.tileBrewingStand.getStackInSlot(i) != null && !isInventoryFull() && timer.finished(nextDelay)) {
                            mc.playerController.windowClick(brewingStandContainer.windowId, i, 0, 1, mc.thePlayer);
                            nextDelay = (int) (delay.getValue() * MathHelper.getRandomDoubleInRange(0.75, 1.25));
                            timer.reset();
                        }
                    }

                    // 箱子
                } else if (container instanceof ContainerChest && chest.getValue() && isChest) {
                    ContainerChest chestContainer = (ContainerChest) container;

                    for (int i = 0; i < chestContainer.getLowerChestInventory().getSizeInventory(); ++i) {
                        ItemStack stack = chestContainer.getLowerChestInventory().getStackInSlot(i);
                        if (stack != null && stack.getItem() == Items.golden_apple) {
                            break;
                        }
                    }

                    if (!canStealAnything(chestContainer)) {
                        if (openChestTimer.finished(150) && timer.finished(150)) {
                            dropMouseItemIfNeeded(container);
                            mc.thePlayer.closeScreen();
                            working = false;
                            return;
                        }
                    }

                    for (int i = 0; i < chestContainer.getLowerChestInventory().getSizeInventory(); ++i) {
                        ItemStack stack = chestContainer.getLowerChestInventory().getStackInSlot(i);
                        if (stack != null && stack.getItem() == Items.golden_apple && !isInventoryFull() && timer.finished(nextDelay)) {
                            mc.playerController.windowClick(chestContainer.windowId, i, 0, 1, mc.thePlayer);
                            clickedSlots.add(i);
                            nextDelay = (int) (double) delay.getValue();
                            timer.reset();
                            ChestStealer.stealingNow = true;
                            ChestStealer.lastStealTime = System.currentTimeMillis();
                            return;
                        }
                    }
                    for (int i = 0; i < chestContainer.getLowerChestInventory().getSizeInventory(); ++i) {
                        if (clickedSlots.contains(i)) continue;

                        ItemStack stack = chestContainer.getLowerChestInventory().getStackInSlot(i);
                        if (stack != null && (isItemUseful(chestContainer, i) || trash.getValue()) && !isInventoryFull() && timer.finished(nextDelay)) {
                            mc.playerController.windowClick(chestContainer.windowId, i, 0, 1, mc.thePlayer);
                            nextDelay = (int) (double) delay.getValue();
                            timer.reset();
                            ChestStealer.stealingNow = true;
                            ChestStealer.lastStealTime = System.currentTimeMillis();
                        }
                    }
                }
            }
        } finally {
            isStealing = false;
            if (!working && stealCompleteTimer.finished(100)) {
                stealCompleteTimer.reset();
                clickedSlots.clear();
            }
        }

        if (System.currentTimeMillis() - lastStealTime >= 300) {
            stealingNow = false;
        }
    }





    private void dropMouseItemIfNeeded(Container container) {
        if (mc.thePlayer.inventory.getItemStack() == null) return;
        ItemStack carried = mc.thePlayer.inventory.getItemStack();
        for (Slot s : container.inventorySlots) {
            if (s.inventory != mc.thePlayer.inventory && !s.getHasStack()) {
                mc.playerController.windowClick(container.windowId, s.slotNumber, 0, 0, mc.thePlayer);
                return;
            }
        }
        for (Slot s : container.inventorySlots) {
            if (s.inventory != mc.thePlayer.inventory && s.getHasStack()) {
                ItemStack inSlot = s.getStack();
                if (ItemStack.areItemsEqual(carried, inSlot)
                        && ItemStack.areItemStackTagsEqual(carried, inSlot)
                        && inSlot.stackSize < inSlot.getMaxStackSize()) {
                    mc.playerController.windowClick(container.windowId, s.slotNumber, 0, 0, mc.thePlayer);
                    return;
                }
            }
        }
        for (Slot s : container.inventorySlots) {
            if (s.inventory == mc.thePlayer.inventory && !s.getHasStack()) {
                mc.playerController.windowClick(container.windowId, s.slotNumber, 0, 0, mc.thePlayer);
                return;
            }
        }
        for (Slot s : container.inventorySlots) {
            if (s.inventory == mc.thePlayer.inventory && s.getHasStack()) {
                ItemStack inSlot = s.getStack();
                if (ItemStack.areItemsEqual(carried, inSlot)
                        && ItemStack.areItemStackTagsEqual(carried, inSlot)
                        && inSlot.stackSize < inSlot.getMaxStackSize()) {
                    mc.playerController.windowClick(container.windowId, s.slotNumber, 0, 0, mc.thePlayer);
                    return;
                }
            }
        }
        mc.thePlayer.inventory.setItemStack(null);
    }



    private boolean canSafelyClose() {
        return mc.thePlayer.inventory.getItemStack() == null;
    }


    public static boolean stealingNow = false;
    public static long lastStealTime = 0;


    @EventTarget
    public void onPacket(EventPacketReceive event) {
        if (event.getPacket() instanceof S2EPacketCloseWindow) {
            working = false;
            isStealing = false;
            currentContainerPos = null;
            stealCompleteTimer.reset();
            canOpenNewContainer = true;
        }
    }



    private boolean isFurnaceEmpty(ContainerFurnace c) {
        for (int i = 0; i < c.tileFurnace.getSizeInventory(); ++i) {
            if (c.tileFurnace.getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean isBrewingStandEmpty(ContainerBrewingStand c) {
        for (int i = 0; i < c.tileBrewingStand.getSizeInventory(); ++i) {
            if (c.tileBrewingStand.getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean isInventoryFull() {
        for (int i = 9; i < 45; i++) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                return false;
            }
        }
        return true;
    }

    private boolean isItemUseful(ContainerChest c, int i) {
        ItemStack itemStack = c.getLowerChestInventory().getStackInSlot(i);
        Item item = itemStack.getItem();
        if (item == Items.golden_apple)
            return true;
        if (item instanceof ItemAxe || item instanceof ItemPickaxe) {
            return true;
        }
        if (item instanceof ItemSnowball)
            return true;
        if (item instanceof ItemEgg)
            return true;
        if (item instanceof ItemFood)
            return true;
        if (item instanceof ItemBow || item == Items.arrow)
            return true;

        if (item instanceof ItemPotion && !ItemUtils.isPotionNegative(itemStack))
            return true;
        if (item instanceof ItemSword && InventoryUtil.isBestSword(mc.thePlayer, itemStack))
            return true;
        if (item instanceof ItemArmor && ItemUtils.isBestArmor(c, itemStack))
            return true;
        if (item instanceof ItemBlock)
            return true;

        return item instanceof ItemEnderPearl;
    }
}

