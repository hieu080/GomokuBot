package com.hieupt.gomokubot.web.controller;

import com.hieupt.gomokubot.web.dto.GameStateResponse;
import com.hieupt.gomokubot.web.dto.MoveRequest;
import com.hieupt.gomokubot.web.dto.NewGameRequest;
import com.hieupt.gomokubot.web.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/game")
public class GameApiController {

    private final GameService gameService;

    public GameApiController(GameService gameService) {
        this.gameService = Objects.requireNonNull(gameService, "GameService must not be null");
    }

    @GetMapping("/state")
    public ResponseEntity<GameStateResponse> getState() {
        return ResponseEntity.ok(gameService.getGameState());
    }

    @PostMapping("/new")
    public ResponseEntity<GameStateResponse> newGame(@RequestBody(required = false) NewGameRequest request) {
        return ResponseEntity.ok(gameService.startNewGame(request));
    }

    @PostMapping("/move")
    public ResponseEntity<GameStateResponse> move(@RequestBody MoveRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(gameService.playHumanMove(request.x(), request.y()));
    }

    @PostMapping("/undo")
    public ResponseEntity<GameStateResponse> undo() {
        return ResponseEntity.ok(gameService.undo());
    }

    @PostMapping("/switch-side")
    public ResponseEntity<GameStateResponse> switchSide() {
        return ResponseEntity.ok(gameService.switchSide());
    }

    @PostMapping("/ai-move")
    public ResponseEntity<GameStateResponse> aiMove() {
        return ResponseEntity.ok(gameService.playAiMove());
    }
}

