package com.connectcolor.Model.game;
import com.connectcolor.Model.algorithm.EndpointPair;
import com.connectcolor.Model.algorithm.Grid;
import com.connectcolor.Model.algorithm.NumberlinkGenerator;
import com.connectcolor.Model.listeners.BoardListener;
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
    private Long puzzleSeed;
    private final Map<CellState, ArrayList<Cell>> playerPaths = new HashMap<>();
    private final Set<CellState> finishedColors = new HashSet<>();

    public Board(GameSettings settings) {
        this(settings.getRows(), settings.getCols(), settings.getPairs());
    }

    public Board(GameSettings settings, long seed) {
        this(settings.getRows(), settings.getCols(), settings.getPairs(), seed);
    }

    public Board(GameSettings settings, List<EndpointPair> endpointPairs) {
        this(settings.getRows(), settings.getCols(), settings.getPairs(), null, endpointPairs);
    }

    public Board(int rows, int cols, int pairs) {
        this(rows, cols, pairs, null, null);
    }

    public Board(int rows, int cols, int pairs, Long seed) {
        this(rows, cols, pairs, seed, null);
    }

    private Board(int rows, int cols, int pairs, Long seed, List<EndpointPair> endpointPairs) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Cell[rows][cols];
        this.pairs = pairs;
        this.puzzleSeed = seed;
        //algorithm for board randomizer method
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Cell(i, j, false);
            }
        }

        if (endpointPairs == null) {
            setBoard();
        } else {
            setBoard(endpointPairs);
        }

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

    public boolean isSolved() {
        if (!isFull()) return false;

        for (CellState color : playerPaths.keySet()) {
            if (!finishedColors.contains(color)) {
                return false;
            }
        }

        return true;
    }

    public Map<CellState, List<int[]>> exportPlayerPaths() {
        Map<CellState, List<int[]>> exported = new HashMap<>();

        for (Map.Entry<CellState, ArrayList<Cell>> entry : playerPaths.entrySet()) {
            ArrayList<Cell> path = entry.getValue();
            if (path == null || path.size() <= 1) {
                continue;
            }

            List<int[]> coordinates = new ArrayList<>();
            for (Cell cell : path) {
                coordinates.add(new int[] { cell.getRow(), cell.getCol() });
            }
            exported.put(entry.getKey(), coordinates);
        }

        return exported;
    }

    public boolean restorePlayerPaths(Map<CellState, List<int[]>> savedPaths) {
        clearPlayerProgress();

        if (savedPaths == null || savedPaths.isEmpty()) {
            return true;
        }

        Set<Cell> occupiedPathCells = new HashSet<>();
        Map<CellState, ArrayList<Cell>> restoredPaths = new HashMap<>();
        Set<CellState> restoredFinishedColors = new HashSet<>();

        for (Map.Entry<CellState, List<int[]>> entry : savedPaths.entrySet()) {
            CellState color = entry.getKey();
            List<int[]> coordinates = entry.getValue();
            if (color == null || !color.isColor() || coordinates == null || coordinates.size() <= 1) {
                continue;
            }

            ArrayList<Cell> path = restorePath(color, coordinates, occupiedPathCells);
            if (path == null) {
                clearPlayerProgress();
                return false;
            }

            restoredPaths.put(color, path);
            Cell tail = path.get(path.size() - 1);
            if (tail.isFixed() && tail.getSolutionState() == color) {
                restoredFinishedColors.add(color);
            }
        }

        for (Map.Entry<CellState, ArrayList<Cell>> entry : restoredPaths.entrySet()) {
            playerPaths.put(entry.getKey(), entry.getValue());
        }
        finishedColors.addAll(restoredFinishedColors);
        return true;
    }

    public boolean isEmpty(int row, int col) {
        Cell cell = getCell(row, col);
        return cell.getPlayerState() == CellState.Empty;
    }

    public void addListener(BoardListener listener) {
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
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
        prepareForNewBoard();

        // 1. Generate puzzle grid
        long seed = puzzleSeed != null ? puzzleSeed : rng.nextLong();
        Grid puzzle = NumberlinkGenerator.generateUnique(cols, rows, seed, pairs);
        List<EndpointPair> endpointPairs = NumberlinkGenerator.extractEndpointPairs(puzzle);
        placeEndpointPairs(endpointPairs);
    }

    private void clearPlayerProgress() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = getCell(r, c);
                if (!cell.isFixed()) {
                    cell.setPlayerState(CellState.Empty);
                }
            }
        }

        for (CellState color : new ArrayList<>(playerPaths.keySet())) {
            playerPaths.put(color, new ArrayList<>());
        }
        finishedColors.clear();
    }

    private ArrayList<Cell> restorePath(CellState color, List<int[]> coordinates, Set<Cell> occupiedPathCells) {
        ArrayList<Cell> path = new ArrayList<>();

        for (int i = 0; i < coordinates.size(); i++) {
            int[] coordinate = coordinates.get(i);
            if (coordinate == null || coordinate.length != 2) {
                return null;
            }

            int row = coordinate[0];
            int col = coordinate[1];
            if (!isInBounds(row, col)) {
                return null;
            }

            Cell cell = getCell(row, col);
            if (i == 0) {
                if (!cell.isFixed() || cell.getSolutionState() != color) {
                    return null;
                }
            } else {
                Cell previous = path.get(path.size() - 1);
                if (!areOrthogonalNeighbors(previous.getRow(), previous.getCol(), row, col)) {
                    return null;
                }

                if (cell.isFixed()) {
                    if (cell.getSolutionState() != color || i != coordinates.size() - 1) {
                        return null;
                    }
                } else if (!occupiedPathCells.add(cell)) {
                    return null;
                }
            }

            if (path.contains(cell)) {
                return null;
            }

            path.add(cell);
        }

        for (Cell cell : path) {
            if (!cell.isFixed()) {
                cell.setPlayerState(color);
            }
        }

        return path;
    }

    public void setBoard(List<EndpointPair> endpointPairs) {
        prepareForNewBoard();
        placeEndpointPairs(endpointPairs);
    }

    private void prepareForNewBoard() {
        clearSolutions();
        resetBoard();
        clearParents();
        playerPaths.clear();
        finishedColors.clear();

        int availableColors = CellState.getColorStates().size();
        if (pairs > availableColors) {
            throw new IllegalStateException(
                "Not enough colors for requested pairs=" + pairs + ", available=" + availableColors
            );
        }
    }

    private void placeEndpointPairs(List<EndpointPair> endpointPairs) {
        int availableColors = CellState.getColorStates().size();
        if (endpointPairs.size() > availableColors) {
            throw new IllegalStateException(
                "Not enough colors for generated pairs=" + endpointPairs.size()
                + ", available=" + availableColors
            );
        }
        if (endpointPairs.size() != pairs) {
            throw new IllegalStateException(
                "Generated puzzle has pairs=" + endpointPairs.size()
                + ", expected=" + pairs
            );
        }

        int colorIdx = 0;
        fixedCells.clear();

        for (EndpointPair pair : endpointPairs) {
            CellState color = colorForIndex(colorIdx++);
            playerPaths.put(color, new ArrayList<>());
            finishedColors.remove(color);

            Cell first = getCell(pair.getStartRow(), pair.getStartCol());
            Cell second = getCell(pair.getEndRow(), pair.getEndCol());

            for (Cell cell : List.of(first, second)) {
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
        markPathUnfinished(color);

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

    public boolean resumePathFromCell(int row, int col, CellState color) {
        if (color == null || color == CellState.Empty) return false;

        ArrayList<Cell> path = playerPaths.get(color);
        if (path == null || path.isEmpty()) return false;

        int idx = indexOf(path, row, col);
        if (idx <= 0) return false;

        markPathUnfinished(color);

        while (path.size() > idx + 1) {
            Cell removed = path.remove(path.size() - 1);
            if (removed.isFixed()) {
                notifyCellUpdate(removed.getRow(), removed.getCol(), removed.getSolutionState());
            } else {
                removed.setPlayerState(CellState.Empty);
                notifyCellUpdate(removed.getRow(), removed.getCol(), CellState.Empty);
            }
        }

        redrawPathTail(color);
        return true;
    }

    public boolean isPathUnfinished(CellState color) {
        return !finishedColors.contains(color);
    }

    public boolean hasDrawnPath(CellState color) {
        ArrayList<Cell> path = playerPaths.get(color);
        return path != null && path.size() > 1;
    }

    public boolean isFinishedPathCell(int row, int col) {
        return getFinishedPathColor(row, col) != CellState.Empty;
    }

    public CellState getFinishedPathColor(int row, int col) {
        for (CellState color : finishedColors) {
            ArrayList<Cell> path = playerPaths.get(color);
            if (path != null && indexOf(path, row, col) != -1) {
                return color;
            }
        }

        return CellState.Empty;
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
                markPathUnfinished(color);
                Cell popped = path.remove(path.size() - 1);
                notifyPathCell(path.get(path.size() - 1)); // new head
                if (path.size() >= 2) notifyPathCell(path.get(path.size() - 2));

                // popped should never be fixed, but guard anyway
                if (!popped.isFixed()) {
                    popped.setPlayerState(CellState.Empty);
                    notifyCellUpdate(popped.getRow(), popped.getCol(), CellState.Empty);
                }

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
            notifyWholePath(color);
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
        markPathUnfinished(color);
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
        CellState s = cell.isFixed() ? cell.getSolutionState() : cell.getPlayerState();
        notifyCellUpdate(cell.getRow(), cell.getCol(), s);
    }

    private void notifyWholePath(CellState color) {
        ArrayList<Cell> path = playerPaths.get(color);
        if (path == null) return;

        for (Cell cell : path) {
            notifyPathCell(cell);
        }
    }

    private void markPathUnfinished(CellState color) {
        if (finishedColors.remove(color)) {
            notifyWholePath(color);
        }
    }




}
