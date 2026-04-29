package com.connectcolor.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;

public final class CachedPuzzle {
    private final Difficulty difficulty;
    private final int level;
    private final long seed;
    private final GameSettings settings;
    private final List<EndpointPair> endpointPairs;

    public CachedPuzzle(Difficulty difficulty, int level, long seed, GameSettings settings, List<EndpointPair> endpointPairs) {
        this.difficulty = difficulty;
        this.level = level;
        this.seed = seed;
        this.settings = settings;
        this.endpointPairs = Collections.unmodifiableList(new ArrayList<>(endpointPairs));
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

    public Board createBoard() {
        return new Board(settings, endpointPairs);
    }
}
