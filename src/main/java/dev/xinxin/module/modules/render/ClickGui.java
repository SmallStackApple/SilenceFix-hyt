package dev.xinxin.module.modules.render;

import dev.xinxin.gui.clickgui.MilkpowderClickgui.NewClickGui;
import dev.xinxin.gui.clickgui.drop.DropdownClickGUI;
import dev.xinxin.gui.clickgui.express.NormalClickGUI;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.ModeValue;
import org.lwjgl.input.Keyboard;

public class ClickGui
        extends Module {
    public ModeValue<ClickGuiMode> mode = new ModeValue("Mode", (Enum[])ClickGuiMode.values(), (Enum)ClickGuiMode.Naven);

    public ClickGui() {
        super("ClickGui", Category.Render,"配置界面");
        this.setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnable() {
        switch (this.mode.getValue()) {
            case Normal: {
                mc.displayGuiScreen(new NormalClickGUI());
                break;
            }
            case DropDown: {
                mc.displayGuiScreen(new DropdownClickGUI());
                break;
            }
            case Naven: {
                mc.displayGuiScreen(new NewClickGui());
                break;
            }
        }
        this.setState(false);
    }

    public static enum ClickGuiMode {
        Normal,
        DropDown,
        Naven

    }
}

