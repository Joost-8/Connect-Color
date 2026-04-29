package com.connectcolor.Model;

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

import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;

public class PuzzleCache {
    private static final int VERSION = 1;

    private final Path root;

    public PuzzleCache() {
        this(Paths.get(System.getProperty("user.home"), ".connect-color", "puzzles", "v1"));
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

        properties.setProperty("version", Integer.toString(VERSION));
        properties.setProperty("difficulty", difficulty.getKey());
        properties.setProperty("level", Integer.toString(level));
        properties.setProperty("rows", Integer.toString(settings.getRows()));
        properties.setProperty("cols", Integer.toString(settings.getCols()));
        properties.setProperty("pairs", Integer.toString(settings.getPairs()));
        properties.setProperty("cellSize", Integer.toString(settings.getCellSize()));
        properties.setProperty("seed", Long.toString(puzzle.getSeed()));
        properties.setProperty("endpoint.count", Integer.toString(endpointPairs.size()));

        for (int i = 0; i < endpointPairs.size(); i++) {
            EndpointPair pair = endpointPairs.get(i);
            properties.setProperty(
                "endpoint." + i,
                pair.getStartRow() + "," + pair.getStartCol() + ","
                + pair.getEndRow() + "," + pair.getEndCol()
            );
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

        if (version != VERSION
                || !difficulty.getKey().equals(difficultyKey)
                || storedLevel != level
                || rows != settings.getRows()
                || cols != settings.getCols()
                || pairs != settings.getPairs()
                || cellSize != settings.getCellSize()
                || seed != LevelSeed.forLevel(difficulty, level)
                || endpointCount != settings.getPairs()) {
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

        return Optional.of(new CachedPuzzle(difficulty, level, seed, settings, endpointPairs));
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
}
