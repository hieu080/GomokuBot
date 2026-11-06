package com.hieupt.gomokubot.evaluation;

import com.hieupt.gomokubot.core.Board;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Evaluator {
    public static final int WIN_SCORE = 1_000_000;

    private static final Map<Pattern, Integer> PATTERN_WEIGHTS =
            createPatternWeights();

    private final PatternRecognizer patternRecognizer;
    private final boolean comboBonusesEnabled;

    public Evaluator() {
        this(new PatternRecognizer(), true);
    }

    public Evaluator(boolean comboBonusesEnabled) {
        this(new PatternRecognizer(), comboBonusesEnabled);
    }

    public Evaluator(PatternRecognizer patternRecognizer) {
        this(patternRecognizer, true);
    }

    public Evaluator(PatternRecognizer patternRecognizer, boolean comboBonusesEnabled) {
        this.patternRecognizer = Objects.requireNonNull(
                patternRecognizer,
                "PatternRecognizer must not be null"
        );
        this.comboBonusesEnabled = comboBonusesEnabled;
    }

    public int evaluate(Board board, byte perspectivePlayer) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(perspectivePlayer);

        byte opponent = getOpponent(perspectivePlayer);

        long ownScore = evaluatePlayer(board, perspectivePlayer);
        long opponentScore = evaluatePlayer(board, opponent);

        long finalScore = ownScore - opponentScore;

        return clampScore(finalScore);
    }

    public long evaluatePlayer(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(player);

        List<PatternResult> patterns =
                patternRecognizer.recognize(board, player);

        EnumMap<Pattern, Integer> patternCounts =
                new EnumMap<>(Pattern.class);

        for (PatternResult result : patterns) {
            patternCounts.merge(
                    result.pattern(),
                    1,
                    Integer::sum
            );
        }

        // A five-in-a-row is a terminal winning condition.
        if (patternCounts.containsKey(Pattern.FIVE)) {
            return WIN_SCORE;
        }

        long totalScore = 0;

        for (Map.Entry<Pattern, Integer> entry
                : patternCounts.entrySet()) {

            Pattern pattern = entry.getKey();
            int count = entry.getValue();

            int weight = PATTERN_WEIGHTS.getOrDefault(pattern, 0);

            totalScore += calculateDiminishingScore(weight, count);
        }

        if (comboBonusesEnabled) {
            totalScore += calculateComboBonuses(patternCounts);
        }

        return Math.min(totalScore, WIN_SCORE - 1L);
    }

    private long calculateComboBonuses(EnumMap<Pattern, Integer> counts) {
        int openFours = counts.getOrDefault(Pattern.OPEN_FOUR, 0);
        int closedFours = counts.getOrDefault(Pattern.CLOSED_FOUR, 0)
                + counts.getOrDefault(Pattern.BROKEN_FOUR, 0);
        int openThrees = counts.getOrDefault(Pattern.OPEN_THREE, 0)
                + counts.getOrDefault(Pattern.BROKEN_THREE, 0);
        int openTwos = counts.getOrDefault(Pattern.OPEN_TWO, 0);

        long bonus = 0;

        // Double Four (>= 2 fours) -> Thắng không thể cản
        if ((openFours + closedFours) >= 2) {
            bonus += 125_000;
        }
        // Four-Three Fork (>= 1 four và >= 1 three) -> Thắng không thể cản
        else if ((openFours + closedFours) >= 1 && openThrees >= 1) {
            bonus += 80_000;
        }
        // Double Three Fork (>= 2 threes) -> Thắng không thể cản
        else if (openThrees >= 2) {
            bonus += 60_000;
        }
        // Trap 3-2 (1 three + 1 two) -> Nguy cơ tạo 4-3 hoặc 3-3 rất cao
        else if (openThrees >= 1 && openTwos >= 1) {
            bonus += 10_000;
        }
        // Trap 2-2 seed (>= 2 twos) -> Mầm bẫy giao điểm
        else if (openTwos >= 2) {
            bonus += 3_000;
        }

        return bonus;
    }

    private long calculateDiminishingScore(int weight, int count) {
        if (count <= 0 || weight <= 0) {
            return 0;
        }

        long score = weight;

        long contribution = weight;

        for (int i = 1; i < count; i++) {
            contribution /= 2;

            if (contribution == 0) {
                break;
            }

            score += contribution;
        }

        return score;
    }

    private static EnumMap<Pattern, Integer> createPatternWeights() {
        EnumMap<Pattern, Integer> weights =
                new EnumMap<>(Pattern.class);

        weights.put(Pattern.FIVE, WIN_SCORE);
        weights.put(Pattern.OPEN_FOUR, 100_000);
        weights.put(Pattern.BROKEN_FOUR, 80_000);
        weights.put(Pattern.CLOSED_FOUR, 30_000);
        weights.put(Pattern.OPEN_THREE, 10_000);
        weights.put(Pattern.BROKEN_THREE, 9_000);
        weights.put(Pattern.OPEN_TWO, 500);

        return weights;
    }

    private byte getOpponent(byte player) {
        return player == Board.BLACK
                ? Board.WHITE
                : Board.BLACK;
    }

    private void validatePlayer(byte player) {
        if (player != Board.BLACK && player != Board.WHITE) {
            throw new IllegalArgumentException(
                    "Invalid player: " + player
            );
        }
    }

    private int clampScore(long score) {
        if (score > WIN_SCORE) {
            return WIN_SCORE;
        }

        if (score < -WIN_SCORE) {
            return -WIN_SCORE;
        }

        return (int) score;
    }
}
