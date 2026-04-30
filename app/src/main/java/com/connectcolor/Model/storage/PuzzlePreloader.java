package com.connectcolor.Model.storage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.connectcolor.Model.algorithm.EndpointPair;
import com.connectcolor.Model.algorithm.Grid;
import com.connectcolor.Model.algorithm.NumberlinkGenerator;
import com.connectcolor.Model.game.Board;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;

public class PuzzlePreloader {
    public interface PuzzleGenerator {
        CachedPuzzle generate(Difficulty difficulty, int level);
    }

    private static final int DEFAULT_THREADS = 2;

    private final PuzzleCache cache;
    private final PuzzleGenerator generator;
    private final ExecutorService executor;
    private final ConcurrentHashMap<String, CompletableFuture<CachedPuzzle>> inFlight = new ConcurrentHashMap<>();

    public PuzzlePreloader() {
        this(new PuzzleCache());
    }

    public PuzzlePreloader(PuzzleCache cache) {
        this(cache, PuzzlePreloader::generatePuzzle, DEFAULT_THREADS);
    }

    PuzzlePreloader(PuzzleCache cache, PuzzleGenerator generator, int threads) {
        this.cache = cache;
        this.generator = generator;
        this.executor = Executors.newFixedThreadPool(Math.max(1, threads), new PreloadThreadFactory());
    }

    public void preloadAround(Difficulty difficulty, int startLevel, int count) {
        int safeStart = Math.max(1, startLevel);
        for (int offset = 0; offset < count; offset++) {
            loadPuzzle(difficulty, safeStart + offset);
        }
    }

    public CompletableFuture<Board> loadBoard(Difficulty difficulty, int level) {
        return loadPuzzle(difficulty, level).thenApply(CachedPuzzle::createBoard);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private CompletableFuture<CachedPuzzle> loadPuzzle(Difficulty difficulty, int level) {
        int safeLevel = Math.max(1, level);
        String key = key(difficulty, safeLevel);
        CompletableFuture<CachedPuzzle> future = inFlight.computeIfAbsent(
            key,
            ignored -> CompletableFuture.supplyAsync(() -> loadOrGenerate(difficulty, safeLevel), executor)
        );

        future.whenComplete((puzzle, error) -> {
            if (error != null) {
                inFlight.remove(key, future);
            }
        });

        return future;
    }

    private CachedPuzzle loadOrGenerate(Difficulty difficulty, int level) {
        Optional<CachedPuzzle> cached = cache.load(difficulty, level);
        if (cached.isPresent()) {
            return cached.get();
        }

        CachedPuzzle generated = generator.generate(difficulty, level);
        cache.save(difficulty, level, generated);
        return generated;
    }

    private static CachedPuzzle generatePuzzle(Difficulty difficulty, int level) {
        GameSettings settings = difficulty.createSettings();
        long seed = LevelSeed.forLevel(difficulty, level);
        Grid puzzle = NumberlinkGenerator.generateUnique(
            settings.getCols(),
            settings.getRows(),
            seed,
            settings.getPairs()
        );
        List<EndpointPair> endpointPairs = NumberlinkGenerator.extractEndpointPairs(puzzle);
        return new CachedPuzzle(difficulty, level, seed, settings, endpointPairs);
    }

    private static String key(Difficulty difficulty, int level) {
        return difficulty.getKey() + ":" + level;
    }

    private static final class PreloadThreadFactory implements ThreadFactory {
        private int nextId = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "connect-color-preloader-" + nextId++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
