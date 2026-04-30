package com.connectcolor.Model.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.connectcolor.Model.algorithm.EndpointPair;
import com.connectcolor.Model.game.Board;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;

public final class CachedPuzzle {
    private final Difficulty difficulty;
    private final int level;
    private final long seed;
    private final GameSettings settings;
    private final List<EndpointPair> endpointPairs;
    private final List<List<int[]>> solutionPaths;

    public CachedPuzzle(Difficulty difficulty, int level, long seed, GameSettings settings, List<EndpointPair> endpointPairs) {
        this(difficulty, level, seed, settings, endpointPairs, List.of());
    }

    public CachedPuzzle(
            Difficulty difficulty,
            int level,
            long seed,
            GameSettings settings,
            List<EndpointPair> endpointPairs,
            List<List<int[]>> solutionPaths) {
        this.difficulty = difficulty;
        this.level = level;
        this.seed = seed;
        this.settings = settings;
        this.endpointPairs = Collections.unmodifiableList(new ArrayList<>(endpointPairs));
        this.solutionPaths = copySolutionPaths(solutionPaths);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getLevel() {
        return level;
    }

    public long getSeed() {
        return seed;
    }

    public GameSettings getSettings() {
        return settings;
    }

    public List<EndpointPair> getEndpointPairs() {
        return endpointPairs;
    }

    public List<List<int[]>> getSolutionPaths() {
        return copySolutionPaths(solutionPaths);
    }

    public Board createBoard() {
        return new Board(settings, endpointPairs, solutionPaths);
    }

    private static List<List<int[]>> copySolutionPaths(List<List<int[]>> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }

        List<List<int[]>> copiedPaths = new ArrayList<>();
        for (List<int[]> path : paths) {
            List<int[]> copiedPath = new ArrayList<>();
            if (path != null) {
                for (int[] coordinate : path) {
                    if (coordinate != null && coordinate.length == 2) {
                        copiedPath.add(new int[] { coordinate[0], coordinate[1] });
                    }
                }
            }
            copiedPaths.add(Collections.unmodifiableList(copiedPath));
        }

        return Collections.unmodifiableList(copiedPaths);
    }
}
