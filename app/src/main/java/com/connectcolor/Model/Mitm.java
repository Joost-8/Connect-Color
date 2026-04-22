package com.connectcolor.Model;

import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Mitm {
    Random rng;
    int lrPrice, tPrice;
    Map<Key, List<List<Step>>> inv;
    List<PreparedEntry> list;
    
    public Mitm(int lrPrice, int tPrice) {
        this(lrPrice, tPrice, System.nanoTime());
    }

    public Mitm(int lrPrice, int tPrice, long seed) {
        this.lrPrice = lrPrice;
        this.tPrice = tPrice;
        this.rng = new Random(seed);
        this.inv = new HashMap<>();
        this.list = new ArrayList<>();
    }
    

    static final class Key {
        final int x, y, dx, dy;

        Key(int x, int y, int dx, int dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return x == k.x && y == k.y && dx == k.dx && dy == k.dy;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(x, y, dx, dy);
        }
    }


    static final class PreparedEntry {
        final List<Step> steps;   // the step sequence for this prefix
        final int x, y;           // end position
        final int dx, dy;         // end direction

        PreparedEntry(List<Step> steps, int x, int y, int dx, int dy) {
            this.steps = steps;
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static int[] unrotate(int x, int y, int dx, int dy) {
        // Rotate (x,y) and (dx,dy) until direction becomes (0,1)
        while (!(dx == 0 && dy == 1)) {
            int nx = -y;
            int ny = x;

            int ndx = -dy;
            int ndy = dx;

            x = nx;
            y = ny;
            dx = ndx;
            dy = ndy;
        }
        return new int[] { x, y, dx, dy };
    }

    public void prepare(int budget) {
        inv.clear();
        list.clear();

        // start state: (0,0) facing down (0,1)
        List<Step> prefix = new ArrayList<>();
        Set<Pt> seen = new HashSet<>();

        goodPaths(
            0, 0,          // x, y
            0, 1,          // dx, dy
            budget,
            seen,
            prefix
        );
    }

    private void addEntry(List<Step> prefix, int x, int y, int dx, int dy) {
        // snapshot the current prefix (because prefix is mutated during recursion)
        List<Step> snap = new ArrayList<>(prefix);

        // add to random prefix list
        list.add(new PreparedEntry(snap, x, y, dx, dy));

        // add to inverse lookup table
        Key k = new Key(x, y, dx, dy);
        inv.computeIfAbsent(k, kk -> new ArrayList<>()).add(snap);
    }

    private void goodPaths(int x, int y, int dx, int dy,
                       int budget,
                       Set<Pt> seen,
                       List<Step> prefix) {

        // Python: if budget >= 0 yield (), end-state
        if (budget >= 0) {
            addEntry(prefix, x, y, dx, dy);
        }

        // Python: if budget <= 0 return
        if (budget <= 0) {
            return;
        }

        Pt p0 = new Pt(x, y);
        seen.add(p0); // Remember cleaning up (A)

        int x1 = x + dx;
        int y1 = y + dy;
        Pt p1 = new Pt(x1, y1);

        if (!seen.contains(p1)) {
            // ---- L step (turn left after moving 1) ----
            prefix.add(Step.L);
            goodPaths(x1, y1, -dy, dx, budget - lrPrice, seen, prefix);
            prefix.remove(prefix.size() - 1);

            // ---- R step (turn right after moving 1) ----
            prefix.add(Step.R);
            goodPaths(x1, y1, dy, -dx, budget - lrPrice, seen, prefix);
            prefix.remove(prefix.size() - 1);

            // ---- T step (move 2 straight) ----
            // Python marks (x1,y1) as seen because T passes through it
            seen.add(p1); // Remember cleaning up (B)

            int x2 = x1 + dx;
            int y2 = y1 + dy;
            Pt p2 = new Pt(x2, y2);

            if (!seen.contains(p2)) {
                prefix.add(Step.T);
                goodPaths(x2, y2, dx, dy, budget - tPrice, seen, prefix);
                prefix.remove(prefix.size() - 1);
            }

            seen.remove(p1); // Clean up (B)
        }

        seen.remove(p0); // Clean up (A)
    }

    private List<List<Step>> lookup(int dx, int dy, int xn, int yn, int dxn, int dyn) {
        // Rotate target position and target direction into canonical frame (0,1)
        int[] a = unrotate(xn, yn, dx, dy);
        int xt = a[0];
        int yt = a[1];

        int[] b = unrotate(dxn, dyn, dx, dy);
        int dxt = b[0];
        int dyt = b[1];

        Key k = new Key(xt, yt, dxt, dyt);
        List<List<Step>> res = inv.get(k);
        return (res == null) ? List.of() : res;
    }

    public Path randLoop(int clock) {
        while (true) {
            // Pick a random prepared prefix
            PreparedEntry e = list.get(rng.nextInt(list.size()));

            // Look for paths that return to origin with direction (0,1)
            List<List<Step>> suffixes =
                lookup(e.dx, e.dy, -e.x, -e.y, 0, 1);

            if (suffixes.isEmpty()) continue;

            // Pick one suffix and join
            List<Step> suffix = suffixes.get(rng.nextInt(suffixes.size()));
            List<Step> joined = new ArrayList<>(e.steps);
            joined.addAll(suffix);

            Path p = new Path(joined);

            // Clockwise constraint:
            // winding() == 4 for clockwise, -4 for anti-clockwise
            if (clock != 0 && p.winding() != clock * 4) {
                continue;
            }

            // Loop must be non-overlapping except start/end
            if (p.testLoop()) {
                return p;
            }
        }
    }

    public Path randPath2(int xn, int yn, int dxn, int dyn) {
        // weights proportional to Python's:
        // [1/lrPrice, 1/lrPrice, 2/tPrice]
        // multiply by lrPrice*tPrice -> [tPrice, tPrice, 2*lrPrice]
        final int wL = tPrice;
        final int wR = tPrice;
        final int wT = 2 * lrPrice;
        final int total = wL + wR + wT;

        Set<Pt> seen = new HashSet<>();
        List<Step> prefix = new ArrayList<>();

        while (true) {
            seen.clear();
            prefix.clear();

            int x = 0, y = 0;
            int dx = 0, dy = 1; // canonical start direction
            seen.add(new Pt(x, y));

            int maxSteps = 2 * (Math.abs(xn) + Math.abs(yn));

            for (int i = 0; i < maxSteps; i++) {
                // weighted random choice among L, R, T
                int r = rng.nextInt(total);
                Step step;
                if (r < wL) step = Step.L;
                else if (r < wL + wR) step = Step.R;
                else step = Step.T;

                prefix.add(step);

                // move 1 step forward
                x += dx;
                y += dy;
                Pt p1 = new Pt(x, y);
                if (seen.contains(p1)) break;
                seen.add(p1);

                // apply turn/move logic
                if (step == Step.L) {
                    int ndx = -dy, ndy = dx;
                    dx = ndx; dy = ndy;
                } else if (step == Step.R) {
                    int ndx = dy, ndy = -dx;
                    dx = ndx; dy = ndy;
                } else { // Step.T : move a second step forward (straight)
                    x += dx;
                    y += dy;
                    Pt p2 = new Pt(x, y);
                    if (seen.contains(p2)) break;
                    seen.add(p2);
                }

                // reached target exactly
                if (x == xn && y == yn) {
                    return new Path(new ArrayList<>(prefix));
                }

                // try to finish using lookup table
                List<List<Step>> suffixes = lookup(dx, dy, xn - x, yn - y, dxn, dyn);
                if (!suffixes.isEmpty()) {
                    List<Step> suffix = suffixes.get(rng.nextInt(suffixes.size()));
                    List<Step> joined = new ArrayList<>(prefix);
                    joined.addAll(suffix);
                    return new Path(joined);
                }
            }
        // if we broke due to self-intersection or ran out of steps, restart
        }
    }

    public Path randPath(int xn, int yn, int dxn, int dyn) {
        while (true) {
            // Pick a random cached prefix
            PreparedEntry e = list.get(rng.nextInt(list.size()));

            // Try to complete it to the target
            List<List<Step>> suffixes =
                lookup(e.dx, e.dy, xn - e.x, yn - e.y, dxn, dyn);

            if (suffixes.isEmpty()) continue;

            // Join prefix + suffix
            List<Step> suffix = suffixes.get(rng.nextInt(suffixes.size()));
            List<Step> joined = new ArrayList<>(e.steps);
            joined.addAll(suffix);

            Path p = new Path(joined);

            // Must be non-overlapping
            if (p.test()) {
                return p;
            }
        }   
    }

}
