package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import com.hieupt.gomokubot.tactical.SpatialTrapDetector;

import java.util.*;

final class MoveOrdering {

    private final CandidateMoveGenerator moveGenerator;
    private final WinChecker winChecker;
    private final SpatialTrapDetector spatialDetector;

    MoveOrdering(
            CandidateMoveGenerator moveGenerator,
            WinChecker winChecker
    ) {
        this(moveGenerator, winChecker, new SpatialTrapDetector());
    }

    MoveOrdering(
            CandidateMoveGenerator moveGenerator,
            WinChecker winChecker,
            SpatialTrapDetector spatialDetector
    ) {
        this.moveGenerator = Objects.requireNonNull(moveGenerator, "CandidateMoveGenerator must not be null");
        this.winChecker = Objects.requireNonNull(winChecker, "WinChecker must not be null");
        this.spatialDetector = Objects.requireNonNull(spatialDetector, "SpatialTrapDetector must not be null");
    }

    List<Move> orderMoves(
            Board board,
            byte player,
            Move ttMove,
            Move previousBestMove
    ) {
        return orderMoves(board, player, ttMove, previousBestMove, null, null);
    }

    List<Move> orderMoves(
            Board board,
            byte player,
            Move ttMove,
            Move previousBestMove,
            Move killer1,
            Move killer2
    ) {
        List<Move> candidates = new ArrayList<>(
                moveGenerator.generate(board)
        );

        if (candidates.size() <= 1) {
            return candidates;
        }

        byte opponent = oppositePlayer(player);

        List<ScoredMove> scoredMoves =
                new ArrayList<>(candidates.size());

        double centerX = (board.getWidth() - 1) / 2.0;
        double centerY = (board.getHeight() - 1) / 2.0;

        for (Move move : candidates) {
            int priority = 0;

            SpatialTrapDetector.MoveAnalysis attack =
                    spatialDetector.analyzeMove(board, move.x(), move.y(), player);
            SpatialTrapDetector.MoveAnalysis defense =
                    spatialDetector.analyzeMove(board, move.x(), move.y(), opponent);

            if (attack.isWin()) {
                priority = 100;
            } else if (defense.isWin()) {
                priority = 90;
            } else if (attack.isOpenFour()) {
                priority = 85;
            } else if (defense.isOpenFour()) {
                priority = 80;
            } else if (attack.closedFourCount() > 0) {
                priority = 75;
            } else if (defense.closedFourCount() > 0) {
                priority = 72;
            } else if (move.equals(ttMove)) {
                priority = 70;
            } else if (attack.isFork()) {
                priority = 65;
            } else if (defense.isFork()) {
                priority = 60;
            } else if (attack.openThreeCount() > 0) {
                priority = 55;
            } else if (defense.openThreeCount() > 0) {
                priority = 50;
            } else if (move.equals(previousBestMove)) {
                priority = 45;
            } else if (move.equals(killer1)) {
                priority = 40;
            } else if (move.equals(killer2)) {
                priority = 35;
            } else if (attack.isSpatialTrap() || attack.tacticalScore() >= SpatialTrapDetector.SCORE_TRAP_3_2) {
                priority = 25;
            } else if (defense.isSpatialTrap() || defense.tacticalScore() >= SpatialTrapDetector.SCORE_TRAP_3_2) {
                priority = 20;
            }

            int tacticalPotential = attack.tacticalScore() * 2 + defense.tacticalScore();

            double dx = move.x() - centerX;
            double dy = move.y() - centerY;
            double centerDistance = dx * dx + dy * dy;

            scoredMoves.add(
                    new ScoredMove(move, priority, tacticalPotential, centerDistance)
            );
        }

        scoredMoves.sort((m1, m2) -> {
            int pCompare = Integer.compare(m2.priority(), m1.priority());
            if (pCompare != 0) {
                return pCompare;
            }
            if (m1.priority() == 0) {
                return Double.compare(m1.centerDistance(), m2.centerDistance());
            }
            int tCompare = Integer.compare(m2.tacticalPotential(), m1.tacticalPotential());
            if (tCompare != 0) {
                return tCompare;
            }
            return Double.compare(m1.centerDistance(), m2.centerDistance());
        });

        List<Move> orderedMoves = new ArrayList<>(
                scoredMoves.size()
        );

        for (ScoredMove scoredMove : scoredMoves) {
            orderedMoves.add(scoredMove.move());
        }

        return orderedMoves;
    }

    private byte oppositePlayer(byte player) {
        return player == Board.BLACK
                ? Board.WHITE
                : Board.BLACK;
    }

    private record ScoredMove(
            Move move,
            int priority,
            int tacticalPotential,
            double centerDistance
    ) {}
}
