package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fork Detection Tests (Đòn chẽ 3-3, 4-3, 4-4)")
class ForkDetectionTest {

    private ThreatDetector detector;
    private Board board;

    @BeforeEach
    void setUp() {
        detector = new ThreatDetector();
        board = new Board(15, 15);
    }

    @Test
    @DisplayName("Phát hiện đòn chẽ Ba-Ba mở (Double Three 3-3)")
    void testDetectDoubleThree() {
        // Thiết lập thế cờ chữ L quanh ô (7, 7):
        // Hàng 7: (5, 7), (6, 7) -> Nếu đặt (7, 7) và có (8, 7) trống, tạo Open Three ngang
        // Cột 7: (7, 5), (7, 6) -> Nếu đặt (7, 7) và có (7, 8) trống, tạo Open Three dọc
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        board.placeStone(7, 5, Board.BLACK);
        board.placeStone(7, 6, Board.BLACK);

        List<Fork> forks = detector.findForks(board, Board.BLACK);

        assertThat(forks).isNotEmpty();
        assertThat(forks).anyMatch(f ->
                f.type() == ForkType.DOUBLE_THREE && f.move().equals(new Move(7, 7))
        );
    }

    @Test
    @DisplayName("Phát hiện đòn chẽ Bốn - Ba mở (Four-Three 4-3)")
    void testDetectFourThree() {
        // Ô (7, 7) nối với:
        // Hàng 7: Đen đã có 3 quân (4, 7), (5, 7), (6, 7) bị chặn ở (3, 7) -> đặt (7, 7) tạo Closed Four
        // Cột 7: Đen có 2 quân (7, 5), (7, 6) -> đặt (7, 7) tạo Open Three
        board.placeStone(3, 7, Board.WHITE); // Chặn 1 đầu
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        board.placeStone(7, 5, Board.BLACK);
        board.placeStone(7, 6, Board.BLACK);

        List<Fork> forks = detector.findForks(board, Board.BLACK);

        assertThat(forks).isNotEmpty();
        assertThat(forks).anyMatch(f ->
                f.type() == ForkType.FOUR_THREE && f.move().equals(new Move(7, 7))
        );
    }

    @Test
    @DisplayName("Phát hiện đòn chẽ Bốn đôi (Double Four 4-4)")
    void testDetectDoubleFour() {
        // Hàng 7 bị chặn đầu: (3, 7) là White, (4, 7), (5, 7), (6, 7) là Black -> đặt (7, 7) tạo Four ngang
        // Cột 7 bị chặn đầu: (7, 3) là White, (7, 4), (7, 5), (7, 6) là Black -> đặt (7, 7) tạo Four dọc
        board.placeStone(3, 7, Board.WHITE);
        board.placeStone(4, 7, Board.BLACK);
        board.placeStone(5, 7, Board.BLACK);
        board.placeStone(6, 7, Board.BLACK);

        board.placeStone(7, 3, Board.WHITE);
        board.placeStone(7, 4, Board.BLACK);
        board.placeStone(7, 5, Board.BLACK);
        board.placeStone(7, 6, Board.BLACK);

        List<Fork> forks = detector.findForks(board, Board.BLACK);

        assertThat(forks).isNotEmpty();
        assertThat(forks).anyMatch(f ->
                f.type() == ForkType.DOUBLE_FOUR && f.move().equals(new Move(7, 7))
        );
    }
}

