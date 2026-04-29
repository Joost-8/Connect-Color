package com.connectcolor.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;

class PuzzlePreloaderTest {
    @TempDir
    Path tempDir;

    @Test
    void cachedPuzzleLoadsWithoutGeneration() throws Exception {
        PuzzleCache cache = new PuzzleCache(tempDir);
        cache.save(Difficulty.EASY, 1, easyPuzzle(1));
        AtomicInteger generatorCalls = new AtomicInteger();
        PuzzlePreloader preloader = new PuzzlePreloader(
            cache,
            (difficulty, level) -> {
                generatorCalls.incrementAndGet();
                return easyPuzzle(level);
            },
            1
        );

        try {
            Board board = preloader.loadBoard(Difficulty.EASY, 1).get(2, TimeUnit.SECONDS);

            assertEquals(0, generatorCalls.get());
            assertEquals(10, board.getFixedCells().size());
        } finally {
            preloader.shutdown();
        }
    }

    @Test
    void missingPuzzleGeneratesAndSaves() throws Exception {
        PuzzleCache cache = new PuzzleCache(tempDir);
        AtomicInteger generatorCalls = new AtomicInteger();
        PuzzlePreloader preloader = new PuzzlePreloader(
            cache,
            (difficulty, level) -> {
                generatorCalls.incrementAndGet();
                return easyPuzzle(level);
            },
            1
        );

        try {
            Board board = preloader.loadBoard(Difficulty.EASY, 2).get(2, TimeUnit.SECONDS);

            assertEquals(1, generatorCalls.get());
            assertEquals(10, board.getFixedCells().size());
            assertTrue(cache.load(Difficulty.EASY, 2).isPresent());
        } finally {
            preloader.shutdown();
        }
    }

    private CachedPuzzle easyPuzzle(int level) {
        GameSettings settings = Difficulty.EASY.createSettings();
        List<EndpointPair> endpointPairs = List.of(
            new EndpointPair(0, 0, 0, 0, 5),
            new EndpointPair(1, 1, 0, 1, 5),
            new EndpointPair(2, 2, 0, 2, 5),
            new EndpointPair(3, 3, 0, 3, 5),
            new EndpointPair(4, 4, 0, 4, 5)
        );
        return new CachedPuzzle(
            Difficulty.EASY,
            level,
            LevelSeed.forLevel(Difficulty.EASY, level),
            settings,
            endpointPairs
        );
    }
}
