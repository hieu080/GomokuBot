package com.hieupt.gomokubot.movegen;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class CandidateMoveGenerator {

    private static final int DEFAULT_RADIUS = 2;

    private final int radius;

    public CandidateMoveGenerator() {
        this(DEFAULT_RADIUS);
    }

    public CandidateMoveGenerator(int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException(
                    "Radius must be at least 1"
            );
        }

        this.radius = radius;
    }

    public List<Move> generate(Board board) {
        Objects.requireNonNull(board, "Board must not be null");

        List<Move> candidates = new ArrayList<>();

        if (board.isFull()) {
            return candidates;
        }

        if (board.isEmpty()) {
            candidates.add(getCenterMove(board));
            return candidates;
        }

        boolean[][] visited = new boolean[board.getHeight()][board.getWidth()];

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {

                if (!board.isEmpty(x, y)) {
                    addNearbyCandidates(board, x, y, visited, candidates);
                }
            }
        }

        sortByCentrality(board, candidates);

        return List.copyOf(candidates);
    }

    private void addNearbyCandidates(
            Board board,
            int stoneX,
            int stoneY,
            boolean[][] visited,
            List<Move> candidates
    ) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {

                int x = stoneX + dx;
                int y = stoneY + dy;

                if (!board.isInside(x, y)) {
                    continue;
                }

                if (visited[y][x] || !board.isEmpty(x, y)) {
                    continue;
                }

                visited[y][x] = true;
                candidates.add(new Move(x, y));
            }
        }
    }

    private Move getCenterMove(Board board) {
        int centerX = board.getWidth() / 2;
        int centerY = board.getHeight() / 2;

        return new Move(centerX, centerY);
    }

    private void sortByCentrality(
            Board board,
            List<Move> candidates
    ) {
        double centerX = (board.getWidth() - 1) / 2.0;
        double centerY = (board.getHeight() - 1) / 2.0;

        candidates.sort(
                Comparator.comparingDouble(move -> {
                    double dx = move.x() - centerX;
                    double dy = move.y() - centerY;

                    return dx * dx + dy * dy;
                })
        );
    }

    public int getRadius() {
        return radius;
    }
}