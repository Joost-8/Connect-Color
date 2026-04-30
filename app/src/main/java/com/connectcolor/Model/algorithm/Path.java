package com.connectcolor.Model.algorithm;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Path {
    private final List<Step> steps;

    public Path(List<Step> steps) {
        // store a copy so callers can't mutate your internal list
        this.steps = List.copyOf(steps);
    }

    public List<Step> steps() {
        return steps;
    }

    /**
     * Returns the sequence of local coordinates on this path,
     * starting at (0,0) facing direction (dx,dy).
     *
     * This is a direct port of Python Path.xys(dx,dy).
     */
    public List<Pt> xys(int dx, int dy) {
        int x = 0, y = 0;
        ArrayList<Pt> pts = new ArrayList<>();
        pts.add(new Pt(x, y));

        for (Step s : steps) {
            // move forward once
            x += dx;
            y += dy;
            pts.add(new Pt(x, y));

            if (s == Step.L) {
                // rotate left: (dx,dy)=(-dy,dx)
                int ndx = -dy;
                int ndy = dx;
                dx = ndx;
                dy = ndy;
            } else if (s == Step.R) {
                // rotate right: (dx,dy)=(dy,-dx)
                int ndx = dy;
                int ndy = -dx;
                dx = ndx;
                dy = ndy;
            } else if (s == Step.T) {
                // move forward one more (straight twice)
                x += dx;
                y += dy;
                pts.add(new Pt(x, y));
            }
        }
        return pts;
    }

    /** True iff no coordinate appears twice (in local coordinates). */
    public boolean test() {
        List<Pt> ps = xys(0, 1);
        Set<Pt> seen = new HashSet<>(ps);
        return seen.size() == ps.size();
    }

    /**
     * True iff path is non-overlapping, OR it overlaps only by returning
     * to the start at the very end (a simple loop).
     */
    public boolean testLoop() {
        List<Pt> ps = xys(0, 1);
        Set<Pt> seen = new HashSet<>(ps);

        if (seen.size() == ps.size()) return true;
        if (seen.size() == ps.size() - 1) {
            return ps.get(0).equals(ps.get(ps.size() - 1));
        }
        return false;
    }

    /** Winding = (#R) - (#L). */
    public int winding() {
        int w = 0;
        for (Step s : steps) {
            if (s == Step.R) w++;
            else if (s == Step.L) w--;
        }
        return w;
    }

    @Override
    public String toString() {
        // matches python-ish: T->'2', R->'R', L->'L'
        StringBuilder sb = new StringBuilder();
        for (Step s : steps) {
            sb.append(s == Step.T ? '2' : (s == Step.L ? 'L' : 'R'));
        }
        return sb.toString();
    }

}

