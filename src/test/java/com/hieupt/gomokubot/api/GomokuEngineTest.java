package com.hieupt.gomokubot.api;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.GameState;
import com.hieupt.gomokubot.core.GameStatus;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.search.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GomokuEngine Tests")
class GomokuEngineTest {

    @Nested
    @DisplayName("Khởi tạo ván đấu (Initialization)")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo mặc định: người chơi quân Đen, bàn cờ trống, lượt người chơi")
        void testDefaultInitialization() {
            GomokuEngine engine = new GomokuEngine();

            assertThat(engine.getHumanPlayer()).isEqualTo(Board.BLACK);
            assertThat(engine.getAiPlayerColor()).isEqualTo(Board.WHITE);
            assertThat(engine.getGameState()).isNotNull();
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(0);
            assertThat(engine.isHumanTurn()).isTrue();
            assertThat(engine.isAiTurn()).isFalse();
            assertThat(engine.getLastSearchResult()).isNull();
        }

        @Test
        @DisplayName("Khởi tạo người chơi chọn quân Trắng: AI đi trước nước đầu tiên")
        void testInitializationHumanAsWhite() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.WHITE);

            assertThat(engine.getHumanPlayer()).isEqualTo(Board.WHITE);
            assertThat(engine.getAiPlayerColor()).isEqualTo(Board.BLACK);
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(1);
            assertThat(engine.isHumanTurn()).isTrue();
            assertThat(engine.isAiTurn()).isFalse();
            assertThat(engine.getLastSearchResult()).isNotNull();
            // Nước đầu tiên trên bàn cờ trống của AI luôn là ô trung tâm (7, 7)
            Move firstMove = engine.getGameState().getLastMove();
            assertThat(firstMove).isEqualTo(new Move(7, 7));
        }

        @Test
        @DisplayName("Khởi tạo với humanPlayer không hợp lệ ném IllegalArgumentException")
        void testInvalidHumanPlayer() {
            assertThatThrownBy(() -> new GomokuEngine(15, 15, Board.EMPTY))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new GomokuEngine(15, 15, (byte) 99))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Thực hiện nước đi (Playing Moves)")
    class MoveExecutionTests {

        @Test
        @DisplayName("Người đi quân Đen đánh 1 nước hợp lệ, AI tự động đáp trả quân Trắng")
        void testHumanMoveTriggersAiMove() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.BLACK);

            GameStatus status = engine.playHumanMove(7, 7);

            assertThat(status).isEqualTo(GameStatus.IN_PROGRESS);
            // Cả người và AI đều đã đi -> tổng 2 nước
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(2);
            assertThat(engine.getGameState().getBoard().getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(engine.getLastSearchResult()).isNotNull();
            assertThat(engine.getLastSearchResult().hasBestMove()).isTrue();
            assertThat(engine.isHumanTurn()).isTrue();
        }

        @Test
        @DisplayName("Ném IllegalStateException khi người chơi đánh vào lượt AI hoặc ván kết thúc")
        void testIllegalHumanMoveThrowsException() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.BLACK);

            // Giả lập ván đã kết thúc bằng cách thiết lập cờ thắng
            Board board = engine.getGameState().getBoard();
            board.placeStone(0, 0, Board.BLACK);
            board.placeStone(1, 0, Board.BLACK);
            board.placeStone(2, 0, Board.BLACK);
            board.placeStone(3, 0, Board.BLACK);

            // Nước thắng
            engine.playHumanMove(4, 0);
            assertThat(engine.getGameState().getGameStatus()).isEqualTo(GameStatus.WON);
            assertThat(engine.getGameState().getWinner()).isEqualTo(Board.BLACK);

            // Đánh tiếp khi game over
            assertThatThrownBy(() -> engine.playHumanMove(5, 5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already over");
        }

        @Test
        @DisplayName("Nước đi của người thắng trận không kích hoạt lượt đi tiếp theo của AI")
        void testHumanWinningMoveDoesNotTriggerAi() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.BLACK);
            Board board = engine.getGameState().getBoard();
            board.placeStone(0, 0, Board.BLACK);
            board.placeStone(1, 0, Board.BLACK);
            board.placeStone(2, 0, Board.BLACK);
            board.placeStone(3, 0, Board.BLACK);

            GameStatus status = engine.playHumanMove(4, 0);

            assertThat(status).isEqualTo(GameStatus.WON);
            assertThat(engine.getGameState().getWinner()).isEqualTo(Board.BLACK);
            // Move count chỉ tăng 1 vì AI không đi
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Hoàn tác nước đi (Undo)")
    class UndoTests {

        @Test
        @DisplayName("Hoàn tác khi chưa có nước đi nào trả về false")
        void testUndoEmptyBoard() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.BLACK);
            assertThat(engine.undo()).isFalse();
        }

        @Test
        @DisplayName("Hoàn tác rút lại cả nước của AI và nước của người chơi")
        void testUndoBothMoves() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.BLACK);
            engine.playHumanMove(7, 7);
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(2);

            boolean undone = engine.undo();

            assertThat(undone).isTrue();
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(0);
            assertThat(engine.getGameState().getBoard().isEmpty()).isTrue();
            assertThat(engine.isHumanTurn()).isTrue();
        }

        @Test
        @DisplayName("Hoàn tác khi chỉ có 1 nước đi (AI đi trước ở lượt đầu)")
        void testUndoSingleMove() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.WHITE);
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(1);

            boolean undone = engine.undo();

            assertThat(undone).isTrue();
            assertThat(engine.getGameState().getMoveCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Khởi động lại ván cờ (startNewGame)")
    class ResetTests {

        @Test
        @DisplayName("Bắt đầu ván mới xóa sạch bàn cờ và TT")
        void testStartNewGameResetsBoard() {
            GomokuEngine engine = new GomokuEngine(15, 15, Board.BLACK);
            engine.playHumanMove(7, 7);
            assertThat(engine.getGameState().getMoveCount()).isGreaterThan(0);

            engine.startNewGame(15, 15, Board.BLACK);

            assertThat(engine.getGameState().getMoveCount()).isEqualTo(0);
            assertThat(engine.getGameState().getBoard().isEmpty()).isTrue();
            assertThat(engine.getLastSearchResult()).isNull();
        }
    }
}
