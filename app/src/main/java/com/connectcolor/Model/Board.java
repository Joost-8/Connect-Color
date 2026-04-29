package com.connectcolor.Model;
import com.connectcolor.Util.CellState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.Map;
import com.connectcolor.Util.Dir;
import com.connectcolor.Util.GameSettings;



public class Board {
    
    private final Cell[][] grid;
    private final int rows;
    private final int cols;
    private ArrayList<BoardListener> listeners = new ArrayList<>();
    private int pairs;
    private ArrayList<Cell> fixedCells = new ArrayList<>();
    private Map<CellState, List<Cell>> solutionPaths = new HashMap<>();
    private Random rng = new Random();
    private final Map<CellState, ArrayList<Cell>> playerPaths = new HashMap<>();
    private final Set<CellState> finishedColors = new HashSet<>();

    public Board(GameSettings settings) {
        this(settings.getRows(), settings.getCols(), settings.getPairs());
    }

    public Board(int rows, int cols, int pairs) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Cell[rows][cols];
        this.pairs = pairs;
        //algorithm for board randomizer method
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Cell(i, j, false);
            }
        }

        setBoard();

    }

    public Cell getCell(int row, int col) {
        return grid[row][col];
    }

    public ArrayList<Cell> getFixedCells() {
        return this.fixedCells;
    }

    public void resetBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Cell cell = grid[i][j];
                cell.setPlayerState(CellState.Empty);
            }
        }
    }

    public void clearParents() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j].setParent(null);
            }
        }
    }

    public boolean isFull() {

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Cell cell = grid[i][j];
                if (cell.getPlayerState() == CellState.Empty) {
                    return false;
                }
            }
        }

        return true;

    } 

    public boolean isEmpty(int row, int col) {
        Cell cell = getCell(row, col);
        return cell.getPlayerState() == CellState.Empty;
    }

    public void addListener(BoardListener listener) {
        listeners.add(listener);
    }

    public Cell[][] copyGrid() {
        Cell[][] copy = new Cell[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Cell cell = grid[i][j];
                Cell cCell = new Cell(cell.getRow(), cell.getCol(), false);
                cCell.setPlayerState(cell.getPlayerState());
                cCell.setSolutionState(cell.getSolutionState());
                copy[i][j] = cCell;
            }
        }
  
        return copy;
    }

    public boolean placeColor(int row, int col, CellState color) {
        Cell cell = getCell(row, col);

        if(!isEmpty(row, col)) {
            return false;
        }

        cell.setPlayerState(color);

        notifyCellUpdate(row, col, color);


        return true;

    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void clearSolutions() {
        fixedCells.clear();
        solutionPaths.clear();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = getCell(r, c);
                cell.setFixed(false);
                cell.setSolutionState(CellState.Empty);
            }
        }
    }


    //add notifiers
    public void setBoard() {
        clearSolutions();
        resetBoard();
        clearParents();
        playerPaths.clear();
        finishedColors.clear();

        // 1. Generate puzzle grid
        long seed = rng.nextLong();
        Grid puzzle = NumberlinkGenerator.generate(cols, rows, seed);

        // 2. Build tubes + union-find
        TubeResult tr = puzzle.makeTubes();
        Grid tubeGrid = tr.getTube();
        UnionFind uf = tr.getUF();

        // 3. Group endpoint cells by component
        Map<Integer, List<Cell>> endpointsByRoot = new HashMap<>();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (tubeGrid.get(x, y) == 'x') {
                    int root = uf.find(y * cols + x);

                    endpointsByRoot
                        .computeIfAbsent(root, k -> new ArrayList<>())
                        .add(getCell(y, x)); // Board uses (row,col)
                }
            }
        }

        // 4. Assign colors + mark fixed endpoints
        int colorIdx = 0;
        fixedCells.clear();

        for (List<Cell> pair : endpointsByRoot.values()) {
            if (pair.size() != 2) {
                throw new IllegalStateException("Invalid endpoint count: " + pair.size());
            }

            CellState color = colorForIndex(colorIdx++);
            playerPaths.put(color, new ArrayList<>());
            finishedColors.remove(color);

            for (Cell cell : pair) {
                cell.setFixed(true);
                cell.setSolutionState(color);
                cell.forcePlayerState(color); 
                fixedCells.add(cell);

                notifyCellUpdate(cell.getRow(), cell.getCol(), color);
            }
        }

        // update pair count to match generated puzzle
        this.pairs = colorIdx;
    }

    private CellState colorForIndex(int idx) {
        List<CellState> colors = CellState.getColorStates();
        if (idx >= colors.size()) {
            throw new IllegalStateException("Not enough colors for pairs=" + idx);
        }
        return colors.get(idx);
    }

    

    private void placeFixedEndpoint(int row, int col, CellState color) {
        Cell cell = getCell(row, col);

        cell.setFixed(true);
        cell.setSolutionState(color);   // so your solver/validator can know it
        cell.setPlayerState(color);     // so it draws immediately

        fixedCells.add(cell);

        // notify UI to paint it
        notifyCellUpdate(row, col, color);
    }

    public void startPathFromFixed(int row, int col) {
        Cell start = getCell(row, col);

        if (!start.isFixed()) return;

        CellState color = start.getSolutionState();

        // Clear any previous player path for this color
        ArrayList<Cell> oldPath = playerPaths.get(color);
        if (oldPath != null) {
            ArrayList<Cell> fixedToRefresh = new ArrayList<>();
            for (Cell cell : oldPath) {
                if (!cell.isFixed()) {
                    cell.setPlayerState(CellState.Empty);
                    notifyCellUpdate(cell.getRow(), cell.getCol(), CellState.Empty);
                } else {
                    fixedToRefresh.add(cell);
                }
            }
            oldPath.clear();

            for (Cell cell : fixedToRefresh) {
                notifyCellUpdate(cell.getRow(), cell.getCol(), cell.getSolutionState());
            }
        }

        // Start new path from this fixed cell
        ArrayList<Cell> path = new ArrayList<>();
        path.add(start);
        playerPaths.put(color, path);

        finishedColors.remove(color); // path is now unfinished
    }





    public void notifyCellUpdate(int row, int col, CellState color) {
        for(BoardListener listener : listeners) {
            listener.onCellUpdate(row, col, color);
        }


    }

    public void notifyD() {
        for(BoardListener listener : listeners) {
            //write method
        }


    }

    public void notify(int row, int col) {
        for(BoardListener listener : listeners) {
            //write method
        }


    }

    public boolean isHeadCell(int row, int col, CellState color) {
        ArrayList<Cell> path = playerPaths.get(color);
        if (path == null || path.isEmpty()) return false;

        Cell head = path.get(path.size() - 1);
        return head.getRow() == row && head.getCol() == col;
    }

    public boolean isPathUnfinished(CellState color) {
        return !finishedColors.contains(color);
    }

    public boolean areOrthogonalNeighbors(int r1, int c1, int r2, int c2) {
    return (Math.abs(r1 - r2) + Math.abs(c1 - c2)) == 1;
    }

    private int indexOf(ArrayList<Cell> path, int r, int c) {
        for (int i = 0; i < path.size(); i++) {
            Cell cell = path.get(i);
            if (cell.getRow() == r && cell.getCol() == c) return i;
        }
        return -1;
    }

    public int[] getHead(CellState color) {
        ArrayList<Cell> path = playerPaths.get(color);
        if (path == null || path.isEmpty()) return null;
        Cell head = path.get(path.size() - 1);
        return new int[]{ head.getRow(), head.getCol() };
    }

    public void redrawPathTail(CellState color) {
        ArrayList<Cell> path = playerPaths.get(color);
        if (path == null || path.isEmpty()) return;

        notifyPathCell(path.get(path.size() - 1));
        if (path.size() >= 2) notifyPathCell(path.get(path.size() - 2));
        if (path.size() >= 3) notifyPathCell(path.get(path.size() - 3));
    }

    public boolean tryDragStep(CellState color, int headR, int headC, int targetR, int targetC) {
        if (color == null || color == CellState.Empty) return false;
        if (!isInBounds(targetR, targetC)) return false;

        // must be adjacent to head (controller should check too, but board enforces)
        if (!areOrthogonalNeighbors(headR, headC, targetR, targetC)) return false;

        ArrayList<Cell> path = playerPaths.get(color);
        if (path == null || path.isEmpty()) return false; // you must have started from fixed

        // safety: ensure the provided head matches the actual head
        Cell realHead = path.get(path.size() - 1);
        if (realHead.getRow() != headR || realHead.getCol() != headC) return false;

        Cell target = getCell(targetR, targetC);

        // --- A) UNDO one step: move back onto previous cell ---
        if (path.size() >= 2) {
            Cell prev = path.get(path.size() - 2);
            if (prev.getRow() == targetR && prev.getCol() == targetC) {
                Cell popped = path.remove(path.size() - 1);
                notifyPathCell(path.get(path.size() - 1)); // new head
                if (path.size() >= 2) notifyPathCell(path.get(path.size() - 2));

                // popped should never be fixed, but guard anyway
                if (!popped.isFixed()) {
                    popped.setPlayerState(CellState.Empty);
                    notifyCellUpdate(popped.getRow(), popped.getCol(), CellState.Empty);
                }

                finishedColors.remove(color);
                return true;
            }
        }

        // --- B) FINISH: step onto the other fixed endpoint of same color ---
        if (target.isFixed()) {
            // can only finish on matching color endpoint
            if (target.getSolutionState() != color) {
                redrawPathTail(color);
                return false;
            }
            if (indexOf(path, targetR, targetC) != -1) {
                redrawPathTail(color);
                return false;
            }

            // If you want to prevent "finish" without any path, enforce at least 2 cells:
            // if (path.size() < 2) return false;

            path.add(target);
            finishedColors.add(color);
            notifyPathCell(path.get(path.size() - 2));
            notifyPathCell(path.get(path.size() - 1));
            if (path.size() >= 3) notifyPathCell(path.get(path.size() - 3));
            return true;
        }

        // --- C) EXTEND: only into EMPTY cells ---
        if (target.getPlayerState() != CellState.Empty) {
            // If it's your own color, we do NOT allow "jump trimming" yet.
            // For now only allow undo by 1 (handled above).
            redrawPathTail(color);
            return false;
        }

        // prevent self-intersection into your own path
        if (indexOf(path, targetR, targetC) != -1) {
            redrawPathTail(color);
            return false;
        }

        // paint & add
        target.setPlayerState(color);
        notifyCellUpdate(targetR, targetC, color);

        path.add(target);
        notifyPathCell(path.get(path.size() - 2));
        notifyPathCell(path.get(path.size() - 1)); // target
        if (path.size() >= 3) notifyPathCell(path.get(path.size() - 3));
        finishedColors.remove(color);
        return true;
    }


    private Dir dirBetween(Cell a, Cell b) {
        int dr = b.getRow() - a.getRow();
        int dc = b.getCol() - a.getCol();
        if (dr == -1 && dc == 0) return Dir.UP;
        if (dr ==  1 && dc == 0) return Dir.DOWN;
        if (dr == 0 && dc == -1) return Dir.LEFT;
        if (dr == 0 && dc ==  1) return Dir.RIGHT;
        return null;
    }

    public Dir[] getPrevNextDirs(int row, int col) {
        Cell cell = getCell(row, col);
        CellState color = cell.isFixed() ? cell.getSolutionState() : cell.getPlayerState();
        if (color == CellState.Empty) return new Dir[] { null, null };

        ArrayList<Cell> path = playerPaths.get(color);
        if (path == null) return new Dir[] { null, null };

        int idx = indexOf(path, row, col);
        if (idx == -1) return new Dir[] { null, null };

        Dir prevDir = null;
        Dir nextDir = null;

        if (idx > 0) prevDir = dirBetween(path.get(idx), path.get(idx - 1));
        if (idx < path.size() - 1) nextDir = dirBetween(path.get(idx), path.get(idx + 1));

        return new Dir[] { prevDir, nextDir };
    }


    private void notifyPathCell(Cell cell) {
        CellState s = cell.getPlayerState();
        notifyCellUpdate(cell.getRow(), cell.getCol(), s);
    }




}
