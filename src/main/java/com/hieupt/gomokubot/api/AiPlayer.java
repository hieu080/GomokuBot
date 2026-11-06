package com.hieupt.gomokubot.api;

import com.hieupt.gomokubot.book.OpeningBook;
import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.evaluation.Evaluator;
import com.hieupt.gomokubot.hash.TranspositionTable;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import com.hieupt.gomokubot.search.NegamaxSearch;
import com.hieupt.gomokubot.search.SearchLimits;
import com.hieupt.gomokubot.search.SearchResult;
import com.hieupt.gomokubot.time.TimeManager;

import java.util.Objects;
import java.util.Optional;

public class AiPlayer {

    private final NegamaxSearch searchEngine;
    private final TimeManager timeManager;
    private final BotLevel botLevel;
    private final int defaultMaxDepth;
    private final OpeningBook openingBook;

    public AiPlayer() {
        this(15, 15, BotLevel.MASTER);
    }

    public AiPlayer(int width, int height) {
        this(width, height, BotLevel.MASTER);
    }

    public AiPlayer(int width, int height, BotLevel level) {
        this(
                new NegamaxSearch(
                        new Evaluator(),
                        new CandidateMoveGenerator(2),
                        new WinChecker(5),
                        new ZobristHash(width, height),
                        new TranspositionTable(width, height, 65536)
                ),
                new TimeManager(level != null ? level.getDefaultTimeMs() : 2000L),
                level != null ? level : BotLevel.HARD,
                new OpeningBook()
        );
    }

    public AiPlayer(NegamaxSearch searchEngine, TimeManager timeManager, int defaultMaxDepth) {
        this(searchEngine, timeManager, BotLevel.HARD, defaultMaxDepth, new OpeningBook());
    }

    public AiPlayer(
            NegamaxSearch searchEngine,
            TimeManager timeManager,
            BotLevel botLevel,
            OpeningBook openingBook
    ) {
        this(
                searchEngine,
                timeManager,
                botLevel,
                botLevel != null ? botLevel.getMaxDepth() : 12,
                openingBook
        );
    }

    public AiPlayer(
            NegamaxSearch searchEngine,
            TimeManager timeManager,
            BotLevel botLevel,
            int defaultMaxDepth,
            OpeningBook openingBook
    ) {
        this.searchEngine = Objects.requireNonNull(searchEngine, "NegamaxSearch must not be null");
        this.timeManager = Objects.requireNonNull(timeManager, "TimeManager must not be null");
        if (defaultMaxDepth < 1) {
            throw new IllegalArgumentException("Default max depth must be at least 1");
        }
        this.botLevel = botLevel != null ? botLevel : BotLevel.HARD;
        this.defaultMaxDepth = defaultMaxDepth;
        this.openingBook = openingBook;

        // Cập nhật cấu hình VCF/VCT của search engine theo level
        this.searchEngine.setVcfEnabled(this.botLevel.isVcfEnabled());
        this.searchEngine.setVctEnabled(this.botLevel.isVctEnabled());
    }

    public SearchResult findBestMove(Board board, byte player) {
        Objects.requireNonNull(board, "Board must not be null");

        // 1. Kiểm tra Sách khai cuộc (Opening Book) nếu được bật ở cấp độ hiện tại
        if (botLevel.isUseOpeningBook() && openingBook != null) {
            Optional<Move> bookMove = openingBook.getMove(board, player);
            if (bookMove.isPresent()) {
                return new SearchResult(
                        bookMove.get(),
                        0,
                        1,
                        1,
                        0L,
                        true
                );
            }
        }

        SearchLimits limits = timeManager.createSearchLimits(board, defaultMaxDepth);
        return findBestMove(board, player, limits);
    }

    public SearchResult findBestMove(Board board, byte player, SearchLimits limits) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(limits, "SearchLimits must not be null");

        searchEngine.setVcfEnabled(botLevel.isVcfEnabled());
        searchEngine.setVctEnabled(botLevel.isVctEnabled());

        SearchResult result = searchEngine.search(board, player, limits);
        timeManager.updateRemainingTime(result.elapsedTimeMillis());
        return result;
    }

    public NegamaxSearch getSearchEngine() {
        return searchEngine;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public BotLevel getBotLevel() {
        return botLevel;
    }

    public int getDefaultMaxDepth() {
        return defaultMaxDepth;
    }

    public OpeningBook getOpeningBook() {
        return openingBook;
    }
}
