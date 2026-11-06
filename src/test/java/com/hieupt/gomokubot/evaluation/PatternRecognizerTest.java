package com.hieupt.gomokubot.evaluation;

import com.hieupt.gomokubot.core.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PatternRecognizer Tests")
class PatternRecognizerTest {

    private PatternRecognizer recognizer;
    private Board board;

    @BeforeEach
    void setUp() {
        recognizer = new PatternRecognizer();
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Kiểm tra tham số đầu vào và bàn cờ rỗng")
    class ValidationTests {

        @Test
        @DisplayName("Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> recognizer.recognize(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }

        @ParameterizedTest(name = "Player không hợp lệ: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("Player không hợp lệ ném IllegalArgumentException")
        void testInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> recognizer.recognize(board, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player");
        }

        @Test
        @DisplayName("Bàn cờ hoàn toàn rỗng trả về danh sách kết quả rỗng")
        void testEmptyBoardReturnsEmptyList() {
            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Nhận diện đủ 7 loại pattern cơ bản")
    class AllSevenPatternsTests {

        @Test
        @DisplayName("1. Nhận diện FIVE (.XXXXX.)")
        void testRecognizeFive() {
            // Đặt 5 quân Đen tại hàng 7: x = 5, 6, 7, 8, 9
            for (int x = 5; x <= 9; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.FIVE)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.direction()).isEqualTo(PatternResult.Direction.HORIZONTAL);
                        assertThat(r.startX()).isEqualTo(5);
                        assertThat(r.startY()).isEqualTo(7);
                        assertThat(r.endX()).isEqualTo(9);
                        assertThat(r.endY()).isEqualTo(7);
                        assertThat(r.stoneCount()).isEqualTo(5);
                    });
        }

        @Test
        @DisplayName("2. Nhận diện OPEN_FOUR (.XXXX.)")
        void testRecognizeOpenFour() {
            // Đặt 4 quân Đen: x = 5, 6, 7, 8 (x=4 và x=9 để trống)
            for (int x = 5; x <= 8; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.OPEN_FOUR)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.direction()).isEqualTo(PatternResult.Direction.HORIZONTAL);
                        assertThat(r.startX()).isEqualTo(5);
                        assertThat(r.endX()).isEqualTo(8);
                        assertThat(r.stoneCount()).isEqualTo(4);
                    });
        }

        @Test
        @DisplayName("3. Nhận diện CLOSED_FOUR (#XXXX. hoặc .XXXX#)")
        void testRecognizeClosedFour() {
            // TH1: Chặn đầu bên trái bởi quân đối phương (OXXXX.)
            board.placeStone(4, 5, Board.WHITE); // Chặn bên trái
            for (int x = 5; x <= 8; x++) {
                board.placeStone(x, 5, Board.BLACK);
            }
            // Ô (9, 5) để trống

            // TH2: Chặn đầu bên phải bởi quân đối phương (.XXXXO)
            // Ô (4, 9) để trống
            for (int x = 5; x <= 8; x++) {
                board.placeStone(x, 9, Board.BLACK);
            }
            board.placeStone(9, 9, Board.WHITE); // Chặn bên phải

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            List<PatternResult> closedFours = results.stream()
                    .filter(r -> r.pattern() == Pattern.CLOSED_FOUR)
                    .toList();

            assertThat(closedFours).hasSize(2);
        }

        @Test
        @DisplayName("4. Nhận diện BROKEN_FOUR (.XX.XX., .X.XXX., .XXX.X.)")
        void testRecognizeBrokenFour() {
            // .XX.XX. tại y = 3 (x=3 để trống, x=4,5 Đen, x=6 trống, x=7,8 Đen, x=9 trống)
            board.placeStone(4, 3, Board.BLACK);
            board.placeStone(5, 3, Board.BLACK);
            board.placeStone(7, 3, Board.BLACK);
            board.placeStone(8, 3, Board.BLACK);

            // .X.XXX. tại y = 6 (x=3 trống, x=4 Đen, x=5 trống, x=6,7,8 Đen, x=9 trống)
            board.placeStone(4, 6, Board.BLACK);
            board.placeStone(6, 6, Board.BLACK);
            board.placeStone(7, 6, Board.BLACK);
            board.placeStone(8, 6, Board.BLACK);

            // .XXX.X. tại y = 9 (x=3 trống, x=4,5,6 Đen, x=7 trống, x=8 Đen, x=9 trống)
            board.placeStone(4, 9, Board.BLACK);
            board.placeStone(5, 9, Board.BLACK);
            board.placeStone(6, 9, Board.BLACK);
            board.placeStone(8, 9, Board.BLACK);

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            List<PatternResult> brokenFours = results.stream()
                    .filter(r -> r.pattern() == Pattern.BROKEN_FOUR)
                    .toList();

            assertThat(brokenFours).hasSize(3);
            assertThat(brokenFours).allSatisfy(r -> assertThat(r.stoneCount()).isEqualTo(4));
        }

        @Test
        @DisplayName("5. Nhận diện OPEN_THREE (.XXX.)")
        void testRecognizeOpenThree() {
            // Đặt 3 quân Đen tại hàng 7: x = 5, 6, 7 (x=4 và x=8 để trống)
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(7, 7, Board.BLACK);

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.OPEN_THREE)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.startX()).isEqualTo(5);
                        assertThat(r.endX()).isEqualTo(7);
                        assertThat(r.stoneCount()).isEqualTo(3);
                    });
        }

        @Test
        @DisplayName("6. Nhận diện BROKEN_THREE (.X.X.X., .XX.X., .X.XX.)")
        void testRecognizeBrokenThree() {
            // .X.X.X. tại y = 3
            board.placeStone(4, 3, Board.BLACK);
            board.placeStone(6, 3, Board.BLACK);
            board.placeStone(8, 3, Board.BLACK);

            // .XX.X. tại y = 6
            board.placeStone(4, 6, Board.BLACK);
            board.placeStone(5, 6, Board.BLACK);
            board.placeStone(7, 6, Board.BLACK);

            // .X.XX. tại y = 9
            board.placeStone(4, 9, Board.BLACK);
            board.placeStone(6, 9, Board.BLACK);
            board.placeStone(7, 9, Board.BLACK);

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            List<PatternResult> brokenThrees = results.stream()
                    .filter(r -> r.pattern() == Pattern.BROKEN_THREE)
                    .toList();

            assertThat(brokenThrees).hasSize(3);
            assertThat(brokenThrees).allSatisfy(r -> assertThat(r.stoneCount()).isEqualTo(3));
        }

        @Test
        @DisplayName("7. Nhận diện OPEN_TWO (.XX.)")
        void testRecognizeOpenTwo() {
            // Đặt 2 quân Đen tại hàng 7: x = 6, 7 (x=5 và x=8 để trống)
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(7, 7, Board.BLACK);

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.OPEN_TWO)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.startX()).isEqualTo(6);
                        assertThat(r.endX()).isEqualTo(7);
                        assertThat(r.stoneCount()).isEqualTo(2);
                    });
        }
    }

    @Nested
    @DisplayName("Kiểm tra đầy đủ 4 hướng quét")
    class DirectionTests {

        @Test
        @DisplayName("Hướng ngang (HORIZONTAL: dx=1, dy=0)")
        void testHorizontalDirection() {
            for (int x = 5; x <= 8; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).hasSize(1);
            PatternResult result = results.get(0);
            assertThat(result.direction()).isEqualTo(PatternResult.Direction.HORIZONTAL);
            assertThat(result.pattern()).isEqualTo(Pattern.OPEN_FOUR);
            assertThat(result.startX()).isEqualTo(5);
            assertThat(result.startY()).isEqualTo(7);
            assertThat(result.endX()).isEqualTo(8);
            assertThat(result.endY()).isEqualTo(7);
        }

        @Test
        @DisplayName("Hướng dọc (VERTICAL: dx=0, dy=1)")
        void testVerticalDirection() {
            for (int y = 5; y <= 8; y++) {
                board.placeStone(7, y, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).hasSize(1);
            PatternResult result = results.get(0);
            assertThat(result.direction()).isEqualTo(PatternResult.Direction.VERTICAL);
            assertThat(result.pattern()).isEqualTo(Pattern.OPEN_FOUR);
            assertThat(result.startX()).isEqualTo(7);
            assertThat(result.startY()).isEqualTo(5);
            assertThat(result.endX()).isEqualTo(7);
            assertThat(result.endY()).isEqualTo(8);
        }

        @Test
        @DisplayName("Hướng chéo xuống (DIAGONAL_DOWN: dx=1, dy=1)")
        void testDiagonalDownDirection() {
            for (int i = 0; i < 4; i++) {
                board.placeStone(4 + i, 4 + i, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).hasSize(1);
            PatternResult result = results.get(0);
            assertThat(result.direction()).isEqualTo(PatternResult.Direction.DIAGONAL_DOWN);
            assertThat(result.pattern()).isEqualTo(Pattern.OPEN_FOUR);
            assertThat(result.startX()).isEqualTo(4);
            assertThat(result.startY()).isEqualTo(4);
            assertThat(result.endX()).isEqualTo(7);
            assertThat(result.endY()).isEqualTo(7);
        }

        @Test
        @DisplayName("Hướng chéo lên (DIAGONAL_UP: dx=1, dy=-1)")
        void testDiagonalUpDirection() {
            // (4, 10), (5, 9), (6, 8), (7, 7)
            for (int i = 0; i < 4; i++) {
                board.placeStone(4 + i, 10 - i, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).hasSize(1);
            PatternResult result = results.get(0);
            assertThat(result.direction()).isEqualTo(PatternResult.Direction.DIAGONAL_UP);
            assertThat(result.pattern()).isEqualTo(Pattern.OPEN_FOUR);
            assertThat(result.startX()).isEqualTo(4);
            assertThat(result.startY()).isEqualTo(10);
            assertThat(result.endX()).isEqualTo(7);
            assertThat(result.endY()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Phân biệt quân Đen và quân Trắng")
    class PlayerDistinctionTests {

        @Test
        @DisplayName("Chỉ nhận diện pattern của người chơi được yêu cầu")
        void testDistinguishBetweenBlackAndWhite() {
            // Đen có OPEN_FOUR hàng ngang
            for (int x = 4; x <= 7; x++) {
                board.placeStone(x, 2, Board.BLACK);
            }

            // Trắng có OPEN_THREE hàng dọc
            for (int y = 5; y <= 7; y++) {
                board.placeStone(10, y, Board.WHITE);
            }

            // Quét quân Đen
            List<PatternResult> blackResults = recognizer.recognize(board, Board.BLACK);
            assertThat(blackResults).hasSize(1);
            assertThat(blackResults.get(0).pattern()).isEqualTo(Pattern.OPEN_FOUR);

            // Quét quân Trắng
            List<PatternResult> whiteResults = recognizer.recognize(board, Board.WHITE);
            assertThat(whiteResults).hasSize(1);
            assertThat(whiteResults.get(0).pattern()).isEqualTo(Pattern.OPEN_THREE);
        }

        @Test
        @DisplayName("Quân đối thủ đóng vai trò chặn (OPPONENT/BLOCKED)")
        void testOpponentStoneActsAsBlocker() {
            // Đen: x=5,6,7 (3 quân). Nếu không bị chặn là OPEN_THREE (.XXX.)
            // Đặt Trắng tại (4, 7) để chặn đầu trái: OXXX.
            board.placeStone(4, 7, Board.WHITE);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(7, 7, Board.BLACK);
            // Ô (8, 7) để trống

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            // Không còn là OPEN_THREE vì bị Trắng chặn đầu
            assertThat(results).noneMatch(r -> r.pattern() == Pattern.OPEN_THREE);
        }
    }

    @Nested
    @DisplayName("Pattern sát biên bàn cờ (Board boundary)")
    class BoundaryTests {

        @Test
        @DisplayName("4 quân sát biên tạo thành CLOSED_FOUR (#XXXX.)")
        void testFourStonesAgainstBorderIsClosedFour() {
            // Đặt 4 quân Đen từ x=0 đến x=3 (x=0 là mép bàn cờ)
            for (int x = 0; x < 4; x++) {
                board.placeStone(x, 5, Board.BLACK);
            }
            // Ô (4, 5) để trống

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.CLOSED_FOUR)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.startX()).isZero();
                        assertThat(r.endX()).isEqualTo(3);
                    });

            // Không được nhận diện là OPEN_FOUR vì mép bàn cờ đóng vai trò là '#'
            assertThat(results).noneMatch(r -> r.pattern() == Pattern.OPEN_FOUR);
        }

        @Test
        @DisplayName("4 quân sát biên phải tạo thành CLOSED_FOUR (.XXXX#)")
        void testFourStonesAgainstRightBorderIsClosedFour() {
            // Đặt 4 quân Đen từ x=11 đến x=14 (x=14 là mép phải)
            for (int x = 11; x <= 14; x++) {
                board.placeStone(x, 5, Board.BLACK);
            }
            // Ô (10, 5) để trống

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.CLOSED_FOUR)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.startX()).isEqualTo(11);
                        assertThat(r.endX()).isEqualTo(14);
                    });
        }

        @Test
        @DisplayName("3 quân sát biên không được tính là OPEN_THREE")
        void testThreeStonesAgainstBorderNotOpenThree() {
            // Đặt 3 quân Đen từ x=0 đến x=2 tại mép trái (#XXX.)
            for (int x = 0; x < 3; x++) {
                board.placeStone(x, 5, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).noneMatch(r -> r.pattern() == Pattern.OPEN_THREE);
        }

        @Test
        @DisplayName("5 quân sát mép bàn cờ vẫn nhận diện đúng FIVE")
        void testFiveStonesAgainstBorderRecognizedAsFive() {
            for (int x = 0; x < 5; x++) {
                board.placeStone(x, 0, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.FIVE)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.startX()).isZero();
                        assertThat(r.endX()).isEqualTo(4);
                    });
        }

        @Test
        @DisplayName("Đường chéo bắt đầu từ góc (0, 0)")
        void testCornerDiagonalPattern() {
            for (int i = 0; i < 5; i++) {
                board.placeStone(i, i, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.FIVE)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.direction()).isEqualTo(PatternResult.Direction.DIAGONAL_DOWN);
                        assertThat(r.startX()).isZero();
                        assertThat(r.startY()).isZero();
                        assertThat(r.endX()).isEqualTo(4);
                        assertThat(r.endY()).isEqualTo(4);
                    });
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật")
    class RectangularBoardTests {

        @Test
        @DisplayName("Nhận diện chính xác trên bàn cờ chữ nhật 10x20")
        void testPatternsOn10x20Board() {
            Board rectBoard = new Board(10, 20);

            // Ngang tại y=18: x=2,3,4,5 (OPEN_FOUR)
            for (int x = 2; x <= 5; x++) {
                rectBoard.placeStone(x, 18, Board.BLACK);
            }

            // Dọc tại x=7: y=12,13,14 (OPEN_THREE)
            for (int y = 12; y <= 14; y++) {
                rectBoard.placeStone(7, y, Board.BLACK);
            }

            // Chéo tại x=1..5, y=5..9 (FIVE)
            for (int i = 0; i < 5; i++) {
                rectBoard.placeStone(1 + i, 5 + i, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(rectBoard, Board.BLACK);

            assertThat(results).hasSize(3);
            assertThat(results).anyMatch(r -> r.pattern() == Pattern.OPEN_FOUR
                    && r.direction() == PatternResult.Direction.HORIZONTAL);
            assertThat(results).anyMatch(r -> r.pattern() == Pattern.OPEN_THREE
                    && r.direction() == PatternResult.Direction.VERTICAL);
            assertThat(results).anyMatch(r -> r.pattern() == Pattern.FIVE
                    && r.direction() == PatternResult.Direction.DIAGONAL_DOWN);
        }

        @Test
        @DisplayName("Nhận diện chính xác trên bàn cờ chữ nhật 20x10")
        void testPatternsOn20x10Board() {
            Board rectBoard = new Board(20, 10);

            // Ngang dài ở x=12..16 (FIVE)
            for (int x = 12; x <= 16; x++) {
                rectBoard.placeStone(x, 4, Board.WHITE);
            }

            List<PatternResult> results = recognizer.recognize(rectBoard, Board.WHITE);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).pattern()).isEqualTo(Pattern.FIVE);
            assertThat(results.get(0).direction()).isEqualTo(PatternResult.Direction.HORIZONTAL);
        }
    }

    @Nested
    @DisplayName("Quy tắc Overline (6 hoặc nhiều hơn quân liên tiếp)")
    class OverlineTests {

        @Test
        @DisplayName("6 quân liên tiếp được biểu diễn bằng 1 kết quả FIVE duy nhất với stoneCount = 6")
        void testSixStonesOverlineIsSingleFive() {
            // 6 quân liên tiếp từ x=3 đến x=8
            for (int x = 3; x <= 8; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.FIVE)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.startX()).isEqualTo(3);
                        assertThat(r.endX()).isEqualTo(8);
                        assertThat(r.stoneCount()).isEqualTo(6);
                    });
        }

        @Test
        @DisplayName("7 quân liên tiếp được biểu diễn bằng 1 kết quả FIVE với stoneCount = 7")
        void testSevenStonesOverlineIsSingleFive() {
            // 7 quân liên tiếp từ x=2 đến x=8
            for (int x = 2; x <= 8; x++) {
                board.placeStone(x, 7, Board.WHITE);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.WHITE);

            assertThat(results)
                    .filteredOn(r -> r.pattern() == Pattern.FIVE)
                    .hasSize(1)
                    .first()
                    .satisfies(r -> {
                        assertThat(r.startX()).isEqualTo(2);
                        assertThat(r.endX()).isEqualTo(8);
                        assertThat(r.stoneCount()).isEqualTo(7);
                    });
        }
    }

    @Nested
    @DisplayName("Pattern bị đối thủ chặn (Blocked Patterns)")
    class BlockedPatternTests {

        @Test
        @DisplayName("4 quân bị chặn 2 đầu bởi quân đối phương (OXXXXO) không tạo thành bất kỳ Four nào")
        void testFourStonesBlockedOnBothEndsByOpponent() {
            board.placeStone(3, 7, Board.WHITE); // Chặn trái
            for (int x = 4; x <= 7; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }
            board.placeStone(8, 7, Board.WHITE); // Chặn phải

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).noneMatch(r -> r.pattern() == Pattern.OPEN_FOUR);
            assertThat(results).noneMatch(r -> r.pattern() == Pattern.CLOSED_FOUR);
        }

        @Test
        @DisplayName("4 quân bị chặn 1 đầu bởi mép bàn cờ và 1 đầu bởi đối phương (#XXXXO) không tạo thành Four")
        void testFourStonesBlockedByBorderAndOpponent() {
            for (int x = 0; x < 4; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }
            board.placeStone(4, 7, Board.WHITE); // Chặn đầu phải

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).noneMatch(r -> r.pattern() == Pattern.CLOSED_FOUR);
            assertThat(results).noneMatch(r -> r.pattern() == Pattern.OPEN_FOUR);
        }

        @Test
        @DisplayName("Chuỗi bị đối thủ xen vào giữa (XXOXX) không tạo thành Four")
        void testChainSplitInMiddleByOpponent() {
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.WHITE); // Xen giữa
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(7, 7, Board.BLACK);

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThat(results).noneMatch(r -> r.pattern() == Pattern.OPEN_FOUR
                    || r.pattern() == Pattern.CLOSED_FOUR
                    || r.pattern() == Pattern.BROKEN_FOUR);
        }
    }

    @Nested
    @DisplayName("Không trả về kết quả trùng lặp")
    class DuplicatePreventionTests {

        @Test
        @DisplayName("Danh sách kết quả không có phần tử nào bị trùng lặp")
        void testNoDuplicateResults() {
            // Đặt nhiều pattern khác nhau trên bàn cờ
            // 1. OPEN_FOUR ngang tại (4..7, 3)
            for (int x = 4; x <= 7; x++) {
                board.placeStone(x, 3, Board.BLACK);
            }

            // 2. OPEN_THREE dọc tại (10, 5..7)
            for (int y = 5; y <= 7; y++) {
                board.placeStone(10, y, Board.BLACK);
            }

            // 3. FIVE chéo tại (1..5, 1..5)
            for (int i = 0; i < 5; i++) {
                board.placeStone(1 + i, 1 + i, Board.BLACK);
            }

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            // Kiểm tra tính duy nhất (mọi phần tử trong List là độc nhất)
            HashSet<PatternResult> uniqueResults = new HashSet<>(results);
            assertThat(results).hasSameSizeAs(uniqueResults);
        }

        @Test
        @DisplayName("Danh sách trả về là bất biến (Immutable)")
        void testResultsListIsImmutable() {
            board.placeStone(5, 5, Board.BLACK);
            board.placeStone(6, 5, Board.BLACK);

            List<PatternResult> results = recognizer.recognize(board, Board.BLACK);

            assertThatThrownBy(() -> results.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
