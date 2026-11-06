package com.hieupt.gomokubot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GameState Tests")
class GameStateTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState(15, 15, Board.BLACK);
    }

    @Nested
    @DisplayName("Khởi tạo GameState")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo với quân Đen đi trước")
        void testInitializeWithBlackStarting() {
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
            assertThat(gameState.getGameStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(gameState.getWinner()).isEqualTo(Board.EMPTY);
            assertThat(gameState.getMoveCount()).isZero();
            assertThat(gameState.getLastMove()).isNull();
            assertThat(gameState.getMoveHistory()).isEmpty();
            assertThat(gameState.isGameOver()).isFalse();
            assertThat(gameState.getBoard()).isNotNull();
            assertThat(gameState.getBoard().getWidth()).isEqualTo(15);
            assertThat(gameState.getBoard().getHeight()).isEqualTo(15);
        }

        @Test
        @DisplayName("Khởi tạo với quân Trắng đi trước")
        void testInitializeWithWhiteStarting() {
            GameState whiteStarts = new GameState(15, 15, Board.WHITE);
            assertThat(whiteStarts.getCurrentPlayer()).isEqualTo(Board.WHITE);
            assertThat(whiteStarts.getGameStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(whiteStarts.getWinner()).isEqualTo(Board.EMPTY);
        }

        @Test
        @DisplayName("Khởi tạo với Board tùy chỉnh")
        void testInitializeWithCustomBoard() {
            Board customBoard = new Board(10, 10);
            GameState state = new GameState(customBoard, Board.BLACK);
            assertThat(state.getBoard()).isSameAs(customBoard);
        }

        @ParameterizedTest(name = "Starting player không hợp lệ: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("Khởi tạo với người chơi không hợp lệ ném IllegalArgumentException")
        void testInvalidStartingPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> new GameState(15, 15, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player value");
        }

        @Test
        @DisplayName("Khởi tạo với Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> new GameState(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }
    }

    @Nested
    @DisplayName("Đánh nước đi (makeMove) và chuyển lượt")
    class MakeMoveTests {

        @Test
        @DisplayName("Đánh một nước hợp lệ thành công")
        void testValidMovePlacement() {
            Move move = new Move(7, 7);
            gameState.makeMove(move);

            assertThat(gameState.getBoard().getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(gameState.getMoveCount()).isEqualTo(1);
            assertThat(gameState.getLastMove()).isEqualTo(move);
            assertThat(gameState.getMoveHistory()).containsExactly(move);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.WHITE);
        }

        @Test
        @DisplayName("Chuyển lượt tuần tự qua lại giữa Đen và Trắng")
        void testTurnAlternation() {
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);

            gameState.makeMove(new Move(7, 7)); // Đen đánh
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.WHITE);

            gameState.makeMove(new Move(7, 8)); // Trắng đánh
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);

            gameState.makeMove(new Move(6, 7)); // Đen đánh
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.WHITE);

            assertThat(gameState.getMoveCount()).isEqualTo(3);
            assertThat(gameState.getMoveHistory()).containsExactly(
                    new Move(7, 7),
                    new Move(7, 8),
                    new Move(6, 7)
            );
        }

        @Test
        @DisplayName("Không cho phép đánh nước null ném NullPointerException")
        void testNullMoveThrowsException() {
            assertThatThrownBy(() -> gameState.makeMove(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Move must not be null");
        }

        @Test
        @DisplayName("Không cho phép đánh vào ô đã có quân")
        void testMoveOnOccupiedCellThrowsException() {
            gameState.makeMove(new Move(7, 7));
            assertThatThrownBy(() -> gameState.makeMove(new Move(7, 7)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already occupied");
        }

        @Test
        @DisplayName("Không cho phép đánh ra ngoài phạm vi bàn cờ")
        void testMoveOutsideBoardThrowsException() {
            assertThatThrownBy(() -> gameState.makeMove(new Move(-1, 0)))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> gameState.makeMove(new Move(15, 15)))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        @DisplayName("Không cho phép đánh khi ván đấu đã kết thúc")
        void testCannotMoveWhenGameEnded() {
            gameState.makeMove(new Move(7, 7));
            gameState.declareWinner(Board.BLACK);

            assertThatThrownBy(() -> gameState.makeMove(new Move(8, 8)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Game has already ended");
        }
    }

    @Nested
    @DisplayName("Hoàn tác nước đi (undoMove)")
    class UndoMoveTests {

        @Test
        @DisplayName("Undo một nước đi thành công và khôi phục ô cờ rỗng")
        void testUndoSingleMove() {
            Move move = new Move(7, 7);
            gameState.makeMove(move);

            Move undone = gameState.undoMove();

            assertThat(undone).isEqualTo(move);
            assertThat(gameState.getBoard().getCell(7, 7)).isEqualTo(Board.EMPTY);
            assertThat(gameState.getMoveCount()).isZero();
            assertThat(gameState.getLastMove()).isNull();
            assertThat(gameState.getMoveHistory()).isEmpty();
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
        }

        @Test
        @DisplayName("Undo nhiều nước đi liên tiếp")
        void testUndoMultipleMovesConsecutively() {
            Move m1 = new Move(7, 7);
            Move m2 = new Move(7, 8);
            Move m3 = new Move(8, 8);

            gameState.makeMove(m1); // Đen
            gameState.makeMove(m2); // Trắng
            gameState.makeMove(m3); // Đen

            // Undo m3
            Move u3 = gameState.undoMove();
            assertThat(u3).isEqualTo(m3);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
            assertThat(gameState.getLastMove()).isEqualTo(m2);
            assertThat(gameState.getMoveCount()).isEqualTo(2);

            // Undo m2
            Move u2 = gameState.undoMove();
            assertThat(u2).isEqualTo(m2);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.WHITE);
            assertThat(gameState.getLastMove()).isEqualTo(m1);
            assertThat(gameState.getMoveCount()).isEqualTo(1);

            // Undo m1
            Move u1 = gameState.undoMove();
            assertThat(u1).isEqualTo(m1);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
            assertThat(gameState.getLastMove()).isNull();
            assertThat(gameState.getMoveCount()).isZero();
            assertThat(gameState.getBoard().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Undo khi chưa có nước đi nào ném IllegalStateException")
        void testUndoWhenNoMovesThrowsException() {
            assertThatThrownBy(() -> gameState.undoMove())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No move to undo");
        }

        @Test
        @DisplayName("Undo sau khi khai báo thắng khôi phục trạng thái IN_PROGRESS")
        void testUndoAfterWinRestoresInProgress() {
            gameState.makeMove(new Move(7, 7)); // Đen
            gameState.declareWinner(Board.BLACK);

            assertThat(gameState.isGameOver()).isTrue();
            assertThat(gameState.getWinner()).isEqualTo(Board.BLACK);

            gameState.undoMove();

            assertThat(gameState.getGameStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(gameState.getWinner()).isEqualTo(Board.EMPTY);
            assertThat(gameState.isGameOver()).isFalse();
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
        }
    }

    @Nested
    @DisplayName("Khai báo người thắng (declareWinner)")
    class DeclareWinnerTests {

        @Test
        @DisplayName("Khai báo người thắng hợp lệ")
        void testDeclareWinnerSuccess() {
            gameState.makeMove(new Move(7, 7)); // Đen đánh nước cuối
            gameState.declareWinner(Board.BLACK);

            assertThat(gameState.getGameStatus()).isEqualTo(GameStatus.WON);
            assertThat(gameState.getWinner()).isEqualTo(Board.BLACK);
            assertThat(gameState.isGameOver()).isTrue();
        }

        @Test
        @DisplayName("Khai báo thắng khi chưa có nước đi nào ném IllegalStateException")
        void testDeclareWinnerWithoutMovesThrowsException() {
            assertThatThrownBy(() -> gameState.declareWinner(Board.BLACK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("without any moves");
        }

        @Test
        @DisplayName("Không cho khai báo người thắng không phải người vừa đánh nước cuối")
        void testDeclareWrongWinnerThrowsException() {
            gameState.makeMove(new Move(7, 7)); // Đen đánh nước cuối

            // Khai báo Trắng thắng trong khi Đen vừa đánh
            assertThatThrownBy(() -> gameState.declareWinner(Board.WHITE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Winner must be the player who made the last move");
        }

        @Test
        @DisplayName("Không cho khai báo người thắng khi ván đấu đã kết thúc")
        void testDeclareWinnerWhenAlreadyEndedThrowsException() {
            gameState.makeMove(new Move(7, 7));
            gameState.declareWinner(Board.BLACK);

            assertThatThrownBy(() -> gameState.declareWinner(Board.BLACK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Game has already ended");
        }

        @ParameterizedTest(name = "Winning player không hợp lệ: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("Khai báo người thắng không hợp lệ ném IllegalArgumentException")
        void testDeclareInvalidWinnerThrowsException(byte invalidPlayer) {
            gameState.makeMove(new Move(7, 7));

            assertThatThrownBy(() -> gameState.declareWinner(invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player value");
        }
    }

    @Nested
    @DisplayName("Khai báo hòa (declareDraw)")
    class DeclareDrawTests {

        @Test
        @DisplayName("Khai báo hòa thành công khi bàn cờ đầy")
        void testDeclareDrawSuccessWhenBoardFull() {
            GameState smallState = new GameState(2, 2, Board.BLACK);
            smallState.makeMove(new Move(0, 0)); // Đen
            smallState.makeMove(new Move(0, 1)); // Trắng
            smallState.makeMove(new Move(1, 0)); // Đen
            smallState.makeMove(new Move(1, 1)); // Trắng
            assertThat(smallState.getBoard().isFull()).isTrue();

            smallState.declareDraw();

            assertThat(smallState.getGameStatus()).isEqualTo(GameStatus.DRAW);
            assertThat(smallState.getWinner()).isEqualTo(Board.EMPTY);
            assertThat(smallState.isGameOver()).isTrue();
        }

        @Test
        @DisplayName("Không cho khai báo hòa khi bàn cờ còn ô trống")
        void testDeclareDrawWhenNotFullThrowsException() {
            gameState.makeMove(new Move(7, 7));

            assertThatThrownBy(() -> gameState.declareDraw())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty cells remain");
        }

        @Test
        @DisplayName("Không cho khai báo hòa khi ván đấu đã kết thúc")
        void testDeclareDrawWhenAlreadyEndedThrowsException() {
            GameState smallState = new GameState(2, 2, Board.BLACK);
            smallState.makeMove(new Move(0, 0));
            smallState.makeMove(new Move(0, 1));
            smallState.makeMove(new Move(1, 0));
            smallState.makeMove(new Move(1, 1));
            smallState.declareDraw();

            assertThatThrownBy(() -> smallState.declareDraw())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Game has already ended");
        }
    }

    @Nested
    @DisplayName("Sao chép trạng thái (copy)")
    class CopyTests {

        @Test
        @DisplayName("Bản copy giữ nguyên toàn bộ trạng thái và lịch sử nước đi")
        void testCopyRetainsStateAndHistory() {
            gameState.makeMove(new Move(7, 7));
            gameState.makeMove(new Move(7, 8));

            GameState copy = gameState.copy();

            assertThat(copy.getCurrentPlayer()).isEqualTo(gameState.getCurrentPlayer());
            assertThat(copy.getGameStatus()).isEqualTo(gameState.getGameStatus());
            assertThat(copy.getWinner()).isEqualTo(gameState.getWinner());
            assertThat(copy.getMoveCount()).isEqualTo(gameState.getMoveCount());
            assertThat(copy.getLastMove()).isEqualTo(gameState.getLastMove());
            assertThat(copy.getMoveHistory()).isEqualTo(gameState.getMoveHistory());
            assertThat(copy.getBoard().getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(copy.getBoard().getCell(7, 8)).isEqualTo(Board.WHITE);

            // Thao tác trên copy không ảnh hưởng original
            copy.makeMove(new Move(8, 8));
            assertThat(copy.getMoveCount()).isEqualTo(3);
            assertThat(gameState.getMoveCount()).isEqualTo(2);
            assertThat(gameState.getBoard().getCell(8, 8)).isEqualTo(Board.EMPTY);

            // Thao tác trên original không ảnh hưởng copy
            gameState.makeMove(new Move(6, 6));
            assertThat(gameState.getMoveCount()).isEqualTo(3);
            assertThat(copy.getBoard().getCell(6, 6)).isEqualTo(Board.EMPTY);
        }

        @Test
        @DisplayName("Copy ván đấu đã kết thúc vẫn giữ nguyên status và winner")
        void testCopyFinishedGame() {
            gameState.makeMove(new Move(7, 7));
            gameState.declareWinner(Board.BLACK);

            GameState copy = gameState.copy();
            assertThat(copy.getGameStatus()).isEqualTo(GameStatus.WON);
            assertThat(copy.getWinner()).isEqualTo(Board.BLACK);
            assertThat(copy.isGameOver()).isTrue();
        }
    }
}

