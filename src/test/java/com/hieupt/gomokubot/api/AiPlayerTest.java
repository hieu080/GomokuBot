package com.hieupt.gomokubot.api;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AiPlayer Tests")
class AiPlayerTest {

    private AiPlayer aiPlayer;
    private Board board;

    @BeforeEach
    void setUp() {
        aiPlayer = new AiPlayer(15, 15);
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo mặc định 15x15 thành công")
        void testDefaultConstructor() {
            AiPlayer defaultPlayer = new AiPlayer();
            assertThat(defaultPlayer.getSearchEngine()).isNotNull();
            assertThat(defaultPlayer.getTimeManager()).isNotNull();
            assertThat(defaultPlayer.getDefaultMaxDepth()).isEqualTo(12);
        }

        @Test
        @DisplayName("Dependency null ném NullPointerException")
        void testNullDependenciesThrowException() {
            assertThatThrownBy(() -> new AiPlayer(null, aiPlayer.getTimeManager(), 10))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AiPlayer(aiPlayer.getSearchEngine(), null, 10))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AiPlayer(aiPlayer.getSearchEngine(), aiPlayer.getTimeManager(), 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("findBestMove với Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> aiPlayer.findBestMove(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Tìm kiếm nước đi của AI")
    class MoveFindingTests {

        @Test
        @DisplayName("Bàn cờ rỗng AI đánh vào trung tâm (7, 7)")
        void testEmptyBoardReturnsCenterMove() {
            SearchResult result = aiPlayer.findBestMove(board, Board.BLACK);

            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.bestMove()).isEqualTo(new Move(7, 7));
        }

        @Test
        @DisplayName("AI tìm thấy nước đi kết thúc trận đấu (Mate in 1)")
        void testImmediateWin() {
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(0, 0, Board.WHITE);

            SearchResult result = aiPlayer.findBestMove(board, Board.BLACK);

            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.bestMove()).isIn(new Move(2, 7), new Move(7, 7));
        }

        @Test
        @DisplayName("Cấp độ Kiện tướng (MASTER) tìm ra nước đi hợp lệ và tôn trọng thời gian tối đa")
        void testMasterLevelMoveAndRespectTimeBudget() {
            AiPlayer masterPlayer = new AiPlayer(15, 15, BotLevel.MASTER);

            // Bàn cờ trung cuộc có 6 quân
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 7, Board.BLACK);
            board.placeStone(6, 8, Board.WHITE);
            board.placeStone(8, 8, Board.BLACK);
            board.placeStone(9, 9, Board.WHITE);

            long start = System.currentTimeMillis();
            SearchResult result = masterPlayer.findBestMove(board, Board.BLACK);
            long elapsed = System.currentTimeMillis() - start;

            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.bestMove()).isNotNull();
            assertThat(board.isEmpty(result.bestMove().x(), result.bestMove().y())).isTrue();
            // MASTER mặc định 4000ms, không được vượt quá 4500ms
            assertThat(elapsed).isLessThanOrEqualTo(4500L);
        }
    }
}

