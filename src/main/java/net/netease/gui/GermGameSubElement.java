package net.netease.gui;

import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import dev.xinxin.utils.render.animation.impl.RippleAnimation;
import lombok.Getter;
import lombok.Setter;
import net.netease.GsonUtil;
import net.netease.PacketProcessor;
import net.netease.packet.impl.Packet26;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ByteBreaker
 * create 29/12/2023
 */

@Getter
@Setter
public class GermGameSubElement {
    private final int index;
    private final String sid;
    public String name;
    private final List<String> desc;
    private final Animation hoverAnim = new DecelerateAnimation(300,1);
    private RippleAnimation animation;
    private Runnable runnable;

    public GermGameSubElement(int index, String sid, String name, List<String> desc) {
        this.index = index;
        this.sid = sid;
        this.name = name;
        this.desc = desc;
        this.animation = new RippleAnimation();
    }

    public void joinGame(String guiName) {
        Map<String, Object> data = new HashMap<>();
        data.put("entry", index);
        data.put("sid", sid);
        String json = GsonUtil.toJson(data);
        String message = new StringBuilder().insert(0, "GUI$").append(guiName).append("@").append("entry/").append(index).toString();
        PacketProcessor.INSTANCE.setLastGameElement(this);

        PacketProcessor.INSTANCE.sendPacket(new Packet26(message, json));
    }
}
