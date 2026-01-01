package dev.xinxin.utils;

import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.misc.EventKey;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.gui.clickgui.neverlose.NeverLoseClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

public class TickDispatcher {
    private final boolean[] mousePressed = new boolean[5]; // Mouse0 ~ Mouse4

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (Minecraft.getMinecraft().currentScreen != null &&
                !(Minecraft.getMinecraft().currentScreen instanceof NeverLoseClickGui)) return;

        for (int i = 3; i <= 4; i++) { // Mouse4, Mouse5
            boolean pressed = Mouse.isButtonDown(i);
            if (pressed && !mousePressed[i]) {
                mousePressed[i] = true;
                EventManager.call(new EventKey(i));
            } else if (!pressed && mousePressed[i]) {
                mousePressed[i] = false;
            }
        }
    }


}
