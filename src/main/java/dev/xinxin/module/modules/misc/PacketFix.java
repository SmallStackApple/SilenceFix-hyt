package dev.xinxin.module.modules.misc;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xinxin.client.viaversion.vialoadingbase.ViaLoadingBase;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.misc.EventMouseOver;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;

public class PacketFix extends Module {
    public PacketFix() {
        super("PacketFix", Category.Misc,"发包");
    }
    @EventTarget
    public void onMouseOver(EventMouseOver event) {
        if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() > ProtocolVersion.v1_8.getVersion()) {
            event.setExpand(event.getExpand() - 0.1f);
        }
    }


}
