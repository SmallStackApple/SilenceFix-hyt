package net.netease.gui.party;

import dev.xinxin.gui.CustomMenuButton;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ByteBreaker
 * create 02/02/2024
 */
@Data
public class GermPartyData {
    private String text;
    private final List<CustomMenuButton> buttons = new ArrayList<>();
}
