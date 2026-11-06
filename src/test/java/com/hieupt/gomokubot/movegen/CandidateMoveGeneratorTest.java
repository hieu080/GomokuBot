package com.hieupt.gomokubot.movegen;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CandidateMoveGenerator Tests")
class CandidateMoveGeneratorTest {

    private CandidateMoveGenerator generator;
    private Board board;

    @BeforeEach
    void setUp() {
        generator = new CandidateMoveGenerator();
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và cấu hình Radius")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo mặc định có radius = 2")
        void testDefaultConstructor() {
            CandidateMoveGenerator defaultGen = new CandidateMoveGenerator();
            assertThat(defaultGen.getRadius()).isEqualTo(2);
        }

        @Test
        @DisplayName("Khởi tạo với custom radius hợp lệ")
        void testCustomRadius() {
            CandidateMoveGenerator customGen = new CandidateMoveGenerator(1);
            assertThat(customGen.getRadius()).isEqualTo(1);

            CandidateMoveGenerator customGen3 = new CandidateMoveGenerator(3);
            assertThat(customGen3.getRadius()).isEqualTo(3);
        }

        @ParameterizedTest(name = "Radius không hợp lệ: {0}")
        @ValueSource(ints = {0, -1, -5})
        @DisplayName("Radius < 1 ném IllegalArgumentException")
        void testInvalidRadiusThrowsException(int invalidRadius) {
            assertThatThrownBy(() -> new CandidateMoveGenerator(invalidRadius))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 1");
        }

        @Test
        @DisplayName("Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            assertThatThrownBy(() -> generator.generate(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }
    }

    @Nested
    @DisplayName("Bàn cờ đặc biệt: Bàn cờ rỗng và Bàn cờ đầy")
    class SpecialBoardStateTests {

        @Test
        @DisplayName("Bàn cờ rỗng trả về duy nhất 1 nước đi ở chính giữa bàn cờ (Center Move)")
        void testEmptyStandardBoardReturnsCenterMove() {
            List<Move> candidates = generator.generate(board);

            // Bàn cờ 15x15: tâm là (15/2, 15/2) = (7, 7)
            assertThat(candidates).containsExactly(new Move(7, 7));
        }

        @Test
        @DisplayName("Bàn cờ rỗng kích thước chẵn trả về nước trung tâm đúng theo công thức")
        void testEmptyEvenSizedBoardReturnsCenterMove() {
            Board evenBoard = new Board(10, 10);
            List<Move> candidates = generator.generate(evenBoard);

            // 10 / 2 = 5 -> (5, 5)
            assertThat(candidates).containsExactly(new Move(5, 5));
        }

        @Test
        @DisplayName("Bàn cờ đầy không còn ô trống trả về danh sách rỗng")
        void testFullBoardReturnsEmptyList() {
            Board smallBoard = new Board(2, 2);
            smallBoard.placeStone(0, 0, Board.BLACK);
            smallBoard.placeStone(0, 1, Board.WHITE);
            smallBoard.placeStone(1, 0, Board.BLACK);
            smallBoard.placeStone(1, 1, Board.WHITE);
            assertThat(smallBoard.isFull()).isTrue();

            List<Move> candidates = generator.generate(smallBoard);

            assertThat(candidates).isEmpty();
        }
    }

    @Nested
    @DisplayName("Sinh nước đi xung quanh quân cờ (Nearby Candidates)")
    class NearbyGenerationTests {

        @Test
        @DisplayName("Quân cờ ở giữa với radius = 1 sinh đúng 8 ô bao quanh")
        void testSingleStoneWithRadiusOne() {
            CandidateMoveGenerator radius1Gen = new CandidateMoveGenerator(1);
            board.placeStone(7, 7, Board.BLACK);

            List<Move> candidates = radius1Gen.generate(board);

            // Hình vuông 3x3 trừ ô tâm (7, 7) = 8 ô
            assertThat(candidates).hasSize(8);
            assertThat(candidates).doesNotContain(new Move(7, 7));

            for (Move move : candidates) {
                assertThat(Math.abs(move.x() - 7)).isLessThanOrEqualTo(1);
                assertThat(Math.abs(move.y() - 7)).isLessThanOrEqualTo(1);
                assertThat(board.isEmpty(move.x(), move.y())).isTrue();
            }
        }

        @Test
        @DisplayName("Quân cờ ở giữa với radius = 2 mặc định sinh đúng 24 ô bao quanh")
        void testSingleStoneWithRadiusTwo() {
            board.placeStone(7, 7, Board.BLACK);

            List<Move> candidates = generator.generate(board);

            // Hình vuông 5x5 trừ ô tâm = 24 ô
            assertThat(candidates).hasSize(24);
            assertThat(candidates).doesNotContain(new Move(7, 7));

            for (Move move : candidates) {
                assertThat(Math.abs(move.x() - 7)).isLessThanOrEqualTo(2);
                assertThat(Math.abs(move.y() - 7)).isLessThanOrEqualTo(2);
                assertThat(board.isEmpty(move.x(), move.y())).isTrue();
            }
        }

        @Test
        @DisplayName("Không bao giờ sinh nước đi vào ô đã có quân cờ (cả Đen lẫn Trắng)")
        void testCandidatesNeverIncludeOccupiedCells() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 7, Board.BLACK);

            List<Move> candidates = generator.generate(board);

            assertThat(candidates).doesNotContain(
                    new Move(7, 7),
                    new Move(7, 8),
                    new Move(8, 7)
            );

            for (Move move : candidates) {
                assertThat(board.isEmpty(move.x(), move.y())).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Không trùng lặp nước đi (Deduplication)")
    class DeduplicationTests {

        @Test
        @DisplayName("Nhiều quân cờ gần nhau không sinh ra các nước đi trùng lặp")
        void testNoDuplicateCandidatesForAdjacentStones() {
            // Đặt 3 quân cờ liền kề nhau: vùng radius giao nhau rất nhiều
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(8, 8, Board.BLACK);

            List<Move> candidates = generator.generate(board);

            HashSet<Move> uniqueSet = new HashSet<>(candidates);
            assertThat(candidates).hasSameSizeAs(uniqueSet);
        }
    }

    @Nested
    @DisplayName("Sắp xếp theo độ gần trung tâm (Centrality Sorting)")
    class SortingTests {

        @Test
        @DisplayName("Các nước đi được sắp xếp ưu tiên gần tâm bàn cờ hơn đứng trước")
        void testCandidatesSortedByCentrality() {
            // Đặt 1 quân tại (7, 7) - ngay tâm bàn cờ 15x15 (tâm chính xác là 7.0, 7.0)
            board.placeStone(7, 7, Board.BLACK);

            List<Move> candidates = generator.generate(board);

            double centerX = 7.0;
            double centerY = 7.0;

            // Kiểm tra danh sách đã được sort tăng dần theo bình phương khoảng cách tới tâm
            for (int i = 0; i < candidates.size() - 1; i++) {
                Move m1 = candidates.get(i);
                Move m2 = candidates.get(i + 1);

                double dist1 = Math.pow(m1.x() - centerX, 2) + Math.pow(m1.y() - centerY, 2);
                double dist2 = Math.pow(m2.x() - centerX, 2) + Math.pow(m2.y() - centerY, 2);

                assertThat(dist1).isLessThanOrEqualTo(dist2);
            }

            // Các ô kề cạnh tâm (khoảng cách d^2 = 1): (6,7), (8,7), (7,6), (7,8) phải đứng trước
            // các ô ở góc ngoài cùng của radius 2 (d^2 = 8): ví dụ (5,5), (9,9)
            Move firstMove = candidates.get(0);
            double firstDist = Math.pow(firstMove.x() - centerX, 2) + Math.pow(firstMove.y() - centerY, 2);
            assertThat(firstDist).isEqualTo(1.0);

            Move lastMove = candidates.get(candidates.size() - 1);
            double lastDist = Math.pow(lastMove.x() - centerX, 2) + Math.pow(lastMove.y() - centerY, 2);
            assertThat(lastDist).isEqualTo(8.0);
        }
    }

    @Nested
    @DisplayName("Quân cờ sát biên và các góc bàn cờ (Boundaries & Corners)")
    class BoundaryTests {

        @Test
        @DisplayName("Quân cờ tại góc (0, 0) với radius = 1 sinh đúng 3 ô lân cận hợp lệ")
        void testCornerStoneRadiusOne() {
            CandidateMoveGenerator radius1Gen = new CandidateMoveGenerator(1);
            board.placeStone(0, 0, Board.BLACK);

            List<Move> candidates = radius1Gen.generate(board);

            // Các ô hợp lệ là (0, 1), (1, 0), (1, 1)
            assertThat(candidates).containsExactlyInAnyOrder(
                    new Move(0, 1),
                    new Move(1, 0),
                    new Move(1, 1)
            );

            // Đảm bảo không có ô nào ngoài bàn cờ
            for (Move move : candidates) {
                assertThat(board.isInside(move.x(), move.y())).isTrue();
            }
        }

        @Test
        @DisplayName("Quân cờ tại góc (0, 0) với radius = 2 sinh đúng 8 ô lân cận hợp lệ")
        void testCornerStoneRadiusTwo() {
            board.placeStone(0, 0, Board.BLACK);

            List<Move> candidates = generator.generate(board);

            // Vùng [0..2] x [0..2] có 3x3 = 9 ô, trừ ô (0,0) còn 8 ô
            assertThat(candidates).hasSize(8);
            assertThat(candidates).doesNotContain(new Move(0, 0));

            for (Move move : candidates) {
                assertThat(move.x()).isBetween(0, 2);
                assertThat(move.y()).isBetween(0, 2);
                assertThat(board.isInside(move.x(), move.y())).isTrue();
            }
        }

        @Test
        @DisplayName("Quân cờ tại góc dưới phải (14, 14)")
        void testBottomRightCornerStone() {
            board.placeStone(14, 14, Board.WHITE);

            List<Move> candidates = generator.generate(board);

            // Vùng [12..14] x [12..14] trừ (14,14) = 8 ô
            assertThat(candidates).hasSize(8);
            assertThat(candidates).doesNotContain(new Move(14, 14));

            for (Move move : candidates) {
                assertThat(move.x()).isBetween(12, 14);
                assertThat(move.y()).isBetween(12, 14);
                assertThat(board.isInside(move.x(), move.y())).isTrue();
            }
        }

        @Test
        @DisplayName("Quân cờ sát mép trên (7, 0)")
        void testTopEdgeStone() {
            CandidateMoveGenerator radius1Gen = new CandidateMoveGenerator(1);
            board.placeStone(7, 0, Board.BLACK);

            List<Move> candidates = radius1Gen.generate(board);

            // Hàng 0: (6,0), (8,0). Hàng 1: (6,1), (7,1), (8,1) -> 5 ô
            assertThat(candidates).hasSize(5);
            for (Move move : candidates) {
                assertThat(board.isInside(move.x(), move.y())).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật")
    class RectangularBoardTests {

        @Test
        @DisplayName("Bàn cờ chữ nhật 10x20 rỗng trả về trung tâm (5, 10)")
        void testEmptyRectangularBoardCenter() {
            Board rectBoard = new Board(10, 20);
            List<Move> candidates = generator.generate(rectBoard);

            assertThat(candidates).containsExactly(new Move(5, 10));
        }

        @Test
        @DisplayName("Sinh nước đi và sắp xếp chính xác trên bàn cờ chữ nhật")
        void testRectangularBoardCandidatesGeneration() {
            Board rectBoard = new Board(10, 20);
            rectBoard.placeStone(5, 10, Board.BLACK);

            List<Move> candidates = generator.generate(rectBoard);

            assertThat(candidates).hasSize(24);
            assertThat(candidates).doesNotContain(new Move(5, 10));

            for (Move move : candidates) {
                assertThat(rectBoard.isInside(move.x(), move.y())).isTrue();
                assertThat(rectBoard.isEmpty(move.x(), move.y())).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Tính bất biến của danh sách trả về (Immutability)")
    class ImmutabilityTests {

        @Test
        @DisplayName("Danh sách candidates trả về không thể bị sửa đổi từ bên ngoài")
        void testCandidatesListIsImmutable() {
            board.placeStone(7, 7, Board.BLACK);

            List<Move> candidates = generator.generate(board);

            assertThatThrownBy(() -> candidates.add(new Move(0, 0)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}

