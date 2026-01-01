package dev.xinxin.module.modules.world;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventMoveInput;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.AutoProjectile;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.modules.world.Stuck;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.DebugUtil;
import dev.xinxin.utils.InventoryUtil;
import dev.xinxin.utils.ProjectileUtil;
import dev.xinxin.utils.TimerUtil;
import dev.xinxin.utils.client.MathUtil;
import dev.xinxin.utils.component.FallDistanceComponent;
import dev.xinxin.utils.player.PlayerUtil;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;

import javax.vecmath.Vector2f;
import java.awt.*;

public class AntiVoid extends Module {
    public final NumberValue scaffoldFallDistance = new NumberValue("Scaffold Fall Distance", 3, 1, 5, 1);
    private final NumberValue stuckFallDistance = new NumberValue("Stuck Fall Distance", 6, 5, 10, 1);
    private boolean attempted;

    private Scaffold scaffold = null;
    private Stuck stuck = null;

    boolean overVoid;

    public AntiVoid() {
        super("AntiVoid", Category.Misc, "自动虚空自救");
    }


    @EventTarget
    public void onMotion(EventMotion event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        scaffold = getModule(Scaffold.class);
        stuck = getModule(Stuck.class);
        overVoid = !mc.thePlayer.onGround && !PlayerUtil.isBlockUnder(40.0, true);

        if (mc.thePlayer.onGround) {
            this.attempted = false;
        }
        if (!this.attempted && !mc.thePlayer.onGround && overVoid && FallDistanceComponent.distance > scaffoldFallDistance.getValue().floatValue() && FallDistanceComponent.distance < stuckFallDistance.getValue().floatValue() && !scaffold.getState()) {
            scaffold.setState(true);
        } else if (!this.attempted && !mc.thePlayer.onGround && overVoid && FallDistanceComponent.distance > stuckFallDistance.getValue().floatValue()) {
            FallDistanceComponent.distance = 0.0f;
            this.attempted = true;


//            scaffold.setState(false);
            stuck.setState(true);
        }
    }

    private int findBestPearlSlot() {
        for (int i = InventoryUtil.ONLY_HOT_BAR_BEGIN; i < InventoryUtil.END; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemEnderPearl) {
                return i;
            }
        }
        for (int i = InventoryUtil.INCLUDE_ARMOR_BEGIN; i < InventoryUtil.ONLY_HOT_BAR_BEGIN; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemEnderPearl) {

                return i;
            }
        }

        return -1;
    }

}