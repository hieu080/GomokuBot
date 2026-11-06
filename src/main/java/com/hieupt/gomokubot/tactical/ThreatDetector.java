package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;

import java.util.*;

public class ThreatDetector {

    private static final int[][] DIRECTIONS = {
            {1, 0},   // Horizontal
            {0, 1},   // Vertical
            {1, 1},   // Diagonal \
            {1, -1}   // Diagonal /
    };

    private final WinChecker winChecker;
    private final CandidateMoveGenerator moveGenerator;

    public ThreatDetector() {
        this(new WinChecker(5), new CandidateMoveGenerator(2));
    }

    public ThreatDetector(WinChecker winChecker) {
        this(winChecker, new CandidateMoveGenerator(2));
    }

    public ThreatDetector(
            WinChecker winChecker,
            CandidateMoveGenerator moveGenerator
    ) {
        this.winChecker = Objects.requireNonNull(winChecker, "WinChecker must not be null");
        this.moveGenerator = Objects.requireNonNull(moveGenerator, "CandidateMoveGenerator must not be null");
    }

    public WinChecker getWinChecker() {
        return winChecker;
    }

    public CandidateMoveGenerator getMoveGenerator() {
        return moveGenerator;
    }

    public List<Move> findWinningMoves(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(player);

        List<Move> candidates = moveGenerator.generate(board);
        List<Move> winningMoves = new ArrayList<>();

        for (Move move : candidates) {
            if (!board.isEmpty(move.x(), move.y())) {
                continue;
            }

            board.placeStone(move.x(), move.y(), player);
            try {
                if (winChecker.isWinningMove(board, move)) {
                    winningMoves.add(move);
                }
            } finally {
                board.removeStone(move.x(), move.y());
            }
        }

        return winningMoves;
    }

    public List<Threat> findFourThreats(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(player);

        List<Move> candidates = moveGenerator.generate(board);
        List<Threat> openFours = new ArrayList<>();
        List<Threat> closedFours = new ArrayList<>();

        for (Move move : candidates) {
            if (!board.isEmpty(move.x(), move.y())) {
                continue;
            }

            board.placeStone(move.x(), move.y(), player);
            try {
                // If the move itself is a winning move, it is a FIVE, not a Four threat
                if (winChecker.isWinningMove(board, move)) {
                    continue;
                }

                List<Move> defenseMoves = findDefenseSquaresForMove(board, move, player);
                if (defenseMoves.size() >= 2) {
                    openFours.add(new Threat(ThreatType.OPEN_FOUR, move, defenseMoves));
                } else if (defenseMoves.size() == 1) {
                    closedFours.add(new Threat(ThreatType.FOUR, move, defenseMoves));
                }
            } finally {
                board.removeStone(move.x(), move.y());
            }
        }

        List<Threat> result = new ArrayList<>(openFours.size() + closedFours.size());
        result.addAll(openFours);
        result.addAll(closedFours);
        return result;
    }

    public List<Threat> findThreeThreats(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(player);

        List<Move> candidates = moveGenerator.generate(board);
        List<Threat> threeThreats = new ArrayList<>();

        for (Move move : candidates) {
            if (!board.isEmpty(move.x(), move.y())) {
                continue;
            }

            board.placeStone(move.x(), move.y(), player);
            try {
                if (winChecker.isWinningMove(board, move)) {
                    continue;
                }
                List<Move> immediateDefense = findDefenseSquaresForMove(board, move, player);
                if (!immediateDefense.isEmpty()) {
                    continue;
                }

                List<Threat> foursInNewPos = findFourThreats(board, player);
                List<Move> openFourMoves = new ArrayList<>();
                for (Threat fourThreat : foursInNewPos) {
                    if (fourThreat.isOpenFour()) {
                        openFourMoves.add(fourThreat.attackMove());
                    }
                }

                if (!openFourMoves.isEmpty()) {
                    threeThreats.add(new Threat(ThreatType.OPEN_THREE, move, openFourMoves));
                }
            } finally {
                board.removeStone(move.x(), move.y());
            }
        }

        return threeThreats;
    }

    public List<Fork> findForks(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(player);

        List<Move> candidates = moveGenerator.generate(board);
        List<Fork> forks = new ArrayList<>();

        for (Move move : candidates) {
            if (!board.isEmpty(move.x(), move.y())) {
                continue;
            }

            board.placeStone(move.x(), move.y(), player);
            try {
                if (winChecker.isWinningMove(board, move)) {
                    continue;
                }

                List<Move> directDefenses = findDefenseSquaresForMove(board, move, player);
                int fourCount = directDefenses.size();

                List<Threat> foursInNewPos = findFourThreats(board, player);
                int openFourCount = 0;
                for (Threat f : foursInNewPos) {
                    if (f.isOpenFour()) {
                        openFourCount++;
                    }
                }

                if (fourCount >= 2) {
                    forks.add(new Fork(ForkType.DOUBLE_FOUR, move));
                } else if (fourCount == 1 && openFourCount >= 1) {
                    forks.add(new Fork(ForkType.FOUR_THREE, move));
                } else if (fourCount == 0 && openFourCount >= 2) {
                    forks.add(new Fork(ForkType.DOUBLE_THREE, move));
                }
            } finally {
                board.removeStone(move.x(), move.y());
            }
        }

        return forks;
    }

    public List<Threat> findAllThreats(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(player);

        List<Threat> result = new ArrayList<>();

        // 1. FIVE threats (Mate in 1)
        List<Move> winningMoves = findWinningMoves(board, player);
        for (Move move : winningMoves) {
            result.add(new Threat(ThreatType.FIVE, move, List.of()));
        }

        // 2. OPEN_FOUR and FOUR threats
        result.addAll(findFourThreats(board, player));

        return result;
    }

    public List<Move> findDefenseMovesForAttack(Board board, Move attackMove, byte attacker) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(attackMove, "Attack move must not be null");
        validatePlayer(attacker);

        if (!board.isInside(attackMove.x(), attackMove.y())) {
            throw new IndexOutOfBoundsException(
                    "Attack move is outside the board: " + attackMove
            );
        }

        if (!board.isEmpty(attackMove.x(), attackMove.y())) {
            throw new IllegalArgumentException(
                    "Attack move position is already occupied: " + attackMove
            );
        }

        board.placeStone(attackMove.x(), attackMove.y(), attacker);
        try {
            return findDefenseSquaresForMove(board, attackMove, attacker);
        } finally {
            board.removeStone(attackMove.x(), attackMove.y());
        }
    }

    public boolean isCounterThreat(Board board, Move defenseMove, byte defender) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(defenseMove, "Defense move must not be null");
        validatePlayer(defender);

        if (!board.isInside(defenseMove.x(), defenseMove.y())) {
            throw new IndexOutOfBoundsException(
                    "Defense move is outside the board: " + defenseMove
            );
        }

        if (!board.isEmpty(defenseMove.x(), defenseMove.y())) {
            throw new IllegalArgumentException(
                    "Defense move position is already occupied: " + defenseMove
            );
        }

        board.placeStone(defenseMove.x(), defenseMove.y(), defender);
        try {
            if (winChecker.isWinningMove(board, defenseMove)) {
                return true;
            }
            List<Move> defenseSquares = findDefenseSquaresForMove(board, defenseMove, defender);
            return !defenseSquares.isEmpty();
        } finally {
            board.removeStone(defenseMove.x(), defenseMove.y());
        }
    }

    private List<Move> findDefenseSquaresForMove(Board board, Move attackMove, byte attacker) {
        int winLength = winChecker.getWinLength();
        int maxDist = winLength - 1;
        Set<Move> defenseSet = new LinkedHashSet<>();

        for (int[] dir : DIRECTIONS) {
            int dx = dir[0];
            int dy = dir[1];

            for (int sign : new int[]{1, -1}) {
                int stepDx = dx * sign;
                int stepDy = dy * sign;

                for (int dist = 1; dist <= maxDist; dist++) {
                    int cx = attackMove.x() + stepDx * dist;
                    int cy = attackMove.y() + stepDy * dist;

                    if (!board.isInside(cx, cy)) {
                        break;
                    }

                    byte cell = board.getCell(cx, cy);
                    if (cell != Board.EMPTY && cell != attacker) {
                        break;
                    }

                    if (cell == Board.EMPTY) {
                        board.placeStone(cx, cy, attacker);
                        try {
                            if (winChecker.isWinningMove(board, new Move(cx, cy))) {
                                defenseSet.add(new Move(cx, cy));
                            }
                        } finally {
                            board.removeStone(cx, cy);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(defenseSet);
    }

    private void validatePlayer(byte player) {
        if (player != Board.BLACK && player != Board.WHITE) {
            throw new IllegalArgumentException("Invalid player: " + player);
        }
    }
}
