package com.connectcolor.Model;

import java.util.ArrayDeque;
import java.util.List;

public final class NumberlinkSolver {
    private static final int[][] DIRS = {
        {-1, 0},
        {1, 0},
        {0, -1},
        {0, 1}
    };

    private NumberlinkSolver() {}

    public enum CountResult {
        ZERO,
        ONE,
        MULTIPLE,
        UNKNOWN_LIMIT
    }

    public static CountResult countSolutions(int rows, int cols, List<EndpointPair> pairs) {
        return countSolutions(rows, cols, pairs, 2, 1_000_000L, 1_000L);
    }

    public static CountResult countSolutions(
            int rows,
            int cols,
            List<EndpointPair> pairs,
            int maxSolutions,
            long maxNodes,
            long timeoutMillis) {

        Solver solver = new Solver(rows, cols, pairs, maxSolutions, maxNodes, timeoutMillis);
        return solver.count();
    }

    private static final class Solver {
        private final int rows;
        private final int cols;
        private final int cellCount;
        private final List<EndpointPair> pairs;
        private final int pairCount;
        private final int maxSolutions;
        private final long maxNodes;
        private final long deadlineNanos;

        private final int[] start;
        private final int[] end;
        private final int[] head;
        private final boolean[] finished;
        private final boolean[] occupied;
        private final boolean[] isEndpoint;

        private int occupiedCount;
        private int solutions;
        private long nodes;
        private boolean limited;

        Solver(int rows, int cols, List<EndpointPair> pairs, int maxSolutions, long maxNodes, long timeoutMillis) {
            this.rows = rows;
            this.cols = cols;
            this.cellCount = rows * cols;
            this.pairs = pairs;
            this.pairCount = pairs.size();
            this.maxSolutions = Math.max(1, maxSolutions);
            this.maxNodes = maxNodes <= 0 ? Long.MAX_VALUE : maxNodes;
            this.deadlineNanos = timeoutMillis <= 0
                    ? Long.MAX_VALUE
                    : System.nanoTime() + timeoutMillis * 1_000_000L;

            this.start = new int[pairCount];
            this.end = new int[pairCount];
            this.head = new int[pairCount];
            this.finished = new boolean[pairCount];
            this.occupied = new boolean[cellCount];
            this.isEndpoint = new boolean[cellCount];
        }

        CountResult count() {
            if (!initialize()) return CountResult.ZERO;

            search();

            if (solutions >= maxSolutions) return CountResult.MULTIPLE;
            if (limited) return CountResult.UNKNOWN_LIMIT;
            if (solutions == 1) return CountResult.ONE;
            return CountResult.ZERO;
        }

        private boolean initialize() {
            if (rows <= 0 || cols <= 0 || pairCount == 0) return false;

            for (int i = 0; i < pairCount; i++) {
                EndpointPair pair = pairs.get(i);
                if (!inBounds(pair.getStartRow(), pair.getStartCol())
                        || !inBounds(pair.getEndRow(), pair.getEndCol())) {
                    return false;
                }

                int startId = id(pair.getStartRow(), pair.getStartCol());
                int endId = id(pair.getEndRow(), pair.getEndCol());
                if (startId == endId || isEndpoint[startId] || isEndpoint[endId]) {
                    return false;
                }

                start[i] = startId;
                end[i] = endId;
                head[i] = startId;
                isEndpoint[startId] = true;
                isEndpoint[endId] = true;
                occupied[startId] = true;
                occupied[endId] = true;
                occupiedCount += 2;
            }

            return true;
        }

        private void search() {
            if (limited || solutions >= maxSolutions) return;

            nodes++;
            if (nodes > maxNodes || System.nanoTime() > deadlineNanos) {
                limited = true;
                return;
            }

            if (!emptyCellsAreStillUsable()) return;

            int pairIndex = selectPair();
            if (pairIndex == -2) return;

            if (pairIndex == -1) {
                if (occupiedCount == cellCount) {
                    solutions++;
                }
                return;
            }

            int[] moves = new int[4];
            int moveCount = collectMoves(pairIndex, moves);
            sortMoves(pairIndex, moves, moveCount);

            for (int i = 0; i < moveCount; i++) {
                int next = moves[i];
                int oldHead = head[pairIndex];
                boolean oldFinished = finished[pairIndex];
                boolean addedCell = !occupied[next];

                if (addedCell) {
                    occupied[next] = true;
                    occupiedCount++;
                }

                head[pairIndex] = next;
                finished[pairIndex] = next == end[pairIndex];

                search();

                head[pairIndex] = oldHead;
                finished[pairIndex] = oldFinished;

                if (addedCell) {
                    occupied[next] = false;
                    occupiedCount--;
                }

                if (limited || solutions >= maxSolutions) return;
            }
        }

        private int selectPair() {
            int bestPair = -1;
            int bestMoves = Integer.MAX_VALUE;
            int bestDistance = Integer.MAX_VALUE;

            for (int i = 0; i < pairCount; i++) {
                if (finished[i]) continue;
                if (!targetReachable(i)) return -2;

                int[] moves = new int[4];
                int moveCount = collectMoves(i, moves);
                if (moveCount == 0) return -2;

                int distance = manhattan(head[i], end[i]);
                if (moveCount < bestMoves || (moveCount == bestMoves && distance < bestDistance)) {
                    bestPair = i;
                    bestMoves = moveCount;
                    bestDistance = distance;
                }
            }

            return bestPair;
        }

        private int collectMoves(int pairIndex, int[] moves) {
            int count = 0;
            int current = head[pairIndex];
            int row = row(current);
            int col = col(current);

            for (int[] dir : DIRS) {
                int nextRow = row + dir[0];
                int nextCol = col + dir[1];
                if (!inBounds(nextRow, nextCol)) continue;

                int next = id(nextRow, nextCol);
                if (next == end[pairIndex] || !occupied[next]) {
                    moves[count++] = next;
                }
            }

            return count;
        }

        private void sortMoves(int pairIndex, int[] moves, int moveCount) {
            for (int i = 0; i < moveCount - 1; i++) {
                for (int j = i + 1; j < moveCount; j++) {
                    if (moveScore(pairIndex, moves[j]) < moveScore(pairIndex, moves[i])) {
                        int tmp = moves[i];
                        moves[i] = moves[j];
                        moves[j] = tmp;
                    }
                }
            }
        }

        private int moveScore(int pairIndex, int move) {
            if (move == end[pairIndex] && occupiedCount < cellCount) {
                return 10_000;
            }
            return manhattan(move, end[pairIndex]);
        }

        private boolean targetReachable(int pairIndex) {
            boolean[] seen = new boolean[cellCount];
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(head[pairIndex]);
            seen[head[pairIndex]] = true;

            while (!queue.isEmpty()) {
                int current = queue.removeFirst();
                if (current == end[pairIndex]) return true;

                int row = row(current);
                int col = col(current);
                for (int[] dir : DIRS) {
                    int nextRow = row + dir[0];
                    int nextCol = col + dir[1];
                    if (!inBounds(nextRow, nextCol)) continue;

                    int next = id(nextRow, nextCol);
                    if (seen[next]) continue;
                    if (next != end[pairIndex] && occupied[next]) continue;

                    seen[next] = true;
                    queue.addLast(next);
                }
            }

            return false;
        }

        private boolean emptyCellsAreStillUsable() {
            boolean[] seen = new boolean[cellCount];
            ArrayDeque<Integer> queue = new ArrayDeque<>();

            for (int cell = 0; cell < cellCount; cell++) {
                if (occupied[cell] || seen[cell]) continue;

                int componentSize = 0;
                boolean hasHeadPort = false;
                boolean hasTargetPort = false;

                seen[cell] = true;
                queue.add(cell);

                while (!queue.isEmpty()) {
                    int current = queue.removeFirst();
                    componentSize++;

                    int possibleNeighbors = 0;
                    int row = row(current);
                    int col = col(current);

                    for (int[] dir : DIRS) {
                        int nextRow = row + dir[0];
                        int nextCol = col + dir[1];
                        if (!inBounds(nextRow, nextCol)) continue;

                        int next = id(nextRow, nextCol);
                        if (!occupied[next]) {
                            possibleNeighbors++;
                            if (!seen[next]) {
                                seen[next] = true;
                                queue.addLast(next);
                            }
                            continue;
                        }

                        if (isUnfinishedHead(next)) {
                            hasHeadPort = true;
                            possibleNeighbors++;
                        }
                        if (isUnfinishedTarget(next)) {
                            hasTargetPort = true;
                            possibleNeighbors++;
                        }
                    }

                    if (possibleNeighbors < 2) return false;
                }

                if (componentSize > 0 && (!hasHeadPort || !hasTargetPort)) {
                    return false;
                }
            }

            return true;
        }

        private boolean isUnfinishedHead(int cell) {
            for (int i = 0; i < pairCount; i++) {
                if (!finished[i] && head[i] == cell) return true;
            }
            return false;
        }

        private boolean isUnfinishedTarget(int cell) {
            for (int i = 0; i < pairCount; i++) {
                if (!finished[i] && end[i] == cell) return true;
            }
            return false;
        }

        private int manhattan(int a, int b) {
            return Math.abs(row(a) - row(b)) + Math.abs(col(a) - col(b));
        }

        private boolean inBounds(int row, int col) {
            return row >= 0 && row < rows && col >= 0 && col < cols;
        }

        private int id(int row, int col) {
            return row * cols + col;
        }

        private int row(int id) {
            return id / cols;
        }

        private int col(int id) {
            return id % cols;
        }
    }
}
