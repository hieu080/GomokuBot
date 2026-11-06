package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.evaluation.Evaluator;
import com.hieupt.gomokubot.hash.TranspositionTable;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;

import java.util.List;
import java.util.Objects;

final class AlphaBeta {

    private static final int INFINITY = 2_000_000;
    private static final int MATE_SCORE = 1_000_000;

    private final Evaluator evaluator;
    private final MoveOrdering moveOrdering;
    private final WinChecker winChecker;
    private final ZobristHash zobristHash;
    private final TranspositionTable transpositionTable;

    AlphaBeta(
            Evaluator evaluator,
            MoveOrdering moveOrdering,
            WinChecker winChecker,
            ZobristHash zobristHash,
            TranspositionTable transpositionTable
    ) {
        this.evaluator = evaluator;
        this.moveOrdering = moveOrdering;
        this.winChecker = winChecker;
        this.zobristHash = zobristHash;
        this.transpositionTable = transpositionTable;
    }

    AlphaBeta(
            Evaluator evaluator,
            CandidateMoveGenerator moveGenerator,
            WinChecker winChecker,
            ZobristHash zobristHash,
            TranspositionTable transpositionTable
    ) {
        this(
                evaluator,
                new MoveOrdering(moveGenerator, winChecker),
                winChecker,
                zobristHash,
                transpositionTable
        );
    }

    int search(
            Board board,
            byte player,
            int depth,
            int alpha,
            int beta,
            long hash,
            int ply,
            SearchContext context
    ) {
        return search(
                board,
                player,
                depth,
                alpha,
                beta,
                hash,
                ply,
                null,
                context
        );
    }

    int search(
            Board board,
            byte player,
            int depth,
            int alpha,
            int beta,
            long hash,
            int ply,
            Move previousBestMove,
            SearchContext context
    ) {
        if (!context.enterNode()) {
            return 0;
        }

        if (board.isFull()) {
            return 0;
        }

        if (depth <= 0) {
            return evaluator.evaluate(board, player);
        }

        int originalAlpha = alpha;
        int originalBeta = beta;

        Move ttMove = null;
        TranspositionTable.ProbeResult probe =
                context.probeResult;

        if (transpositionTable.probe(hash, probe)) {
            int ttDepth = probe.getDepth();
            int ttScore = probe.getScore();

            if (probe.hasBestMove()) {
                ttMove = new Move(
                        probe.getBestMoveX(),
                        probe.getBestMoveY()
                );
            }

            if (ttDepth >= depth) {
                switch (probe.getBound()) {
                    case EXACT -> {
                        if (ply == 0) {
                            context.rootBestMove = ttMove;
                        }
                        return ttScore;
                    }
                    case LOWER_BOUND -> alpha = Math.max(alpha, ttScore);
                    case UPPER_BOUND -> beta = Math.min(beta, ttScore);
                }

                if (alpha >= beta) {
                    if (ply == 0) {
                        context.rootBestMove = ttMove;
                    }
                    return ttScore;
                }
            }
        }

        List<Move> moves = moveOrdering.orderMoves(
                board,
                player,
                ttMove,
                previousBestMove,
                context.getKiller1(ply),
                context.getKiller2(ply)
        );

        if (moves.isEmpty()) {
            return 0;
        }

        int bestScore = -INFINITY;
        Move bestMove = null;

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            if (context.isStopRequested()) {
                return 0;
            }

            board.placeStone(move.x(), move.y(), player);

            long childHash = zobristHash.updateAfterMove(
                    hash,
                    move.x(),
                    move.y(),
                    player
            );

            int score;

            try {
                if (winChecker.isWinningMove(board, move)) {
                    score = MATE_SCORE;
                } else if (i == 0) {
                    // Full window search on PV move
                    score = -search(
                            board,
                            oppositePlayer(player),
                            depth - 1,
                            -beta,
                            -alpha,
                            childHash,
                            ply + 1,
                            null,
                            context
                    );
                } else {
                    // PVS: Null-window search to test whether move can beat alpha
                    score = -search(
                            board,
                            oppositePlayer(player),
                            depth - 1,
                            -alpha - 1,
                            -alpha,
                            childHash,
                            ply + 1,
                            null,
                            context
                    );

                    // If null-window failed high, re-search with full window
                    if (score > alpha && score < beta && !context.stopped) {
                        score = -search(
                                board,
                                oppositePlayer(player),
                                depth - 1,
                                -beta,
                                -score,
                                childHash,
                                ply + 1,
                                null,
                                context
                        );
                    }
                }
            } finally {
                board.removeStone(move.x(), move.y());
            }

            if (context.stopped) {
                return 0;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;

                if (ply == 0) {
                    context.rootBestMove = move;
                }
            }

            alpha = Math.max(alpha, score);

            if (alpha >= beta) {
                if (score != MATE_SCORE) {
                    context.storeKiller(ply, move);
                }
                break;
            }
        }

        if (context.stopped || bestMove == null) {
            return 0;
        }

        TranspositionTable.Bound bound;

        if (bestScore <= originalAlpha) {
            bound = TranspositionTable.Bound.UPPER_BOUND;
        } else if (bestScore >= originalBeta) {
            bound = TranspositionTable.Bound.LOWER_BOUND;
        } else {
            bound = TranspositionTable.Bound.EXACT;
        }

        transpositionTable.store(
                hash,
                depth,
                bestScore,
                bound,
                bestMove
        );

        return bestScore;
    }

    private static byte oppositePlayer(byte player) {
        return player == Board.BLACK
                ? Board.WHITE
                : Board.BLACK;
    }

    static final class SearchContext {

        private final long maxNodes;
        private final long startNanos;
        private final long timeLimitNanos;

        final TranspositionTable.ProbeResult probeResult =
                new TranspositionTable.ProbeResult();

        private final Move[][] killerMoves = new Move[128][2];

        long nodesSearched;
        boolean stopped;
        Move rootBestMove;

        void storeKiller(int ply, Move move) {
            if (ply >= 128 || move == null) {
                return;
            }
            if (!Objects.equals(killerMoves[ply][0], move)) {
                killerMoves[ply][1] = killerMoves[ply][0];
                killerMoves[ply][0] = move;
            }
        }

        Move getKiller1(int ply) {
            return (ply < 128) ? killerMoves[ply][0] : null;
        }

        Move getKiller2(int ply) {
            return (ply < 128) ? killerMoves[ply][1] : null;
        }

        SearchContext(SearchLimits limits) {
            this(
                    limits,
                    System.nanoTime(),
                    limits.timeLimitMillis() == 0
                            ? 0
                            : limits.timeLimitMillis() * 1_000_000L
            );
        }

        SearchContext(SearchLimits limits, long startNanos, long timeLimitNanos) {
            this.maxNodes = limits.maxNodes();
            this.startNanos = startNanos;
            this.timeLimitNanos = timeLimitNanos;
        }

        boolean enterNode() {
            if (isStopRequested()) {
                return false;
            }

            nodesSearched++;
            return true;
        }

        boolean isStopRequested() {
            if (stopped) {
                return true;
            }

            if (maxNodes > 0 && nodesSearched >= maxNodes) {
                stopped = true;
                return true;
            }

            if (timeLimitNanos > 0
                    && System.nanoTime() - startNanos >= timeLimitNanos) {
                stopped = true;
                return true;
            }

            return false;
        }

        long getElapsedTimeMillis() {
            return (System.nanoTime() - startNanos) / 1_000_000L;
        }
    }
}