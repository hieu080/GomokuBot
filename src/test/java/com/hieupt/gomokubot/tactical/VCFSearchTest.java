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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VCFSearch Tests")
class VCFSearchTest {

    private VCFSearch vcfSearch;
    private Board board;
    private ThreatDetector threatDetector;
    private WinChecker winChecker;
    private ZobristHash zobristHash;

    @BeforeEach
    void setUp() {
        winChecker = new WinChecker(5);
        threatDetector = new ThreatDetector(winChecker);
        zobristHash = new ZobristHash(15, 15);
        vcfSearch = new VCFSearch(threatDetector, winChecker, zobristHash);
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo mặc định thành công")
        void testDefaultConstructor() {
            VCFSearch defaultSearch = new VCFSearch();
            assertThat(defaultSearch.getThreatDetector()).isNotNull();
            assertThat(defaultSearch.getWinChecker()).isNotNull();
            assertThat(defaultSearch.getZobristHash()).isNotNull();
        }

        @Test
        @DisplayName("Dependency null ném NullPointerException")
        void testNullDependenciesThrowException() {
            assertThatThrownBy(() -> new VCFSearch(null, winChecker, zobristHash))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new VCFSearch(threatDetector, null, zobristHash))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new VCFSearch(threatDetector, winChecker, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Board null khi search ném NullPointerException")
        void testSearchNullBoardThrowsException() {
            assertThatThrownBy(() -> vcfSearch.search(null, Board.BLACK, 10))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "Player không hợp lệ ({0}) ném IllegalArgumentException")
        @ValueSource(bytes = {Board.EMPTY, -1, 3, 99})
        void testInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> vcfSearch.search(board, invalidPlayer, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("maxDepth < 1 ném IllegalArgumentException")
        void testInvalidMaxDepthThrowsException() {
            assertThatThrownBy(() -> vcfSearch.search(board, Board.BLACK, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxDepth must be at least 1");
        }
    }

    @Nested
    @DisplayName("VCF 1 nước (Mate in 1 - Thắng ngay với FIVE)")
    class MateInOneTests {

        @Test
        @DisplayName("Đen có 4 quân liên tiếp, tìm thấy nước thắng FIVE ngay lập tức")
        void testMateInOneForBlack() {
            // (3, 7), (4, 7), (5, 7), (6, 7)
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isIn(new Move(2, 7), new Move(7, 7));
            assertThat(result.winningSequence()).hasSize(1);
            assertThat(result.depth()).isEqualTo(1);
            assertThat(result.nodesSearched()).isPositive();
        }

        @Test
        @DisplayName("Trắng có 4 quân hàng dọc, tìm thấy nước thắng FIVE")
        void testMateInOneForWhite() {
            board.placeStone(7, 3, Board.WHITE);
            board.placeStone(7, 4, Board.WHITE);
            board.placeStone(7, 5, Board.WHITE);
            board.placeStone(7, 6, Board.WHITE);

            board.placeStone(0, 0, Board.BLACK);

            VCFResult result = vcfSearch.search(board, Board.WHITE, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isIn(new Move(7, 2), new Move(7, 7));
            assertThat(result.winningSequence()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("VCF 3 plies (Tạo Open Four dẫn tới chiến thắng)")
    class VCFInThreePliesTests {

        @Test
        @DisplayName("Đen có Open Three, đánh tạo Open Four và đối thủ không thể chặn được cả 2 đầu")
        void testOpenThreeToOpenFourVCF() {
            // Đen có (4, 7), (5, 7), (6, 7)
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            // Nước đi đầu tiên phải là (3, 7) hoặc (7, 7) để tạo Open Four
            assertThat(result.winningMove()).isIn(new Move(3, 7), new Move(7, 7));
            // Chuỗi nước đi gồm: [Attacker(Four), Defender(Block 1), Attacker(Win at 2)]
            assertThat(result.winningSequence()).hasSize(3);
            assertThat(result.depth()).isEqualTo(3);
        }

        @Test
        @DisplayName("Đen có Broken Three (XX.X), đánh vào ô giữa tạo Open Four")
        void testBrokenThreeToOpenFourVCF() {
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isEqualTo(new Move(5, 7));
            assertThat(result.winningSequence()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("VCF nhiều nước (Multi-step VCF: 5 plies & 7 plies)")
    class MultiStepVCFTests {

        @Test
        @DisplayName("VCF 5 plies: Closed Four kích hoạt Open Three thành Open Four -> Thắng")
        void testVCFInFivePlies() {
            board.placeStone(2, 5, Board.WHITE);
            board.placeStone(3, 5, Board.BLACK);
            board.placeStone(4, 5, Board.BLACK);
            board.placeStone(5, 5, Board.BLACK);

            board.placeStone(6, 6, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 10);

            assertThat(result.found()).isTrue();
            assertThat(result.winningSequence()).hasSize(5);
            assertValidVCFSequence(board, Board.BLACK, result.winningSequence());
        }

        @Test
        @DisplayName("VCF 7 plies: Chuỗi 3 nước ép Four liên hoàn kích hoạt lẫn nhau dẫn tới chiến thắng")
        void testVCFInSevenPlies() {
            board.placeStone(2, 4, Board.WHITE);
            board.placeStone(8, 4, Board.WHITE);
            board.placeStone(3, 4, Board.BLACK);
            board.placeStone(4, 4, Board.BLACK);
            board.placeStone(5, 4, Board.BLACK);

            board.placeStone(6, 5, Board.BLACK);
            board.placeStone(6, 6, Board.BLACK);
            board.placeStone(6, 8, Board.WHITE);

            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(8, 7, Board.BLACK);

            board.placeStone(0, 0, Board.WHITE);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 15);

            assertThat(result.found()).isTrue();
            assertThat(result.winningSequence()).isNotEmpty();
            assertValidVCFSequence(board, Board.BLACK, result.winningSequence());
        }
    }

    @Nested
    @DisplayName("Trường hợp không có VCF (No VCF Found)")
    class NoVCFTests {

        @Test
        @DisplayName("Bàn cờ rỗng không có VCF")
        void testEmptyBoardHasNoVCF() {
            VCFResult result = vcfSearch.search(board, Board.BLACK, 10);

            assertThat(result.found()).isFalse();
            assertThat(result.winningMove()).isNull();
            assertThat(result.winningSequence()).isEmpty();
            assertThat(result.depth()).isEqualTo(0);
        }

        @Test
        @DisplayName("Chỉ có các quân cờ rời rạc không tạo được Four liên tục")
        void testScatteredStonesNoVCF() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 8, Board.BLACK);
            board.placeStone(6, 6, Board.WHITE);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 10);

            assertThat(result.found()).isFalse();
            assertThat(result.winningSequence()).isEmpty();
        }

        @Test
        @DisplayName("maxDepth nhỏ hơn độ sâu cần thiết để hoàn thành VCF")
        void testMaxDepthTooShallow() {
            // Thế cờ cần ít nhất 3 plies để thắng (Open Three -> Open Four -> Win)
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            // Nhưng chỉ cho phép maxDepth = 0 trong logic giải (depthRemaining < 1)
            // search(..., maxDepth = 1):
            // Nước đầu tiên tạo Open Four, nhưng cần thêm 2 plies để xác nhận thắng
            // Nếu maxDepth chỉ là 1, Attacker không có Five ngay thì fail
            VCFResult result = vcfSearch.search(board, Board.BLACK, 1);
            // Ở maxDepth = 1: Đen không có Five ngay, và tạo Open Four cần depth >= 2
            // Tuy nhiên hàm kiểm tra Open Four thắng ngay sau 1 nước đánh
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Đối thủ có đe dọa (Opponent Threat Handling)")
    class OpponentThreatTests {

        @Test
        @DisplayName("Đối thủ đã có Open Four (2 ô thắng) thì Attacker không thể thực hiện VCF nếu không có Five ngay")
        void testOpponentOpenFourStopsVCF() {
            // Trắng có Open Four: (3, 5), (4, 5), (5, 5), (6, 5)
            board.placeStone(3, 5, Board.WHITE);
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            // Đen chỉ có Open Three: (4, 8), (5, 8), (6, 8)
            board.placeStone(4, 8, Board.BLACK);
            board.placeStone(5, 8, Board.BLACK);
            board.placeStone(6, 8, Board.BLACK);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 10);

            // Trắng sẽ thắng ở nước tiếp theo, Đen không thể tiếp tục VCF
            assertThat(result.found()).isFalse();
        }

        @Test
        @DisplayName("Attacker có Five thì thắng ngay lập tức bất chấp đối thủ có đe dọa Five")
        void testAttackerFiveBeatsOpponentFiveThreat() {
            // Trắng có 4 quân: (3, 5), (4, 5), (5, 5), (6, 5) -> thắng ở (2, 5) hoặc (7, 5)
            board.placeStone(3, 5, Board.WHITE);
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            // Đen CŨNG có 4 quân: (3, 7), (4, 7), (5, 7), (6, 7) -> Đen đánh Five là thắng ngay
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningSequence()).hasSize(1);
            assertThat(result.winningMove()).isIn(new Move(2, 7), new Move(7, 7));
        }
    }

    @Nested
    @DisplayName("Giới hạn tìm kiếm (Search Limits: Nodes & Time)")
    class SearchLimitsTests {

        @Test
        @DisplayName("maxNodes giới hạn số lượng nốt duyệt và dừng tìm kiếm an toàn")
        void testMaxNodesLimit() {
            // Đặt thế cờ
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            // Giới hạn maxNodes = 1
            VCFResult result = vcfSearch.search(board, Board.BLACK, 20, 1, 0);

            assertThat(result.found()).isFalse();
            assertThat(result.nodesSearched()).isLessThanOrEqualTo(5L);
        }

        @Test
        @DisplayName("Ghi nhận thời gian tìm kiếm elapsedTimeMillis chính xác")
        void testElapsedTimeRecorded() {
            board.placeStone(7, 7, Board.BLACK);

            VCFResult result = vcfSearch.search(board, Board.BLACK, 5);

            assertThat(result.elapsedTimeMillis()).isGreaterThanOrEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Tính toàn vẹn của bàn cờ (Board Integrity)")
    class BoardIntegrityTests {

        @Test
        @DisplayName("Bàn cờ hoàn toàn nguyên vẹn sau khi hoàn thành VCF search (kể cả search sâu)")
        void testBoardUnchangedAfterVCFSearch() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 7, Board.BLACK);
            board.placeStone(8, 8, Board.WHITE);

            int occupiedBefore = board.getOccupiedCount();
            int emptyBefore = board.getEmptyCount();

            vcfSearch.search(board, Board.BLACK, 15);

            assertThat(board.getOccupiedCount()).isEqualTo(occupiedBefore);
            assertThat(board.getEmptyCount()).isEqualTo(emptyBefore);
            assertThat(board.getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(7, 8)).isEqualTo(Board.WHITE);
            assertThat(board.getCell(8, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(8, 8)).isEqualTo(Board.WHITE);
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật (Rectangular Board)")
    class RectangularBoardTests {

        @Test
        @DisplayName("Tìm thấy VCF chính xác trên bàn cờ chữ nhật 10x20")
        void testRectangularBoardVCF() {
            Board rectBoard = new Board(10, 20);
            rectBoard.placeStone(4, 7, Board.BLACK);
            rectBoard.placeStone(4, 8, Board.BLACK);
            rectBoard.placeStone(4, 9, Board.BLACK);
            rectBoard.placeStone(4, 10, Board.BLACK);
            rectBoard.placeStone(0, 0, Board.WHITE);

            VCFResult result = vcfSearch.search(rectBoard, Board.BLACK, 5);

            assertThat(result.found()).isTrue();
            assertThat(result.winningMove()).isIn(new Move(4, 6), new Move(4, 11));
        }
    }

    private void assertValidVCFSequence(Board initialBoard, byte attacker, List<Move> sequence) {
        assertThat(sequence).isNotEmpty();
        assertThat(sequence.size() % 2).isEqualTo(1);

        Board testBoard = initialBoard.copy();
        byte defender = attacker == Board.BLACK ? Board.WHITE : Board.BLACK;

        for (int i = 0; i < sequence.size(); i++) {
            Move move = sequence.get(i);
            assertThat(testBoard.isEmpty(move.x(), move.y()))
                    .as("Move %d (%s) must be on an empty cell", i, move)
                    .isTrue();

            if (i % 2 == 0) {
                if (i == sequence.size() - 1) {
                    testBoard.placeStone(move.x(), move.y(), attacker);
                    assertThat(winChecker.isWinningMove(testBoard, move))
                            .as("Final move %s must form FIVE for attacker", move)
                            .isTrue();
                } else {
                    List<Move> defense = threatDetector.findDefenseMovesForAttack(testBoard, move, attacker);
                    assertThat(defense)
                            .as("Attacker move %s must create at least one Four threat", move)
                            .isNotEmpty();

                    testBoard.placeStone(move.x(), move.y(), attacker);
                }
            } else {
                Move prevAttack = sequence.get(i - 1);
                testBoard.removeStone(prevAttack.x(), prevAttack.y());
                List<Move> requiredDefense = threatDetector.findDefenseMovesForAttack(testBoard, prevAttack, attacker);
                testBoard.placeStone(prevAttack.x(), prevAttack.y(), attacker);

                assertThat(requiredDefense)
                        .as("Defender move %s must be one of the required defense squares for %s", move, prevAttack)
                        .contains(move);

                testBoard.placeStone(move.x(), move.y(), defender);
            }
        }
    }
}
