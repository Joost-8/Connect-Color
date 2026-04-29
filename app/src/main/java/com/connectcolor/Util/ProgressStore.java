package com.connectcolor.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ProgressStore {
    private static final String SELECTED_DIFFICULTY = "selectedDifficulty";
    private static final int DEFAULT_LEVEL = 1;

    private final Path file;
    private final Properties properties = new Properties();

    public ProgressStore() {
        this(Paths.get(System.getProperty("user.home"), ".connect-color", "progress.properties"));
    }

    public ProgressStore(Path file) {
        this.file = file;
        load();
    }

    public Difficulty getSelectedDifficulty() {
        return Difficulty.fromKey(properties.getProperty(SELECTED_DIFFICULTY));
    }

    public void setSelectedDifficulty(Difficulty difficulty) {
        properties.setProperty(SELECTED_DIFFICULTY, difficulty.getKey());
    }

    public int getLevel(Difficulty difficulty) {
        return readPositiveInt(levelKey(difficulty), DEFAULT_LEVEL);
    }

    public void setLevel(Difficulty difficulty, int level) {
        properties.setProperty(levelKey(difficulty), Integer.toString(Math.max(DEFAULT_LEVEL, level)));
    }

    public int incrementLevel(Difficulty difficulty) {
        int nextLevel = getLevel(difficulty) + 1;
        setLevel(difficulty, nextLevel);
        return nextLevel;
    }

    public void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Connect Color progress");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save progress to " + file, e);
        }
    }

    private void load() {
        properties.clear();
        if (!Files.exists(file)) {
            applyDefaults();
            return;
        }

        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            properties.clear();
        }

        applyDefaults();
    }

    private void applyDefaults() {
        properties.setProperty(SELECTED_DIFFICULTY, getSelectedDifficulty().getKey());
        for (Difficulty difficulty : Difficulty.values()) {
            setLevel(difficulty, getLevel(difficulty));
        }
    }

    private int readPositiveInt(String key, int fallback) {
        try {
            int value = Integer.parseInt(properties.getProperty(key));
            return value >= DEFAULT_LEVEL ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String levelKey(Difficulty difficulty) {
        return difficulty.getKey() + ".level";
    }
}
