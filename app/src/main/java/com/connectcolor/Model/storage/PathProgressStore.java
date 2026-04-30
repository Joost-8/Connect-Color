package com.connectcolor.Model.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.connectcolor.Model.game.Board;
import com.connectcolor.Util.CellState;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;

public class PathProgressStore {
    private static final int VERSION = 1;

    private final Path root;

    public PathProgressStore() {
        this(Paths.get(System.getProperty("user.home"), ".connect-color", "paths", "v1"));
    }

    public PathProgressStore(Path root) {
        this.root = root;
    }

    public Optional<Map<CellState, List<int[]>>> load(Difficulty difficulty, int level) {
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

    public void save(Difficulty difficulty, int level, Board board) {
        Map<CellState, List<int[]>> paths = board.exportPlayerPaths();
        Path file = fileFor(difficulty, level);

        if (paths.isEmpty()) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new IllegalStateException("Could not delete path progress", e);
            }
            return;
        }

        GameSettings settings = difficulty.createSettings();
        Properties properties = new Properties();
        properties.setProperty("version", Integer.toString(VERSION));
        properties.setProperty("difficulty", difficulty.getKey());
        properties.setProperty("level", Integer.toString(Math.max(1, level)));
        properties.setProperty("rows", Integer.toString(settings.getRows()));
        properties.setProperty("cols", Integer.toString(settings.getCols()));
        properties.setProperty("pairs", Integer.toString(settings.getPairs()));
        properties.setProperty("seed", Long.toString(LevelSeed.forLevel(difficulty, level)));

        for (Map.Entry<CellState, List<int[]>> entry : paths.entrySet()) {
            properties.setProperty("path." + entry.getKey().name(), encodePath(entry.getValue()));
        }

        try {
            Files.createDirectories(root);
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Connect Color path progress");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save path progress", e);
        }
    }

    private Optional<Map<CellState, List<int[]>>> parse(Difficulty difficulty, int level, Properties properties) {
        GameSettings settings = difficulty.createSettings();
        if (readInt(properties, "version") != VERSION
                || !difficulty.getKey().equals(properties.getProperty("difficulty"))
                || readInt(properties, "level") != Math.max(1, level)
                || readInt(properties, "rows") != settings.getRows()
                || readInt(properties, "cols") != settings.getCols()
                || readInt(properties, "pairs") != settings.getPairs()
                || readLong(properties, "seed") != LevelSeed.forLevel(difficulty, level)) {
            return Optional.empty();
        }

        Map<CellState, List<int[]>> paths = new HashMap<>();
        for (CellState color : CellState.getColorStates()) {
            String rawPath = properties.getProperty("path." + color.name());
            if (rawPath == null || rawPath.isBlank()) {
                continue;
            }

            List<int[]> coordinates = decodePath(rawPath);
            if (coordinates.size() > 1) {
                paths.put(color, coordinates);
            }
        }

        return Optional.of(paths);
    }

    private Path fileFor(Difficulty difficulty, int level) {
        return root.resolve(difficulty.getKey() + "-" + Math.max(1, level) + ".properties");
    }

    private static String encodePath(List<int[]> path) {
        return path.stream()
            .map(coordinate -> coordinate[0] + "," + coordinate[1])
            .collect(Collectors.joining(";"));
    }

    private static List<int[]> decodePath(String rawPath) {
        return List.of(rawPath.split(";")).stream()
            .map(rawCoordinate -> {
                String[] parts = rawCoordinate.split(",");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid coordinate");
                }
                return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
            })
            .collect(Collectors.toList());
    }

    private static int readInt(Properties properties, String key) {
        String value = properties.getProperty(key);
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
