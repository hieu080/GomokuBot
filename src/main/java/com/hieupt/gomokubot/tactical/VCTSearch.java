package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.rules.WinChecker;

import java.util.*;

public class VCTSearch {

    private final ThreatDetector threatDetector;
    private final VCFSearch vcfSearch;
    private final WinChecker winChecker;
    private final ZobristHash zobristHash;

    public VCTSearch() {
        this(new ThreatDetector(), new VCFSearch());
    }

    public VCTSearch(ThreatDetector threatDetector, VCFSearch vcfSearch) {
        this(threatDetector, vcfSearch, threatDetector.getWinChecker(), vcfSearch.getZobristHash());
    }

    public VCTSearch(
            ThreatDetector threatDetector,
            VCFSearch vcfSearch,
            WinChecker winChecker,
            ZobristHash zobristHash
    ) {
        this.threatDetector = Objects.requireNonNull(threatDetector, "ThreatDetector must not be null");
        this.vcfSearch = Objects.requireNonNull(vcfSearch, "VCFSearch must not be null");
        this.winChecker = Objects.requireNonNull(winChecker, "WinChecker must not be null");
        this.zobristHash = Objects.requireNonNull(zobristHash, "ZobristHash must not be null");
    }

    public ThreatDetector getThreatDetector() {
        return threatDetector;
    }

    public VCFSearch getVcfSearch() {
        return vcfSearch;
    }

    public WinChecker getWinChecker() {
        return winChecker;
    }

    public ZobristHash getZobristHash() {
        return zobristHash;
    }

    public VCTResult search(Board board, byte attacker, int maxDepth) {
        return search(board, attacker, maxDepth, 0, 0);
    }

    public VCTResult search(
            Board board,
            byte attacker,
            int maxDepth,
            long maxNodes,
            long timeLimitMillis
    ) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(attacker);

        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be at least 1");
        }

        ZobristHash hashEngine = getZobristHashForBoard(board);
        long initialHash = hashEngine.computeHash(board, attacker);

        SearchContext context = new SearchContext(
                attacker,
                oppositePlayer(attacker),
                maxDepth,
                maxNodes,
                timeLimitMillis,
                hashEngine
        );

        List<Move> sequence = new ArrayList<>();
        boolean found = solveVCT(board, attacker, initialHash, maxDepth, sequence, context);

        long elapsedTime = context.getElapsedTimeMillis();
        if (found && !sequence.isEmpty()) {
            return VCTResult.success(sequence, context.nodesSearched, elapsedTime);
        } else {
            return VCTResult.failure(context.nodesSearched, elapsedTime);
        }
    }

    private boolean solveVCT(
            Board board,
            byte currentAttacker,
            long hash,
            int depthRemaining,
            List<Move> sequence,
            SearchContext context
    ) {
        if (!context.enterNode()) {
            return false;
        }

        if (depthRemaining <= 0) {
            context.recordFailedState(hash, depthRemaining);
            return false;
        }

        if (context.hasFailedState(hash, depthRemaining)) {
            return false;
        }

        byte defender = oppositePlayer(currentAttacker);

        // 1. Kiểm tra VCF (Terminal solver): Nếu Attacker có VCF thì thắng ngay!
        if (context.isStopRequested()) {
            return false;
        }
        int vcfDepth = Math.max(1, Math.min(depthRemaining * 2, 20));
        long vcfRemainingTime = context.getRemainingTimeMillis();
        long vcfMaxNodes = 300L;
        VCFResult vcfResult = vcfSearch.search(board, currentAttacker, vcfDepth, vcfMaxNodes, vcfRemainingTime);
        if (vcfResult.found()) {
            sequence.addAll(vcfResult.winningSequence());
            return true;
        }
        if (context.isStopRequested()) {
            return false;
        }

        // 2. Nếu Defender đang có FIVE, Attacker không thể đi Three
        if (!threatDetector.findWinningMoves(board, defender).isEmpty()) {
            context.recordFailedState(hash, depthRemaining);
            return false;
        }

        // 3. Tìm tất cả các Three-threats của Attacker
        List<Threat> threeThreats = threatDetector.findThreeThreats(board, currentAttacker);
        if (threeThreats.isEmpty()) {
            context.recordFailedState(hash, depthRemaining);
            return false;
        }

        for (Threat threat : threeThreats) {
            if (context.isStopRequested()) {
                return false;
            }

            Move attackMove = threat.attackMove();
            List<Move> defenseMoves = threat.defenseMoves();
            if (defenseMoves.isEmpty()) {
                continue;
            }

            board.placeStone(attackMove.x(), attackMove.y(), currentAttacker);
            long hashAfterAttack = context.hashEngine.updateAfterMove(
                    hash, attackMove.x(), attackMove.y(), currentAttacker
            );

            try {
                // Phản công của Defender: Attacker đi nước Three (chưa đe dọa Five ngay lập tức).
                // Nếu Defender có thể tạo FIVE ngay lập tức hoặc tạo FOUR (Open Four / Four),
                // Defender sẽ phản công ngay chứ không đi các nước defenseMoves bị động!
                if (!threatDetector.findWinningMoves(board, defender).isEmpty()
                        || !threatDetector.findFourThreats(board, defender).isEmpty()) {
                    continue;
                }

                // Kiểm tra xem Attacker có thắng đối với MỌI nước phòng thủ của Defender không (AND node)
                boolean allDefensesDefeated = true;
                List<Move> winningLineForFirstDefense = null;

                for (Move defenseMove : defenseMoves) {
                    if (context.isStopRequested()) {
                        allDefensesDefeated = false;
                        break;
                    }

                    // Defender đánh nước chặn
                    board.placeStone(defenseMove.x(), defenseMove.y(), defender);
                    long hashAfterDefense = context.hashEngine.updateAfterMove(
                            hashAfterAttack, defenseMove.x(), defenseMove.y(), defender
                    );

                    try {
                        // Nếu nước chặn của Defender tạo thành Five cho Defender -> phản công thắng
                        if (winChecker.isWinningMove(board, defenseMove)) {
                            allDefensesDefeated = false;
                            break;
                        }

                        List<Move> childSequence = new ArrayList<>();
                        boolean defeated = solveVCT(
                                board,
                                currentAttacker,
                                hashAfterDefense,
                                depthRemaining - 1,
                                childSequence,
                                context
                        );

                        if (!defeated) {
                            allDefensesDefeated = false;
                            break;
                        }

                        if (winningLineForFirstDefense == null) {
                            winningLineForFirstDefense = childSequence;
                        }
                    } finally {
                        board.removeStone(defenseMove.x(), defenseMove.y());
                    }
                }

                if (allDefensesDefeated && winningLineForFirstDefense != null) {
                    sequence.add(attackMove);
                    sequence.add(defenseMoves.get(0));
                    sequence.addAll(winningLineForFirstDefense);
                    return true;
                }
            } finally {
                board.removeStone(attackMove.x(), attackMove.y());
            }
        }

        context.recordFailedState(hash, depthRemaining);
        return false;
    }

    private ZobristHash getZobristHashForBoard(Board board) {
        if (zobristHash.getWidth() == board.getWidth()
                && zobristHash.getHeight() == board.getHeight()) {
            return zobristHash;
        }
        return new ZobristHash(board.getWidth(), board.getHeight());
    }

    private void validatePlayer(byte player) {
        if (player != Board.BLACK && player != Board.WHITE) {
            throw new IllegalArgumentException("Invalid player: " + player);
        }
    }

    private static byte oppositePlayer(byte player) {
        return player == Board.BLACK
                ? Board.WHITE
                : Board.BLACK;
    }

    static final class SearchContext {
        final byte attacker;
        final byte defender;
        final int maxDepth;
        final long maxNodes;
        final long timeLimitNanos;
        final long startNanos;
        final ZobristHash hashEngine;
        final Map<Long, Integer> failedStates = new HashMap<>();

        long nodesSearched;
        boolean stopped;

        SearchContext(
                byte attacker,
                byte defender,
                int maxDepth,
                long maxNodes,
                long timeLimitMillis,
                ZobristHash hashEngine
        ) {
            this.attacker = attacker;
            this.defender = defender;
            this.maxDepth = maxDepth;
            this.maxNodes = maxNodes;
            this.startNanos = System.nanoTime();
            this.timeLimitNanos = timeLimitMillis <= 0
                    ? 0
                    : timeLimitMillis * 1_000_000L;
            this.hashEngine = hashEngine;
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
                    && (System.nanoTime() - startNanos) >= timeLimitNanos) {
                stopped = true;
                return true;
            }
            return false;
        }

        boolean hasFailedState(long hash, int depthRemaining) {
            Integer recordedDepth = failedStates.get(hash);
            return recordedDepth != null && recordedDepth >= depthRemaining;
        }

        void recordFailedState(long hash, int depthRemaining) {
            failedStates.merge(hash, depthRemaining, Math::max);
        }

        long getElapsedTimeMillis() {
            return (System.nanoTime() - startNanos) / 1_000_000L;
        }

        long getRemainingTimeMillis() {
            if (timeLimitNanos <= 0) {
                return 0L;
            }
            long elapsedNanos = System.nanoTime() - startNanos;
            long remainingNanos = timeLimitNanos - elapsedNanos;
            return Math.max(1L, remainingNanos / 1_000_000L);
        }
    }
}
