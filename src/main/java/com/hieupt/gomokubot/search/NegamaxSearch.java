package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.evaluation.Evaluator;
import com.hieupt.gomokubot.hash.TranspositionTable;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import com.hieupt.gomokubot.tactical.Threat;
import com.hieupt.gomokubot.tactical.ThreatDetector;
import com.hieupt.gomokubot.tactical.VCFResult;
import com.hieupt.gomokubot.tactical.VCFSearch;
import com.hieupt.gomokubot.tactical.VCTResult;
import com.hieupt.gomokubot.tactical.VCTSearch;

import java.util.List;
import java.util.Objects;

public class NegamaxSearch {

    private static final int INFINITY = 2_000_000;
    private static final int MATE_SCORE = 1_000_000;
    private static final int ASPIRATION_DELTA = 5000;

    private final AlphaBeta alphaBeta;
    private final MoveOrdering moveOrdering;
    private final WinChecker winChecker;
    private final ZobristHash zobristHash;
    private final TranspositionTable transpositionTable;
    private final ThreatDetector threatDetector;
    private final VCFSearch vcfSearch;
    private final VCTSearch vctSearch;

    private boolean vcfEnabled = true;
    private boolean vctEnabled = true;

    public NegamaxSearch(
            Evaluator evaluator,
            CandidateMoveGenerator moveGenerator,
            WinChecker winChecker,
            ZobristHash zobristHash,
            TranspositionTable transpositionTable
    ) {
        this(
                evaluator,
                moveGenerator,
                winChecker,
                zobristHash,
                transpositionTable,
                createDefaultVCFSearch(winChecker, moveGenerator, zobristHash),
                createDefaultVCTSearch(winChecker, moveGenerator, zobristHash)
        );
    }

    private static VCFSearch createDefaultVCFSearch(
            WinChecker winChecker,
            CandidateMoveGenerator moveGenerator,
            ZobristHash zobristHash
    ) {
        if (winChecker == null || moveGenerator == null || zobristHash == null) {
            return null;
        }
        return new VCFSearch(new ThreatDetector(winChecker, moveGenerator), winChecker, zobristHash);
    }

    private static VCTSearch createDefaultVCTSearch(
            WinChecker winChecker,
            CandidateMoveGenerator moveGenerator,
            ZobristHash zobristHash
    ) {
        if (winChecker == null || moveGenerator == null || zobristHash == null) {
            return null;
        }
        ThreatDetector detector = new ThreatDetector(winChecker, moveGenerator);
        VCFSearch vcf = new VCFSearch(detector, winChecker, zobristHash);
        return new VCTSearch(detector, vcf, winChecker, zobristHash);
    }

    public NegamaxSearch(
            Evaluator evaluator,
            CandidateMoveGenerator moveGenerator,
            WinChecker winChecker,
            ZobristHash zobristHash,
            TranspositionTable transpositionTable,
            VCFSearch vcfSearch
    ) {
        this(
                evaluator,
                moveGenerator,
                winChecker,
                zobristHash,
                transpositionTable,
                vcfSearch,
                createDefaultVCTSearch(winChecker, moveGenerator, zobristHash)
        );
    }

    public NegamaxSearch(
            Evaluator evaluator,
            CandidateMoveGenerator moveGenerator,
            WinChecker winChecker,
            ZobristHash zobristHash,
            TranspositionTable transpositionTable,
            VCFSearch vcfSearch,
            VCTSearch vctSearch
    ) {
        Objects.requireNonNull(evaluator, "Evaluator must not be null");
        Objects.requireNonNull(
                moveGenerator,
                "Move generator must not be null"
        );
        Objects.requireNonNull(winChecker, "WinChecker must not be null");
        this.winChecker = winChecker;

        this.zobristHash = Objects.requireNonNull(
                zobristHash,
                "ZobristHash must not be null"
        );

        this.transpositionTable = Objects.requireNonNull(
                transpositionTable,
                "TranspositionTable must not be null"
        );

        this.moveOrdering = new MoveOrdering(moveGenerator, winChecker);

        this.threatDetector = new ThreatDetector(winChecker, moveGenerator);

        this.vcfSearch = vcfSearch != null
                ? vcfSearch
                : new VCFSearch(this.threatDetector, winChecker, this.zobristHash);

        this.vctSearch = vctSearch != null
                ? vctSearch
                : new VCTSearch(this.threatDetector, this.vcfSearch, winChecker, this.zobristHash);

        if (zobristHash.getWidth() != transpositionTable.getWidth()
                || zobristHash.getHeight()
                != transpositionTable.getHeight()) {
            throw new IllegalArgumentException(
                    "ZobristHash and TranspositionTable dimensions "
                            + "must match"
            );
        }

        this.alphaBeta = new AlphaBeta(
                evaluator,
                this.moveOrdering,
                winChecker,
                zobristHash,
                transpositionTable
        );
    }

    public VCFSearch getVcfSearch() {
        return vcfSearch;
    }

    public VCTSearch getVctSearch() {
        return vctSearch;
    }

    public boolean isVcfEnabled() {
        return vcfEnabled;
    }

    public void setVcfEnabled(boolean vcfEnabled) {
        this.vcfEnabled = vcfEnabled;
    }

    public boolean isVctEnabled() {
        return vctEnabled;
    }

    public void setVctEnabled(boolean vctEnabled) {
        this.vctEnabled = vctEnabled;
    }

    public SearchResult search(
            Board board,
            byte currentPlayer,
            SearchLimits limits
    ) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(limits, "SearchLimits must not be null");

        if (currentPlayer != Board.BLACK
                && currentPlayer != Board.WHITE) {
            throw new IllegalArgumentException(
                    "Invalid current player: " + currentPlayer
            );
        }

        if (board.getWidth() != zobristHash.getWidth()
                || board.getHeight() != zobristHash.getHeight()) {
            throw new IllegalArgumentException(
                    "Board dimensions do not match search configuration"
            );
        }

        long globalStartNanos = System.nanoTime();
        long totalTimeLimitNanos = limits.hasTimeLimit()
                ? limits.timeLimitMillis() * 1_000_000L
                : 0L;

        if (board.isEmpty()) {
            Move centerMove = new Move(board.getWidth() / 2, board.getHeight() / 2);
            int depth = limits.hasDepthLimit() ? limits.maxDepth() : 1;
            return new SearchResult(centerMove, 0, depth, 1, 0, true);
        }

        if (board.isFull()) {
            return new SearchResult(null, 0, 0, 0, 0, true);
        }

        // Tạo sẵn nước đi dự phòng an toàn từ moveOrdering
        List<Move> initialCandidates = moveOrdering.orderMoves(
                board,
                currentPlayer,
                null,
                null,
                null,
                null
        );
        Move fallbackMove = !initialCandidates.isEmpty()
                ? initialCandidates.get(0)
                : new Move(board.getWidth() / 2, board.getHeight() / 2);

        // Kiểm tra nước thắng ngay tức thì ở root
        if (!initialCandidates.isEmpty()) {
            Move topCandidate = initialCandidates.get(0);
            board.placeStone(topCandidate.x(), topCandidate.y(), currentPlayer);
            try {
                if (winChecker.isWinningMove(board, topCandidate)) {
                    long elapsed = (System.nanoTime() - globalStartNanos) / 1_000_000L;
                    return new SearchResult(topCandidate, MATE_SCORE, 1, 1, elapsed, true);
                }
            } finally {
                board.removeStone(topCandidate.x(), topCandidate.y());
            }
        }

        int maxDepth = limits.hasDepthLimit()
                ? limits.maxDepth()
                : board.getEmptyCount();

        Move previousBestMove = fallbackMove;

        // Kiểm tra VCF / VCT chiến thuật ở root nếu bàn cờ đã có đủ quân
        if (board.getOccupiedCount() >= 4 && !limits.hasNodeLimit()) {
            int tacticalDepth = Math.min(Math.max(maxDepth * 2, 10), 20);

            long totalTimeMs = limits.timeLimitMillis();
            long vcfBudgetMs = limits.hasTimeLimit()
                    ? Math.max(30L, Math.min(250L, totalTimeMs / 8))
                    : 300L;
            long vcfMaxNodes = 2000L;

            // 1. VCF Search
            if (vcfEnabled && vcfSearch != null) {
                // 1a. Offensive VCF
                VCFResult myVcf = vcfSearch.search(board, currentPlayer, tacticalDepth, vcfMaxNodes, vcfBudgetMs);
                if (myVcf.found()) {
                    long elapsed = (System.nanoTime() - globalStartNanos) / 1_000_000L;
                    return new SearchResult(
                            myVcf.winningMove(),
                            MATE_SCORE,
                            myVcf.depth(),
                            myVcf.nodesSearched(),
                            elapsed,
                            true
                    );
                }

                // Check remaining time before defensive VCF
                long elapsedSoFar = (System.nanoTime() - globalStartNanos) / 1_000_000L;
                long remainingMs = limits.hasTimeLimit() ? totalTimeMs - elapsedSoFar : Long.MAX_VALUE;

                // 1b. Defensive VCF
                if (!limits.hasTimeLimit() || remainingMs > 100) {
                    long defVcfBudgetMs = limits.hasTimeLimit() ? Math.min(vcfBudgetMs, remainingMs / 2) : vcfBudgetMs;
                    VCFResult oppVcf = vcfSearch.search(board, oppositePlayer(currentPlayer), tacticalDepth, vcfMaxNodes, defVcfBudgetMs);
                    if (oppVcf.found()) {
                        previousBestMove = oppVcf.winningMove();
                    }
                }
            }

            // 2. VCT Search (khi không có VCF và bàn cờ đã qua khai cuộc)
            long elapsedSoFar = (System.nanoTime() - globalStartNanos) / 1_000_000L;
            long remainingMs = limits.hasTimeLimit() ? totalTimeMs - elapsedSoFar : Long.MAX_VALUE;

            if (vctEnabled && vctSearch != null && board.getOccupiedCount() >= 6 && (!limits.hasTimeLimit() || remainingMs > 300)) {
                int vctDepth = Math.min(tacticalDepth, 8);
                long vctBudgetMs = limits.hasTimeLimit()
                        ? Math.max(50L, Math.min(400L, remainingMs / 4))
                        : 500L;
                long vctMaxNodes = 2000L;

                // 2a. Offensive VCT
                VCTResult myVct = vctSearch.search(board, currentPlayer, vctDepth, vctMaxNodes, vctBudgetMs);
                if (myVct.found()) {
                    Move vctMove = myVct.winningMove();
                    board.placeStone(vctMove.x(), vctMove.y(), currentPlayer);
                    boolean oppHasInstantWin = false;
                    try {
                        byte opp = oppositePlayer(currentPlayer);
                        if (!threatDetector.findWinningMoves(board, opp).isEmpty()) {
                            oppHasInstantWin = true;
                        } else {
                            for (Threat ft : threatDetector.findFourThreats(board, opp)) {
                                if (ft.isOpenFour()) {
                                    oppHasInstantWin = true;
                                    break;
                                }
                            }
                        }
                    } finally {
                        board.removeStone(vctMove.x(), vctMove.y());
                    }

                    if (!oppHasInstantWin) {
                        long elapsed = (System.nanoTime() - globalStartNanos) / 1_000_000L;
                        return new SearchResult(
                                vctMove,
                                MATE_SCORE - 50,
                                myVct.depth(),
                                myVct.nodesSearched(),
                                elapsed,
                                true
                        );
                    }
                }

                elapsedSoFar = (System.nanoTime() - globalStartNanos) / 1_000_000L;
                remainingMs = limits.hasTimeLimit() ? totalTimeMs - elapsedSoFar : Long.MAX_VALUE;

                // 2b. Defensive VCT (chỉ tìm nếu đối thủ chưa có đe dọa VCF cần chặn)
                if (previousBestMove.equals(fallbackMove) && (!limits.hasTimeLimit() || remainingMs > 300)) {
                    long defVctBudgetMs = limits.hasTimeLimit() ? Math.min(vctBudgetMs, remainingMs / 4) : vctBudgetMs;
                    VCTResult oppVct = vctSearch.search(board, oppositePlayer(currentPlayer), vctDepth, vctMaxNodes, defVctBudgetMs);
                    if (oppVct.found()) {
                        previousBestMove = oppVct.winningMove();
                    }
                }
            }
        }

        AlphaBeta.SearchContext context =
                new AlphaBeta.SearchContext(limits, globalStartNanos, totalTimeLimitNanos);

        long hash = zobristHash.computeHash(board, currentPlayer);

        Move bestMove = previousBestMove;
        int bestScore = 0;
        int completedDepth = 0;

        for (int depth = 1; depth <= maxDepth; depth++) {
            int alpha = -INFINITY;
            int beta = INFINITY;

            // Aspiration Windows cho depth >= 3
            if (depth >= 3 && completedDepth >= 2 && Math.abs(bestScore) < MATE_SCORE - 100) {
                alpha = Math.max(-INFINITY, bestScore - ASPIRATION_DELTA);
                beta = Math.min(INFINITY, bestScore + ASPIRATION_DELTA);
            }

            int score = alphaBeta.search(
                    board,
                    currentPlayer,
                    depth,
                    alpha,
                    beta,
                    hash,
                    0,
                    bestMove,
                    context
            );

            if (context.stopped) {
                if (context.rootBestMove != null && completedDepth == 0) {
                    bestMove = context.rootBestMove;
                }
                break;
            }

            // Aspiration Window Fail-low re-search
            if (score <= alpha && alpha > -INFINITY) {
                score = alphaBeta.search(
                        board,
                        currentPlayer,
                        depth,
                        -INFINITY,
                        beta,
                        hash,
                        0,
                        bestMove,
                        context
                );
            }
            // Aspiration Window Fail-high re-search
            else if (score >= beta && beta < INFINITY) {
                score = alphaBeta.search(
                        board,
                        currentPlayer,
                        depth,
                        alpha,
                        INFINITY,
                        hash,
                        0,
                        bestMove,
                        context
                );
            }

            if (context.stopped) {
                if (context.rootBestMove != null && completedDepth == 0) {
                    bestMove = context.rootBestMove;
                }
                break;
            }

            if (context.rootBestMove != null) {
                bestMove = context.rootBestMove;
            }
            bestScore = score;
            completedDepth = depth;

            if (bestScore >= MATE_SCORE || bestScore <= -MATE_SCORE) {
                break;
            }
        }

        if (bestMove == null) {
            bestMove = fallbackMove;
        }

        long elapsedTime = (System.nanoTime() - globalStartNanos) / 1_000_000L;

        return new SearchResult(
                bestMove,
                bestScore,
                completedDepth,
                context.nodesSearched,
                elapsedTime,
                !context.stopped
        );
    }

    public void clearTranspositionTable() {
        transpositionTable.clear();
    }

    private static byte oppositePlayer(byte player) {
        return player == Board.BLACK
                ? Board.WHITE
                : Board.BLACK;
    }
}
