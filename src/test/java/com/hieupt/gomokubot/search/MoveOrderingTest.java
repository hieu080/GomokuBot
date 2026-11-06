package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoveOrdering Tests")
class MoveOrderingTest {

    private CandidateMoveGenerator moveGenerator;
    private WinChecker winChecker;
    private MoveOrdering moveOrdering;
    private Board board;

    @BeforeEach
    void setUp() {
        moveGenerator = new CandidateMoveGenerator(2);
        winChecker = new WinChecker(5);
        moveOrdering = new MoveOrdering(moveGenerator, winChecker);
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Trường hợp đặc biệt: Bàn cờ rỗng hoặc ít nước đi")
    class EdgeCaseTests {

        @Test
        @DisplayName("Bàn cờ rỗng chỉ có đúng 1 nước đi ứng viên tại tâm (candidates.size() <= 1)")
        void testEmptyBoardReturnsSingleMoveImmediately() {
            // Khi bàn cờ rỗng, CandidateMoveGenerator chỉ sinh 1 nước tại tâm
            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, new Move(0, 0), new Move(1, 1));
            assertThat(ordered).containsExactly(new Move(7, 7));
        }

        @Test
        @DisplayName("Khi bàn cờ đầy không còn nước đi ứng viên nào thì trả về danh sách rỗng")
        void testFullBoardReturnsEmptyList() {
            Board smallBoard = new Board(3, 3);
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    smallBoard.placeStone(x, y, Board.BLACK);
                }
            }
            List<Move> ordered = moveOrdering.orderMoves(smallBoard, Board.BLACK, null, null);
            assertThat(ordered).isEmpty();
        }
    }

    @Nested
    @DisplayName("Priority 5: Nước đi thắng ngay lập tức (Immediate Winning Move)")
    class WinningMoveTests {

        @Test
        @DisplayName("Đen có 4 quân liên tiếp, nước đi thắng phải đứng đầu danh sách")
        void testWinningMoveHasHighestPriorityForBlack() {
            // Đen có hàng ngang (3, 7), (4, 7), (5, 7), (6, 7)
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            // Đặt 1 quân Trắng để tạo thêm candidate
            board.placeStone(0, 0, Board.WHITE);

            Move winningMove1 = new Move(7, 7);
            Move winningMove2 = new Move(2, 7);

            // Giả lập TT move và previous best move ở vị trí khác
            Move ttMove = new Move(8, 7);
            Move prevBest = new Move(6, 8);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, ttMove, prevBest);

            assertThat(ordered).isNotEmpty();
            // Nước đầu tiên phải là một trong các nước thắng
            assertThat(ordered.get(0)).isIn(winningMove1, winningMove2);
            // Cả hai nước thắng đều phải đứng trước ttMove và prevBest
            int win1Idx = ordered.indexOf(winningMove1);
            int win2Idx = ordered.indexOf(winningMove2);
            int ttIdx = ordered.indexOf(ttMove);
            int prevIdx = ordered.indexOf(prevBest);

            assertThat(win1Idx).isLessThan(ttIdx);
            assertThat(win2Idx).isLessThan(ttIdx);
            assertThat(win1Idx).isLessThan(prevIdx);
            assertThat(win2Idx).isLessThan(prevIdx);
        }

        @Test
        @DisplayName("Trắng có 4 quân liên tiếp, nước đi thắng của Trắng phải đứng đầu danh sách")
        void testWinningMoveHasHighestPriorityForWhite() {
            // Trắng có hàng dọc (7, 3), (7, 4), (7, 5), (7, 6)
            board.placeStone(7, 3, Board.WHITE);
            board.placeStone(7, 4, Board.WHITE);
            board.placeStone(7, 5, Board.WHITE);
            board.placeStone(7, 6, Board.WHITE);

            board.placeStone(0, 0, Board.BLACK);

            Move winningMove = new Move(7, 7);
            Move ttMove = new Move(7, 2);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.WHITE, ttMove, null);

            assertThat(ordered.get(0)).isIn(winningMove, ttMove);
            // Cả 2 nước thắng (7, 7) và (7, 2) đều đạt priority 5
            assertThat(ordered.subList(0, 2)).containsExactlyInAnyOrder(winningMove, ttMove);
        }

        @Test
        @DisplayName("Nhiều nước đi thắng được sắp xếp theo độ gần trung tâm bàn cờ")
        void testMultipleWinningMovesSortedByCenterDistance() {
            // Đen có 4 quân dọc từ (7, 3) đến (7, 6)
            // Hai đầu là (7, 2) và (7, 7)
            // Tâm bàn cờ 15x15 là (7.0, 7.0)
            // (7, 7) có khoảng cách tâm = 0.0
            // (7, 2) có khoảng cách tâm = (7-7)^2 + (2-7)^2 = 25.0
            board.placeStone(7, 3, Board.BLACK);
            board.placeStone(7, 4, Board.BLACK);
            board.placeStone(7, 5, Board.BLACK);
            board.placeStone(7, 6, Board.BLACK);
            board.placeStone(1, 1, Board.WHITE);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, null);

            // (7, 7) gần tâm hơn (7, 2) nên phải đứng TRƯỚC (7, 2)
            assertThat(ordered.get(0)).isEqualTo(new Move(7, 7));
            assertThat(ordered.get(1)).isEqualTo(new Move(7, 2));
        }
    }

    @Nested
    @DisplayName("Priority 4: Chặn nước thắng của đối thủ (Block Opponent Win)")
    class OpponentBlockTests {

        @Test
        @DisplayName("Đen phải ưu tiên chặn nước thắng của Trắng (Priority 4)")
        void testOpponentWinBlockHasPriorityFour() {
            // Trắng có 4 quân hàng ngang: (3, 5), (4, 5), (5, 5), (6, 5)
            // Nước thắng của Trắng là (2, 5) hoặc (7, 5)
            board.placeStone(3, 5, Board.WHITE);
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            // Đen chỉ có 1 quân ở xa (7, 7)
            board.placeStone(7, 7, Board.BLACK);

            Move ttMove = new Move(8, 8);
            Move prevBest = new Move(9, 9);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, ttMove, prevBest);

            // Nước chặn là (2, 5) hoặc (7, 5) có priority 4, phải đứng trước ttMove (priority 3)
            Move block1 = new Move(2, 5);
            Move block2 = new Move(7, 5);

            int b1Idx = ordered.indexOf(block1);
            int b2Idx = ordered.indexOf(block2);
            int ttIdx = ordered.indexOf(ttMove);
            int prevIdx = ordered.indexOf(prevBest);

            assertThat(b1Idx).isLessThan(ttIdx);
            assertThat(b2Idx).isLessThan(ttIdx);
            assertThat(b1Idx).isLessThan(prevIdx);
            assertThat(b2Idx).isLessThan(prevIdx);
        }

        @Test
        @DisplayName("Nước đi thắng của người chơi (Priority 5) ưu tiên hơn nước chặn đối thủ (Priority 4)")
        void testPlayerWinBeatsOpponentBlock() {
            // Đen có 4 quân: (3, 7), (4, 7), (5, 7), (6, 7) -> nước thắng (7, 7)
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            // Trắng cũng có 4 quân: (3, 3), (4, 3), (5, 3), (6, 3) -> nước thắng của Trắng là (7, 3), (2, 3)
            board.placeStone(3, 3, Board.WHITE);
            board.placeStone(4, 3, Board.WHITE);
            board.placeStone(5, 3, Board.WHITE);
            board.placeStone(6, 3, Board.WHITE);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, null);

            Move blackWin = new Move(7, 7);
            Move whiteThreatBlock = new Move(7, 3);

            int winIdx = ordered.indexOf(blackWin);
            int blockIdx = ordered.indexOf(whiteThreatBlock);

            assertThat(winIdx).isLessThan(blockIdx);
        }

        @Test
        @DisplayName("Trắng ưu tiên chặn nước thắng của Đen khi Trắng không có nước thắng")
        void testWhiteBlocksBlackWin() {
            // Đen có 4 quân: (2, 2), (3, 2), (4, 2), (5, 2)
            board.placeStone(2, 2, Board.BLACK);
            board.placeStone(3, 2, Board.BLACK);
            board.placeStone(4, 2, Board.BLACK);
            board.placeStone(5, 2, Board.BLACK);

            board.placeStone(10, 10, Board.WHITE);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.WHITE, null, null);

            Move block1 = new Move(1, 2);
            Move block2 = new Move(6, 2);

            assertThat(ordered.get(0)).isIn(block1, block2);
        }
    }

    @Nested
    @DisplayName("Priority 3: Transposition Table Move (ttMove)")
    class TTMoveTests {

        @Test
        @DisplayName("ttMove được ưu tiên đứng trước các nước thường (Priority 3 vs Priority 0)")
        void testTTMoveHasPriorityThree() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            Move ttMove = new Move(6, 6);
            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, ttMove, null);

            // Không có win hay block, ttMove phải đứng đầu
            assertThat(ordered.get(0)).isEqualTo(ttMove);
        }

        @Test
        @DisplayName("ttMove được ưu tiên đứng trước previousBestMove (Priority 3 vs Priority 2)")
        void testTTMovePreferredOverPreviousBestMove() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            Move ttMove = new Move(6, 6);
            Move prevBest = new Move(8, 8);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, ttMove, prevBest);

            int ttIdx = ordered.indexOf(ttMove);
            int prevIdx = ordered.indexOf(prevBest);

            assertThat(ttIdx).isEqualTo(0);
            assertThat(prevIdx).isEqualTo(1);
        }

        @Test
        @DisplayName("ttMove là null được xử lý an toàn không gây lỗi")
        void testNullTTMoveHandledGracefully() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, null);
            assertThat(ordered).isNotEmpty();
        }

        @Test
        @DisplayName("ttMove nằm ngoài vùng candidates thì bị bỏ qua an toàn")
        void testTTMoveNotInCandidatesIgnored() {
            board.placeStone(7, 7, Board.BLACK);
            // Vị trí (0, 0) quá xa ô (7, 7) với radius=2 nên không nằm trong candidate
            Move ttMove = new Move(0, 0);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, ttMove, null);
            assertThat(ordered).doesNotContain(ttMove);
        }
    }

    @Nested
    @DisplayName("Priority 2: Previous Best Move (Iterative Deepening)")
    class PreviousBestMoveTests {

        @Test
        @DisplayName("previousBestMove được ưu tiên đứng trước các nước thường (Priority 2 vs Priority 0)")
        void testPreviousBestMoveHasPriorityTwo() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            Move prevBest = new Move(6, 8);
            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, prevBest);

            // Khi ttMove là null, previousBestMove phải đứng đầu
            assertThat(ordered.get(0)).isEqualTo(prevBest);
        }

        @Test
        @DisplayName("previousBestMove là null được xử lý an toàn")
        void testNullPreviousBestMoveHandledGracefully() {
            board.placeStone(7, 7, Board.BLACK);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, null);
            assertThat(ordered).isNotEmpty();
        }

        @Test
        @DisplayName("previousBestMove nằm ngoài vùng candidates thì bị bỏ qua an toàn")
        void testPreviousBestMoveNotInCandidatesIgnored() {
            board.placeStone(7, 7, Board.BLACK);
            Move prevBest = new Move(0, 0);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, prevBest);
            assertThat(ordered).doesNotContain(prevBest);
        }
    }

    @Nested
    @DisplayName("Cấp bậc ưu tiên đầy đủ (Full Priority Hierarchy: 5 > 4 > 3 > 2 > 0)")
    class HierarchyTests {

        @Test
        @DisplayName("Xác nhận đúng thứ tự phân cấp: Win (5) > Block (4) > TT (3) > PrevBest (2) > Normal (0)")
        void testFullHierarchyOrder() {
            // Đen có 4 quân: (3, 7), (4, 7), (5, 7), (6, 7) -> nước thắng (7, 7) [P5]
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            // Trắng có 4 quân: (3, 5), (4, 5), (5, 5), (6, 5) -> nước chặn của Đen là (7, 5) [P4]
            board.placeStone(3, 5, Board.WHITE);
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            Move winMove = new Move(7, 7);    // Priority 5
            Move blockMove = new Move(7, 5);  // Priority 4
            Move ttMove = new Move(7, 6);     // Priority 3
            Move prevBest = new Move(6, 6);   // Priority 2
            Move normalMove = new Move(2, 6); // Priority 0

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, ttMove, prevBest);

            int winIdx = ordered.indexOf(winMove);
            int blockIdx = ordered.indexOf(blockMove);
            int ttIdx = ordered.indexOf(ttMove);
            int prevIdx = ordered.indexOf(prevBest);
            int normalIdx = ordered.indexOf(normalMove);

            assertThat(winIdx).isLessThan(blockIdx);
            assertThat(blockIdx).isLessThan(ttIdx);
            assertThat(ttIdx).isLessThan(prevIdx);
            assertThat(prevIdx).isLessThan(normalIdx);
        }

        @Test
        @DisplayName("Nếu ttMove trùng với nước thắng, nước đó nhận Priority 5 thay vì 3")
        void testTTMoveIsWinningMoveRetainsPriorityFive() {
            board.placeStone(3, 7, Board.BLACK);
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);

            board.placeStone(3, 5, Board.WHITE);
            board.placeStone(4, 5, Board.WHITE);
            board.placeStone(5, 5, Board.WHITE);
            board.placeStone(6, 5, Board.WHITE);

            Move winMove = new Move(7, 7);
            Move blockMove = new Move(7, 5);

            // Đặt ttMove chính là winMove
            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, winMove, null);

            // winMove vẫn phải đứng trước blockMove
            int winIdx = ordered.indexOf(winMove);
            int blockIdx = ordered.indexOf(blockMove);

            assertThat(winIdx).isLessThan(blockIdx);
        }
    }

    @Nested
    @DisplayName("Sắp xếp theo độ gần trung tâm bàn cờ (Centrality Tie-breaking)")
    class CentralityTests {

        @Test
        @DisplayName("Các nước thường (Priority 0) được sắp xếp theo khoảng cách tới tâm tăng dần")
        void testNormalMovesSortedByDistanceToCenter() {
            // Đặt 1 quân tại (7, 7)
            board.placeStone(7, 7, Board.BLACK);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.WHITE, null, null);

            double centerX = (board.getWidth() - 1) / 2.0; // 7.0
            double centerY = (board.getHeight() - 1) / 2.0; // 7.0

            // Kiểm tra tính đơn điệu tăng dần của khoảng cách tâm
            double prevDist = -1.0;
            for (Move move : ordered) {
                double dx = move.x() - centerX;
                double dy = move.y() - centerY;
                double dist = dx * dx + dy * dy;

                assertThat(dist).isGreaterThanOrEqualTo(prevDist);
                prevDist = dist;
            }
        }
    }

    @Nested
    @DisplayName("Tính toàn vẹn của bàn cờ (Board Integrity)")
    class IntegrityTests {

        @Test
        @DisplayName("Bàn cờ không bị thay đổi bất kỳ ô cờ nào sau khi gọi orderMoves")
        void testBoardRemainsUnchangedAfterOrdering() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(6, 7, Board.BLACK);

            int occupiedBefore = board.getOccupiedCount();
            int emptyBefore = board.getEmptyCount();

            moveOrdering.orderMoves(board, Board.BLACK, new Move(6, 6), new Move(8, 8));

            assertThat(board.getOccupiedCount()).isEqualTo(occupiedBefore);
            assertThat(board.getEmptyCount()).isEqualTo(emptyBefore);
            assertThat(board.getCell(7, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(7, 8)).isEqualTo(Board.WHITE);
            assertThat(board.getCell(6, 7)).isEqualTo(Board.BLACK);
            assertThat(board.getCell(6, 6)).isEqualTo(Board.EMPTY);
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật (Rectangular Board)")
    class RectangularBoardTests {

        @Test
        @DisplayName("Sắp xếp nước đi chính xác trên bàn cờ chữ nhật 10x20")
        void testRectangularBoardOrdering() {
            Board rectBoard = new Board(10, 20);
            rectBoard.placeStone(4, 9, Board.BLACK);
            rectBoard.placeStone(4, 10, Board.WHITE);

            MoveOrdering rectOrdering = new MoveOrdering(moveGenerator, winChecker);

            Move ttMove = new Move(5, 9);
            List<Move> ordered = rectOrdering.orderMoves(rectBoard, Board.BLACK, ttMove, null);

            assertThat(ordered).isNotEmpty();
            assertThat(ordered.get(0)).isEqualTo(ttMove);
        }

        @Test
        @DisplayName("Nhận diện nước thắng và chặn chính xác trên bàn cờ chữ nhật")
        void testRectangularBoardWinningMove() {
            Board rectBoard = new Board(10, 20);
            rectBoard.placeStone(2, 5, Board.BLACK);
            rectBoard.placeStone(3, 5, Board.BLACK);
            rectBoard.placeStone(4, 5, Board.BLACK);
            rectBoard.placeStone(5, 5, Board.BLACK);
            rectBoard.placeStone(0, 0, Board.WHITE);

            MoveOrdering rectOrdering = new MoveOrdering(moveGenerator, winChecker);

            List<Move> ordered = rectOrdering.orderMoves(rectBoard, Board.BLACK, null, null);

            Move winMove1 = new Move(6, 5);
            Move winMove2 = new Move(1, 5);

            assertThat(ordered.get(0)).isIn(winMove1, winMove2);
        }
    }

    @Nested
    @DisplayName("Ưu tiên đòn chẽ và bẫy không gian (Tactical Fork & Spatial Trap Ordering)")
    class TacticalTrapAndForkOrderingTests {

        @Test
        @DisplayName("Nước tạo Bốn mở (Open Four) ưu tiên cao hơn ttMove và Killer")
        void testOpenFourBeatsTTMove() {
            // Đen có (4, 7), (5, 7), (6, 7). Nước (7, 7) tạo thành Bốn mở .XXXX.
            board.placeStone(4, 7, Board.BLACK);
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            board.placeStone(1, 1, Board.WHITE);

            Move openFourMove = new Move(7, 7);
            // ttMove tại (5, 6) (nằm trong candidate của (5, 7))
            Move ttMove = new Move(5, 6);
            // killer tại (5, 8)
            Move killer = new Move(5, 8);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, ttMove, null, killer, null);

            int openFourIdx = ordered.indexOf(openFourMove);
            int ttIdx = ordered.indexOf(ttMove);
            int killerIdx = ordered.indexOf(killer);

            assertThat(openFourIdx).isNotEqualTo(-1);
            assertThat(ttIdx).isNotEqualTo(-1);
            assertThat(killerIdx).isNotEqualTo(-1);
            assertThat(openFourIdx).isLessThan(ttIdx);
            assertThat(ttIdx).isLessThan(killerIdx);
        }

        @Test
        @DisplayName("Nước tạo đòn chẽ Ba - Ba (Double Three) ưu tiên cao hơn Killer và nước thường")
        void testDoubleThreeBeatsKillerAndNormal() {
            // Đen chuẩn bị tạo 3-3 tại (7, 7):
            // Ngang: (5, 7), (6, 7)
            board.placeStone(5, 7, Board.BLACK);
            board.placeStone(6, 7, Board.BLACK);
            // Dọc: (7, 5), (7, 6)
            board.placeStone(7, 5, Board.BLACK);
            board.placeStone(7, 6, Board.BLACK);
            board.placeStone(1, 1, Board.WHITE);

            Move forkMove = new Move(7, 7);
            Move killer = new Move(5, 6);
            Move normal = new Move(6, 5);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, null, killer, null);

            int forkIdx = ordered.indexOf(forkMove);
            int killerIdx = ordered.indexOf(killer);
            int normalIdx = ordered.indexOf(normal);

            assertThat(forkIdx).isNotEqualTo(-1);
            assertThat(killerIdx).isNotEqualTo(-1);
            assertThat(normalIdx).isNotEqualTo(-1);
            assertThat(forkIdx).isLessThan(killerIdx);
            assertThat(killerIdx).isLessThan(normalIdx);
        }

        @Test
        @DisplayName("Nước chặn đòn chẽ đối thủ (Block Fork) ưu tiên cao hơn Killer và nước thường")
        void testBlockOpponentForkBeatsKillerAndNormal() {
            // Trắng chuẩn bị tạo 3-3 tại (7, 7)
            board.placeStone(5, 7, Board.WHITE);
            board.placeStone(6, 7, Board.WHITE);
            board.placeStone(7, 5, Board.WHITE);
            board.placeStone(7, 6, Board.WHITE);
            board.placeStone(1, 1, Board.BLACK);

            Move blockForkMove = new Move(7, 7);
            Move killer = new Move(5, 6);
            Move normal = new Move(6, 5);

            List<Move> ordered = moveOrdering.orderMoves(board, Board.BLACK, null, null, killer, null);

            int blockIdx = ordered.indexOf(blockForkMove);
            int killerIdx = ordered.indexOf(killer);
            int normalIdx = ordered.indexOf(normal);

            assertThat(blockIdx).isNotEqualTo(-1);
            assertThat(killerIdx).isNotEqualTo(-1);
            assertThat(normalIdx).isNotEqualTo(-1);
            assertThat(blockIdx).isLessThan(killerIdx);
            assertThat(killerIdx).isLessThan(normalIdx);
        }
    }
}
