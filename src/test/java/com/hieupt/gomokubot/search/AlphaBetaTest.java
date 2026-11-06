package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.evaluation.Evaluator;
import com.hieupt.gomokubot.hash.TranspositionTable;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlphaBeta Search Tests")
class AlphaBetaTest {

    private static final int INFINITY = 2_000_000;
    private static final int MATE_SCORE = 1_000_000;

    private AlphaBeta alphaBeta;
    private Board board;
    private Evaluator evaluator;
    private CandidateMoveGenerator moveGenerator;
    private WinChecker winChecker;
    private ZobristHash zobristHash;
    private TranspositionTable transpositionTable;

    @BeforeEach
    void setUp() {
        board = new Board(15, 15);
        evaluator = new Evaluator();
        moveGenerator = new CandidateMoveGenerator(2);
        winChecker = new WinChecker(5);
        zobristHash = new ZobristHash(15, 15);
        transpositionTable = new TranspositionTable(15, 15, 4096);

        alphaBeta = new AlphaBeta(
                evaluator,
                moveGenerator,
                winChecker,
                zobristHash,
                transpositionTable
        );
    }

    @Nested
    @DisplayName("Nước đi thắng ngay lập tức (Mate in 1)")
    class ImmediateWinTests {

        @Test
        @DisplayName("Tìm thấy và chọn nước đi tạo 5 quân liên tiếp cho Đen")
        void testFindsImmediateWinningMove() {
            // Đặt 4 quân Đen: (3, 7), (4, 7), (5, 7), (6, 7)
            // Nước thắng là (7, 7) hoặc (2, 7)
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            // Thêm 1 quân Trắng ở xa để bàn cờ không chỉ có Đen
            board.placeStone(0, 0, Board.WHITE);

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(2));
            long hash = zobristHash.computeHash(board, Board.BLACK);

            int score = alphaBeta.search(
                    board,
                    Board.BLACK,
                    2,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            // Nước đi đạt điểm thắng tuyệt đối MATE_SCORE
            assertThat(score).isEqualTo(MATE_SCORE);
            assertThat(context.rootBestMove).isIn(new Move(7, 7), new Move(2, 7));
        }

        @Test
        @DisplayName("Trắng tìm thấy nước đi thắng ngay lập tức")
        void testWhiteImmediateWin() {
            // Đặt 4 quân Trắng hàng dọc: (5, 3), (5, 4), (5, 5), (5, 6)
            board.placeStone(5, 3, Board.WHITE);
            board.placeStone(5, 4, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(5, 6, Board.WHITE);

            board.placeStone(0, 0, Board.BLACK);

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(2));
            long hash = zobristHash.computeHash(board, Board.WHITE);

            int score = alphaBeta.search(
                    board,
                    Board.WHITE,
                    2,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            assertThat(score).isEqualTo(MATE_SCORE);
            assertThat(context.rootBestMove).isIn(new Move(5, 2), new Move(5, 7));
        }
    }

    @Nested
    @DisplayName("Chặn nước thắng của đối phương (Blocking Opponent Win)")
    class BlockOpponentWinTests {

        @Test
        @DisplayName("Đen nhận diện mối đe dọa 4 quân của Trắng và bắt buộc phải chặn")
        void testBlocksOpponentFourInARow() {
            // Trắng có 4 quân: (3, 7), (4, 7), (5, 7), (6, 7) và ô (2, 7) đã bị chặn bởi mép hoặc quân khác
            board.placeStone(2, 7, Board.BLACK); // Chặn 1 đầu
            board.placeStone(3, 7, Board.WHITE);
            board.placeStone(4, 7, Board.WHITE);
            board.placeStone(5, 7, Board.WHITE);
            board.placeStone(6, 7, Board.WHITE);

            // Nước duy nhất của Trắng để thắng là (7, 7). Đến lượt Đen đi.
            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(2));
            long hash = zobristHash.computeHash(board, Board.BLACK);

            alphaBeta.search(
                    board,
                    Board.BLACK,
                    2,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            // Đen bắt buộc phải chọn (7, 7) để chặn
            assertThat(context.rootBestMove).isEqualTo(new Move(7, 7));
        }
    }

    @Nested
    @DisplayName("Cắt tỉa Alpha-Beta và Transposition Table")
    class PruningAndTranspositionTableTests {

        @Test
        @DisplayName("Transposition Table Exact Bound trả về ngay mà không cần duyệt lại nhánh")
        void testTranspositionTableExactHitReturnsImmediately() {
            long hash = zobristHash.computeHash(board, Board.BLACK);

            // Giả lập TT đã lưu thế cờ này tại depth = 4 với EXACT score = 777
            transpositionTable.store(
                    hash,
                    4,
                    777,
                    TranspositionTable.Bound.EXACT,
                    new Move(7, 7)
            );

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(3));

            int score = alphaBeta.search(
                    board,
                    Board.BLACK,
                    3, // depth <= ttDepth (3 <= 4)
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            assertThat(score).isEqualTo(777);
            // Chỉ ghé thăm root node mà không duyệt mở rộng thêm nốt con nào
            assertThat(context.nodesSearched).isEqualTo(1L);
        }

        @Test
        @DisplayName("TT Move được ưu tiên thử trước (Move Ordering) giúp cắt tỉa nhanh")
        void testTTMoveOrderingEfficiency() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            long hash = zobristHash.computeHash(board, Board.BLACK);

            // Giả lập TT gợi ý ttMove = (8, 8)
            transpositionTable.store(
                    hash,
                    2,
                    100,
                    TranspositionTable.Bound.LOWER_BOUND,
                    new Move(8, 8)
            );

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(2));

            alphaBeta.search(
                    board,
                    Board.BLACK,
                    2,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            assertThat(context.nodesSearched).isPositive();
        }

        @Test
        @DisplayName("Alpha-Beta pruning giảm số lượng nốt duyệt so với duyệt mù quáng")
        void testAlphaBetaPruningReducesNodes() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(8, 8, Board.WHITE);
            board.placeStone(6, 6, Board.BLACK);

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(2));
            long hash = zobristHash.computeHash(board, Board.WHITE);

            alphaBeta.search(
                    board,
                    Board.WHITE,
                    2,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            // Kiểm tra số nốt duyệt được kiểm soát tốt
            assertThat(context.nodesSearched).isLessThan(2000L);
        }

        @Test
        @DisplayName("Previous best move được truyền vào search hỗ trợ move ordering tại root")
        void testPreviousBestMoveAssistsSearch() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(2));
            long hash = zobristHash.computeHash(board, Board.BLACK);
            Move previousBest = new Move(6, 6);

            int score = alphaBeta.search(
                    board,
                    Board.BLACK,
                    2,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    previousBest,
                    context
            );

            assertThat(context.rootBestMove).isNotNull();
            assertThat(context.nodesSearched).isPositive();
        }

        @Test
        @DisplayName("Khởi tạo AlphaBeta với MoveOrdering tùy biến trực tiếp")
        void testConstructorWithMoveOrdering() {
            MoveOrdering customOrdering = new MoveOrdering(moveGenerator, winChecker);
            AlphaBeta customAlphaBeta = new AlphaBeta(
                    evaluator,
                    customOrdering,
                    winChecker,
                    zobristHash,
                    transpositionTable
            );

            board.placeStone(7, 7, Board.BLACK);
            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(1));
            long hash = zobristHash.computeHash(board, Board.WHITE);

            int score = customAlphaBeta.search(
                    board,
                    Board.WHITE,
                    1,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            assertThat(context.rootBestMove).isNotNull();
        }
    }

    @Nested
    @DisplayName("Điều kiện dừng và Giới hạn tìm kiếm (Search Limits)")
    class SearchLimitTests {

        @Test
        @DisplayName("Dừng tìm kiếm khi vượt quá maxNodes")
        void testStopsWhenMaxNodesReached() {
            board.placeStone(7, 7, Board.BLACK);

            // Giới hạn chỉ duyệt tối đa 10 nốt
            SearchLimits limits = SearchLimits.nodeLimit(10L);
            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(limits);
            long hash = zobristHash.computeHash(board, Board.WHITE);

            alphaBeta.search(
                    board,
                    Board.WHITE,
                    4,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            assertThat(context.stopped).isTrue();
            assertThat(context.nodesSearched).isGreaterThanOrEqualTo(10L);
        }

        @Test
        @DisplayName("Bàn cờ đầy không thể đi tiếp trả về 0")
        void testFullBoardReturnsZero() {
            Board small = new Board(2, 2);
            small.placeStone(0, 0, Board.BLACK);
            small.placeStone(0, 1, Board.WHITE);
            small.placeStone(1, 0, Board.BLACK);
            small.placeStone(1, 1, Board.WHITE);
            assertThat(small.isFull()).isTrue();

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(2));

            int score = alphaBeta.search(
                    small,
                    Board.BLACK,
                    2,
                    -INFINITY,
                    INFINITY,
                    0L,
                    0,
                    context
            );

            assertThat(score).isZero();
        }

        @Test
        @DisplayName("depth <= 0 kích hoạt hàm đánh giá tĩnh Evaluator")
        void testDepthZeroEvaluatesLeafNode() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.BLACK);
            board.placeStone(7, 9, Board.BLACK); // Black có ưu thế

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.timeLimit(1000L));
            long hash = zobristHash.computeHash(board, Board.BLACK);

            int score = alphaBeta.search(
                    board,
                    Board.BLACK,
                    0,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            int expectedEval = evaluator.evaluate(board, Board.BLACK);
            assertThat(score).isEqualTo(expectedEval);
        }
    }

    @Nested
    @DisplayName("Tính toàn vẹn của bàn cờ khi Backtracking (Board Integrity)")
    class BoardIntegrityTests {

        @Test
        @DisplayName("Sau khi hoàn thành search, bàn cờ quay lại trạng thái y hệt ban đầu")
        void testBoardUnchangedAfterSearch() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            int initialOccupied = board.getOccupiedCount();

            AlphaBeta.SearchContext context = new AlphaBeta.SearchContext(SearchLimits.depthLimit(3));
            long hash = zobristHash.computeHash(board, Board.BLACK);

            alphaBeta.search(
                    board,
                    Board.BLACK,
                    3,
                    -INFINITY,
                    INFINITY,
                    hash,
                    0,
                    context
            );

            // Số lượng quân cờ không đổi
            assertThat(board.getOccupiedCount()).isEqualTo(initialOccupied);
            assertThat(board.getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(7, 8)).isEqualTo(Board.WHITE);
            assertThat(board.isEmpty(6, 6)).isTrue();
        }
    }
}
