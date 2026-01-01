package dev.xinxin.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeAccess {
    private static final Unsafe unsafe;

    static {
        try {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);

            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Unsafe getUnsafe() {
        return unsafe;
    }
}
