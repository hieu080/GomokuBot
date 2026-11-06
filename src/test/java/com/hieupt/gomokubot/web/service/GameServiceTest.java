package com.hieupt.gomokubot.web.service;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.web.dto.GameStateResponse;
import com.hieupt.gomokubot.web.dto.NewGameRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameService Tests")
class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();
    }

    @Test
    @DisplayName("Khởi tạo service thành công và tạo bàn cờ mặc định 15x15 với người chơi quân Trắng (AI đi trước)")
    void testInitialState() {
        GameStateResponse state = gameService.getGameState();

        assertThat(state).isNotNull();
        assertThat(state.width()).isEqualTo(15);
        assertThat(state.height()).isEqualTo(15);
        assertThat(state.humanPlayer()).isEqualTo(Board.WHITE);
        assertThat(state.aiPlayer()).isEqualTo(Board.BLACK);
        assertThat(state.isHumanTurn()).isTrue();
        assertThat(state.isGameOver()).isFalse();
        assertThat(state.moveCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Bắt đầu ván mới với người chơi chọn quân Đen (Người đi trước)")
    void testStartNewGameWithBlack() {
        NewGameRequest request = new NewGameRequest(15, 15, Board.BLACK, 1000L);
        GameStateResponse state = gameService.startNewGame(request);

        assertThat(state.humanPlayer()).isEqualTo(Board.BLACK);
        assertThat(state.aiPlayer()).isEqualTo(Board.WHITE);
        assertThat(state.moveCount()).isEqualTo(0);
        assertThat(state.isHumanTurn()).isTrue();
    }

    @Test
    @DisplayName("Bắt đầu ván mới với người chơi chọn quân Trắng (AI đi trước)")
    void testStartNewGameWithWhite() {
        NewGameRequest request = new NewGameRequest(15, 15, Board.WHITE, 1000L);
        GameStateResponse state = gameService.startNewGame(request);

        assertThat(state.humanPlayer()).isEqualTo(Board.WHITE);
        assertThat(state.aiPlayer()).isEqualTo(Board.BLACK);
        assertThat(state.moveCount()).isEqualTo(1);
        assertThat(state.isHumanTurn()).isTrue();
        assertThat(state.lastAiSearch()).isNotNull();
    }

    @Test
    @DisplayName("Người chơi thực hiện nước đi hợp lệ, AI tự động đáp trả")
    void testPlayHumanMoveTriggersAi() {
        // AI đã đi nước 1 (7, 7). Người chơi đánh nước 2 ở ô (7, 8)
        GameStateResponse state = gameService.playHumanMove(7, 8);

        assertThat(state.board()[8][7]).isEqualTo(Board.WHITE);
        assertThat(state.moveCount()).isEqualTo(3);
        assertThat(state.isHumanTurn()).isTrue();
        assertThat(state.lastAiSearch()).isNotNull();
        assertThat(state.lastAiSearch().bestX()).isNotNull();
        assertThat(state.lastAiSearch().bestY()).isNotNull();
        assertThat(state.lastAiSearch().depth()).isGreaterThan(0);
        assertThat(state.lastAiSearch().nodesSearched()).isPositive();
    }

    @Test
    @DisplayName("Người chơi và AI đánh nhiều nước liên tiếp qua mốc khai cuộc (occupied >= 6) không gặp lỗi")
    void testPlayMultipleTurnsBeyondOpening() {
        // Nước 1 đã do AI đánh. Người chơi đánh nước 2: (7, 8) -> AI đáp trả nước 3 (moveCount = 3)
        GameStateResponse s1 = gameService.playHumanMove(7, 8);
        assertThat(s1.moveCount()).isEqualTo(3);

        // Nước 4: người chơi đánh (6, 6) -> AI đáp trả nước 5 (moveCount = 5)
        GameStateResponse s2 = gameService.playHumanMove(6, 6);
        assertThat(s2.moveCount()).isEqualTo(5);

        // Nước 6: người chơi đánh (5, 5) -> AI đáp trả nước 7 (moveCount = 7)
        GameStateResponse s3 = gameService.playHumanMove(5, 5);
        assertThat(s3.moveCount()).isEqualTo(7);

        // Nước 8: kích hoạt VCT search khi occupied >= 6
        GameStateResponse s4 = gameService.playHumanMove(4, 4);
        assertThat(s4.moveCount()).isEqualTo(9);
        assertThat(s4.isGameOver()).isFalse();
    }

    @Test
    @DisplayName("Hoàn tác nước đi (Undo)")
    void testUndo() {
        gameService.playHumanMove(7, 8);
        assertThat(gameService.getGameState().moveCount()).isEqualTo(3);

        GameStateResponse state = gameService.undo();

        // Hoàn tác cả nước của AI và người chơi, quay về trạng thái moveCount = 1 (nước khai cuộc của AI)
        assertThat(state.moveCount()).isEqualTo(1);
        assertThat(state.board()[8][7]).isEqualTo(0);
        assertThat(state.isHumanTurn()).isTrue();
    }

    @Test
    @DisplayName("Chức năng đổi bên / đổi lượt (Switch Side) hoán đổi phe và AI tự động đi tiếp")
    void testSwitchSide() {
        GameStateResponse s1 = gameService.getGameState();
        assertThat(s1.humanPlayer()).isEqualTo(Board.WHITE);
        assertThat(s1.aiPlayer()).isEqualTo(Board.BLACK);
        assertThat(s1.moveCount()).isEqualTo(1); // AI đã đi nước 1

        // Đổi bên: Người chơi thành Đen, AI thành Trắng
        // Hiện tại đang là lượt của Trắng (nước 2), do đó AI (bây giờ là Trắng) sẽ tự động tính và đi nước 2!
        GameStateResponse s2 = gameService.switchSide();
        assertThat(s2.humanPlayer()).isEqualTo(Board.BLACK);
        assertThat(s2.aiPlayer()).isEqualTo(Board.WHITE);
        assertThat(s2.moveCount()).isEqualTo(2); // AI đã đánh nước 2
        assertThat(s2.isHumanTurn()).isTrue(); // Bây giờ là lượt của Người chơi (Đen) đi nước 3
    }
}
