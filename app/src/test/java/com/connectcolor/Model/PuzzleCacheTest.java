package com.connectcolor.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.connectcolor.Util.CellState;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;

class PuzzleCacheTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsPuzzleRoundTrip() {
        PuzzleCache cache = new PuzzleCache(tempDir);
        CachedPuzzle puzzle = easyPuzzle(1);

        cache.save(Difficulty.EASY, 1, puzzle);

        var loaded = cache.load(Difficulty.EASY, 1);
        assertTrue(loaded.isPresent());
        assertEquals(Difficulty.EASY, loaded.get().getDifficulty());
        assertEquals(1, loaded.get().getLevel());
        assertEquals(LevelSeed.forLevel(Difficulty.EASY, 1), loaded.get().getSeed());
        assertEquals(5, loaded.get().getEndpointPairs().size());
    }

    @Test
    void rejectsWrongVersion() throws IOException {
        PuzzleCache cache = new PuzzleCache(tempDir);
        cache.save(Difficulty.EASY, 1, easyPuzzle(1));
        replaceInCacheFile("version=1", "version=0");

        assertFalse(cache.load(Difficulty.EASY, 1).isPresent());
    }

    @Test
    void rejectsWrongDimensionsAndPairCount() throws IOException {
        PuzzleCache cache = new PuzzleCache(tempDir);
        cache.save(Difficulty.EASY, 1, easyPuzzle(1));
        replaceInCacheFile("rows=6", "rows=7");

        assertFalse(cache.load(Difficulty.EASY, 1).isPresent());

        cache.save(Difficulty.EASY, 1, easyPuzzle(1));
        replaceInCacheFile("pairs=5", "pairs=4");

        assertFalse(cache.load(Difficulty.EASY, 1).isPresent());
    }

    @Test
    void rejectsMalformedEndpointData() throws IOException {
        PuzzleCache cache = new PuzzleCache(tempDir);
        cache.save(Difficulty.EASY, 1, easyPuzzle(1));
        replaceInCacheFile("endpoint.0=0,0,0,5", "endpoint.0=0,0,0,0");

        assertFalse(cache.load(Difficulty.EASY, 1).isPresent());
    }

    @Test
    void boardCanBeCreatedFromCachedEndpointPairs() {
        CachedPuzzle puzzle = easyPuzzle(1);

        Board board = puzzle.createBoard();

        assertEquals(10, board.getFixedCells().size());
        assertEquals(CellState.White, board.getCell(0, 0).getSolutionState());
        assertEquals(CellState.White, board.getCell(0, 5).getSolutionState());
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

    private void replaceInCacheFile(String target, String replacement) throws IOException {
        Path file = tempDir.resolve("easy-1.properties");
        String content = Files.readString(file);
        Files.writeString(file, content.replace(target, replacement));
    }
}
