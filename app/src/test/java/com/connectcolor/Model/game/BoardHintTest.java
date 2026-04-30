package com.connectcolor.Model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.connectcolor.Model.algorithm.EndpointPair;
import com.connectcolor.Util.CellState;
import com.connectcolor.Util.GameSettings;

class BoardHintTest {
    @Test
    void applyHintPlacesCompleteStoredPath() {
        Board board = twoPathBoard();

        assertTrue(board.applyHint(CellState.White));

        assertEquals(CellState.White, board.getCell(0, 0).getPlayerState());
        assertEquals(CellState.White, board.getCell(0, 1).getPlayerState());
        assertEquals(CellState.White, board.getFinishedPathColor(0, 0));
        assertEquals(CellState.White, board.getFinishedPathColor(0, 1));
    }

    @Test
    void randomHintChoosesOnlyUnfinishedPaths() {
        Board board = twoPathBoard();
        assertTrue(board.applyHint(CellState.White));

        assertTrue(board.applyRandomHint());

        assertTrue(board.isSolved());
        assertEquals(CellState.Green, board.getFinishedPathColor(1, 0));
        assertEquals(CellState.Green, board.getFinishedPathColor(1, 1));
    }

    @Test
    void hintClearsConflictingPathsAndPreservesUnrelatedProgress() {
        Board board = conflictBoard();
        board.startPathFromFixed(2, 0);
        board.tryDragStep(CellState.Green, 2, 0, 2, 1);
        board.tryDragStep(CellState.Green, 2, 1, 1, 1);
        board.startPathFromFixed(3, 0);
        board.tryDragStep(CellState.Blue, 3, 0, 3, 1);

        assertTrue(board.applyHint(CellState.White));

        assertEquals(CellState.Empty, board.getCell(2, 1).getPlayerState());
        assertNull(board.getHead(CellState.Green));
        assertEquals(CellState.Blue, board.getCell(3, 1).getPlayerState());
        assertEquals(CellState.White, board.getCell(1, 1).getPlayerState());
        assertEquals(CellState.White, board.getFinishedPathColor(1, 1));
    }

    @Test
    void randomHintDoesNothingWithoutStoredSolutions() {
        GameSettings settings = new GameSettings(2, 2, 2, 20);
        List<EndpointPair> endpointPairs = List.of(
            new EndpointPair(0, 0, 0, 0, 1),
            new EndpointPair(1, 1, 0, 1, 1)
        );
        Board board = new Board(settings, endpointPairs);

        assertFalse(board.applyRandomHint());
    }

    private Board twoPathBoard() {
        GameSettings settings = new GameSettings(2, 2, 2, 20);
        List<EndpointPair> endpointPairs = List.of(
            new EndpointPair(0, 0, 0, 0, 1),
            new EndpointPair(1, 1, 0, 1, 1)
        );
        List<List<int[]>> solutionPaths = List.of(
            List.of(new int[] {0, 0}, new int[] {0, 1}),
            List.of(new int[] {1, 0}, new int[] {1, 1})
        );
        return new Board(settings, endpointPairs, solutionPaths);
    }

    private Board conflictBoard() {
        GameSettings settings = new GameSettings(4, 4, 3, 20);
        List<EndpointPair> endpointPairs = List.of(
            new EndpointPair(0, 0, 0, 0, 3),
            new EndpointPair(1, 2, 0, 2, 3),
            new EndpointPair(2, 3, 0, 3, 3)
        );
        List<List<int[]>> solutionPaths = List.of(
            List.of(
                new int[] {0, 0},
                new int[] {1, 0},
                new int[] {1, 1},
                new int[] {1, 2},
                new int[] {1, 3},
                new int[] {0, 3}
            ),
            List.of(
                new int[] {2, 0},
                new int[] {2, 1},
                new int[] {2, 2},
                new int[] {2, 3}
            ),
            List.of(
                new int[] {3, 0},
                new int[] {3, 1},
                new int[] {3, 2},
                new int[] {3, 3}
            )
        );
        return new Board(settings, endpointPairs, solutionPaths);
    }
}
