package com.hieupt.gomokubot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Board Tests")
class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo bàn cờ")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo bàn cờ tiêu chuẩn 15x15 thành công")
        void testStandardBoardInitialization() {
            assertThat(board.getWidth()).isEqualTo(15);
            assertThat(board.getHeight()).isEqualTo(15);
            assertThat(board.getOccupiedCount()).isZero();
            assertThat(board.getEmptyCount()).isEqualTo(225);
            assertThat(board.isEmpty()).isTrue();
            assertThat(board.isFull()).isFalse();
        }

        @Test
        @DisplayName("Khởi tạo bàn cờ hình chữ nhật với kích thước khác nhau")
        void testRectangularBoardInitialization() {
            Board rectBoard = new Board(10, 20);
            assertThat(rectBoard.getWidth()).isEqualTo(10);
            assertThat(rectBoard.getHeight()).isEqualTo(20);
            assertThat(rectBoard.getOccupiedCount()).isZero();
            assertThat(rectBoard.getEmptyCount()).isEqualTo(200);
            assertThat(rectBoard.isEmpty()).isTrue();
            assertThat(rectBoard.isFull()).isFalse();
        }

        @ParameterizedTest(name = "Kích thước không hợp lệ: width={0}, height={1}")
        @CsvSource({
                "0, 15",
                "15, 0",
                "-1, 15",
                "15, -5",
                "0, 0",
                "-10, -10"
        })
        @DisplayName("Không cho phép khởi tạo bàn cờ có kích thước <= 0")
        void testInvalidDimensionsThrowException(int width, int height) {
            assertThatThrownBy(() -> new Board(width, height))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Mọi ô cờ sau khi khởi tạo đều là EMPTY")
        void testAllCellsInitiallyEmpty() {
            Board smallBoard = new Board(3, 3);
            for (int y = 0; y < smallBoard.getHeight(); y++) {
                for (int x = 0; x < smallBoard.getWidth(); x++) {
                    assertThat(smallBoard.getCell(x, y)).isEqualTo(Board.EMPTY);
                    assertThat(smallBoard.isEmpty(x, y)).isTrue();
                }
            }
        }
    }

    @Nested
    @DisplayName("Kiểm tra tọa độ hợp lệ và ngoài phạm vi")
    class CoordinateTests {

        @ParameterizedTest(name = "Tọa độ hợp lệ: ({0}, {1})")
        @CsvSource({
                "0, 0",
                "14, 14",
                "0, 14",
                "14, 0",
                "7, 7"
        })
        @DisplayName("Tọa độ nằm trong phạm vi bàn cờ")
        void testInsideCoordinates(int x, int y) {
            assertThat(board.isInside(x, y)).isTrue();
        }

        @ParameterizedTest(name = "Tọa độ ngoài phạm vi: ({0}, {1})")
        @CsvSource({
                "-1, 0",
                "0, -1",
                "-1, -1",
                "15, 0",
                "0, 15",
                "15, 15",
                "100, 100",
                "-5, 7"
        })
        @DisplayName("Tọa độ nằm ngoài phạm vi bàn cờ")
        void testOutsideCoordinates(int x, int y) {
            assertThat(board.isInside(x, y)).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
                "-1, 0",
                "0, -1",
                "15, 0",
                "0, 15"
        })
        @DisplayName("getCell với tọa độ ngoài phạm vi ném IndexOutOfBoundsException")
        void testGetCellOutsideThrowsException(int x, int y) {
            assertThatThrownBy(() -> board.getCell(x, y))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessageContaining("out of board");
        }
    }

    @Nested
    @DisplayName("Đặt quân cờ (placeStone)")
    class PlaceStoneTests {

        @Test
        @DisplayName("Đặt quân Đen thành công")
        void testPlaceBlackStoneSuccess() {
            board.placeStone(7, 7, Board.BLACK);

            assertThat(board.getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(board.isEmpty(7, 7)).isFalse();
            assertThat(board.isEmpty()).isFalse();
            assertThat(board.getOccupiedCount()).isEqualTo(1);
            assertThat(board.getEmptyCount()).isEqualTo(224);
        }

        @Test
        @DisplayName("Đặt quân Trắng thành công")
        void testPlaceWhiteStoneSuccess() {
            board.placeStone(3, 4, Board.WHITE);

            assertThat(board.getCell(3, 4)).isEqualTo(Board.WHITE);
            assertThat(board.isEmpty(3, 4)).isFalse();
            assertThat(board.getOccupiedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Không cho đặt quân vào ô đã có quân Đen")
        void testCannotPlaceOnBlackOccupiedCell() {
            board.placeStone(5, 5, Board.BLACK);

            assertThatThrownBy(() -> board.placeStone(5, 5, Board.WHITE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already occupied");
        }

        @Test
        @DisplayName("Không cho đặt quân vào ô đã có quân Trắng")
        void testCannotPlaceOnWhiteOccupiedCell() {
            board.placeStone(5, 5, Board.WHITE);

            assertThatThrownBy(() -> board.placeStone(5, 5, Board.BLACK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already occupied");
        }

        @ParameterizedTest(name = "Player không hợp lệ: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("Không cho đặt quân với player không hợp lệ")
        void testInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> board.placeStone(0, 0, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player value");
        }

        @Test
        @DisplayName("Đặt quân ngoài bàn cờ ném IndexOutOfBoundsException")
        void testPlaceStoneOutsideThrowsException() {
            assertThatThrownBy(() -> board.placeStone(-1, 0, Board.BLACK))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> board.placeStone(15, 15, Board.WHITE))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("Xóa quân cờ (removeStone)")
    class RemoveStoneTests {

        @Test
        @DisplayName("Xóa quân cờ thành công khôi phục trạng thái EMPTY")
        void testRemoveStoneSuccess() {
            board.placeStone(5, 5, Board.BLACK);
            assertThat(board.getOccupiedCount()).isEqualTo(1);

            board.removeStone(5, 5);

            assertThat(board.getCell(5, 5)).isEqualTo(Board.EMPTY);
            assertThat(board.isEmpty(5, 5)).isTrue();
            assertThat(board.getOccupiedCount()).isZero();
            assertThat(board.getEmptyCount()).isEqualTo(225);
            assertThat(board.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Không cho phép xóa ô đang trống ném IllegalStateException")
        void testCannotRemoveEmptyCell() {
            assertThatThrownBy(() -> board.removeStone(5, 5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already empty");
        }

        @Test
        @DisplayName("Xóa quân ngoài bàn cờ ném IndexOutOfBoundsException")
        void testRemoveStoneOutsideThrowsException() {
            assertThatThrownBy(() -> board.removeStone(-1, 0))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("Reset bàn cờ")
    class ResetTests {

        @Test
        @DisplayName("Reset đưa toàn bộ bàn cờ về trạng thái rỗng ban đầu")
        void testResetBoard() {
            board.placeStone(1, 1, Board.BLACK);
            board.placeStone(2, 2, Board.WHITE);
            board.placeStone(3, 3, Board.BLACK);
            assertThat(board.getOccupiedCount()).isEqualTo(3);

            board.reset();

            assertThat(board.getOccupiedCount()).isZero();
            assertThat(board.getEmptyCount()).isEqualTo(225);
            assertThat(board.isEmpty()).isTrue();
            assertThat(board.isFull()).isFalse();

            for (int y = 0; y < board.getHeight(); y++) {
                for (int x = 0; x < board.getWidth(); x++) {
                    assertThat(board.getCell(x, y)).isEqualTo(Board.EMPTY);
                }
            }
        }
    }

    @Nested
    @DisplayName("Sao chép bàn cờ (copy)")
    class CopyTests {

        @Test
        @DisplayName("Bản copy độc lập hoàn toàn với bản gốc")
        void testBoardCopyIndependence() {
            board.placeStone(2, 2, Board.BLACK);
            board.placeStone(4, 4, Board.WHITE);

            Board copy = board.copy();

            assertThat(copy.getWidth()).isEqualTo(board.getWidth());
            assertThat(copy.getHeight()).isEqualTo(board.getHeight());
            assertThat(copy.getOccupiedCount()).isEqualTo(board.getOccupiedCount());
            assertThat(copy.getCell(2, 2)).isEqualTo(Board.BLACK);
            assertThat(copy.getCell(4, 4)).isEqualTo(Board.WHITE);

            // Thay đổi trên bản copy không ảnh hưởng bản gốc
            copy.placeStone(6, 6, Board.BLACK);
            assertThat(copy.getCell(6, 6)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(6, 6)).isEqualTo(Board.EMPTY);
            assertThat(board.getOccupiedCount()).isEqualTo(2);
            assertThat(copy.getOccupiedCount()).isEqualTo(3);

            // Thay đổi trên bản gốc không ảnh hưởng bản copy
            board.placeStone(8, 8, Board.WHITE);
            assertThat(board.getCell(8, 8)).isEqualTo(Board.WHITE);
            assertThat(copy.getCell(8, 8)).isEqualTo(Board.EMPTY);
        }
    }

    @Nested
    @DisplayName("Đếm quân và trạng thái đầy bàn cờ")
    class CountAndFullStateTests {

        @Test
        @DisplayName("Kiểm tra occupiedCount, emptyCount và isFull khi điền đầy bàn cờ")
        void testFullBoardState() {
            Board small = new Board(2, 2);
            assertThat(small.isFull()).isFalse();
            assertThat(small.getOccupiedCount()).isZero();
            assertThat(small.getEmptyCount()).isEqualTo(4);

            small.placeStone(0, 0, Board.BLACK);
            assertThat(small.getOccupiedCount()).isEqualTo(1);
            assertThat(small.getEmptyCount()).isEqualTo(3);
            assertThat(small.isFull()).isFalse();

            small.placeStone(1, 0, Board.WHITE);
            assertThat(small.getOccupiedCount()).isEqualTo(2);
            assertThat(small.getEmptyCount()).isEqualTo(2);
            assertThat(small.isFull()).isFalse();

            small.placeStone(0, 1, Board.BLACK);
            assertThat(small.getOccupiedCount()).isEqualTo(3);
            assertThat(small.getEmptyCount()).isEqualTo(1);
            assertThat(small.isFull()).isFalse();

            small.placeStone(1, 1, Board.WHITE);
            assertThat(small.getOccupiedCount()).isEqualTo(4);
            assertThat(small.getEmptyCount()).isZero();
            assertThat(small.isFull()).isTrue();
        }
    }

    @Nested
    @DisplayName("Biểu diễn chuỗi (toString)")
    class ToStringTests {

        @Test
        @DisplayName("toString hiển thị đúng ký hiệu quân cờ")
        void testToStringFormat() {
            Board small = new Board(2, 2);
            small.placeStone(0, 0, Board.BLACK);
            small.placeStone(1, 1, Board.WHITE);

            String repr = small.toString();
            assertThat(repr).contains("X").contains("O").contains(".");
        }
    }
}

