package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;

import java.util.Objects;

/**
 * Bộ phát hiện bẫy không gian và đòn chẽ (Spatial Trap & Fork Detector).
 * <p>
 * Thực hiện quét tia 4 hướng theo cửa sổ trượt 9 ô O(1) quanh một ô cờ:
 * - Nhận diện chính xác 100% các biến thể nhảy cóc / ngắt quãng:
 *   + Nước thắng ngay (FIVE: XXXXX)
 *   + Bốn mở hai đầu (OPEN_FOUR: .XXXX.)
 *   + Bốn đóng / Bốn ngắt (CLOSED_FOUR / BROKEN_FOUR: XXX.X, X.XXX, XX.XX, #XXXX., .XXXX#)
 *   + Ba mở / Ba nhảy cóc (OPEN_THREE / BROKEN_THREE: .XXX.., ..XXX., .XX.X., .X.XX., .X.X.X.)
 *   + Ba đóng / Ba ngắt (CLOSED_THREE: #XX.X., .XX.X#, #X.XX., #XXX..)
 *   + Hai mở / Hai nhảy cóc (OPEN_TWO: ..XX.., .X.X.., ..X.X.)
 *   + Đòn chẽ 4-4, 4-3, 3-3 (Double Four, Four-Three, Double Three)
 *   + Mầm bẫy 3-2, 2-2
 */
public class SpatialTrapDetector {

    public static final int SCORE_FIVE = 1_000_000;
    public static final int SCORE_OPEN_FOUR = 400_000;
    public static final int SCORE_DOUBLE_FOUR = 300_000;
    public static final int SCORE_FOUR_THREE = 250_000;
    public static final int SCORE_DOUBLE_THREE = 200_000;
    public static final int SCORE_CLOSED_FOUR = 35_000;
    public static final int SCORE_TRAP_3_2 = 25_000;
    public static final int SCORE_OPEN_THREE = 15_000;
    public static final int SCORE_TRAP_2_2 = 10_000;
    public static final int SCORE_OPEN_TWO = 2_000;

    private static final int[][] DIRECTIONS = {
            {1, 0},   // Ngang (Horizontal)
            {0, 1},   // Dọc (Vertical)
            {1, 1},   // Chéo xuống (Diagonal Down)
            {1, -1}   // Chéo lên (Diagonal Up)
    };

    public record MoveAnalysis(
            int winCount,
            int openFourCount,
            int closedFourCount,
            int openThreeCount,
            int closedThreeCount,
            int openTwoCount,
            int tacticalScore
    ) {
        public boolean isWin() {
            return winCount > 0;
        }

        public boolean isOpenFour() {
            return openFourCount > 0;
        }

        public boolean isDoubleFour() {
            return (openFourCount + closedFourCount) >= 2;
        }

        public boolean isFourThree() {
            return (openFourCount + closedFourCount) >= 1 && openThreeCount >= 1;
        }

        public boolean isDoubleThree() {
            return openThreeCount >= 2;
        }

        public boolean isFork() {
            return isDoubleFour() || isFourThree() || isDoubleThree();
        }

        public boolean isSpatialTrap() {
            return (openThreeCount >= 1 && openTwoCount >= 1)
                    || openTwoCount >= 2
                    || (closedFourCount >= 1 && openTwoCount >= 1)
                    || (openThreeCount >= 1 && closedThreeCount >= 1);
        }
    }

    public MoveAnalysis analyzeMove(Board board, int x, int y, byte player) {
        Objects.requireNonNull(board, "Board must not be null");
        if (!board.isInside(x, y)) {
            throw new IndexOutOfBoundsException("Position outside board: (" + x + ", " + y + ")");
        }
        if (!board.isEmpty(x, y)) {
            return new MoveAnalysis(0, 0, 0, 0, 0, 0, 0);
        }

        board.placeStone(x, y, player);

        int winCount = 0;
        int openFourCount = 0;
        int closedFourCount = 0;
        int openThreeCount = 0;
        int closedThreeCount = 0;
        int openTwoCount = 0;

        try {
            for (int[] dir : DIRECTIONS) {
                int dx = dir[0];
                int dy = dir[1];

                // Trích xuất mảng 9 ô với ô (x, y) nằm tại tâm (chỉ số 4)
                char[] line = new char[9];
                for (int k = -4; k <= 4; k++) {
                    int cx = x + k * dx;
                    int cy = y + k * dy;
                    if (!board.isInside(cx, cy)) {
                        line[k + 4] = '#';
                    } else {
                        byte cell = board.getCell(cx, cy);
                        if (cell == player) {
                            line[k + 4] = 'X';
                        } else if (cell == Board.EMPTY) {
                            line[k + 4] = '.';
                        } else {
                            line[k + 4] = '#';
                        }
                    }
                }

                // 1. Kiểm tra FIVE (chiến thắng ngay lập tức)
                boolean hasWin = false;
                for (int w = 0; w <= 4; w++) {
                    if (line[w] == 'X' && line[w + 1] == 'X' && line[w + 2] == 'X' && line[w + 3] == 'X' && line[w + 4] == 'X') {
                        winCount++;
                        hasWin = true;
                        break;
                    }
                }
                if (hasWin) {
                    continue;
                }

                // 2. Kiểm tra OPEN_FOUR (.XXXX.)
                boolean hasOpenFour = false;
                for (int w = 0; w <= 3; w++) {
                    if (line[w] == '.' && line[w + 1] == 'X' && line[w + 2] == 'X' && line[w + 3] == 'X' && line[w + 4] == 'X' && line[w + 5] == '.') {
                        openFourCount++;
                        hasOpenFour = true;
                        break;
                    }
                }

                // 3. Kiểm tra CLOSED_FOUR / BROKEN_FOUR (XXX.X, X.XXX, XX.XX, #XXXX., .XXXX#)
                boolean hasFour = hasOpenFour;
                if (!hasOpenFour) {
                    for (int w = 0; w <= 4; w++) {
                        int countX = 0;
                        int countDot = 0;
                        for (int i = 0; i < 5; i++) {
                            if (line[w + i] == 'X') countX++;
                            else if (line[w + i] == '.') countDot++;
                        }
                        if (countX == 4 && countDot == 1) {
                            closedFourCount++;
                            hasFour = true;
                            break;
                        }
                    }
                }
                if (hasFour) {
                    continue;
                }

                // 4. Kiểm tra OPEN_THREE (Ba mở liên tiếp hoặc nhảy cóc: .XXX.., ..XXX., .XX.X., .X.XX.)
                boolean hasThree = false;
                for (int w = 0; w <= 3; w++) {
                    if (line[w] == '.' && line[w + 5] == '.') {
                        int countX = 0;
                        int countDot = 0;
                        for (int i = 1; i <= 4; i++) {
                            if (line[w + i] == 'X') countX++;
                            else if (line[w + i] == '.') countDot++;
                        }
                        if (countX == 3 && countDot == 1) {
                            openThreeCount++;
                            hasThree = true;
                            break;
                        }
                    }
                }

                // 4b. Kiểm tra Double Jump Three (.X.X.X.)
                if (!hasThree) {
                    for (int w = 0; w <= 2; w++) {
                        if (line[w] == '.' && line[w + 1] == 'X' && line[w + 2] == '.' && line[w + 3] == 'X' && line[w + 4] == '.' && line[w + 5] == 'X' && line[w + 6] == '.') {
                            openThreeCount++;
                            hasThree = true;
                            break;
                        }
                    }
                }

                // 5. Kiểm tra CLOSED_THREE (Ba đóng / Ba ngắt: #XXX.., #XX.X., .XX.X#, #X.XX.)
                if (!hasThree) {
                    for (int w = 0; w <= 4; w++) {
                        int countX = 0;
                        int countDot = 0;
                        for (int i = 0; i < 5; i++) {
                            if (line[w + i] == 'X') countX++;
                            else if (line[w + i] == '.') countDot++;
                        }
                        if (countX == 3 && countDot == 2) {
                            closedThreeCount++;
                            hasThree = true;
                            break;
                        }
                    }
                }
                if (hasThree) {
                    continue;
                }

                // 6. Kiểm tra OPEN_TWO (Hai mở liên tiếp hoặc nhảy cóc: ..XX.., .X.X.., ..X.X.)
                for (int w = 0; w <= 3; w++) {
                    if (line[w] == '.' && line[w + 5] == '.') {
                        int countX = 0;
                        int countDot = 0;
                        for (int i = 1; i <= 4; i++) {
                            if (line[w + i] == 'X') countX++;
                            else if (line[w + i] == '.') countDot++;
                        }
                        if (countX == 2 && countDot == 2) {
                            openTwoCount++;
                            break;
                        }
                    }
                }
            }
        } finally {
            board.removeStone(x, y);
        }

        int score = computeTacticalScore(winCount, openFourCount, closedFourCount, openThreeCount, closedThreeCount, openTwoCount);
        return new MoveAnalysis(winCount, openFourCount, closedFourCount, openThreeCount, closedThreeCount, openTwoCount, score);
    }

    public int evaluateMoveTacticalScore(Board board, int x, int y, byte player) {
        return analyzeMove(board, x, y, player).tacticalScore();
    }

    private static int computeTacticalScore(
            int winCount,
            int openFourCount,
            int closedFourCount,
            int openThreeCount,
            int closedThreeCount,
            int openTwoCount
    ) {
        if (winCount > 0) {
            return SCORE_FIVE;
        }
        if (openFourCount > 0) {
            return SCORE_OPEN_FOUR;
        }

        int totalFours = openFourCount + closedFourCount;
        if (totalFours >= 2) {
            return SCORE_DOUBLE_FOUR;
        }
        if (totalFours >= 1 && openThreeCount >= 1) {
            return SCORE_FOUR_THREE;
        }
        if (openThreeCount >= 2) {
            return SCORE_DOUBLE_THREE;
        }
        if (totalFours >= 1) {
            return SCORE_CLOSED_FOUR;
        }
        if (openThreeCount >= 1 && openTwoCount >= 1) {
            return SCORE_TRAP_3_2;
        }
        if (openThreeCount >= 1) {
            return SCORE_OPEN_THREE;
        }
        if (openTwoCount >= 2) {
            return SCORE_TRAP_2_2;
        }
        if (closedThreeCount >= 1) {
            return 5_000;
        }
        if (openTwoCount >= 1) {
            return SCORE_OPEN_TWO;
        }
        return 0;
    }
}
