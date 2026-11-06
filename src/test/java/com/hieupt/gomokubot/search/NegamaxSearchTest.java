package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.evaluation.Evaluator;
import com.hieupt.gomokubot.hash.TranspositionTable;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import com.hieupt.gomokubot.tactical.ThreatDetector;
import com.hieupt.gomokubot.tactical.VCFSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NegamaxSearch Tests")
class NegamaxSearchTest {

    private NegamaxSearch searchEngine;
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

        searchEngine = new NegamaxSearch(
                evaluator,
                moveGenerator,
                winChecker,
                zobristHash,
                transpositionTable
        );
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số Dependency")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo NegamaxSearch thành công")
        void testSuccessfulInitialization() {
            assertThat(searchEngine).isNotNull();
        }

        @Test
        @DisplayName("Evaluator null ném NullPointerException")
        void testNullEvaluatorThrowsException() {
            assertThatThrownBy(() -> new NegamaxSearch(null, moveGenerator, winChecker, zobristHash, transpositionTable))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Evaluator must not be null");
        }

        @Test
        @DisplayName("MoveGenerator null ném NullPointerException")
        void testNullMoveGeneratorThrowsException() {
            assertThatThrownBy(() -> new NegamaxSearch(evaluator, null, winChecker, zobristHash, transpositionTable))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Move generator must not be null");
        }

        @Test
        @DisplayName("WinChecker null ném NullPointerException")
        void testNullWinCheckerThrowsException() {
            assertThatThrownBy(() -> new NegamaxSearch(evaluator, moveGenerator, null, zobristHash, transpositionTable))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("WinChecker must not be null");
        }

        @Test
        @DisplayName("ZobristHash null ném NullPointerException")
        void testNullZobristHashThrowsException() {
            assertThatThrownBy(() -> new NegamaxSearch(evaluator, moveGenerator, winChecker, null, transpositionTable))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("ZobristHash must not be null");
        }

        @Test
        @DisplayName("TranspositionTable null ném NullPointerException")
        void testNullTranspositionTableThrowsException() {
            assertThatThrownBy(() -> new NegamaxSearch(evaluator, moveGenerator, winChecker, zobristHash, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("TranspositionTable must not be null");
        }

        @Test
        @DisplayName("Khởi tạo NegamaxSearch với VCFSearch tùy biến thành công")
        void testConstructorWithCustomVCFSearch() {
            VCFSearch customVcf = new VCFSearch(new ThreatDetector(winChecker, moveGenerator), winChecker, zobristHash);
            NegamaxSearch customEngine = new NegamaxSearch(
                    evaluator,
                    moveGenerator,
                    winChecker,
                    zobristHash,
                    transpositionTable,
                    customVcf
            );
            assertThat(customEngine.getVcfSearch()).isSameAs(customVcf);
        }

        @Test
        @DisplayName("Kích thước ZobristHash và TranspositionTable không khớp ném IllegalArgumentException")
        void testMismatchedDimensionsThrowsException() {
            TranspositionTable mismatchedTable = new TranspositionTable(10, 10, 1024);

            assertThatThrownBy(() -> new NegamaxSearch(evaluator, moveGenerator, winChecker, zobristHash, mismatchedTable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dimensions must match");
        }
    }

    @Nested
    @DisplayName("Kiểm tra tham số khi gọi search")
    class SearchValidationTests {

        @Test
        @DisplayName("Board null ném NullPointerException")
        void testSearchNullBoardThrowsException() {
            assertThatThrownBy(() -> searchEngine.search(null, Board.BLACK, SearchLimits.depthLimit(2)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }

        @Test
        @DisplayName("SearchLimits null ném NullPointerException")
        void testSearchNullLimitsThrowsException() {
            assertThatThrownBy(() -> searchEngine.search(board, Board.BLACK, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("SearchLimits must not be null");
        }

        @ParameterizedTest(name = "Current player không hợp lệ: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("Current player không hợp lệ ném IllegalArgumentException")
        void testSearchInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> searchEngine.search(board, invalidPlayer, SearchLimits.depthLimit(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid current player");
        }

        @Test
        @DisplayName("Kích thước Board không khớp cấu hình search ném IllegalArgumentException")
        void testSearchMismatchedBoardDimensionsThrowsException() {
            Board smallBoard = new Board(10, 10);

            assertThatThrownBy(() -> searchEngine.search(smallBoard, Board.BLACK, SearchLimits.depthLimit(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Board dimensions do not match search configuration");
        }
    }

    @Nested
    @DisplayName("Tìm kiếm nước đi cơ bản")
    class BasicSearchTests {

        @Test
        @DisplayName("Bàn cờ rỗng tìm ra nước đi ở chính giữa (7, 7)")
        void testSearchOnEmptyBoardReturnsCenterMove() {
            SearchResult result = searchEngine.search(
                    board,
                    Board.BLACK,
                    SearchLimits.depthLimit(1)
            );

            assertThat(result.completed()).isTrue();
            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.bestMove()).isEqualTo(new Move(7, 7));
            assertThat(result.completedDepth()).isEqualTo(1);
        }

        @Test
        @DisplayName("Tìm thấy nước đi thắng ngay lập tức (Mate in 1)")
        void testSearchFindsImmediateWin() {
            // Đặt 4 quân Đen: (3, 7), (4, 7), (5, 7), (6, 7)
            for (int x = 3; x <= 6; x++) {
                board.placeStone(x, 7, Board.BLACK);
            }
            board.placeStone(0, 0, Board.WHITE);

            SearchResult result = searchEngine.search(
                    board,
                    Board.BLACK,
                    SearchLimits.depthLimit(2)
            );

            assertThat(result.completed()).isTrue();
            assertThat(result.score()).isEqualTo(1_000_000);
            assertThat(result.bestMove()).isIn(new Move(7, 7), new Move(2, 7));
        }

        @Test
        @DisplayName("Tìm thấy nước chặn đe dọa thắng của đối phương")
        void testSearchFindsBlockingMove() {
            // Trắng có 4 quân: (3, 5), (4, 5), (5, 5), (6, 5)
            // Ô (2, 5) bị chặn bởi Đen
            board.placeStone(2, 5, Board.BLACK);
            for (int x = 3; x <= 6; x++) {
                board.placeStone(x, 5, Board.WHITE);
            }

            // Đến lượt Đen, Đen bắt buộc phải chọn (7, 5) để chặn
            SearchResult result = searchEngine.search(
                    board,
                    Board.BLACK,
                    SearchLimits.depthLimit(2)
            );

            assertThat(result.completed()).isTrue();
            assertThat(result.bestMove()).isEqualTo(new Move(7, 5));
        }

        @Test
        @DisplayName("Tìm thấy và thực thi nước đi chiến thắng VCF đa bước (Multi-step VCF)")
        void testSearchFindsVCFWin() {
            // Thiết lập thế cờ VCF 5 plies:
            // Row 5: Trắng chặn (2, 5), Đen (3, 5), (4, 5), (5, 5) -> Nước Four duy nhất là (6, 5)
            // Cột 6: (6, 6), (6, 7) được kích hoạt bởi (6, 5)
            board.placeStone(2, 5, Board.WHITE);
            board.placeStone(3, 5, Board.BLACK);
            board.placeStone(4, 5, Board.BLACK);
            board.placeStone(5, 5, Board.BLACK);

            board.placeStone(6, 6, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            SearchResult result = searchEngine.search(
                    board,
                    Board.BLACK,
                    SearchLimits.depthLimit(2)
            );

            assertThat(result.completed()).isTrue();
            assertThat(result.bestMove()).isEqualTo(new Move(6, 5));
            assertThat(result.score()).isEqualTo(1_000_000);
        }

        @Test
        @DisplayName("Tìm thấy và thực thi nước đi chiến thắng VCT qua đòn phối hợp")
        void testSearchFindsVCTWin() {
            board.placeStone(7, 3, Board.WHITE);
            board.placeStone(7, 4, Board.BLACK);
            board.placeStone(7, 5, Board.BLACK);
            board.placeStone(7, 6, Board.BLACK);

            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            SearchResult result = searchEngine.search(
                    board,
                    Board.BLACK,
                    SearchLimits.depthLimit(2)
            );

            assertThat(result.completed()).isTrue();
            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.bestMove()).isEqualTo(new Move(7, 7));
        }
    }

    @Nested
    @DisplayName("Các loại SearchLimits (Depth, Nodes, Time)")
    class SearchLimitsBehaviorTests {

        @Test
        @DisplayName("Tìm kiếm hoàn tất với depthLimit")
        void testDepthLimitSearch() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            SearchResult result = searchEngine.search(
                    board,
                    Board.BLACK,
                    SearchLimits.depthLimit(2)
            );

            assertThat(result.completed()).isTrue();
            assertThat(result.completedDepth()).isEqualTo(2);
            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.nodesSearched()).isPositive();
        }

        @Test
        @DisplayName("Dừng tìm kiếm khi vượt quá nodeLimit")
        void testNodeLimitSearch() {
            board.placeStone(7, 7, Board.BLACK);

            SearchResult result = searchEngine.search(
                    board,
                    Board.WHITE,
                    SearchLimits.nodeLimit(5L)
            );

            assertThat(result.completed()).isFalse();
            assertThat(result.completedDepth()).isZero();
            assertThat(result.nodesSearched()).isGreaterThanOrEqualTo(5L);
        }

        @Test
        @DisplayName("Giới hạn thời gian timeLimit hoạt động và ghi nhận elapsedTime")
        void testTimeLimitSearch() {
            board.placeStone(7, 7, Board.BLACK);

            SearchResult result = searchEngine.search(
                    board,
                    Board.WHITE,
                    SearchLimits.timeLimit(100L)
            );

            assertThat(result.elapsedTimeMillis()).isGreaterThanOrEqualTo(0L);
            assertThat(result.hasBestMove()).isTrue();
        }

        @Test
        @DisplayName("Tìm kiếm trong trung cuộc phức tạp tuân thủ chặt chẽ ngân sách thời gian")
        void testSearchRespectsTimeLimitInComplexMiddleGame() {
            // Thiết lập thế cờ trung cuộc với 6 quân cờ tạo nhiều Three-threats
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 7, Board.BLACK);
            board.placeStone(6, 8, Board.WHITE);
            board.placeStone(8, 8, Board.BLACK);
            board.placeStone(9, 9, Board.WHITE);

            long timeLimitMs = 400L;
            SearchResult result = searchEngine.search(
                    board,
                    Board.BLACK,
                    SearchLimits.timeLimit(timeLimitMs)
            );

            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.bestMove()).isNotNull();
            // Đảm bảo thời gian không vượt quá giới hạn đáng kể (cho phép dung sai JVM/OS nhỏ)
            assertThat(result.elapsedTimeMillis()).isLessThanOrEqualTo(timeLimitMs + 300L);
        }

        @Test
        @DisplayName("Luôn đảm bảo trả về nước đi hợp lệ ngay cả khi timeout cực ngắn")
        void testGuaranteedMoveEvenOnImmediateTimeout() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 8, Board.BLACK);

            SearchResult result = searchEngine.search(
                    board,
                    Board.WHITE,
                    SearchLimits.timeLimit(1L)
            );

            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.bestMove()).isNotNull();
            assertThat(board.isEmpty(result.bestMove().x(), result.bestMove().y())).isTrue();
        }
    }

    @Nested
    @DisplayName("Xóa Transposition Table")
    class TranspositionTableClearTests {

        @Test
        @DisplayName("clearTranspositionTable xóa sạch cache thành công")
        void testClearTranspositionTable() {
            board.placeStone(7, 7, Board.BLACK);
            searchEngine.search(board, Board.WHITE, SearchLimits.depthLimit(2));

            // Xóa cache
            searchEngine.clearTranspositionTable();

            // Tìm kiếm lại vẫn hoạt động bình thường
            SearchResult result = searchEngine.search(board, Board.WHITE, SearchLimits.depthLimit(2));
            assertThat(result.completed()).isTrue();
            assertThat(result.hasBestMove()).isTrue();
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật")
    class RectangularBoardTests {

        @Test
        @DisplayName("Tìm kiếm chính xác trên bàn cờ chữ nhật 10x20")
        void testSearchOnRectangularBoard() {
            Board rectBoard = new Board(10, 20);
            ZobristHash rectZobrist = new ZobristHash(10, 20);
            TranspositionTable rectTable = new TranspositionTable(10, 20, 2048);

            NegamaxSearch rectSearch = new NegamaxSearch(
                    evaluator,
                    moveGenerator,
                    winChecker,
                    rectZobrist,
                    rectTable
            );

            // Bàn cờ chữ nhật rỗng trả về trung tâm (5, 10)
            SearchResult result = rectSearch.search(
                    rectBoard,
                    Board.BLACK,
                    SearchLimits.depthLimit(1)
            );

            assertThat(result.completed()).isTrue();
            assertThat(result.bestMove()).isEqualTo(new Move(5, 10));
        }
    }
}

