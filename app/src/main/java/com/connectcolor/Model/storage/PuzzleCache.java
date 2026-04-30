package com.connectcolor.Model.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.connectcolor.Model.algorithm.EndpointPair;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;

public class PuzzleCache {
    private static final int VERSION = 2;

    private final Path root;

    public PuzzleCache() {
        this(Paths.get(System.getProperty("user.home"), ".connect-color", "puzzles", "v2"));
    }

    public PuzzleCache(Path root) {
        this.root = root;
    }

    public Optional<CachedPuzzle> load(Difficulty difficulty, int level) {
        Path file = fileFor(difficulty, level);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
            return parse(difficulty, level, properties);
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void save(Difficulty difficulty, int level, CachedPuzzle puzzle) {
        Properties properties = new Properties();
        GameSettings settings = puzzle.getSettings();
        List<EndpointPair> endpointPairs = puzzle.getEndpointPairs();
        List<List<int[]>> solutionPaths = puzzle.getSolutionPaths();

        properties.setProperty("version", Integer.toString(VERSION));
        properties.setProperty("difficulty", difficulty.getKey());
        properties.setProperty("level", Integer.toString(level));
        properties.setProperty("rows", Integer.toString(settings.getRows()));
        properties.setProperty("cols", Integer.toString(settings.getCols()));
        properties.setProperty("pairs", Integer.toString(settings.getPairs()));
        properties.setProperty("cellSize", Integer.toString(settings.getCellSize()));
        properties.setProperty("seed", Long.toString(puzzle.getSeed()));
        properties.setProperty("endpoint.count", Integer.toString(endpointPairs.size()));
        properties.setProperty("solution.count", Integer.toString(solutionPaths.size()));

        for (int i = 0; i < endpointPairs.size(); i++) {
            EndpointPair pair = endpointPairs.get(i);
            properties.setProperty(
                "endpoint." + i,
                pair.getStartRow() + "," + pair.getStartCol() + ","
                + pair.getEndRow() + "," + pair.getEndCol()
            );
        }

        for (int i = 0; i < solutionPaths.size(); i++) {
            properties.setProperty("solution." + i, encodePath(solutionPaths.get(i)));
        }

        try {
            Files.createDirectories(root);
            try (OutputStream out = Files.newOutputStream(fileFor(difficulty, level))) {
                properties.store(out, "Connect Color puzzle cache");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save puzzle cache", e);
        }
    }

    private Optional<CachedPuzzle> parse(Difficulty difficulty, int level, Properties properties) {
        GameSettings settings = difficulty.createSettings();
        int version = readInt(properties, "version");
        String difficultyKey = properties.getProperty("difficulty");
        int storedLevel = readInt(properties, "level");
        int rows = readInt(properties, "rows");
        int cols = readInt(properties, "cols");
        int pairs = readInt(properties, "pairs");
        int cellSize = readInt(properties, "cellSize");
        long seed = readLong(properties, "seed");
        int endpointCount = readInt(properties, "endpoint.count");
        int solutionCount = readInt(properties, "solution.count");

        if (version != VERSION
                || !difficulty.getKey().equals(difficultyKey)
                || storedLevel != level
                || rows != settings.getRows()
                || cols != settings.getCols()
                || pairs != settings.getPairs()
                || cellSize != settings.getCellSize()
                || seed != LevelSeed.forLevel(difficulty, level)
                || endpointCount != settings.getPairs()
                || solutionCount != endpointCount) {
            return Optional.empty();
        }

        List<EndpointPair> endpointPairs = new ArrayList<>();
        Set<Integer> usedEndpoints = new HashSet<>();

        for (int i = 0; i < endpointCount; i++) {
            String raw = properties.getProperty("endpoint." + i);
            if (raw == null) {
                return Optional.empty();
            }

            String[] parts = raw.split(",");
            if (parts.length != 4) {
                return Optional.empty();
            }

            int startRow = parseInt(parts[0]);
            int startCol = parseInt(parts[1]);
            int endRow = parseInt(parts[2]);
            int endCol = parseInt(parts[3]);

            if (!inBounds(startRow, startCol, rows, cols)
                    || !inBounds(endRow, endCol, rows, cols)
                    || (startRow == endRow && startCol == endCol)) {
                return Optional.empty();
            }

            int startId = startRow * cols + startCol;
            int endId = endRow * cols + endCol;
            if (!usedEndpoints.add(startId) || !usedEndpoints.add(endId)) {
                return Optional.empty();
            }

            endpointPairs.add(new EndpointPair(i, startRow, startCol, endRow, endCol));
        }

        List<List<int[]>> solutionPaths = new ArrayList<>();
        for (int i = 0; i < solutionCount; i++) {
            String raw = properties.getProperty("solution." + i);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }

            List<int[]> path = decodePath(raw);
            if (!isValidSolutionPath(path, endpointPairs.get(i), rows, cols)) {
                return Optional.empty();
            }
            solutionPaths.add(path);
        }

        return Optional.of(new CachedPuzzle(difficulty, level, seed, settings, endpointPairs, solutionPaths));
    }

    private Path fileFor(Difficulty difficulty, int level) {
        return root.resolve(difficulty.getKey() + "-" + Math.max(1, level) + ".properties");
    }

    private static boolean inBounds(int row, int col, int rows, int cols) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private static int readInt(Properties properties, String key) {
        return parseInt(properties.getProperty(key));
    }

    private static int parseInt(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing int value");
        }
        return Integer.parseInt(value.trim());
    }

    private static long readLong(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing long value");
        }
        return Long.parseLong(value.trim());
    }

    private static String encodePath(List<int[]> path) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                encoded.append(';');
            }
            int[] coordinate = path.get(i);
            encoded.append(coordinate[0]).append(',').append(coordinate[1]);
        }
        return encoded.toString();
    }

    private static List<int[]> decodePath(String rawPath) {
        List<int[]> path = new ArrayList<>();
        String[] coordinates = rawPath.split(";");
        for (String rawCoordinate : coordinates) {
            String[] parts = rawCoordinate.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid coordinate");
            }
            path.add(new int[] { parseInt(parts[0]), parseInt(parts[1]) });
        }
        return path;
    }

    private static boolean isValidSolutionPath(List<int[]> path, EndpointPair pair, int rows, int cols) {
        if (path.size() < 2) {
            return false;
        }

        int[] first = path.get(0);
        int[] last = path.get(path.size() - 1);
        if (first[0] != pair.getStartRow()
                || first[1] != pair.getStartCol()
                || last[0] != pair.getEndRow()
                || last[1] != pair.getEndCol()) {
            return false;
        }

        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < path.size(); i++) {
            int[] coordinate = path.get(i);
            if (!inBounds(coordinate[0], coordinate[1], rows, cols)) {
                return false;
            }

            int id = coordinate[0] * cols + coordinate[1];
            if (!seen.add(id)) {
                return false;
            }

            if (i > 0) {
                int[] previous = path.get(i - 1);
                int distance = Math.abs(coordinate[0] - previous[0]) + Math.abs(coordinate[1] - previous[1]);
                if (distance != 1) {
                    return false;
                }
            }
        }

        return true;
    }
}
