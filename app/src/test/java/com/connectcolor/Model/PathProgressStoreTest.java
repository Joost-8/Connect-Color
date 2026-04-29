package com.connectcolor.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.connectcolor.Util.CellState;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;

class PathProgressStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndRestoresDrawnPaths() {
        PathProgressStore store = new PathProgressStore(tempDir);
        Board board = easyBoard();
        board.startPathFromFixed(0, 0);
        board.tryDragStep(CellState.White, 0, 0, 0, 1);
        board.tryDragStep(CellState.White, 0, 1, 0, 2);

        store.save(Difficulty.EASY, 1, board);

        Board restored = easyBoard();
        assertTrue(store.load(Difficulty.EASY, 1).isPresent());
        assertTrue(restored.restorePlayerPaths(store.load(Difficulty.EASY, 1).get()));

        assertEquals(CellState.White, restored.getCell(0, 1).getPlayerState());
        assertEquals(CellState.White, restored.getCell(0, 2).getPlayerState());
        assertEquals(0, restored.getHead(CellState.White)[0]);
        assertEquals(2, restored.getHead(CellState.White)[1]);
    }

    @Test
    void savingEmptyBoardDeletesOldPathProgress() {
        PathProgressStore store = new PathProgressStore(tempDir);
        Board board = easyBoard();
        board.startPathFromFixed(0, 0);
        board.tryDragStep(CellState.White, 0, 0, 0, 1);
        store.save(Difficulty.EASY, 1, board);
        assertTrue(store.load(Difficulty.EASY, 1).isPresent());

        store.save(Difficulty.EASY, 1, easyBoard());

        assertFalse(store.load(Difficulty.EASY, 1).isPresent());
    }

    private Board easyBoard() {
        GameSettings settings = Difficulty.EASY.createSettings();
        List<EndpointPair> endpointPairs = List.of(
            new EndpointPair(0, 0, 0, 0, 5),
            new EndpointPair(1, 1, 0, 1, 5),
            new EndpointPair(2, 2, 0, 2, 5),
            new EndpointPair(3, 3, 0, 3, 5),
            new EndpointPair(4, 4, 0, 4, 5)
        );
        return new Board(settings, endpointPairs);
    }
}
