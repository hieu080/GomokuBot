package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
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

@DisplayName("ThreatDetector Tests")
class ThreatDetectorTest {

    private ThreatDetector threatDetector;
    private Board board;
    private WinChecker winChecker;
    private CandidateMoveGenerator moveGenerator;

    @BeforeEach
    void setUp() {
        winChecker = new WinChecker(5);
        moveGenerator = new CandidateMoveGenerator(2);
        threatDetector = new ThreatDetector(winChecker, moveGenerator);
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số")
    class ValidationTests {

        @Test
        @DisplayName("Khởi tạo với constructor mặc định thành công")
        void testDefaultConstructor() {
            ThreatDetector defaultDetector = new ThreatDetector();
            assertThat(defaultDetector.getWinChecker()).isNotNull();
            assertThat(defaultDetector.getMoveGenerator()).isNotNull();
        }

        @Test
        @DisplayName("WinChecker null ném NullPointerException")
        void testNullWinCheckerThrowsException() {
            assertThatThrownBy(() -> new ThreatDetector(null, moveGenerator))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("WinChecker must not be null");
        }

        @Test
        @DisplayName("CandidateMoveGenerator null ném NullPointerException")
        void testNullMoveGeneratorThrowsException() {
            assertThatThrownBy(() -> new ThreatDetector(winChecker, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CandidateMoveGenerator must not be null");
        }

        @Test
        @DisplayName("Board null khi gọi method ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> threatDetector.findWinningMoves(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> threatDetector.findFourThreats(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> threatDetector.findAllThreats(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "Player không hợp lệ ({0}) ném IllegalArgumentException")
        @ValueSource(bytes = {Board.EMPTY, -1, 3, 100})
        void testInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> threatDetector.findWinningMoves(board, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> threatDetector.findFourThreats(board, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("findDefenseMovesForAttack với move ngoài biên ném IndexOutOfBoundsException")
        void testAttackMoveOutsideBoardThrowsException() {
            assertThatThrownBy(() -> threatDetector.findDefenseMovesForAttack(board, new Move(-1, 0), Board.BLACK))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        @DisplayName("findDefenseMovesForAttack vào ô đã có quân ném IllegalArgumentException")
        void testAttackMoveOnOccupiedCellThrowsException() {
            board.placeStone(7, 7, Board.BLACK);
            assertThatThrownBy(() -> threatDetector.findDefenseMovesForAttack(board, new Move(7, 7), Board.BLACK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already occupied");
        }
    }

    @Nested
    @DisplayName("Phát hiện nước đi thắng ngay lập tức (FIVE / Mate in 1)")
    class WinningMoveTests {

        @Test
        @DisplayName("Bàn cờ rỗng không có nước thắng nào")
        void testEmptyBoardHasNoWinningMoves() {
            List<Move> wins = threatDetector.findWinningMoves(board, Board.BLACK);
            assertThat(wins).isEmpty();
        }

        @Test
        @DisplayName("Đen có 4 quân hàng ngang mở 2 đầu, phát hiện đúng 2 nước thắng")
        void testHorizontalOpenFourWinningMoves() {
            // (3, 7), (4, 7), (5, 7), (6, 7)
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            List<Move> wins = threatDetector.findWinningMoves(board, Board.BLACK);
            assertThat(wins).containsExactlyInAnyOrder(new Move(2, 7), new Move(7, 7));
        }

        @Test
        @DisplayName("Trắng có 4 quân hàng dọc bị chặn 1 đầu, phát hiện đúng 1 nước thắng")
        void testVerticalClosedFourWinningMoves() {
            // (7, 3), (7, 4), (7, 5), (7, 6) của Trắng, Đen chặn tại (7, 2)
            board.placeStone(7, 2, Board.BLACK);
            board.placeStone(7, 3, Board.WHITE);
            board.placeStone(7, 4, Board.WHITE);
            board.placeStone(7, 5, Board.WHITE);
            board.placeStone(7, 6, Board.WHITE);

            List<Move> wins = threatDetector.findWinningMoves(board, Board.WHITE);
            assertThat(wins).containsExactly(new Move(7, 7));
        }

        @Test
        @DisplayName("Phát hiện nước thắng ở đường chéo chính và chéo phụ")
        void testDiagonalWinningMoves() {
            // Chéo chính \: (3, 3), (4, 4), (5, 5), (6, 6)
            board.placeStone(3, 3, Board.BLACK);
            board.placeStone(4, 4, Board.BLACK);
            board.placeStone(5, 5, Board.BLACK);
            board.placeStone(6, 6, Board.BLACK);

            List<Move> wins = threatDetector.findWinningMoves(board, Board.BLACK);
            assertThat(wins).contains(new Move(2, 2), new Move(7, 7));
        }

        @Test
        @DisplayName("Đen có 4 quân ngắt quãng (XX.XX), nước thắng ở chính giữa")
        void testBrokenFourWinningMove() {
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            // (5, 7) trống
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(7, 7, Board.BLACK);

            List<Move> wins = threatDetector.findWinningMoves(board, Board.BLACK);
            assertThat(wins).containsExactly(new Move(5, 7));
        }
    }

    @Nested
    @DisplayName("Phát hiện mối đe dọa Bốn mở (OPEN_FOUR)")
    class OpenFourTests {

        @Test
        @DisplayName("Đen có 3 quân mở (Open Three), đánh nối thêm 1 quân tạo thành Open Four (2 ô phòng thủ)")
        void testOpenThreeToOpenFour() {
            // Đen có (4, 7), (5, 7), (6, 7)
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            List<Threat> fours = threatDetector.findFourThreats(board, Board.BLACK);

            // Nước (3, 7) và (7, 7) tạo Open Four
            Threat threatLeft = fours.stream()
                    .filter(t -> t.attackMove().equals(new Move(3, 7)))
                    .findFirst()
                    .orElseThrow();

            assertThat(threatLeft.isOpenFour()).isTrue();
            assertThat(threatLeft.type()).isEqualTo(ThreatType.OPEN_FOUR);
            assertThat(threatLeft.defenseMoves()).containsExactlyInAnyOrder(new Move(2, 7), new Move(7, 7));

            Threat threatRight = fours.stream()
                    .filter(t -> t.attackMove().equals(new Move(7, 7)))
                    .findFirst()
                    .orElseThrow();

            assertThat(threatRight.isOpenFour()).isTrue();
            assertThat(threatRight.defenseMoves()).containsExactlyInAnyOrder(new Move(3, 7), new Move(8, 7));
        }

        @Test
        @DisplayName("Đen có Broken Three (XX.X), đánh vào ô giữa tạo thành Open Four")
        void testBrokenThreeToOpenFour() {
            // (3, 7), (4, 7), (6, 7)
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            List<Threat> fours = threatDetector.findFourThreats(board, Board.BLACK);

            Threat threatCenter = fours.stream()
                    .filter(t -> t.attackMove().equals(new Move(5, 7)))
                    .findFirst()
                    .orElseThrow();

            assertThat(threatCenter.isOpenFour()).isTrue();
            assertThat(threatCenter.defenseMoves()).containsExactlyInAnyOrder(new Move(2, 7), new Move(7, 7));
        }
    }

    @Nested
    @DisplayName("Phát hiện mối đe dọa Bốn đóng / Bốn ngắt (FOUR / 1 ô phòng thủ)")
    class ClosedFourTests {

        @Test
        @DisplayName("Đen có 3 quân bị chặn 1 đầu, đánh tiếp tạo thành Closed Four với đúng 1 ô phòng thủ")
        void testClosedThreeToClosedFour() {
            // Trắng chặn ở (2, 7), Đen có (3, 7), (4, 7), (5, 7)
            board.placeStone(2, 7, Board.WHITE);
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);

            List<Threat> fours = threatDetector.findFourThreats(board, Board.BLACK);

            Threat threat = fours.stream()
                    .filter(t -> t.attackMove().equals(new Move(6, 7)))
                    .findFirst()
                    .orElseThrow();

            assertThat(threat.type()).isEqualTo(ThreatType.FOUR);
            assertThat(threat.isOpenFour()).isFalse();
            assertThat(threat.defenseMoves()).containsExactly(new Move(7, 7));
        }

        @Test
        @DisplayName("Đen có Broken Three (XX.X), đánh ở đầu ngoài tạo Broken Four (1 ô phòng thủ tại khe hở)")
        void testBrokenFourHasSingleDefenseMoveInGap() {
            // (3, 7), (4, 7), ô trống (5, 7), (6, 7)
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            List<Threat> fours = threatDetector.findFourThreats(board, Board.BLACK);

            // Nước đánh ở (7, 7) tạo Broken Four: (3,7), (4,7), .(5,7), (6,7), (7,7)
            Threat threat = fours.stream()
                    .filter(t -> t.attackMove().equals(new Move(7, 7)))
                    .findFirst()
                    .orElseThrow();

            assertThat(threat.type()).isEqualTo(ThreatType.FOUR);
            assertThat(threat.defenseMoves()).containsExactly(new Move(5, 7));
        }
    }

    @Nested
    @DisplayName("Đòn tấn công Bốn đôi (Double Four - Hai hướng khác nhau)")
    class DoubleFourTests {

        @Test
        @DisplayName("Đánh 1 quân tạo 2 mối đe dọa Four cùng lúc (ngang và dọc)")
        void testDoubleFourCreatesMultipleDefenseMoves() {
            // Hàng ngang: (5, 5), (6, 5), (7, 5) -> nếu đánh (8, 5) sẽ tạo Four ngang
            // Hàng dọc: (8, 2), (8, 3), (8, 4) -> nếu đánh (8, 5) sẽ tạo Four dọc
            // Bị chặn ở (4, 5) và (8, 1) để tạo 2 Closed Four riêng biệt
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.BLACK);
            board.placeStone(6, 5, Board.BLACK);
            board.placeStone(7, 5, Board.BLACK);

            board.placeStone(8, 1, Board.WHITE);
            board.placeStone(8, 2, Board.BLACK);
            board.placeStone(8, 3, Board.BLACK);
            board.placeStone(8, 4, Board.BLACK);

            List<Threat> fours = threatDetector.findFourThreats(board, Board.BLACK);

            Threat doubleFour = fours.stream()
                    .filter(t -> t.attackMove().equals(new Move(8, 5)))
                    .findFirst()
                    .orElseThrow();

            // Vì tạo 2 ô phòng thủ riêng biệt ((9, 5) và (8, 6)), đối thủ không thể đỡ cả hai
            assertThat(doubleFour.isOpenFour()).isTrue();
            assertThat(doubleFour.defenseMoves()).containsExactlyInAnyOrder(new Move(9, 5), new Move(8, 6));
        }
    }

    @Nested
    @DisplayName("Tổng hợp và phân cấp mối đe dọa (findAllThreats)")
    class AllThreatsTests {

        @Test
        @DisplayName("findAllThreats sắp xếp FIVE trước rồi đến OPEN_FOUR và FOUR")
        void testFindAllThreatsOrdering() {
            // 1. Tạo 1 thế thắng ngay (FIVE): 4 quân Đen tại hàng 3
            board.placeStone(1, 3, Board.BLACK);
            board.placeStone(2, 3, Board.BLACK);
            board.placeStone(3, 3, Board.BLACK);
            board.placeStone(4, 3, Board.BLACK);

            // 2. Tạo 1 thế Open Three tại hàng 7: (5, 7), (6, 7), (7, 7)
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(7, 7, Board.BLACK);

            List<Threat> allThreats = threatDetector.findAllThreats(board, Board.BLACK);
            assertThat(allThreats).isNotEmpty();

            // Phần tử đầu tiên phải là FIVE
            assertThat(allThreats.get(0).type()).isEqualTo(ThreatType.FIVE);

            // Sau các FIVE là OPEN_FOUR hoặc FOUR
            boolean seenNonWinning = false;
            for (Threat t : allThreats) {
                if (!t.isWinning()) {
                    seenNonWinning = true;
                }
                if (seenNonWinning) {
                    assertThat(t.type()).isNotEqualTo(ThreatType.FIVE);
                }
            }
        }
    }

    @Nested
    @DisplayName("Tìm kiếm ô phòng thủ trực tiếp (findDefenseMovesForAttack)")
    class DirectDefenseTests {

        @Test
        @DisplayName("findDefenseMovesForAttack trả về đúng các ô đối thủ cần chặn")
        void testFindDefenseMovesForAttack() {
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            List<Move> defense = threatDetector.findDefenseMovesForAttack(board, new Move(7, 7), Board.BLACK);
            assertThat(defense).containsExactlyInAnyOrder(new Move(3, 7), new Move(8, 7));
        }
    }

    @Nested
    @DisplayName("Kiểm tra phản công của đối thủ khi đỡ (isCounterThreat)")
    class CounterThreatTests {

        @Test
        @DisplayName("Nước đỡ của đối thủ tạo thành FIVE là phản công trực tiếp (counter-win)")
        void testDefenseMoveFormsFiveIsCounterThreat() {
            // Trắng có 4 quân liên tiếp: (3, 5), (4, 5), (5, 5), (6, 5)
            board.placeStone(3, 5, Board.WHITE);
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            // Ô (7, 5) là nước đỡ, đồng thời hoàn thành 5 quân cho Trắng
            boolean isCounter = threatDetector.isCounterThreat(board, new Move(7, 5), Board.WHITE);
            assertThat(isCounter).isTrue();
        }

        @Test
        @DisplayName("Nước đỡ của đối thủ tạo thành Four là phản công (counter-four)")
        void testDefenseMoveFormsFourIsCounterThreat() {
            // Trắng có 3 quân: (4, 5), (5, 5), (6, 5)
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            // Ô (7, 5) là nước đi tạo thành Four cho Trắng
            boolean isCounter = threatDetector.isCounterThreat(board, new Move(7, 5), Board.WHITE);
            assertThat(isCounter).isTrue();
        }

        @Test
        @DisplayName("Nước đỡ đơn thuần không tạo đe dọa Four/Five thì không phải là counter-threat")
        void testNormalDefenseIsNotCounterThreat() {
            board.placeStone(0, 0, Board.WHITE);

            boolean isCounter = threatDetector.isCounterThreat(board, new Move(7, 7), Board.WHITE);
            assertThat(isCounter).isFalse();
        }
    }

    @Nested
    @DisplayName("Tính toàn vẹn của bàn cờ (Board Integrity)")
    class BoardIntegrityTests {

        @Test
        @DisplayName("Bàn cờ hoàn toàn nguyên vẹn sau khi gọi tất cả các phương thức phân tích mối đe dọa")
        void testBoardUnchangedAfterAnalysis() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 7, Board.BLACK);

            int occupiedBefore = board.getOccupiedCount();
            int emptyBefore = board.getEmptyCount();

            threatDetector.findWinningMoves(board, Board.BLACK);
            threatDetector.findFourThreats(board, Board.BLACK);
            threatDetector.findAllThreats(board, Board.BLACK);
            threatDetector.findDefenseMovesForAttack(board, new Move(6, 7), Board.BLACK);
            threatDetector.isCounterThreat(board, new Move(6, 6), Board.WHITE);

            assertThat(board.getOccupiedCount()).isEqualTo(occupiedBefore);
            assertThat(board.getEmptyCount()).isEqualTo(emptyBefore);
            assertThat(board.getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(7, 8)).isEqualTo(Board.WHITE);
            assertThat(board.getCell(8, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(6, 7)).isEqualTo(Board.EMPTY);
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật (Rectangular Board)")
    class RectangularBoardTests {

        @Test
        @DisplayName("Phát hiện mối đe dọa chính xác trên bàn cờ chữ nhật 10x20")
        void testThreatDetectionOnRectangularBoard() {
            Board rectBoard = new Board(10, 20);
            rectBoard.placeStone(4, 8, Board.BLACK);
            rectBoard.placeStone(4, 9, Board.BLACK);
            rectBoard.placeStone(4, 10, Board.BLACK);

            List<Threat> fours = threatDetector.findFourThreats(rectBoard, Board.BLACK);

            Threat threat = fours.stream()
                    .filter(t -> t.attackMove().equals(new Move(4, 11)))
                    .findFirst()
                    .orElseThrow();

            assertThat(threat.isOpenFour()).isTrue();
            assertThat(threat.defenseMoves()).contains(new Move(4, 7), new Move(4, 12));
        }
    }
}

