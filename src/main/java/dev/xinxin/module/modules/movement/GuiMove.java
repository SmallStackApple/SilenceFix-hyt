package dev.xinxin.module.modules.movement;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.gui.clickgui.MilkpowderClickgui.NewClickGui;
import dev.xinxin.gui.clickgui.drop.DropdownClickGUI;
import dev.xinxin.gui.clickgui.express.NormalClickGUI;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

public class GuiMove
extends Module {
    private static final List<KeyBinding> keys = Arrays.asList(GuiMove.mc.gameSettings.keyBindForward, GuiMove.mc.gameSettings.keyBindBack, GuiMove.mc.gameSettings.keyBindLeft, GuiMove.mc.gameSettings.keyBindRight, GuiMove.mc.gameSettings.keyBindJump);

    public GuiMove() {
        super("InvMove", Category.Movement,"背包移动");
    }

    public static void updateStates() {
        if (GuiMove.mc.currentScreen != null) {
            for (KeyBinding k2 : keys) {
                k2.setPressed(GameSettings.isKeyDown(k2));
                if (Keyboard.isKeyDown(Keyboard.KEY_UP) && GuiMove.mc.thePlayer.rotationPitch > -90.0f) {
                    GuiMove.mc.thePlayer.rotationPitch -= 5.0f;
                }
                if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) && GuiMove.mc.thePlayer.rotationPitch < 90.0f) {
                    GuiMove.mc.thePlayer.rotationPitch += 5.0f;
                }
                if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                    GuiMove.mc.thePlayer.rotationYaw -= 5.0f;
                }
                if (!Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) continue;
                GuiMove.mc.thePlayer.rotationYaw += 5.0f;
            }
        }
    }

    @EventTarget
    public void onMotion(EventUpdate event) {
        if (GuiMove.mc.currentScreen instanceof GuiContainer || GuiMove.mc.currentScreen instanceof NormalClickGUI || GuiMove.mc.currentScreen instanceof DropdownClickGUI || GuiMove.mc.currentScreen instanceof NewClickGui) {
            GuiMove.updateStates();
        }
    }
}

