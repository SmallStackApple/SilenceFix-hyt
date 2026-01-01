package net.netease.image;

import net.netease.gui.GermGameElement;
import net.netease.gui.GermGameGui;

import java.io.IOException;

/**
 * @author ByteBreaker
 * create 29/12/2023
 */
public class ImageLoader {
    public static boolean loadedImage;

    public static void loadImage() throws IOException {
        if (!loadedImage) {
            for (GermGameElement element : GermGameGui.INSTANCE.getElements()) {
                element.loadTexture();
                element.loadHoverTexture();
            }
            loadedImage = true;
        }
    }
}
