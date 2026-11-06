package com.hieupt.gomokubot.evaluation;

import com.hieupt.gomokubot.core.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Evaluator Tests")
class EvaluatorTest {

    private Evaluator evaluator;
    private Board board;

    @BeforeEach
    void setUp() {
        evaluator = new Evaluator();
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Kiểm tra tham số và xử lý ngoại lệ")
    class ValidationTests {

        @Test
        @DisplayName("Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> evaluator.evaluate(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");

            assertThatThrownBy(() -> evaluator.evaluatePlayer(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }

        @ParameterizedTest(name = "Player không hợp lệ: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("Player không hợp lệ ném IllegalArgumentException")
        void testInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> evaluator.evaluate(board, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player");

            assertThatThrownBy(() -> evaluator.evaluatePlayer(board, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player");
        }

        @Test
        @DisplayName("Khởi tạo Evaluator với PatternRecognizer null ném NullPointerException")
        void testNullPatternRecognizerThrowsException() {
            assertThatThrownBy(() -> new Evaluator(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("PatternRecognizer must not be null");
        }
    }

    @Nested
    @DisplayName("Đánh giá thế cờ cân bằng và bàn cờ trống")
    class BalanceEvaluationTests {

        @Test
        @DisplayName("Bàn cờ trống trả về điểm 0")
        void testEmptyBoardEvaluatesToZero() {
            int scoreFromBlack = evaluator.evaluate(board, Board.BLACK);
            int scoreFromWhite = evaluator.evaluate(board, Board.WHITE);

            assertThat(scoreFromBlack).isZero();
            assertThat(scoreFromWhite).isZero();
            assertThat(evaluator.evaluatePlayer(board, Board.BLACK)).isZero();
            assertThat(evaluator.evaluatePlayer(board, Board.WHITE)).isZero();
        }

        @Test
        @DisplayName("Hai bên có thế trận giống nhau trả về điểm 0")
        void testSymmetricPositionsEvaluateToZero() {
            // Đen có 1 OPEN_THREE tại hàng 3: (5, 3), (6, 3), (7, 3)
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 3, Board.BLACK);
            }

            // Trắng có 1 OPEN_THREE tại hàng 11: (5, 11), (6, 11), (7, 11)
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 11, Board.WHITE);
            }

            int scoreFromBlack = evaluator.evaluate(board, Board.BLACK);
            int scoreFromWhite = evaluator.evaluate(board, Board.WHITE);

            assertThat(scoreFromBlack).isZero();
            assertThat(scoreFromWhite).isZero();
            assertThat(evaluator.evaluatePlayer(board, Board.BLACK))
                    .isEqualTo(evaluator.evaluatePlayer(board, Board.WHITE));
        }
    }

    @Nested
    @DisplayName("Đánh giá ưu thế (Open Three)")
    class OpenThreeEvaluationTests {

        @Test
        @DisplayName("Black có Open Three -> Điểm dương khi nhìn từ Black")
        void testBlackHasOpenThreeScoresPositive() {
            // Đặt OPEN_THREE cho Đen: .XXX. tại y=7, x=5..7
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }

            int score = evaluator.evaluate(board, Board.BLACK);

            assertThat(score).isPositive();
            assertThat(score).isEqualTo(10_000); // Trọng số của OPEN_THREE
        }

        @Test
        @DisplayName("White có Open Three -> Điểm âm khi nhìn từ Black")
        void testWhiteHasOpenThreeScoresNegativeFromBlackPerspective() {
            // Đặt OPEN_THREE cho Trắng: .OOO. tại y=7, x=5..7
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 7, Board.WHITE);
            }

            int score = evaluator.evaluate(board, Board.BLACK);

            assertThat(score).isNegative();
            assertThat(score).isEqualTo(-10_000);
        }
    }

    @Nested
    @DisplayName("Đánh giá thế cờ chiến thắng (Five in a row)")
    class FiveEvaluationTests {

        @Test
        @DisplayName("Black có Five -> +WIN_SCORE khi nhìn từ Black")
        void testBlackHasFiveScoresPositiveWinScore() {
            // Đặt 5 quân Đen: x=5..9, y=7
            for (int x = 5; x <= 9; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }

            int score = evaluator.evaluate(board, Board.BLACK);

            assertThat(score).isEqualTo(Evaluator.WIN_SCORE);
            assertThat(evaluator.evaluatePlayer(board, Board.BLACK))
                    .isEqualTo(Evaluator.WIN_SCORE);
        }

        @Test
        @DisplayName("White có Five -> -WIN_SCORE khi nhìn từ Black")
        void testWhiteHasFiveScoresNegativeWinScore() {
            // Đặt 5 quân Trắng: x=5..9, y=7
            for (int x = 5; x <= 9; x++) {
                board.placeStone(x, 7, Board.WHITE);
            }

            int score = evaluator.evaluate(board, Board.BLACK);

            assertThat(score).isEqualTo(-Evaluator.WIN_SCORE);
            assertThat(evaluator.evaluatePlayer(board, Board.WHITE))
                    .isEqualTo(Evaluator.WIN_SCORE);
        }
    }

    @Nested
    @DisplayName("Đổi góc nhìn (Perspective Symmetry)")
    class PerspectiveChangeTests {

        @Test
        @DisplayName("Đổi góc nhìn từ Black sang White phải đảo dấu điểm số")
        void testPerspectiveInversion() {
            // Đen có OPEN_FOUR (100,000)
            for (int x = 4; x <= 7; x++) {
                board.placeStone(x, 4, Board.BLACK);
            }

            // Trắng có OPEN_THREE (10,000)
            for (int x = 4; x <= 6; x++) {
                board.placeStone(x, 10, Board.WHITE);
            }

            int scoreFromBlack = evaluator.evaluate(board, Board.BLACK);
            int scoreFromWhite = evaluator.evaluate(board, Board.WHITE);

            // Điểm nhìn từ Đen: 100,000 - 10,000 = +90,000
            // Điểm nhìn từ Trắng: 10,000 - 100,000 = -90,000
            assertThat(scoreFromBlack).isEqualTo(-scoreFromWhite);
            assertThat(scoreFromBlack).isEqualTo(90_000);
            assertThat(scoreFromWhite).isEqualTo(-90_000);
        }

        @Test
        @DisplayName("Đổi góc nhìn khi một bên chiến thắng vẫn đảm bảo đảo dấu")
        void testPerspectiveInversionOnWin() {
            for (int x = 2; x <= 6; x++) {
                board.placeStone(x, 2, Board.BLACK);
            }

            int blackScore = evaluator.evaluate(board, Board.BLACK);
            int whiteScore = evaluator.evaluate(board, Board.WHITE);

            assertThat(blackScore).isEqualTo(Evaluator.WIN_SCORE);
            assertThat(whiteScore).isEqualTo(-Evaluator.WIN_SCORE);
            assertThat(blackScore).isEqualTo(-whiteScore);
        }
    }

    @Nested
    @DisplayName("Quy tắc suy giảm điểm khi có nhiều pattern cùng loại (Diminishing Score)")
    class DiminishingReturnsTests {

        @Test
        @DisplayName("2 OPEN_THREE cho 1 người chơi áp dụng quy tắc suy giảm: 10,000 + 5,000 = 15,000 (khi tắt combo)")
        void testMultiplePatternsDiminishingContribution() {
            // OPEN_THREE thứ nhất: hàng 3
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 3, Board.BLACK);
            }

            // OPEN_THREE thứ hai: hàng 9
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 9, Board.BLACK);
            }

            Evaluator pureEvaluator = new Evaluator(false);
            long playerScore = pureEvaluator.evaluatePlayer(board, Board.BLACK);

            // 10,000 (đầu tiên) + 10,000/2 (thứ hai) = 15,000
            assertThat(playerScore).isEqualTo(15_000);
        }

        @Test
        @DisplayName("2 OPEN_THREE khi bật combo nhận thưởng đòn chẽ Double Three: 15,000 + 60,000 = 75,000")
        void testDoubleThreeComboBonus() {
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 3, Board.BLACK);
            }
            for (int x = 5; x <= 7; x++) {
                board.placeStone(x, 9, Board.BLACK);
            }

            long playerScore = evaluator.evaluatePlayer(board, Board.BLACK);

            assertThat(playerScore).isEqualTo(75_000);
        }

        @Test
        @DisplayName("Điểm của người chơi không bao giờ vượt quá WIN_SCORE - 1 nếu chưa có FIVE")
        void testNonWinningScoreCappedBelowWinScore() {
            // Nhiều OPEN_FOUR nhưng chưa có FIVE
            for (int y = 2; y <= 12; y += 2) {
                for (int x = 4; x <= 7; x++) {
                    board.placeStone(x, y, Board.BLACK);
                }
            }

            long playerScore = evaluator.evaluatePlayer(board, Board.BLACK);
            assertThat(playerScore).isLessThan(Evaluator.WIN_SCORE);
        }
    }
}

