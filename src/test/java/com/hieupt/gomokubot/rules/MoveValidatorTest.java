package com.hieupt.gomokubot.rules;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.GameState;
import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MoveValidator Tests")
class MoveValidatorTest {

    private MoveValidator validator;
    private GameState gameState;
    private Board board;

    @BeforeEach
    void setUp() {
        validator = new MoveValidator();
        gameState = new GameState(15, 15, Board.BLACK);
        board = gameState.getBoard();
    }

    @Nested
    @DisplayName("Kiểm tra nước đi hợp lệ")
    class ValidMoveTests {

        @Test
        @DisplayName("Nước đi vào ô trống trong bàn cờ khi game đang diễn ra là hợp lệ")
        void testValidMove() {
            Move move = new Move(7, 7);

            assertThat(validator.isValidMove(gameState, move)).isTrue();
            assertThatCode(() -> validator.validateMove(gameState, move)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Các nước đi tại 4 góc biên hợp lệ khi còn trống")
        void testCornerMovesValid() {
            assertThat(validator.isValidMove(gameState, new Move(0, 0))).isTrue();
            assertThat(validator.isValidMove(gameState, new Move(14, 0))).isTrue();
            assertThat(validator.isValidMove(gameState, new Move(0, 14))).isTrue();
            assertThat(validator.isValidMove(gameState, new Move(14, 14))).isTrue();
        }
    }

    @Nested
    @DisplayName("Kiểm tra tọa độ ngoài phạm vi bàn cờ")
    class CoordinateOutOfBoundsTests {

        @ParameterizedTest(name = "Tọa độ âm: ({0}, {1})")
        @CsvSource({
                "-1, 0",
                "0, -1",
                "-5, -5"
        })
        @DisplayName("Tọa độ âm không hợp lệ")
        void testNegativeCoordinates(int x, int y) {
            Move move = new Move(x, y);

            assertThat(validator.isValidMove(gameState, move)).isFalse();
            assertThatThrownBy(() -> validator.validateMove(gameState, move))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outside the board");
        }

        @ParameterizedTest(name = "Tọa độ vượt biên: ({0}, {1})")
        @CsvSource({
                "15, 0",
                "0, 15",
                "15, 15",
                "20, 20"
        })
        @DisplayName("Tọa độ vượt kích thước bàn cờ không hợp lệ")
        void testExceedingBoardSize(int x, int y) {
            Move move = new Move(x, y);

            assertThat(validator.isValidMove(gameState, move)).isFalse();
            assertThatThrownBy(() -> validator.validateMove(gameState, move))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outside the board");
        }
    }

    @Nested
    @DisplayName("Kiểm tra ô đã có quân")
    class OccupiedCellTests {

        @Test
        @DisplayName("Đánh vào ô đã có quân Đen không hợp lệ")
        void testMoveOnCellOccupiedByBlack() {
            gameState.makeMove(new Move(7, 7)); // Đen đặt quân

            Move move = new Move(7, 7);
            assertThat(validator.isValidMove(gameState, move)).isFalse();
            assertThatThrownBy(() -> validator.validateMove(gameState, move))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already occupied");
        }

        @Test
        @DisplayName("Đánh vào ô đã có quân Trắng không hợp lệ")
        void testMoveOnCellOccupiedByWhite() {
            gameState.makeMove(new Move(7, 7)); // Đen
            gameState.makeMove(new Move(7, 8)); // Trắng

            Move move = new Move(7, 8);
            assertThat(validator.isValidMove(gameState, move)).isFalse();
            assertThatThrownBy(() -> validator.validateMove(gameState, move))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already occupied");
        }
    }

    @Nested
    @DisplayName("Kiểm tra trạng thái khi game đã kết thúc")
    class GameOverTests {

        @Test
        @DisplayName("Khi game đã kết thúc (thắng), không cho phép đánh thêm nước nào")
        void testCannotMoveWhenGameWon() {
            gameState.makeMove(new Move(7, 7));
            gameState.declareWinner(Board.BLACK);

            Move nextMove = new Move(8, 8);
            assertThat(validator.isValidMove(gameState, nextMove)).isFalse();
            assertThatThrownBy(() -> validator.validateMove(gameState, nextMove))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("game is over");
        }

        @Test
        @DisplayName("Khi game đã kết thúc (hòa), không cho phép đánh thêm")
        void testCannotMoveWhenGameDraw() {
            GameState smallGame = new GameState(2, 2, Board.BLACK);
            smallGame.makeMove(new Move(0, 0));
            smallGame.makeMove(new Move(0, 1));
            smallGame.makeMove(new Move(1, 0));
            smallGame.makeMove(new Move(1, 1));
            smallGame.declareDraw();

            Move nextMove = new Move(0, 0);
            assertThat(validator.isValidMove(smallGame, nextMove)).isFalse();
            assertThatThrownBy(() -> validator.validateMove(smallGame, nextMove))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("game is over");
        }
    }

    @Nested
    @DisplayName("Kiểm tra tham số null")
    class NullValidationTests {

        @Test
        @DisplayName("Move null: isValidMove trả về false, validateMove ném IllegalArgumentException")
        void testNullMove() {
            assertThat(validator.isValidMove(gameState, null)).isFalse();
            assertThatThrownBy(() -> validator.validateMove(gameState, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Move must not be null");
        }

        @Test
        @DisplayName("GameState null ném NullPointerException")
        void testNullGameState() {
            Move move = new Move(7, 7);

            assertThatThrownBy(() -> validator.isValidMove(null, move))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("GameState must not be null");

            assertThatThrownBy(() -> validator.validateMove(null, move))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("GameState must not be null");
        }

        @Test
        @DisplayName("Board null trong isValidPosition ném NullPointerException")
        void testNullBoardInIsValidPosition() {
            assertThatThrownBy(() -> validator.isValidPosition(null, new Move(0, 0)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }
    }

    @Nested
    @DisplayName("Kiểm tra isValidPosition không phụ thuộc trạng thái ván đấu")
    class IsValidPositionTests {

        @Test
        @DisplayName("isValidPosition trả về true cho ô trống trong bàn cờ")
        void testIsValidPositionTrue() {
            assertThat(validator.isValidPosition(board, new Move(5, 5))).isTrue();
        }

        @Test
        @DisplayName("isValidPosition trả về false khi ô đã có quân")
        void testIsValidPositionFalseWhenOccupied() {
            board.placeStone(5, 5, Board.BLACK);
            assertThat(validator.isValidPosition(board, new Move(5, 5))).isFalse();
        }

        @Test
        @DisplayName("isValidPosition trả về false cho tọa độ ngoài bàn cờ hoặc Move null")
        void testIsValidPositionFalseWhenOutsideOrNull() {
            assertThat(validator.isValidPosition(board, new Move(-1, 0))).isFalse();
            assertThat(validator.isValidPosition(board, new Move(15, 0))).isFalse();
            assertThat(validator.isValidPosition(board, null)).isFalse();
        }

        @Test
        @DisplayName("isValidPosition chỉ phụ thuộc Board, không phụ thuộc ván đấu đã kết thúc hay chưa")
        void testIsValidPositionIndependentOfGameState() {
            gameState.makeMove(new Move(7, 7));
            gameState.declareWinner(Board.BLACK);

            // isValidMove trả về false vì game over
            assertThat(validator.isValidMove(gameState, new Move(0, 0))).isFalse();

            // Nhưng isValidPosition trên board vẫn trả về true vì ô (0, 0) hợp lệ và trống
            assertThat(validator.isValidPosition(gameState.getBoard(), new Move(0, 0))).isTrue();
        }
    }
}

