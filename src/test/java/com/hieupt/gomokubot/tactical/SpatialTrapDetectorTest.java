package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpatialTrapDetector Tests (Phát hiện bẫy không gian & đòn chẽ)")
class SpatialTrapDetectorTest {

    private SpatialTrapDetector detector;
    private Board board;

    @BeforeEach
    void setUp() {
        detector = new SpatialTrapDetector();
        board = new Board(15, 15);
    }

    @Test
    @DisplayName("Phát hiện nước thắng ngay lập tức (FIVE)")
    void testDetectFive() {
        // Đặt 4 quân: (3, 7), (4, 7), (5, 7), (6, 7) -> đặt vào (7, 7) thắng
        for (int x = 3; x <= 6; x++) {
            board.placeStone(x, 7, Board.BLACK);
        }

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isWin()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_FIVE);
    }

    @Test
    @DisplayName("Phát hiện nước tạo Bốn mở (OPEN_FOUR)")
    void testDetectOpenFour() {
        // Đặt 3 quân mở 2 đầu: (4, 7), (5, 7), (6, 7) -> đặt (7, 7) tạo .XXXX. vì (3, 7) và (8, 7) trống
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isOpenFour()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_OPEN_FOUR);
    }

    @Test
    @DisplayName("Phát hiện đòn chẽ Bốn đôi (DOUBLE_FOUR: 4-4)")
    void testDetectDoubleFour() {
        // Hàng 7 bị chặn đầu: (3, 7) là White, (4, 7), (5, 7), (6, 7) là Black -> đặt (7, 7) tạo Four ngang
        board.placeStone(3, 7, Board.WHITE);
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        // Cột 7 bị chặn đầu: (7, 3) là White, (7, 4), (7, 5), (7, 6) là Black -> đặt (7, 7) tạo Four dọc
        board.placeStone(7, 3, Board.WHITE);
        board.placeStone(7, 4, Board.BLACK);
        board.placeStone(7, 5, Board.BLACK);
        board.placeStone(7, 6, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isDoubleFour()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_DOUBLE_FOUR);
    }

    @Test
    @DisplayName("Phát hiện đòn chẽ Bốn - Ba mở (FOUR_THREE: 4-3)")
    void testDetectFourThree() {
        // Hàng 7 tạo Four: (3, 7) là White, (4, 7), (5, 7), (6, 7) là Black
        board.placeStone(3, 7, Board.WHITE);
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        // Cột 7 tạo Open Three: (7, 5), (7, 6) là Black, (7, 4) và (7, 8) trống
        board.placeStone(7, 5, Board.BLACK);
        board.placeStone(7, 6, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isFourThree()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_FOUR_THREE);
    }

    @Test
    @DisplayName("Phát hiện đòn chẽ Ba - Ba mở (DOUBLE_THREE: 3-3)")
    void testDetectDoubleThree() {
        // Hàng 7: (5, 7), (6, 7) -> đặt (7, 7) tạo Open Three ngang
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        // Cột 7: (7, 5), (7, 6) -> đặt (7, 7) tạo Open Three dọc
        board.placeStone(7, 5, Board.BLACK);
        board.placeStone(7, 6, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isDoubleThree()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_DOUBLE_THREE);
    }

    @Test
    @DisplayName("Phát hiện mầm bẫy không gian Ba - Hai (TRAP_3_2: 3-2)")
    void testDetectTrap32() {
        // Hàng 7: (5, 7), (6, 7) -> đặt (7, 7) tạo Open Three
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        // Đường chéo: chỉ có 1 quân (6, 6) -> đặt (7, 7) tạo Open Two
        board.placeStone(6, 6, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isSpatialTrap()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_TRAP_3_2);
    }

    @Test
    @DisplayName("Phát hiện mầm bẫy giao điểm Hai - Hai (TRAP_2_2: 2-2)")
    void testDetectTrap22() {
        // Hàng 7: chỉ có 1 quân (6, 7) -> đặt (7, 7) tạo Open Two ngang
        board.placeStone(6, 7, Board.BLACK);

        // Cột 7: chỉ có 1 quân (7, 6) -> đặt (7, 7) tạo Open Two dọc
        board.placeStone(7, 6, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isSpatialTrap()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_TRAP_2_2);
    }

    @Test
    @DisplayName("Phát hiện thế nhảy cóc OO.O (Jump Three) khi đánh vào ô tạo 3 quân mở")
    void testDetectJumpThree() {
        // Đen có (4, 7), (5, 7) [OO], ô (6, 7) trống [.], đặt vào (7, 7) [O] -> tạo .XX.X.
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.openThreeCount()).isGreaterThanOrEqualTo(1);
        assertThat(analysis.tacticalScore()).isGreaterThanOrEqualTo(SpatialTrapDetector.SCORE_OPEN_THREE);
    }

    @Test
    @DisplayName("Phát hiện thế nhảy cóc OO.O: đánh vào ô giữa (GAP) tạo thành Bốn mở .XXXX.")
    void testDetectJumpThreeGapBecomesOpenFour() {
        // Đen có (4, 7), (5, 7) và (7, 7) -> đánh vào ô giữa (6, 7) tạo .XXXX.
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(7, 7, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 6, 7, Board.BLACK);

        assertThat(analysis.isOpenFour()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_OPEN_FOUR);
    }

    @Test
    @DisplayName("Phát hiện thế nhảy cóc OOO.O: một đầu bị chặn, đánh vào ô giữa (GAP) tạo thành FIVE")
    void testDetectJumpFourGapBecomesFive() {
        // Trắng có I9(B), I8(W), I7(W), I6(W), I5(trống), I4(W)
        // Đặt vào I5 tạo thành 5 quân liên tiếp!
        board.placeStone(8, 8, Board.BLACK); // I9 bị chặn
        board.placeStone(8, 7, Board.WHITE); // I8
        board.placeStone(8, 6, Board.WHITE); // I7
        board.placeStone(8, 5, Board.WHITE); // I6
        board.placeStone(8, 3, Board.WHITE); // I4

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 8, 4, Board.WHITE); // I5

        assertThat(analysis.isWin()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_FIVE);
    }

    @Test
    @DisplayName("Phát hiện đòn chẽ kết hợp giữa Ba mở liên tiếp và Ba nhảy cóc (3-3 Fork với OO.O)")
    void testDetectDoubleThreeWithJumpThree() {
        // Hàng dọc: (7, 5), (7, 6) -> đặt (7, 7) tạo Open Three dọc
        board.placeStone(7, 5, Board.BLACK);
        board.placeStone(7, 6, Board.BLACK);

        // Hàng ngang: (4, 7), (5, 7) [hai quân], ô (6, 7) trống, đặt vào (7, 7) tạo Jump Three .XX.X.
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);

        SpatialTrapDetector.MoveAnalysis analysis = detector.analyzeMove(board, 7, 7, Board.BLACK);

        assertThat(analysis.isDoubleThree()).isTrue();
        assertThat(analysis.tacticalScore()).isEqualTo(SpatialTrapDetector.SCORE_DOUBLE_THREE);
    }
}

