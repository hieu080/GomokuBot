package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ThreatSpaceSearch Tests")
class ThreatSpaceSearchTest {

    private ThreatSpaceSearch tss;
    private Board board;

    @BeforeEach
    void setUp() {
        tss = new ThreatSpaceSearch();
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo mặc định thành công")
        void testDefaultConstructor() {
            assertThat(tss.getVcfSearch()).isNotNull();
            assertThat(tss.getVctSearch()).isNotNull();
        }

        @Test
        @DisplayName("Dependency null ném NullPointerException")
        void testNullDependenciesThrowException() {
            assertThatThrownBy(() -> new ThreatSpaceSearch(null, new VCTSearch()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ThreatSpaceSearch(new VCFSearch(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Tìm kiếm chiến thuật tổng hợp (findForcedWinningMove)")
    class TacticalMoveTests {

        @Test
        @DisplayName("Ưu tiên tìm thấy VCF chiến thắng")
        void testFindsVCFWinningMove() {
            // (4, 7), (5, 7), (6, 7) của Đen -> Open Three có VCF
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(0, 0, Board.WHITE);

            Optional<Move> winningMove = tss.findForcedWinningMove(board, Board.BLACK, 5);

            assertThat(winningMove).isPresent();
            assertThat(winningMove.get()).isIn(new Move(3, 7), new Move(7, 7));
        }

        @Test
        @DisplayName("Bàn cờ rỗng không có nước chiến thuật tất thắng")
        void testEmptyBoardHasNoWinningMove() {
            Optional<Move> winningMove = tss.findForcedWinningMove(board, Board.BLACK, 5);
            assertThat(winningMove).isEmpty();
        }

        @Test
        @DisplayName("solveVCF và solveVCT hoạt động chuẩn xác qua facade")
        void testSolveVCFAndSolveVCT() {
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(0, 0, Board.WHITE);

            VCFResult vcf = tss.solveVCF(board, Board.BLACK, 5);
            assertThat(vcf.found()).isTrue();

            VCTResult vct = tss.solveVCT(board, Board.BLACK, 5);
            assertThat(vct.found()).isTrue();
        }
    }
}

