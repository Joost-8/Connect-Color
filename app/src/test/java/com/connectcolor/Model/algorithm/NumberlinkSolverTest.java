package com.connectcolor.Model.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.connectcolor.Util.CellState;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;

class NumberlinkSolverTest {
    @Test
    void countsSingleFullCoverSolution() {
        List<EndpointPair> pairs = List.of(
            new EndpointPair(0, 0, 0, 0, 1),
            new EndpointPair(1, 1, 0, 1, 1)
        );

        assertEquals(
            NumberlinkSolver.CountResult.ONE,
            NumberlinkSolver.countSolutions(2, 2, pairs)
        );
    }

    @Test
    void countsNoSolution() {
        List<EndpointPair> pairs = List.of(
            new EndpointPair(0, 0, 0, 1, 1),
            new EndpointPair(1, 0, 1, 1, 0)
        );

        assertEquals(
            NumberlinkSolver.CountResult.ZERO,
            NumberlinkSolver.countSolutions(2, 2, pairs)
        );
    }

    @Test
    void countsMultipleFullCoverSolutions() {
        List<EndpointPair> pairs = List.of(
            new EndpointPair(0, 0, 0, 2, 2)
        );

        assertEquals(
            NumberlinkSolver.CountResult.MULTIPLE,
            NumberlinkSolver.countSolutions(3, 3, pairs, 2, 1_000_000L, 1_000L)
        );
    }

    @Test
    void countsMultipleHamiltonianPathsOnLargerOpenGrid() {
        List<EndpointPair> pairs = List.of(
            new EndpointPair(0, 0, 0, 3, 2)
        );

        assertEquals(
            NumberlinkSolver.CountResult.MULTIPLE,
            NumberlinkSolver.countSolutions(4, 4, pairs, 2, 1_000_000L, 1_000L)
        );
    }

    @Test
    void directConnectionThatLeavesCellsUnusedDoesNotCount() {
        List<EndpointPair> pairs = List.of(
            new EndpointPair(0, 0, 0, 0, 1)
        );

        assertEquals(
            NumberlinkSolver.CountResult.ZERO,
            NumberlinkSolver.countSolutions(3, 3, pairs, 2, 1_000_000L, 1_000L)
        );
    }

    @Test
    void solverReportsUnknownWhenSearchLimitPreventsProof() {
        List<EndpointPair> pairs = List.of(
            new EndpointPair(0, 0, 0, 2, 2)
        );

        assertEquals(
            NumberlinkSolver.CountResult.UNKNOWN_LIMIT,
            NumberlinkSolver.countSolutions(3, 3, pairs, 2, 1L, 1_000L)
        );
    }

    @Test
    void uniqueGeneratorFailsClearlyWhenCandidateLimitIsTooSmall() {
        assertThrows(
            IllegalStateException.class,
            () -> NumberlinkGenerator.generateUnique(6, 6, 1234L, 0, 1_000L, 10L)
        );
    }

    @Test
    void uniqueGeneratorReturnsPuzzleWithOneSolution() {
        Grid puzzle = NumberlinkGenerator.generateUnique(6, 6, 1234L, 20, 1_000_000L, 1_000L);
        List<EndpointPair> pairs = NumberlinkGenerator.extractEndpointPairs(puzzle);

        assertEquals(
            NumberlinkSolver.CountResult.ONE,
            NumberlinkSolver.countSolutions(6, 6, pairs, 2, 1_000_000L, 1_000L)
        );
    }

    @Test
    void extractedSolutionPathsAlignWithEndpointPairs() {
        Grid puzzle = NumberlinkGenerator.generateUnique(6, 6, 1234L, 20, 1_000_000L, 1_000L);
        List<EndpointPair> pairs = NumberlinkGenerator.extractEndpointPairs(puzzle);
        List<List<int[]>> solutionPaths = NumberlinkGenerator.extractSolutionPaths(puzzle);

        assertEquals(pairs.size(), solutionPaths.size());
        for (int i = 0; i < pairs.size(); i++) {
            EndpointPair pair = pairs.get(i);
            List<int[]> path = solutionPaths.get(i);
            int[] first = path.get(0);
            int[] last = path.get(path.size() - 1);

            assertEquals(pair.getStartRow(), first[0]);
            assertEquals(pair.getStartCol(), first[1]);
            assertEquals(pair.getEndRow(), last[0]);
            assertEquals(pair.getEndCol(), last[1]);

            for (int step = 1; step < path.size(); step++) {
                int[] previous = path.get(step - 1);
                int[] current = path.get(step);
                int distance = Math.abs(previous[0] - current[0]) + Math.abs(previous[1] - current[1]);
                assertEquals(1, distance);
            }
        }
    }

    @Test
    void mediumUniqueGeneratorReturnsPuzzleWithOneSolution() {
        Grid puzzle = NumberlinkGenerator.generateUnique(8, 8, 1234L, 20, 1_000_000L, 1_000L);
        List<EndpointPair> pairs = NumberlinkGenerator.extractEndpointPairs(puzzle);

        assertEquals(
            NumberlinkSolver.CountResult.ONE,
            NumberlinkSolver.countSolutions(8, 8, pairs, 2, 1_000_000L, 1_000L)
        );
    }

    @Test
    void generatorHonorsExactDifficultyPairCounts() {
        for (Difficulty difficulty : Difficulty.values()) {
            GameSettings settings = difficulty.createSettings();
            long seed = LevelSeed.forLevel(difficulty, 1);

            Grid puzzle = NumberlinkGenerator.generate(
                settings.getCols(),
                settings.getRows(),
                seed,
                settings.getPairs(),
                settings.getPairs()
            );

            assertEquals(
                settings.getPairs(),
                NumberlinkGenerator.extractEndpointPairs(puzzle).size(),
                difficulty + " should use its configured pair count"
            );
        }
    }

    @Test
    void hardPairCountFitsAvailableColors() {
        assertTrue(Difficulty.HARD.createSettings().getPairs() <= CellState.getColorStates().size());

        GameSettings settings = Difficulty.HARD.createSettings();
        Grid puzzle = NumberlinkGenerator.generate(
            settings.getCols(),
            settings.getRows(),
            LevelSeed.forLevel(Difficulty.HARD, 1),
            settings.getPairs(),
            settings.getPairs()
        );

        assertEquals(settings.getPairs(), NumberlinkGenerator.extractEndpointPairs(puzzle).size());
    }

    @Test
    void earlyMediumLevelsAreStrictlyUnique() {
        GameSettings settings = Difficulty.MEDIUM.createSettings();

        for (int level = 1; level <= 2; level++) {
            Grid puzzle = NumberlinkGenerator.generateUnique(
                settings.getCols(),
                settings.getRows(),
                LevelSeed.forLevel(Difficulty.MEDIUM, level),
                settings.getPairs()
            );
            List<EndpointPair> pairs = NumberlinkGenerator.extractEndpointPairs(puzzle);

            assertEquals(settings.getPairs(), pairs.size());
            assertEquals(
                NumberlinkSolver.CountResult.ONE,
                NumberlinkSolver.countSolutions(settings.getRows(), settings.getCols(), pairs, 2, 1_000_000L, 1_000L),
                "Medium level " + level + " should have one full-cover solution"
            );
        }
    }
}
