package com.hieupt.gomokubot.web.controller;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.web.dto.GameStateResponse;
import com.hieupt.gomokubot.web.dto.MoveRequest;
import com.hieupt.gomokubot.web.dto.NewGameRequest;
import com.hieupt.gomokubot.web.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameApiController.class)
@DisplayName("GameApiController Tests")
class GameApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameService gameService;

    private GameStateResponse createMockState(int moveCount, byte currentPlayer) {
        return createMockState(moveCount, currentPlayer, Board.BLACK);
    }

    private GameStateResponse createMockState(int moveCount, byte currentPlayer, byte humanPlayer) {
        int[][] board = new int[15][15];
        byte aiPlayer = humanPlayer == Board.BLACK ? Board.WHITE : Board.BLACK;
        return new GameStateResponse(
                15,
                15,
                board,
                currentPlayer,
                humanPlayer,
                aiPlayer,
                "IN_PROGRESS",
                Board.EMPTY,
                null,
                null,
                moveCount,
                true,
                false,
                null,
                "HARD",
                "Khó (Thi đấu)"
        );
    }

    @Test
    @DisplayName("GET /api/game/state trả về 200 OK với trạng thái ván cờ")
    void testGetState() throws Exception {
        when(gameService.getGameState()).thenReturn(createMockState(0, Board.BLACK));

        mockMvc.perform(get("/api/game/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").value(15))
                .andExpect(jsonPath("$.height").value(15))
                .andExpect(jsonPath("$.moveCount").value(0))
                .andExpect(jsonPath("$.humanPlayer").value((int) Board.BLACK))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(gameService).getGameState();
    }

    @Test
    @DisplayName("POST /api/game/new tạo ván cờ mới trả về 200 OK")
    void testNewGame() throws Exception {
        NewGameRequest request = new NewGameRequest(15, 15, Board.WHITE, 1500L);
        when(gameService.startNewGame(any())).thenReturn(createMockState(1, Board.WHITE));

        mockMvc.perform(post("/api/game/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moveCount").value(1));

        verify(gameService).startNewGame(any());
    }

    @Test
    @DisplayName("POST /api/game/move đi một nước hợp lệ trả về 200 OK")
    void testMove() throws Exception {
        MoveRequest request = new MoveRequest(7, 7);
        when(gameService.playHumanMove(anyInt(), anyInt())).thenReturn(createMockState(2, Board.BLACK));

        mockMvc.perform(post("/api/game/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moveCount").value(2));

        verify(gameService).playHumanMove(7, 7);
    }

    @Test
    @DisplayName("POST /api/game/undo hoàn tác nước đi trả về 200 OK")
    void testUndo() throws Exception {
        when(gameService.undo()).thenReturn(createMockState(0, Board.BLACK));

        mockMvc.perform(post("/api/game/undo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moveCount").value(0));

        verify(gameService).undo();
    }

    @Test
    @DisplayName("POST /api/game/switch-side đổi bên / đổi lượt trả về 200 OK")
    void testSwitchSide() throws Exception {
        when(gameService.switchSide()).thenReturn(createMockState(1, Board.WHITE, Board.WHITE));

        mockMvc.perform(post("/api/game/switch-side"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.humanPlayer").value((int) Board.WHITE));

        verify(gameService).switchSide();
    }
}

