package com.connectcolor.Util;

public final class LevelSeed {
    private static final long BASE = 0x43C6A7E9D4B2F135L;
    private static final long LEVEL_MIX = 0x9E3779B97F4A7C15L;

    private LevelSeed() {}

    public static long forLevel(Difficulty difficulty, int level) {
        long value = BASE;
        value ^= ((long) difficulty.ordinal() + 1L) * 0xBF58476D1CE4E5B9L;
        value ^= ((long) Math.max(1, level)) * LEVEL_MIX;
        return mix(value);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }
}
