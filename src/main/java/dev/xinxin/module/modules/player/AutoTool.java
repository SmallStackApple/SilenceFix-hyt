package dev.xinxin.module.modules.player;


import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;

public class AutoTool
        extends Module {
    private int oldSlot;
    private int tick;

    public AutoTool() {
        super("AutoTool", Category.Player,"自动工具");
    }

    @EventTarget
    public void onClick(EventUpdate event) {
            if (AutoTool.mc.playerController.isBreakingBlock()) {
                ++this.tick;
                if (this.tick == 1) {
                    this.oldSlot = AutoTool.mc.thePlayer.inventory.currentItem;
                }
                AutoTool.mc.thePlayer.updateTool(AutoTool.mc.objectMouseOver.getBlockPos());
            } else if (this.tick > 0) {
                mc.thePlayer.inventory.currentItem = oldSlot;
                SilenceFix.instance.slotSpoofManager.stopSpoofing();
                this.tick = 0;
            }
    }
}

