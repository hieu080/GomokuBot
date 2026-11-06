package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.rules.WinChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VCTSearch Tests")
class VCTSearchTest {

    private VCTSearch vctSearch;
    private VCFSearch vcfSearch;
    private ThreatDetector threatDetector;
    private WinChecker winChecker;
    private ZobristHash zobristHash;
    private Board board;

    @BeforeEach
    void setUp() {
        winChecker = new WinChecker(5);
        threatDetector = new ThreatDetector(winChecker);
        zobristHash = new ZobristHash(15, 15);
        vcfSearch = new VCFSearch(threatDetector, winChecker, zobristHash);
        vctSearch = new VCTSearch(threatDetector, vcfSearch, winChecker, zobristHash);
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo mặc định thành công")
        void testDefaultConstructor() {
            VCTSearch defaultSearch = new VCTSearch();
            assertThat(defaultSearch.getThreatDetector()).isNotNull();
            assertThat(defaultSearch.getVcfSearch()).isNotNull();
            assertThat(defaultSearch.getWinChecker()).isNotNull();
            assertThat(defaultSearch.getZobristHash()).isNotNull();
        }

        @Test
        @DisplayName("Dependency null ném NullPointerException")
        void testNullDependenciesThrowException() {
            assertThatThrownBy(() -> new VCTSearch(null, vcfSearch, winChecker, zobristHash))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new VCTSearch(threatDetector, null, winChecker, zobristHash))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new VCTSearch(threatDetector, vcfSearch, null, zobristHash))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new VCTSearch(threatDetector, vcfSearch, winChecker, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> vctSearch.search(null, Board.BLACK, 5))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "Player không hợp lệ ({0}) ném IllegalArgumentException")
        @ValueSource(bytes = {Board.EMPTY, -1, 3, 100})
        void testInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> vctSearch.search(board, invalidPlayer, 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("maxDepth < 1 ném IllegalArgumentException")
        void testInvalidMaxDepthThrowsException() {
            assertThatThrownBy(() -> vctSearch.search(board, Board.BLACK, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxDepth must be at least 1");
        }
    }

    @Nested
    @DisplayName("Kích hoạt Terminal VCF Solver ngay lập tức")
    class TerminalVCFTests {

        @Test
        @DisplayName("Khi thế cờ đã có VCF trực tiếp, VCT giải quyết ngay qua VCF solver")
        void testImmediateVCFResolvedByVCT() {
            // (4, 7), (5, 7), (6, 7) của Đen -> Open Three có VCF 3 plies
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            VCTResult result = vctSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isIn(new Move(3, 7), new Move(7, 7));
            assertThat(result.winningSequence()).isNotEmpty();
            assertThat(result.depth()).isPositive();
        }

        @Test
        @DisplayName("Trắng có VCF trực tiếp được VCT tìm thấy cho Trắng")
        void testWhiteImmediateVCF() {
            board.placeStone(7, 3, Board.WHITE);
            board.placeStone(7, 4, Board.WHITE);
            board.placeStone(7, 5, Board.WHITE);
            board.placeStone(7, 6, Board.WHITE);

            board.placeStone(0, 0, Board.BLACK);

            VCTResult result = vctSearch.search(board, Board.WHITE, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isIn(new Move(7, 2), new Move(7, 7));
            assertThat(result.winningSequence()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Tìm kiếm VCT qua nước đi Three-threat")
    class ThreeThreatVCTTests {

        @Test
        @DisplayName("Đen đi nước tạo Open Three kết hợp thế cờ độc lập dẫn tới VCF tất thắng")
        void testThreeThreatLeadingToWin() {
            // Hàng 5: Đen có Open Two: (4, 5), (5, 5).
            // Hàng 8: Đen CÓ SẴN Open Three: (4, 8), (5, 8), (6, 8).
            // Do đó Đen đã có VCF trên hàng 8, VCT tìm thấy chiến thắng ngay
            board.placeStone(4, 5, Board.BLACK);
            board.placeStone(5, 5, Board.BLACK);

            board.placeStone(4, 8, Board.BLACK);
            board.placeStone(5, 8, Board.BLACK);
            board.placeStone(6, 8, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            VCTResult result = vctSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Trường hợp không có VCT (No VCT Found)")
    class NoVCTTests {

        @Test
        @DisplayName("Bàn cờ rỗng không có VCT")
        void testEmptyBoardHasNoVCT() {
            VCTResult result = vctSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isFalse();
            assertThat(result.winningMove()).isNull();
            assertThat(result.winningSequence()).isEmpty();
            assertThat(result.depth()).isZero();
        }

        @Test
        @DisplayName("Chỉ có các quân cờ rời rạc không có VCT")
        void testScatteredStonesNoVCT() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(6, 6, Board.BLACK);
            board.placeStone(8, 8, Board.WHITE);

            VCTResult result = vctSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isFalse();
        }

        @Test
        @DisplayName("Đối thủ có FIVE ngăn chặn Attacker đi Three")
        void testOpponentFiveStopsVCT() {
            // Trắng có 4 quân: (3, 5), (4, 5), (5, 5), (6, 5) -> sắp có FIVE
            board.placeStone(3, 5, Board.WHITE);
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            // Đen chỉ có Open Two: (4, 7), (5, 7)
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);

            VCTResult result = vctSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isFalse();
        }
    }

    @Nested
    @DisplayName("Giới hạn tìm kiếm (Search Limits)")
    class SearchLimitsTests {

        @Test
        @DisplayName("maxNodes giới hạn số node duyệt và dừng an toàn")
        void testMaxNodesLimit() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            VCTResult result = vctSearch.search(board, Board.BLACK, 10, 1, 0);

            assertThat(result.found()).isFalse();
            assertThat(result.nodesSearched()).isLessThanOrEqualTo(5L);
        }

        @Test
        @DisplayName("Ghi nhận elapsedTimeMillis hợp lệ")
        void testElapsedTimeRecorded() {
            board.placeStone(7, 7, Board.BLACK);

            VCTResult result = vctSearch.search(board, Board.BLACK, 2);

            assertThat(result.elapsedTimeMillis()).isGreaterThanOrEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Tính toàn vẹn của bàn cờ (Board Integrity)")
    class BoardIntegrityTests {

        @Test
        @DisplayName("Hết độ sâu tìm kiếm không ném ngoại lệ IllegalArgumentException từ VCF")
        void testDepthExhaustionDoesNotThrowException() {
            // Thế cờ có Three nhưng không thể thắng VCT trong 1 nước
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(0, 0, Board.WHITE);

            // Tìm kiếm với maxDepth = 1 hoặc 2
            VCTResult result = vctSearch.search(board, Board.BLACK, 1);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Bàn cờ hoàn toàn nguyên vẹn sau khi gọi VCT search")
        void testBoardUnchangedAfterVCTSearch() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 7, Board.BLACK);

            int occupiedBefore = board.getOccupiedCount();
            int emptyBefore = board.getEmptyCount();

            vctSearch.search(board, Board.BLACK, 5);

            assertThat(board.getOccupiedCount()).isEqualTo(occupiedBefore);
            assertThat(board.getEmptyCount()).isEqualTo(emptyBefore);
            assertThat(board.getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(7, 8)).isEqualTo(Board.WHITE);
            assertThat(board.getCell(8, 7)).isEqualTo(Board.BLACK);
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật (Rectangular Board)")
    class RectangularBoardTests {

        @Test
        @DisplayName("VCT hoạt động chính xác trên bàn cờ chữ nhật 10x20")
        void testRectangularBoardVCT() {
            Board rectBoard = new Board(10, 20);
            rectBoard.placeStone(4, 7, Board.BLACK);
            rectBoard.placeStone(4, 8, Board.BLACK);
            rectBoard.placeStone(4, 9, Board.BLACK);
            rectBoard.placeStone(0, 0, Board.WHITE);

            VCTResult result = vctSearch.search(rectBoard, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isIn(new Move(4, 6), new Move(4, 10));
        }
    }
}

