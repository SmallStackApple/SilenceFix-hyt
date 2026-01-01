package dev.xinxin.utils;

public enum MovementFix {
    HeShuYou("HeShuYou"),
    Silent("XinXin"),
    Strict("SilenceFix"),
    BACKWARDS_SPRINT("Backwards Sprint");

    String name;

    public String toString() {
        return this.name;
    }

    private MovementFix(String name) {
        this.name = name;
    }
}

