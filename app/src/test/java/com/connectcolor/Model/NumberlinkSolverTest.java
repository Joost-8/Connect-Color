package com.connectcolor.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

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
    void mediumUniqueGeneratorReturnsPuzzleWithOneSolution() {
        Grid puzzle = NumberlinkGenerator.generateUnique(8, 8, 1234L, 20, 1_000_000L, 1_000L);
        List<EndpointPair> pairs = NumberlinkGenerator.extractEndpointPairs(puzzle);

        assertEquals(
            NumberlinkSolver.CountResult.ONE,
            NumberlinkSolver.countSolutions(8, 8, pairs, 2, 1_000_000L, 1_000L)
        );
    }
}
