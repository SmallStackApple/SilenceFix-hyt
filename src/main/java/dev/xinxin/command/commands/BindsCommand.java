package dev.xinxin.command.commands;

import dev.xinxin.SilenceFix;
import dev.xinxin.command.Command;
import dev.xinxin.module.Module;
import dev.xinxin.utils.DebugUtil;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class BindsCommand
extends Command {
    public BindsCommand() {
        super("binds");
    }

    @Override
    public List<String> autoComplete(int arg, String[] args) {
        return new ArrayList<String>();
    }

    @Override
    public void run(String[] args) {
        for (Module module : SilenceFix.instance.moduleManager.getModules()) {
            if (module.getKey() == -1) continue;
            DebugUtil.log("\u00a7a[Binds]\u00a7f" + module.name + " :" + Keyboard.getKeyName((int)module.key));
        }
    }
}

