package com.hieupt.gomokubot.core;

import com.hieupt.gomokubot.rules.MoveValidator;
import com.hieupt.gomokubot.rules.WinChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GameEngine Tests")
class GameEngineTest {

    private GameEngine gameEngine;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine(5);
        gameState = new GameState(15, 15, Board.BLACK);
    }

    @Nested
    @DisplayName("Khởi tạo và quản lý Dependency")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo GameEngine với winLength mặc định thành công")
        void testDefaultConstructor() {
            GameEngine engine = new GameEngine(5);
            assertThat(engine.getMoveValidator()).isNotNull();
            assertThat(engine.getWinChecker()).isNotNull();
            assertThat(engine.getWinChecker().getWinLength()).isEqualTo(5);
        }

        @Test
        @DisplayName("Khởi tạo GameEngine với custom MoveValidator và WinChecker")
        void testCustomComponentsConstructor() {
            MoveValidator customValidator = new MoveValidator();
            WinChecker customChecker = new WinChecker(3);

            GameEngine engine = new GameEngine(customValidator, customChecker);

            assertThat(engine.getMoveValidator()).isSameAs(customValidator);
            assertThat(engine.getWinChecker()).isSameAs(customChecker);
            assertThat(engine.getWinChecker().getWinLength()).isEqualTo(3);
        }

        @ParameterizedTest(name = "winLength không hợp lệ: {0}")
        @ValueSource(ints = {0, -1, -5})
        @DisplayName("Khởi tạo với winLength <= 0 ném IllegalArgumentException")
        void testInvalidWinLengthThrowsException(int invalidLength) {
            assertThatThrownBy(() -> new GameEngine(invalidLength))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Khởi tạo với MoveValidator null ném NullPointerException")
        void testNullMoveValidatorThrowsException() {
            WinChecker checker = new WinChecker(5);
            assertThatThrownBy(() -> new GameEngine(null, checker))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("MoveValidator must not be null");
        }

        @Test
        @DisplayName("Khởi tạo với WinChecker null ném NullPointerException")
        void testNullWinCheckerThrowsException() {
            MoveValidator validator = new MoveValidator();
            assertThatThrownBy(() -> new GameEngine(validator, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("WinChecker must not be null");
        }
    }

    @Nested
    @DisplayName("Nước đi thông thường (IN_PROGRESS)")
    class InProgressMoveTests {

        @Test
        @DisplayName("Đánh một nước hợp lệ đầu tiên trả về IN_PROGRESS và chuyển lượt")
        void testPlayFirstMove() {
            Move move = new Move(7, 7);
            GameStatus status = gameEngine.playMove(gameState, move);

            assertThat(status).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(gameState.getGameStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(gameState.getBoard().getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.WHITE);
            assertThat(gameState.getMoveCount()).isEqualTo(1);
            assertThat(gameState.getLastMove()).isEqualTo(move);
            assertThat(gameState.isGameOver()).isFalse();
            assertThat(gameState.getWinner()).isEqualTo(Board.EMPTY);
        }

        @Test
        @DisplayName("Đánh nhiều nước cờ luân phiên không bên nào thắng trả về IN_PROGRESS")
        void testPlayMultipleMovesAlternating() {
            assertThat(gameEngine.playMove(gameState, new Move(7, 7))).isEqualTo(GameStatus.IN_PROGRESS); // Đen
            assertThat(gameEngine.playMove(gameState, new Move(7, 8))).isEqualTo(GameStatus.IN_PROGRESS); // Trắng
            assertThat(gameEngine.playMove(gameState, new Move(8, 7))).isEqualTo(GameStatus.IN_PROGRESS); // Đen
            assertThat(gameEngine.playMove(gameState, new Move(8, 8))).isEqualTo(GameStatus.IN_PROGRESS); // Trắng

            assertThat(gameState.getMoveCount()).isEqualTo(4);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
            assertThat(gameState.isGameOver()).isFalse();
        }
    }

    @Nested
    @DisplayName("Nước đi dẫn đến chiến thắng (WON)")
    class WinningMoveTests {

        @Test
        @DisplayName("Quân Đen thắng khi tạo chuỗi 5 quân ngang")
        void testBlackWinsHorizontal() {
            // Đen: (3,7), (4,7), (5,7), (6,7)
            // Trắng: (3,8), (4,8), (5,8), (6,8)
            gameEngine.playMove(gameState, new Move(3, 7)); // Đen 1
            gameEngine.playMove(gameState, new Move(3, 8)); // Trắng 1
            gameEngine.playMove(gameState, new Move(4, 7)); // Đen 2
            gameEngine.playMove(gameState, new Move(4, 8)); // Trắng 2
            gameEngine.playMove(gameState, new Move(5, 7)); // Đen 3
            gameEngine.playMove(gameState, new Move(5, 8)); // Trắng 3
            gameEngine.playMove(gameState, new Move(6, 7)); // Đen 4
            gameEngine.playMove(gameState, new Move(6, 8)); // Trắng 4

            // Đen đánh nước thứ 5 quyết định tại (7, 7)
            GameStatus finalStatus = gameEngine.playMove(gameState, new Move(7, 7));

            assertThat(finalStatus).isEqualTo(GameStatus.WON);
            assertThat(gameState.getGameStatus()).isEqualTo(GameStatus.WON);
            assertThat(gameState.getWinner()).isEqualTo(Board.BLACK);
            assertThat(gameState.isGameOver()).isTrue();
        }

        @Test
        @DisplayName("Quân Trắng thắng khi tạo chuỗi 5 quân dọc")
        void testWhiteWinsVertical() {
            // Đen đánh hàng 1, Trắng đánh hàng 0 (dọc)
            gameEngine.playMove(gameState, new Move(5, 5)); // Đen 1
            gameEngine.playMove(gameState, new Move(0, 0)); // Trắng 1
            gameEngine.playMove(gameState, new Move(5, 6)); // Đen 2
            gameEngine.playMove(gameState, new Move(0, 1)); // Trắng 2
            gameEngine.playMove(gameState, new Move(5, 7)); // Đen 3
            gameEngine.playMove(gameState, new Move(0, 2)); // Trắng 3
            gameEngine.playMove(gameState, new Move(5, 8)); // Đen 4
            gameEngine.playMove(gameState, new Move(0, 3)); // Trắng 4
            gameEngine.playMove(gameState, new Move(9, 9)); // Đen 5

            // Trắng đánh nước thứ 5 tại (0, 4)
            GameStatus finalStatus = gameEngine.playMove(gameState, new Move(0, 4));

            assertThat(finalStatus).isEqualTo(GameStatus.WON);
            assertThat(gameState.getGameStatus()).isEqualTo(GameStatus.WON);
            assertThat(gameState.getWinner()).isEqualTo(Board.WHITE);
            assertThat(gameState.isGameOver()).isTrue();
        }

        @Test
        @DisplayName("Thắng theo đường chéo chính")
        void testWinDiagonal() {
            // Đen đánh đường chéo: (1,1), (2,2), (3,3), (4,4), (5,5)
            gameEngine.playMove(gameState, new Move(1, 1)); // Đen 1
            gameEngine.playMove(gameState, new Move(1, 2)); // Trắng 1
            gameEngine.playMove(gameState, new Move(2, 2)); // Đen 2
            gameEngine.playMove(gameState, new Move(2, 3)); // Trắng 2
            gameEngine.playMove(gameState, new Move(3, 3)); // Đen 3
            gameEngine.playMove(gameState, new Move(3, 4)); // Trắng 3
            gameEngine.playMove(gameState, new Move(4, 4)); // Đen 4
            gameEngine.playMove(gameState, new Move(4, 5)); // Trắng 4

            GameStatus status = gameEngine.playMove(gameState, new Move(5, 5)); // Đen 5

            assertThat(status).isEqualTo(GameStatus.WON);
            assertThat(gameState.getWinner()).isEqualTo(Board.BLACK);
        }

        @Test
        @DisplayName("Thắng theo đường chéo phụ")
        void testWinAntiDiagonal() {
            // Đen đánh chéo phụ: (5,1), (4,2), (3,3), (2,4), (1,5)
            gameEngine.playMove(gameState, new Move(5, 1)); // Đen 1
            gameEngine.playMove(gameState, new Move(0, 0)); // Trắng 1
            gameEngine.playMove(gameState, new Move(4, 2)); // Đen 2
            gameEngine.playMove(gameState, new Move(0, 1)); // Trắng 2
            gameEngine.playMove(gameState, new Move(3, 3)); // Đen 3
            gameEngine.playMove(gameState, new Move(0, 2)); // Trắng 3
            gameEngine.playMove(gameState, new Move(2, 4)); // Đen 4
            gameEngine.playMove(gameState, new Move(0, 3)); // Trắng 4

            GameStatus status = gameEngine.playMove(gameState, new Move(1, 5)); // Đen 5

            assertThat(status).isEqualTo(GameStatus.WON);
            assertThat(gameState.getWinner()).isEqualTo(Board.BLACK);
        }

        @Test
        @DisplayName("Đánh quân thứ 5 vào giữa chuỗi (chèn giữa) vẫn thắng")
        void testWinningMoveInsertedInMiddle() {
            // Đặt các quân: (2,2), (3,2), [ô trống (4,2)], (5,2), (6,2)
            gameEngine.playMove(gameState, new Move(2, 2)); // Đen 1
            gameEngine.playMove(gameState, new Move(0, 0)); // Trắng 1
            gameEngine.playMove(gameState, new Move(3, 2)); // Đen 2
            gameEngine.playMove(gameState, new Move(0, 1)); // Trắng 2
            gameEngine.playMove(gameState, new Move(5, 2)); // Đen 3
            gameEngine.playMove(gameState, new Move(0, 2)); // Trắng 3
            gameEngine.playMove(gameState, new Move(6, 2)); // Đen 4
            gameEngine.playMove(gameState, new Move(0, 3)); // Trắng 4

            // Đen đi nước quyết định vào giữa (4, 2)
            GameStatus status = gameEngine.playMove(gameState, new Move(4, 2));

            assertThat(status).isEqualTo(GameStatus.WON);
            assertThat(gameState.getWinner()).isEqualTo(Board.BLACK);
        }
    }

    @Nested
    @DisplayName("Nước đi dẫn đến kết quả hòa (DRAW)")
    class DrawMoveTests {

        @Test
        @DisplayName("Ván đấu kết thúc hòa khi nước đi cuối cùng lấp đầy bàn cờ mà không ai thắng")
        void testGameEndsInDrawWhenBoardFull() {
            // Bàn cờ 2x2 với winLength = 5 (không thể có ai thắng)
            GameState smallGame = new GameState(2, 2, Board.BLACK);

            assertThat(gameEngine.playMove(smallGame, new Move(0, 0))).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(gameEngine.playMove(smallGame, new Move(0, 1))).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(gameEngine.playMove(smallGame, new Move(1, 0))).isEqualTo(GameStatus.IN_PROGRESS);

            // Nước đi cuối cùng lấp đầy ô (1, 1)
            GameStatus finalStatus = gameEngine.playMove(smallGame, new Move(1, 1));

            assertThat(finalStatus).isEqualTo(GameStatus.DRAW);
            assertThat(smallGame.getGameStatus()).isEqualTo(GameStatus.DRAW);
            assertThat(smallGame.getWinner()).isEqualTo(Board.EMPTY);
            assertThat(smallGame.isGameOver()).isTrue();
            assertThat(smallGame.getBoard().isFull()).isTrue();
        }

        @Test
        @DisplayName("Nước đi cuối cùng vừa lấp đầy bàn cờ vừa tạo 5 quân thì THẮNG được ưu tiên hơn HÒA")
        void testWinTakesPrecedenceOverDrawOnLastCell() {
            // Tạo bàn cờ 5x1 với winLength = 5
            GameState lineGame = new GameState(5, 1, Board.BLACK);
            GameEngine engine = new GameEngine(1); // winLength = 1

            // Ngay nước đầu tiên lấp ô trên bàn cờ 1x1
            GameState singleCellGame = new GameState(1, 1, Board.BLACK);
            GameStatus status = engine.playMove(singleCellGame, new Move(0, 0));

            // Ô cuối cùng lấp đầy bàn cờ đồng thời đạt điều kiện winLength = 1
            assertThat(status).isEqualTo(GameStatus.WON);
            assertThat(singleCellGame.getGameStatus()).isEqualTo(GameStatus.WON);
            assertThat(singleCellGame.getWinner()).isEqualTo(Board.BLACK);
        }
    }

    @Nested
    @DisplayName("Kiểm tra xử lý nước đi không hợp lệ (Validation & Exceptions)")
    class InvalidMoveHandlingTests {

        @Test
        @DisplayName("GameState null ném NullPointerException")
        void testNullGameStateThrowsException() {
            assertThatThrownBy(() -> gameEngine.playMove(null, new Move(0, 0)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("GameState must not be null");
        }

        @Test
        @DisplayName("Move null ném IllegalArgumentException và không thay đổi trạng thái")
        void testNullMoveThrowsException() {
            assertThatThrownBy(() -> gameEngine.playMove(gameState, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Move must not be null");

            assertThat(gameState.getMoveCount()).isZero();
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
        }

        @Test
        @DisplayName("Tọa độ ngoài bàn cờ ném IllegalArgumentException")
        void testMoveOutsideBoardThrowsException() {
            assertThatThrownBy(() -> gameEngine.playMove(gameState, new Move(-1, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outside the board");

            assertThatThrownBy(() -> gameEngine.playMove(gameState, new Move(15, 15)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outside the board");

            assertThat(gameState.getMoveCount()).isZero();
        }

        @Test
        @DisplayName("Đánh vào ô đã có quân ném IllegalArgumentException")
        void testMoveOnOccupiedCellThrowsException() {
            gameEngine.playMove(gameState, new Move(7, 7)); // Đen đi thành công

            assertThatThrownBy(() -> gameEngine.playMove(gameState, new Move(7, 7))) // Trắng đánh trùng
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already occupied");

            // Lượt vẫn là của Trắng, số nước đi không tăng
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.WHITE);
            assertThat(gameState.getMoveCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Không cho phép đánh tiếp khi game đã kết thúc (thắng) ném IllegalStateException")
        void testCannotPlayAfterGameWon() {
            // Tạo tình huống thắng nhanh với winLength = 1
            GameEngine fastEngine = new GameEngine(1);
            fastEngine.playMove(gameState, new Move(7, 7)); // Đen thắng ngay
            assertThat(gameState.isGameOver()).isTrue();

            assertThatThrownBy(() -> fastEngine.playMove(gameState, new Move(8, 8)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("game is over");
        }

        @Test
        @DisplayName("Không cho phép đánh tiếp khi game đã kết thúc (hòa) ném IllegalStateException")
        void testCannotPlayAfterGameDraw() {
            GameState smallGame = new GameState(1, 1, Board.BLACK);
            GameEngine drawEngine = new GameEngine(5); // winLength = 5, bàn 1x1 sẽ hòa
            drawEngine.playMove(smallGame, new Move(0, 0));
            assertThat(smallGame.getGameStatus()).isEqualTo(GameStatus.DRAW);

            assertThatThrownBy(() -> drawEngine.playMove(smallGame, new Move(0, 0)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("game is over");
        }
    }

    @Nested
    @DisplayName("Kiểm tra tương tác giữa các component (Mock Tests)")
    @ExtendWith(MockitoExtension.class)
    class ComponentInteractionTests {

        @Mock
        private MoveValidator mockValidator;

        @Mock
        private WinChecker mockWinChecker;

        @Test
        @DisplayName("Thứ tự gọi chính xác: validateMove -> makeMove -> isWinningMove")
        void testExecutionOrder() {
            GameEngine engine = new GameEngine(mockValidator, mockWinChecker);
            Move move = new Move(7, 7);

            when(mockWinChecker.isWinningMove(any(Board.class), any(Move.class))).thenReturn(false);

            engine.playMove(gameState, move);

            InOrder inOrder = inOrder(mockValidator, mockWinChecker);
            inOrder.verify(mockValidator).validateMove(gameState, move);
            inOrder.verify(mockWinChecker).isWinningMove(gameState.getBoard(), move);
        }

        @Test
        @DisplayName("Khi validateMove ném ngoại lệ thì không gọi WinChecker và bàn cờ không bị sửa")
        void testWinCheckerNotCalledWhenValidationFails() {
            GameEngine engine = new GameEngine(mockValidator, mockWinChecker);
            Move move = new Move(7, 7);

            doThrow(new IllegalArgumentException("Invalid move"))
                    .when(mockValidator).validateMove(gameState, move);

            assertThatThrownBy(() -> engine.playMove(gameState, move))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(mockWinChecker, never()).isWinningMove(any(), any());
            assertThat(gameState.getMoveCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Tích hợp hoàn tác (Undo) và tiếp tục chơi")
    class UndoAndResumeTests {

        @Test
        @DisplayName("Sau khi hoàn tác nước đi, GameEngine có thể tiếp tục xử lý nước đi mới")
        void testUndoAndResumePlay() {
            gameEngine.playMove(gameState, new Move(7, 7)); // Đen
            gameEngine.playMove(gameState, new Move(7, 8)); // Trắng
            assertThat(gameState.getMoveCount()).isEqualTo(2);

            // Undo nước của Trắng
            gameState.undoMove();
            assertThat(gameState.getMoveCount()).isEqualTo(1);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.WHITE);

            // Trắng đánh lại vào ô khác qua GameEngine
            GameStatus status = gameEngine.playMove(gameState, new Move(8, 8));
            assertThat(status).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(gameState.getBoard().getCell(8, 8)).isEqualTo(Board.WHITE);
            assertThat(gameState.getCurrentPlayer()).isEqualTo(Board.BLACK);
        }
    }
}

