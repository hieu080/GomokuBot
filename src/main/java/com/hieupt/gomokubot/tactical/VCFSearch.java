package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.rules.WinChecker;

import java.util.*;

public class VCFSearch {

    private final ThreatDetector threatDetector;
    private final WinChecker winChecker;
    private final ZobristHash zobristHash;

    public VCFSearch() {
        this(new ThreatDetector(), new WinChecker(5));
    }

    public VCFSearch(ThreatDetector threatDetector) {
        this(threatDetector, threatDetector.getWinChecker());
    }

    public VCFSearch(ThreatDetector threatDetector, WinChecker winChecker) {
        this(threatDetector, winChecker, new ZobristHash(15, 15));
    }

    public VCFSearch(
            ThreatDetector threatDetector,
            WinChecker winChecker,
            ZobristHash zobristHash
    ) {
        this.threatDetector = Objects.requireNonNull(threatDetector, "ThreatDetector must not be null");
        this.winChecker = Objects.requireNonNull(winChecker, "WinChecker must not be null");
        this.zobristHash = Objects.requireNonNull(zobristHash, "ZobristHash must not be null");
    }

    public ThreatDetector getThreatDetector() {
        return threatDetector;
    }

    public WinChecker getWinChecker() {
        return winChecker;
    }

    public ZobristHash getZobristHash() {
        return zobristHash;
    }

    public VCFResult search(Board board, byte attacker, int maxDepth) {
        return search(board, attacker, maxDepth, 0, 0);
    }

    public VCFResult search(
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
        boolean found = solveVCF(board, attacker, initialHash, maxDepth, sequence, context);

        long elapsedTime = context.getElapsedTimeMillis();
        if (found && !sequence.isEmpty()) {
            return VCFResult.success(sequence, context.nodesSearched, elapsedTime);
        } else {
            return VCFResult.failure(context.nodesSearched, elapsedTime);
        }
    }

    private boolean solveVCF(
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

        if (context.hasFailedState(hash, depthRemaining)) {
            return false;
        }

        byte defender = oppositePlayer(currentAttacker);

        // 1. Kiểm tra Attacker có nước thắng FIVE ngay lập tức không (Mate in 1)
        List<Move> attackerWins = threatDetector.findWinningMoves(board, currentAttacker);
        if (!attackerWins.isEmpty()) {
            sequence.add(attackerWins.get(0));
            return true;
        }

        // Hết độ sâu cho phép tấn công
        if (depthRemaining <= 0) {
            context.recordFailedState(hash, depthRemaining);
            return false;
        }

        // 2. Kiểm tra Defender có mối đe dọa FIVE không (Defender có Four cần Attacker phải chặn)
        List<Move> defenderWins = threatDetector.findWinningMoves(board, defender);
        Set<Move> forcedAttackerSquares = defenderWins.isEmpty()
                ? null
                : new HashSet<>(defenderWins);

        if (forcedAttackerSquares != null && forcedAttackerSquares.size() >= 2) {
            // Defender có từ 2 ô thắng độc lập trở lên mà Attacker không có Five -> Attacker thua/không thể tiếp tục
            context.recordFailedState(hash, depthRemaining);
            return false;
        }

        // 3. Tìm tất cả các mối đe dọa Four của Attacker
        List<Threat> fourThreats = threatDetector.findFourThreats(board, currentAttacker);
        if (fourThreats.isEmpty()) {
            context.recordFailedState(hash, depthRemaining);
            return false;
        }

        for (Threat threat : fourThreats) {
            if (context.isStopRequested()) {
                return false;
            }

            Move attackMove = threat.attackMove();

            // Nếu Defender đang có mối đe dọa Five, nước Four của Attacker bắt buộc phải chặn đúng ô đó
            if (forcedAttackerSquares != null && !forcedAttackerSquares.contains(attackMove)) {
                continue;
            }

            // Trường hợp A: Open Four (hoặc Double Four có >= 2 ô phòng thủ độc lập)
            if (threat.isOpenFour()) {
                board.placeStone(attackMove.x(), attackMove.y(), currentAttacker);
                long attackHash = context.hashEngine.updateAfterMove(
                        hash, attackMove.x(), attackMove.y(), currentAttacker
                );

                try {
                    // Kiểm tra sau khi đánh attackMove, Defender có Five nào để thắng trước không
                    List<Move> immediateDefenderWins = threatDetector.findWinningMoves(board, defender);
                    if (immediateDefenderWins.isEmpty()) {
                        // Defender không thể đỡ hết cả 2 đầu -> Attacker thắng
                        sequence.add(attackMove);
                        Move defenseBlock = threat.defenseMoves().get(0);
                        sequence.add(defenseBlock);
                        Move winningStone = threat.defenseMoves().get(1);
                        sequence.add(winningStone);
                        return true;
                    }
                } finally {
                    board.removeStone(attackMove.x(), attackMove.y());
                }
            }

            // Trường hợp B: Closed Four hoặc Broken Four (có đúng 1 ô phòng thủ)
            if (threat.defenseMoves().size() == 1) {
                Move defenseMove = threat.defenseMoves().get(0);

                board.placeStone(attackMove.x(), attackMove.y(), currentAttacker);
                long hashAfterAttack = context.hashEngine.updateAfterMove(
                        hash, attackMove.x(), attackMove.y(), currentAttacker
                );

                try {
                    // Kiểm tra xem Defender có nước thắng FIVE nào khác trên bàn cờ không
                    List<Move> immediateDefenderWins = threatDetector.findWinningMoves(board, defender);
                    boolean defenderHasOtherWin = false;
                    for (Move dWin : immediateDefenderWins) {
                        if (!dWin.equals(defenseMove)) {
                            defenderHasOtherWin = true;
                            break;
                        }
                    }

                    if (!defenderHasOtherWin) {
                        // Defender bắt buộc phải đánh defenseMove
                        board.placeStone(defenseMove.x(), defenseMove.y(), defender);
                        long hashAfterDefense = context.hashEngine.updateAfterMove(
                                hashAfterAttack, defenseMove.x(), defenseMove.y(), defender
                        );

                        try {
                            sequence.add(attackMove);
                            sequence.add(defenseMove);

                            boolean won = solveVCF(
                                    board,
                                    currentAttacker,
                                    hashAfterDefense,
                                    depthRemaining - 1,
                                    sequence,
                                    context
                            );

                            if (won) {
                                return true;
                            }

                            // Backtrack sequence
                            sequence.remove(sequence.size() - 1);
                            sequence.remove(sequence.size() - 1);
                        } finally {
                            board.removeStone(defenseMove.x(), defenseMove.y());
                        }
                    }
                } finally {
                    board.removeStone(attackMove.x(), attackMove.y());
                }
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
    }
}
