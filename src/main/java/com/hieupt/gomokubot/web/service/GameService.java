package com.hieupt.gomokubot.web.service;

import com.hieupt.gomokubot.api.AiPlayer;
import com.hieupt.gomokubot.api.BotLevel;
import com.hieupt.gomokubot.api.GomokuEngine;
import com.hieupt.gomokubot.book.OpeningBook;
import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.GameEngine;
import com.hieupt.gomokubot.evaluation.Evaluator;
import com.hieupt.gomokubot.hash.TranspositionTable;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import com.hieupt.gomokubot.search.NegamaxSearch;
import com.hieupt.gomokubot.time.TimeManager;
import com.hieupt.gomokubot.web.dto.GameStateResponse;
import com.hieupt.gomokubot.web.dto.NewGameRequest;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private GomokuEngine engine;
    private long currentMoveTimeMs = 2000L;

    public GameService() {
        startNewGame(new NewGameRequest(15, 15, Board.WHITE, 4000L, "MASTER"));
    }

    public synchronized GameStateResponse startNewGame(NewGameRequest request) {
        int width = request != null ? request.resolvedWidth() : 15;
        int height = request != null ? request.resolvedHeight() : 15;
        byte humanPlayer = request != null ? request.resolvedHumanPlayer() : Board.BLACK;
        BotLevel level = request != null ? request.resolvedBotLevel() : BotLevel.HARD;
        this.currentMoveTimeMs = request != null ? request.resolvedMoveTimeMs() : level.getDefaultTimeMs();

        TimeManager timeManager = new TimeManager(currentMoveTimeMs);
        CandidateMoveGenerator moveGenerator = new CandidateMoveGenerator(2);
        WinChecker winChecker = new WinChecker(GomokuEngine.WIN_LENGTH);
        ZobristHash zobristHash = new ZobristHash(width, height);
        TranspositionTable transpositionTable = new TranspositionTable(width, height, 65536);

        NegamaxSearch searchEngine = new NegamaxSearch(
                new Evaluator(),
                moveGenerator,
                winChecker,
                zobristHash,
                transpositionTable
        );

        AiPlayer aiPlayer = new AiPlayer(
                searchEngine,
                timeManager,
                level,
                level.getMaxDepth(),
                new OpeningBook()
        );

        this.engine = new GomokuEngine(
                new GameEngine(GomokuEngine.WIN_LENGTH),
                aiPlayer,
                width,
                height,
                humanPlayer
        );

        return GameStateResponse.from(this.engine);
    }

    public synchronized GameStateResponse getGameState() {
        return GameStateResponse.from(this.engine);
    }

    public synchronized GameStateResponse playHumanMove(int x, int y) {
        if (engine.isHumanTurn()) {
            engine.playHumanMove(x, y);
        }
        return GameStateResponse.from(this.engine);
    }

    public synchronized GameStateResponse playAiMove() {
        if (!engine.getGameState().isGameOver()) {
            engine.playAiMove();
        }
        return GameStateResponse.from(this.engine);
    }

    public synchronized GameStateResponse undo() {
        engine.undo();
        return GameStateResponse.from(this.engine);
    }

    public synchronized GameStateResponse switchSide() {
        engine.switchSide();
        return GameStateResponse.from(this.engine);
    }

    public GomokuEngine getEngine() {
        return engine;
    }
}
