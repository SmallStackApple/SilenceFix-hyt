package com.xinxin.client.utils.misc.fixer;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xinxin.client.viaversion.vialoadingbase.ViaLoadingBase;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.misc.EventMouseOver;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
public class PacketFixer
extends Module {

    public PacketFixer() {
        super("PacketFixer", Category.Misc,"发包修复");
    }

    @EventTarget
    public void onMouseOver(EventMouseOver event) {
        if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() > ProtocolVersion.v1_8.getVersion()) {
            event.setExpand(event.getExpand() - 0.1f);
        }
    }

}
