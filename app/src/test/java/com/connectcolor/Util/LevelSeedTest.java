package com.connectcolor.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class LevelSeedTest {
    @Test
    void sameDifficultyAndLevelGiveSameSeed() {
        assertEquals(
            LevelSeed.forLevel(Difficulty.MEDIUM, 12),
            LevelSeed.forLevel(Difficulty.MEDIUM, 12)
        );
    }

    @Test
    void differentLevelChangesSeed() {
        assertNotEquals(
            LevelSeed.forLevel(Difficulty.MEDIUM, 12),
            LevelSeed.forLevel(Difficulty.MEDIUM, 13)
        );
    }

    @Test
    void differentDifficultyChangesSeed() {
        assertNotEquals(
            LevelSeed.forLevel(Difficulty.EASY, 12),
            LevelSeed.forLevel(Difficulty.HARD, 12)
        );
    }
}
