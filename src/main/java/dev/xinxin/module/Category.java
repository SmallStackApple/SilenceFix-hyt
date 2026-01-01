package dev.xinxin.module;

import dev.xinxin.utils.render.animation.impl.RippleAnimation;

public enum Category {
    Combat(Pages.COMBAT),
    Movement(Pages.MOVEMENT),
    Render(Pages.RENDER),
    Player(Pages.PLAYER),
    Misc(Pages.MISC),
    Config(Pages.CONFIGS);
    public final Pages pages;

    Category(Pages pages) {
        this.pages = pages;
    }

    public enum Pages {
        COMBAT(true, false),
        MOVEMENT(true, false),
        RENDER(true, false),
        WORLD(true, false),
        PLAYER(true, false),
        MISC(true, false),
        CONFIGS(false, false);
        public final boolean module;
        public final boolean gradient;

        public final RippleAnimation animation;

        Pages(boolean module, boolean gradient) {
            this.module = module;
            this.gradient = gradient;
            this.animation = new RippleAnimation();
        }
    }

    public Pages[] getSubPages() {
        return Pages.values();
    }
}
