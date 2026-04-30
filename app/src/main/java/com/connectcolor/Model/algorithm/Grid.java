package com.connectcolor.Model.algorithm;
import java.util.List;
import java.util.ArrayList;

public class Grid {
    int w,h;
    char[][] g;

    public Grid(int w, int h) {
        this.w = w;
        this.h = h;
        g = new char[h][w];

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                g[r][c] = ' ';
            }
        }

    }



    public char get(int x, int y) {
        return g[y][x];
    }

    public void set(int x, int y, char c) {
        g[y][x] = c;
    }

    public boolean contains(int x, int y) {
        return get(x, y) != ' ';
    }

    public void clear() {
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                g[r][c] = ' ';
            }
        }
    }  

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < h; r++) {
           
            if (r != 0) {
                sb.append('\n');
            }
            for (int c = 0; c < w; c++) {
                sb.append(g[r][c]);
            }
        }

        return sb.toString();

    }


    public Grid shrink() {
        int newW = this.w / 2;

        int newH = this.h / 2;

        Grid small = new Grid(newW, newH);

        for (int r = 0; r < newH; r++) {
            for (int c = 0; c < newW; c++) {
                int srcX = 2*c + 1;
                int srcY = 2*r + 1;
                char val = get(srcX, srcY);
                small.set(c, r, val);

            }
        }

        return small;

    }

    public boolean testPath(Path path, int x0, int y0) {
        List<Pt> ps = path.xys(0, 1);
        for (Pt p : ps) {
            int x = p.x;
            int y =p.y;

            int gx = x0 - x + y;
            int gy = y0 + x + y;

            if (0 > gx || gx >= w || 0 > gy || gy >= h) {return false;}

            if (contains(gx, gy)) {return false;}


        }

        return true;

    }

    public boolean testPath(Path path, int x0, int y0, int dx0, int dy0) {
        List<Pt> ps = path.xys(dx0, dy0);

        for (Pt p : ps) {
            int x = p.x;
            int y = p.y;

            int gx = x0 - x + y;
            int gy = y0 + x + y;

            if (gx < 0 || gx >= w || gy < 0 || gy >= h) return false;
            if (contains(gx, gy)) return false;
        }

        return true;
    }

    public void drawPath(Path path, int x0, int y0) {
        drawPath(path, x0, y0, 0, 1, false);
    } 

    public void drawPath(Path path, int x0, int y0, int dx0, int dy0, boolean loop) {
        List<Pt> ps = new ArrayList<>(path.xys(dx0, dy0));

        // For loops: ensure we have triples for every corner by appending ps[1]
        if (loop) {
            if (!ps.get(0).equals(ps.get(ps.size() - 1))) {
                throw new IllegalArgumentException("Loop path must end where it started.");
            }
            if (ps.size() < 2) {
                throw new IllegalArgumentException("Loop path too short.");
            }
            ps.add(ps.get(1));
        }

        // Draw internal points only (skip endpoints unless loop logic extends triples)
        for (int i = 1; i < ps.size() - 1; i++) {
            Pt prev = ps.get(i - 1);
            Pt cur  = ps.get(i);
            Pt next = ps.get(i + 1);

            int dxChord = next.x - prev.x;
            int dyChord = next.y - prev.y;

            int cross = (cur.x - prev.x) * (next.y - cur.y)
                  - (next.x - cur.x) * (cur.y - prev.y);

            int turn = sign(cross);

            char ch = pickChar(dxChord, dyChord, turn);

            int gx = x0 - cur.x + cur.y;
            int gy = y0 + cur.x + cur.y;

            set(gx, gy, ch);
        }
    }   

    private static int sign(int v) {
        if (v == 0) return 0;
            return v < 0 ? -1 : 1;
    }

    private static char pickChar(int dxChord, int dyChord, int turn) {
        // Direct port of Python mapping table in draw_path
        if (dxChord == 1 && dyChord == 1 && turn == 1) return '<';
        if (dxChord == -1 && dyChord == -1 && turn == -1) return '<';

        if (dxChord == 1 && dyChord == 1 && turn == -1) return '>';
        if (dxChord == -1 && dyChord == -1 && turn == 1) return '>';

        if (dxChord == -1 && dyChord == 1 && turn == 1) return 'v';
        if (dxChord == 1 && dyChord == -1 && turn == -1) return 'v';

        if (dxChord == -1 && dyChord == 1 && turn == -1) return '^';
        if (dxChord == 1 && dyChord == -1 && turn == 1) return '^';

        if (dxChord == 0 && dyChord == 2 && turn == 0) return '\\';
        if (dxChord == 0 && dyChord == -2 && turn == 0) return '\\';

        if (dxChord == 2 && dyChord == 0 && turn == 0) return '/';
        if (dxChord == -2 && dyChord == 0 && turn == 0) return '/';

        throw new IllegalStateException(
        "Unexpected triple key: (" + dxChord + "," + dyChord + "," + turn + ")"
        );
    }
    private int id(int x, int y) { return y * w + x; }

    public TubeResult makeTubes() {
        UnionFind uf = new UnionFind(w * h);
        Grid tubeGrid = new Grid(w, h); 

        for (int x = 0; x < w; x++) {
            char d = '-';
            for (int y = 0; y < h; y++) {
                char cell = get(x, y);
                String key = "" + cell + d;

                // --- Union rules (down/right) ---
                switch (key) {
                    case "/-":  // down
                    case "v|":  // down
                    case "v-":  // down
                    case " |":  // down
                    if (y + 1 < h) uf.union(id(x, y), id(x, y + 1));
                        break;

                    case "/|":  // right
                    case ">|":  // right
                    case ">-":  // right
                    case " -":  // right
                    if (x + 1 < w) uf.union(id(x, y), id(x + 1, y));
                        break;

                    case "\\-": // right + down
                    if (x + 1 < w) uf.union(id(x, y), id(x + 1, y));
                    if (y + 1 < h) uf.union(id(x, y), id(x, y + 1));
                        break;

                default:
                    // no unions
                    break;
                }

            // --- Output tube character ---
                char out;
                switch (key) {
                    case "/-":  out = '┐'; break;
                    case "\\-": out = '┌'; break;
                    case "/|":  out = '└'; break;
                    case "\\|": out = '┘'; break;
                    case " -":  out = '-'; break;
                    case " |":  out = '|'; break;
                    default:    out = 'x'; break; // endpoints/other markers
                }
                tubeGrid.set(x, y, out);

                // --- Toggle direction state on these chars only ---
                if (cell == '\\' || cell == '/' || cell == 'v' || cell == '^') {
                    d = (d == '-') ? '|' : '-';
                }
            }
        }
        return new TubeResult(tubeGrid, uf);
    }

    public void clearPath(Path loopPath, int x0, int y0) {
        // Draw the loop onto a temporary grid
        Grid pathGrid = new Grid(w, h);
        pathGrid.drawPath(loopPath, x0, y0, 0, 1, true);

        // Convert that temporary loop-grid into tubes
        TubeResult tr = pathGrid.makeTubes();
        Grid tg = tr.getTube();

        // Clear everything "inside" the loop from this grid
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (tg.get(x, y) == '|') {
                    set(x, y, ' ');
                }
            }
        }
    }



}