package com.connectcolor.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgressStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void missingFileUsesDefaults() {
        ProgressStore store = new ProgressStore(tempDir.resolve("progress.properties"));

        assertEquals(Difficulty.MEDIUM, store.getSelectedDifficulty());
        for (Difficulty difficulty : Difficulty.values()) {
            assertEquals(1, store.getLevel(difficulty));
        }
    }

    @Test
    void savesAndLoadsDifficultyLevelsIndependently() {
        Path file = tempDir.resolve("progress.properties");
        ProgressStore store = new ProgressStore(file);
        store.setSelectedDifficulty(Difficulty.HARD);
        store.setLevel(Difficulty.EASY, 3);
        store.setLevel(Difficulty.MEDIUM, 7);
        store.save();

        ProgressStore loaded = new ProgressStore(file);

        assertEquals(Difficulty.HARD, loaded.getSelectedDifficulty());
        assertEquals(3, loaded.getLevel(Difficulty.EASY));
        assertEquals(7, loaded.getLevel(Difficulty.MEDIUM));
        assertEquals(1, loaded.getLevel(Difficulty.HARD));
    }

    @Test
    void malformedValuesFallBackToDefaults() throws IOException {
        Path file = tempDir.resolve("progress.properties");
        Files.writeString(
            file,
            "selectedDifficulty=what\n"
            + "easy.level=bad\n"
            + "medium.level=-2\n"
            + "hard.level=4\n"
        );

        ProgressStore store = new ProgressStore(file);

        assertEquals(Difficulty.MEDIUM, store.getSelectedDifficulty());
        assertEquals(1, store.getLevel(Difficulty.EASY));
        assertEquals(1, store.getLevel(Difficulty.MEDIUM));
        assertEquals(4, store.getLevel(Difficulty.HARD));
        assertEquals(1, store.getLevel(Difficulty.EXPERT));
    }

    @Test
    void incrementLevelSavesNextLevelInMemory() {
        ProgressStore store = new ProgressStore(tempDir.resolve("progress.properties"));

        assertEquals(2, store.incrementLevel(Difficulty.EXPERT));
        assertEquals(2, store.getLevel(Difficulty.EXPERT));
    }
}
