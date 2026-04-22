package com.connectcolor.Model;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class NumberlinkGenerator {

    private NumberlinkGenerator() {}


    private static final int LOOP_TRIES = 1000;


    private static int id(int x, int y, int w) {
        return y * w + x;
    }

    /** True if there exist loop components not attached to endpoints (invalid). */
    public static boolean hasLoops(Grid tubeGrid, UnionFind uf) {
        int w = tubeGrid.w;
        int h = tubeGrid.h;

        Set<Integer> roots = new HashSet<>();
        int ends = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int root = uf.find(id(x, y, w));
                roots.add(root);

                if (tubeGrid.get(x, y) == 'x') {
                    ends++;
                }
            }
        }

        int groups = roots.size();
        return ends != 2 * groups;
    }

    /** True if there is a pair of endpoints adjacent to each other in the same component (invalid). */
    public static boolean hasPair(Grid tubeGrid, UnionFind uf) {
        int w = tubeGrid.w;
        int h = tubeGrid.h;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (tubeGrid.get(x, y) != 'x') continue;

                int r = uf.find(id(x, y, w));

                // right neighbor
                if (x + 1 < w && tubeGrid.get(x + 1, y) == 'x') {
                    if (uf.find(id(x + 1, y, w)) == r) return true;
                }
                // down neighbor
                if (y + 1 < h && tubeGrid.get(x, y + 1) == 'x') {
                    if (uf.find(id(x, y + 1, w)) == r) return true;
                }
            }
        }
        return false;
    }

    /**
     * True if any cell has 3+ orthogonal neighbors in the same component (invalid).
     * (This also catches squares/self-touching.)
     */
    public static boolean hasTripple(Grid tubeGrid, UnionFind uf) {
        int w = tubeGrid.w;
        int h = tubeGrid.h;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = uf.find(id(x, y, w));
                int nbs = 0;

                // 4-neighborhood
                if (x + 1 < w && uf.find(id(x + 1, y, w)) == r) nbs++;
                if (x - 1 >= 0 && uf.find(id(x - 1, y, w)) == r) nbs++;
                if (y + 1 < h && uf.find(id(x, y + 1, w)) == r) nbs++;
                if (y - 1 >= 0 && uf.find(id(x, y - 1, w)) == r) nbs++;

                if (nbs >= 3) return true;
            }
        }
        return false;
    }

    /** Public entry: generate one puzzle for w×h, with default min/max like the Python main(). */
    public static Grid generate(int w, int h, long seed) {
        if (w < 4 || h < 4) {
            throw new IllegalArgumentException("Width/height must be >= 4");
        }

        // Python: n = int((w*h)**.5)
        int n = (int) Math.sqrt(w * (double) h);
        int minNumbers = (n * 2) / 3;
        int maxNumbers = (n * 3) / 2;

        Mitm mitm = new Mitm(2, 1, seed);
        mitm.prepare(Math.min(20, Math.max(h, 6)));

        Random rng = new Random(seed ^ 0x9E3779B97F4A7C15L);
        return make(w, h, mitm, minNumbers, maxNumbers, rng);
    }

    /**
     * Port of Python make(w,h,mitm,min_numbers,max_numbers).
     * Returns the SHRUNK grid (size w×h) with endpoints/turn markers.
     */
    public static Grid make(int w, int h, Mitm mitm, int minNumbers, int maxNumbers, Random rng) {

        // Helper: decides if a big grid is acceptable and ready
        java.util.function.Predicate<Grid> testReady = (Grid big) -> {
            Grid small = big.shrink();
            TubeResult tr = small.makeTubes();
            Grid stg = tr.getTube();
            UnionFind uf = tr.getUF();

            int ends = 0;
            for (int y = 0; y < stg.h; y++) {
                for (int x = 0; x < stg.w; x++) {
                    if (stg.get(x, y) == 'x') ends++;
                }
            }
            int numbers = ends / 2;

            return (minNumbers <= numbers && numbers <= maxNumbers)
                    && !hasLoops(stg, uf)
                    && !hasPair(stg, uf)
                    && !hasTripple(stg, uf);
        };

        // Internally we work on a double-size grid (2w+1)×(2h+1)
        Grid grid = new Grid(2 * w + 1, 2 * h + 1);

        while (true) {
            grid.clear();

            // --- Left side path ---
            // Python: path = mitm.rand_path2(h, h, 0, -1)
            Path left = mitm.randPath2(h, h, 0, -1);

            if (!grid.testPath(left, 0, 0)) {
                continue;
            }
            grid.drawPath(left, 0, 0, 0, 1, false);

            // endpoints (Python: grid[0,0]='\\', grid[0,2*h]='/')
            grid.set(0, 0, '\\');
            grid.set(0, 2 * h, '/');

            // --- Right side path ---
            Path right = mitm.randPath2(h, h, 0, -1);

            if (!grid.testPath(right, 2 * w, 2 * h, 0, -1)) {
                continue;
            }
            grid.drawPath(right, 2 * w, 2 * h, 0, -1, false);

            // endpoints (Python: grid[2*w,0]='/', grid[2*w,2*h]='\\')
            grid.set(2 * w, 0, '/');
            grid.set(2 * w, 2 * h, '\\');

            // Maybe already good
            if (testReady.test(grid)) {
                return grid.shrink();
            }

            // Add loops in the middle
            TubeResult tr0 = grid.makeTubes();
            Grid tg = tr0.getTube();

            for (int tries = 0; tries < LOOP_TRIES; tries++) {
                int x = 2 * rng.nextInt(w);
                int y = 2 * rng.nextInt(h);

                // Only add loops where orientation is '-' or '|'
                char ori = tg.get(x, y);
                if (ori != '-' && ori != '|') continue;

                int clock = (ori == '-') ? 1 : -1;
                Path loop = mitm.randLoop(clock);

                if (grid.testPath(loop, x, y)) {
                    // clear inside, then draw loop, then recompute tubes
                    grid.clearPath(loop, x, y);
                    grid.drawPath(loop, x, y, 0, 1, true);

                    tr0 = grid.makeTubes();
                    tg = tr0.getTube();

                    // reject if too many pairs
                    Grid sg = grid.shrink();
                    TubeResult trSmall = sg.makeTubes();
                    Grid stg = trSmall.getTube();

                    int ends = 0;
                    for (int yy = 0; yy < stg.h; yy++) {
                        for (int xx = 0; xx < stg.w; xx++) {
                            if (stg.get(xx, yy) == 'x') ends++;
                        }
                    }
                    int numbers = ends / 2;
                    if (numbers > maxNumbers) {
                        break; // like Python debug('Exceeded maximum...')
                    }

                    if (testReady.test(grid)) {
                        return grid.shrink();
                    }
                }
            }
            // if we get here: failed to add loops enough; restart main while loop
        }
    }







}


