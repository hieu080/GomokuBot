package com.hieupt.gomokubot.evaluation;

import com.hieupt.gomokubot.core.Board;

import java.util.*;

public final class PatternRecognizer {

    private static final char EMPTY = '.';
    private static final char BLOCKED = '#';
    private static final char OPPONENT = 'O';
    private static final char PLAYER = 'X';

    private static final PatternTemplate[] TEMPLATES = {
            new PatternTemplate(Pattern.OPEN_FOUR, ".XXXX.", 4),

            new PatternTemplate(Pattern.CLOSED_FOUR, "#XXXX.", 4),
            new PatternTemplate(Pattern.CLOSED_FOUR, ".XXXX#", 4),

            new PatternTemplate(Pattern.BROKEN_FOUR, ".XX.XX.", 4),
            new PatternTemplate(Pattern.BROKEN_FOUR, ".X.XXX.", 4),
            new PatternTemplate(Pattern.BROKEN_FOUR, ".XXX.X.", 4),

            new PatternTemplate(Pattern.BROKEN_FOUR, "#XX.XX.", 4),
            new PatternTemplate(Pattern.BROKEN_FOUR, ".XX.XX#", 4),
            new PatternTemplate(Pattern.BROKEN_FOUR, "#X.XXX.", 4),
            new PatternTemplate(Pattern.BROKEN_FOUR, ".X.XXX#", 4),
            new PatternTemplate(Pattern.BROKEN_FOUR, "#XXX.X.", 4),
            new PatternTemplate(Pattern.BROKEN_FOUR, ".XXX.X#", 4),

            new PatternTemplate(Pattern.OPEN_THREE, ".XXX.", 3),

            new PatternTemplate(Pattern.BROKEN_THREE, ".X.X.X.", 3),
            new PatternTemplate(Pattern.BROKEN_THREE, ".XX.X.", 3),
            new PatternTemplate(Pattern.BROKEN_THREE, ".X.XX.", 3),

            new PatternTemplate(Pattern.OPEN_TWO, ".XX.", 2),
            new PatternTemplate(Pattern.OPEN_TWO, ".X.X.", 2)
    };

    public List<PatternResult> recognize(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(player);

        Map<PatternKey, PatternResult> results = new LinkedHashMap<>();

        for (PatternResult.Direction direction : PatternResult.Direction.values()) {
            scanDirection(board, player, direction, results);
        }

        return List.copyOf(results.values());
    }

    private void scanDirection(
            Board board,
            byte player,
            PatternResult.Direction direction,
            Map<PatternKey, PatternResult> results
    ) {
        int dx = direction.getDx();
        int dy = direction.getDy();

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {

                int previousX = x - dx;
                int previousY = y - dy;

                // Only start from the beginning of a line.
                if (board.isInside(previousX, previousY)) {
                    continue;
                }

                scanLine(
                        board,
                        player,
                        direction,
                        x,
                        y,
                        dx,
                        dy,
                        results
                );
            }
        }
    }

    private void scanLine(
            Board board,
            byte player,
            PatternResult.Direction direction,
            int startX,
            int startY,
            int dx,
            int dy,
            Map<PatternKey, PatternResult> results
    ) {
        List<CellPosition> positions = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        int x = startX;
        int y = startY;

        while (board.isInside(x, y)) {
            positions.add(new CellPosition(x, y));
            line.append(toSymbol(board.getCell(x, y), player));

            x += dx;
            y += dy;
        }

        // Add board boundaries to prevent edge patterns
        // from being incorrectly treated as open patterns.
        String paddedLine = BLOCKED + line.toString() + BLOCKED;

        detectFive(
                paddedLine,
                positions,
                direction,
                results
        );

        detectTemplates(
                paddedLine,
                positions,
                direction,
                results
        );
    }

    private void detectFive(
            String line,
            List<CellPosition> positions,
            PatternResult.Direction direction,
            Map<PatternKey, PatternResult> results
    ) {
        int index = 0;

        while (index < line.length()) {

            if (line.charAt(index) != PLAYER) {
                index++;
                continue;
            }

            int runStart = index;

            while (index < line.length()
                    && line.charAt(index) == PLAYER) {
                index++;
            }

            int runLength = index - runStart;

            if (runLength >= 5) {
                addResult(
                        Pattern.FIVE,
                        positions,
                        runStart - 1,
                        index - 2,
                        runLength,
                        direction,
                        results
                );
            }
        }
    }

    private void detectTemplates(
            String line,
            List<CellPosition> positions,
            PatternResult.Direction direction,
            Map<PatternKey, PatternResult> results
    ) {
        for (PatternTemplate template : TEMPLATES) {

            String shape = template.shape();
            int windowLength = shape.length();

            for (int start = 0;
                 start <= line.length() - windowLength;
                 start++) {

                if (!matches(line, start, shape)) {
                    continue;
                }

                int firstStone = -1;
                int lastStone = -1;

                for (int i = 0; i < windowLength; i++) {
                    if (shape.charAt(i) == PLAYER) {
                        if (firstStone == -1) {
                            firstStone = i;
                        }

                        lastStone = i;
                    }
                }

                if (firstStone == -1) {
                    continue;
                }

                addResult(
                        template.pattern(),
                        positions,
                        start + firstStone - 1,
                        start + lastStone - 1,
                        template.stoneCount(),
                        direction,
                        results
                );
            }
        }
    }

    private boolean matches(
            String line,
            int start,
            String template
    ) {
        for (int i = 0; i < template.length(); i++) {

            char expected = template.charAt(i);
            char actual = line.charAt(start + i);

            if (expected == BLOCKED) {
                if (actual != BLOCKED && actual != OPPONENT) {
                    return false;
                }
            } else if (actual != expected) {
                return false;
            }
        }

        return true;
    }

    private void addResult(
            Pattern pattern,
            List<CellPosition> positions,
            int firstIndex,
            int lastIndex,
            int stoneCount,
            PatternResult.Direction direction,
            Map<PatternKey, PatternResult> results
    ) {
        if (firstIndex < 0
                || lastIndex >= positions.size()
                || firstIndex > lastIndex) {
            return;
        }

        CellPosition first = positions.get(firstIndex);
        CellPosition last = positions.get(lastIndex);

        PatternKey key = new PatternKey(
                pattern,
                direction,
                first.x(),
                first.y(),
                last.x(),
                last.y()
        );

        PatternResult result = new PatternResult(
                pattern,
                direction,
                first.x(),
                first.y(),
                last.x(),
                last.y(),
                stoneCount
        );

        results.putIfAbsent(key, result);
    }

    private char toSymbol(byte cell, byte player) {
        if (cell == Board.EMPTY) {
            return EMPTY;
        }

        if (cell == player) {
            return PLAYER;
        }

        return OPPONENT;
    }

    private void validatePlayer(byte player) {
        if (player != Board.BLACK && player != Board.WHITE) {
            throw new IllegalArgumentException(
                    "Invalid player: " + player
            );
        }
    }

    private record CellPosition(int x, int y) {
    }

    private record PatternTemplate(
            Pattern pattern,
            String shape,
            int stoneCount
    ) {
    }

    private record PatternKey(
            Pattern pattern,
            PatternResult.Direction direction,
            int startX,
            int startY,
            int endX,
            int endY
    ) {
    }
}
