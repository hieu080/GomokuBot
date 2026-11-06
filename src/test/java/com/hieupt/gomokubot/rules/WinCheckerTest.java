package com.hieupt.gomokubot.rules;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WinChecker Tests")
class WinCheckerTest {

    private WinChecker winChecker;
    private Board board;

    @BeforeEach
    void setUp() {
        winChecker = new WinChecker(5);
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số đầu vào")
    class InitializationAndValidationTests {

        @Test
        @DisplayName("Khởi tạo WinChecker thành công với winLength hợp lệ")
        void testValidInitialization() {
            WinChecker custom = new WinChecker(5);
            assertThat(custom.getWinLength()).isEqualTo(5);
        }

        @ParameterizedTest(name = "winLength không hợp lệ: {0}")
        @ValueSource(ints = {0, -1, -5})
        @DisplayName("Không cho phép khởi tạo winLength <= 0")
        void testInvalidWinLengthThrowsException(int invalidLength) {
            assertThatThrownBy(() -> new WinChecker(invalidLength))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> winChecker.isWinningMove(null, new Move(0, 0)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }

        @Test
        @DisplayName("Move null ném NullPointerException")
        void testNullMoveThrowsException() {
            assertThatThrownBy(() -> winChecker.isWinningMove(board, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Move must not be null");
        }

        @Test
        @DisplayName("Move nằm ngoài bàn cờ ném IndexOutOfBoundsException")
        void testMoveOutsideBoardThrowsException() {
            assertThatThrownBy(() -> winChecker.isWinningMove(board, new Move(-1, 0)))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> winChecker.isWinningMove(board, new Move(15, 15)))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        @DisplayName("Kiểm tra nước đi tại ô chưa có quân (EMPTY) ném IllegalStateException")
        void testMoveOnEmptyCellThrowsException() {
            assertThatThrownBy(() -> winChecker.isWinningMove(board, new Move(7, 7)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No stone exists");
        }
    }

    @Nested
    @DisplayName("Kiểm tra 4 hướng thắng cơ bản (5 quân liên tiếp)")
    class DirectionTests {

        @Test
        @DisplayName("Thắng theo hàng ngang (Horizontal: dx=1, dy=0)")
        void testHorizontalWin() {
            // Đặt 5 quân Đen từ (3, 5) đến (7, 5)
            for (int x = 3; x <= 7; x++) {
                board.placeStone(x, 5, Board.BLACK);
            }

            // Đánh ở cuối chuỗi
            assertThat(winChecker.isWinningMove(board, new Move(7, 5))).isTrue();
            // Đánh ở đầu chuỗi
            assertThat(winChecker.isWinningMove(board, new Move(3, 5))).isTrue();
            // Đánh ở giữa chuỗi
            assertThat(winChecker.isWinningMove(board, new Move(5, 5))).isTrue();
        }

        @Test
        @DisplayName("Thắng theo hàng dọc (Vertical: dx=0, dy=1)")
        void testVerticalWin() {
            // Đặt 5 quân Trắng từ (5, 3) đến (5, 7)
            for (int y = 3; y <= 7; y++) {
                board.placeStone(5, y, Board.WHITE);
            }

            assertThat(winChecker.isWinningMove(board, new Move(5, 7))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(5, 3))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(5, 5))).isTrue();
        }

        @Test
        @DisplayName("Thắng theo đường chéo chính (Diagonal \\: dx=1, dy=1)")
        void testMainDiagonalWin() {
            // Đặt 5 quân Đen: (2, 2), (3, 3), (4, 4), (5, 5), (6, 6)
            for (int i = 0; i < 5; i++) {
                board.placeStone(2 + i, 2 + i, Board.BLACK);
            }

            assertThat(winChecker.isWinningMove(board, new Move(6, 6))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(2, 2))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(4, 4))).isTrue();
        }

        @Test
        @DisplayName("Thắng theo đường chéo phụ (Anti-Diagonal /: dx=1, dy=-1)")
        void testAntiDiagonalWin() {
            // Đặt 5 quân Đen: (2, 6), (3, 5), (4, 4), (5, 3), (6, 2)
            for (int i = 0; i < 5; i++) {
                board.placeStone(2 + i, 6 - i, Board.BLACK);
            }

            assertThat(winChecker.isWinningMove(board, new Move(6, 2))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(2, 6))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(4, 4))).isTrue();
        }
    }

    @Nested
    @DisplayName("Kiểm tra điều kiện số lượng quân (Overline, 4 quân)")
    class LengthConditionTests {

        @Test
        @DisplayName("Có đúng 5 quân liên tiếp là thắng")
        void testExactlyFiveStonesWin() {
            for (int x = 2; x <= 6; x++) {
                board.placeStone(x, 4, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(6, 4))).isTrue();
        }

        @Test
        @DisplayName("Có 6 quân liên tiếp vẫn thắng (Overline rule: count >= winLength)")
        void testSixStonesOverlineWins() {
            for (int x = 2; x <= 7; x++) {
                board.placeStone(x, 4, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(7, 4))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(4, 4))).isTrue();
        }

        @Test
        @DisplayName("Có 7 quân liên tiếp vẫn thắng")
        void testSevenStonesOverlineWins() {
            for (int x = 2; x <= 8; x++) {
                board.placeStone(x, 4, Board.WHITE);
            }
            assertThat(winChecker.isWinningMove(board, new Move(8, 4))).isTrue();
        }

        @Test
        @DisplayName("Có 4 quân liên tiếp không thắng")
        void testFourStonesDoesNotWin() {
            for (int x = 2; x <= 5; x++) {
                board.placeStone(x, 4, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(5, 4))).isFalse();
            assertThat(winChecker.isWinningMove(board, new Move(2, 4))).isFalse();
        }

        @Test
        @DisplayName("Chỉ có 1 quân cờ không thắng")
        void testSingleStoneDoesNotWin() {
            board.placeStone(7, 7, Board.BLACK);
            assertThat(winChecker.isWinningMove(board, new Move(7, 7))).isFalse();
        }
    }

    @Nested
    @DisplayName("Chuỗi quân bị ngắt quãng")
    class InterruptedChainTests {

        @Test
        @DisplayName("Chuỗi 5 quân bị ngắt bởi quân đối phương không thắng")
        void testChainInterruptedByOpponent() {
            // BLACK, BLACK, WHITE, BLACK, BLACK tại y=7
            board.placeStone(2, 7, Board.BLACK);
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.WHITE);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            assertThat(winChecker.isWinningMove(board, new Move(6, 7))).isFalse();
            assertThat(winChecker.isWinningMove(board, new Move(2, 7))).isFalse();
            assertThat(winChecker.isWinningMove(board, new Move(4, 7))).isFalse();
        }

        @Test
        @DisplayName("Chuỗi 5 ô bị ngắt bởi ô trống không thắng")
        void testChainInterruptedByEmptyCell() {
            // BLACK, BLACK, EMPTY, BLACK, BLACK tại y=7
            board.placeStone(2, 7, Board.BLACK);
            board.placeStone(3, 7, Board.BLACK);
            // Ô (4, 7) để trống
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            assertThat(winChecker.isWinningMove(board, new Move(6, 7))).isFalse();
            assertThat(winChecker.isWinningMove(board, new Move(2, 7))).isFalse();
        }
    }

    @Nested
    @DisplayName("Kiểm tra ở sát biên và các góc bàn cờ")
    class BoardEdgeTests {

        @Test
        @DisplayName("Thắng ngang sát biên trên cùng (y=0)")
        void testTopEdgeHorizontalWin() {
            for (int x = 0; x < 5; x++) {
                board.placeStone(x, 0, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(0, 0))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(4, 0))).isTrue();
        }

        @Test
        @DisplayName("Thắng ngang sát biên dưới cùng (y=14)")
        void testBottomEdgeHorizontalWin() {
            for (int x = 10; x <= 14; x++) {
                board.placeStone(x, 14, Board.WHITE);
            }
            assertThat(winChecker.isWinningMove(board, new Move(14, 14))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(10, 14))).isTrue();
        }

        @Test
        @DisplayName("Thắng dọc sát biên trái (x=0)")
        void testLeftEdgeVerticalWin() {
            for (int y = 0; y < 5; y++) {
                board.placeStone(0, y, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(0, 0))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(0, 4))).isTrue();
        }

        @Test
        @DisplayName("Thắng dọc sát biên phải (x=14)")
        void testRightEdgeVerticalWin() {
            for (int y = 10; y <= 14; y++) {
                board.placeStone(14, y, Board.WHITE);
            }
            assertThat(winChecker.isWinningMove(board, new Move(14, 14))).isTrue();
        }

        @Test
        @DisplayName("Thắng chéo xuất phát từ góc trên-trái (0, 0)")
        void testTopLeftCornerDiagonalWin() {
            for (int i = 0; i < 5; i++) {
                board.placeStone(i, i, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(0, 0))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(4, 4))).isTrue();
        }

        @Test
        @DisplayName("Thắng chéo chạm góc dưới-phải (14, 14)")
        void testBottomRightCornerDiagonalWin() {
            for (int i = 0; i < 5; i++) {
                board.placeStone(10 + i, 10 + i, Board.WHITE);
            }
            assertThat(winChecker.isWinningMove(board, new Move(14, 14))).isTrue();
        }

        @Test
        @DisplayName("Thắng chéo phụ xuất phát từ góc trên-phải (14, 0)")
        void testTopRightCornerAntiDiagonalWin() {
            for (int i = 0; i < 5; i++) {
                board.placeStone(14 - i, i, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(14, 0))).isTrue();
            assertThat(winChecker.isWinningMove(board, new Move(10, 4))).isTrue();
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ chữ nhật")
    class RectangularBoardTests {

        @Test
        @DisplayName("Kiểm tra thắng dọc trên bàn cờ chữ nhật 10x20")
        void testVerticalWinOnRectangularBoard() {
            Board rectBoard = new Board(10, 20);
            for (int y = 14; y <= 18; y++) {
                rectBoard.placeStone(5, y, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(rectBoard, new Move(5, 18))).isTrue();
        }

        @Test
        @DisplayName("Kiểm tra thắng ngang sát biên trên bàn cờ chữ nhật 10x20")
        void testHorizontalWinOnRectangularBoard() {
            Board rectBoard = new Board(10, 20);
            for (int x = 0; x < 5; x++) {
                rectBoard.placeStone(x, 19, Board.WHITE);
            }
            assertThat(winChecker.isWinningMove(rectBoard, new Move(4, 19))).isTrue();
        }

        @Test
        @DisplayName("Kiểm tra thắng chéo trên bàn cờ chữ nhật 10x20")
        void testDiagonalWinOnRectangularBoard() {
            Board rectBoard = new Board(10, 20);
            for (int i = 0; i < 5; i++) {
                rectBoard.placeStone(1 + i, 10 + i, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(rectBoard, new Move(5, 14))).isTrue();
        }
    }

    @Nested
    @DisplayName("Kiểm tra tính độc lập giữa quân Đen và quân Trắng")
    class BlackAndWhiteTests {

        @Test
        @DisplayName("Cả Đen và Trắng đều có thể giành chiến thắng với chuỗi 5 quân của mình")
        void testBothPlayersCanWin() {
            // Đen 5 quân hàng ngang
            for (int x = 0; x < 5; x++) {
                board.placeStone(x, 0, Board.BLACK);
            }
            assertThat(winChecker.isWinningMove(board, new Move(2, 0))).isTrue();

            // Trắng 5 quân hàng dọc
            for (int y = 2; y <= 6; y++) {
                board.placeStone(10, y, Board.WHITE);
            }
            assertThat(winChecker.isWinningMove(board, new Move(10, 4))).isTrue();
        }
    }
}
