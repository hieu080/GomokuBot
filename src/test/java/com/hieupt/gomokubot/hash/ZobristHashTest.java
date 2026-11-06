package com.hieupt.gomokubot.hash;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ZobristHash Tests")
class ZobristHashTest {

    private ZobristHash zobrist;
    private Board board;

    @BeforeEach
    void setUp() {
        zobrist = new ZobristHash(15, 15);
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và cấu hình kích thước bàn cờ")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo ZobristHash với kích thước tiêu chuẩn")
        void testStandardInitialization() {
            assertThat(zobrist.getWidth()).isEqualTo(15);
            assertThat(zobrist.getHeight()).isEqualTo(15);
        }

        @Test
        @DisplayName("Cùng seed sinh ra cùng một bộ key (Deterministic)")
        void testDeterministicKeysWithSameSeed() {
            long seed = 123456789L;
            ZobristHash z1 = new ZobristHash(15, 15, seed);
            ZobristHash z2 = new ZobristHash(15, 15, seed);

            Board testBoard = new Board(15, 15);
            testBoard.placeStone(7, 7, Board.BLACK);
            testBoard.placeStone(8, 8, Board.WHITE);

            assertThat(z1.computeHash(testBoard, Board.BLACK))
                    .isEqualTo(z2.computeHash(testBoard, Board.BLACK));
        }

        @Test
        @DisplayName("Khác seed sinh ra các hash khác nhau")
        void testDifferentSeedsProduceDifferentHashes() {
            ZobristHash z1 = new ZobristHash(15, 15, 11111L);
            ZobristHash z2 = new ZobristHash(15, 15, 22222L);

            Board testBoard = new Board(15, 15);
            testBoard.placeStone(7, 7, Board.BLACK);

            assertThat(z1.computeHash(testBoard, Board.BLACK))
                    .isNotEqualTo(z2.computeHash(testBoard, Board.BLACK));
        }

        @ParameterizedTest(name = "Kích thước không hợp lệ: width={0}, height={1}")
        @CsvSource({
                "0, 15",
                "15, 0",
                "-1, 15",
                "15, -1",
                "0, 0"
        })
        @DisplayName("Kích thước <= 0 ném IllegalArgumentException")
        void testInvalidDimensionsThrowException(int width, int height) {
            assertThatThrownBy(() -> new ZobristHash(width, height))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }
    }

    @Nested
    @DisplayName("Kiểm tra tham số đầu vào và xử lý ngoại lệ")
    class ValidationTests {

        @Test
        @DisplayName("computeHash với Board null ném NullPointerException")
        void testComputeHashNullBoardThrowsException() {
            assertThatThrownBy(() -> zobrist.computeHash(null, Board.BLACK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Board must not be null");
        }

        @ParameterizedTest(name = "Player không hợp lệ trong computeHash: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("computeHash với player không hợp lệ ném IllegalArgumentException")
        void testComputeHashInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> zobrist.computeHash(board, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player");
        }

        @Test
        @DisplayName("computeHash với Board không khớp kích thước ném IllegalArgumentException")
        void testComputeHashMismatchedBoardDimensionsThrowsException() {
            Board smallBoard = new Board(10, 10);
            assertThatThrownBy(() -> zobrist.computeHash(smallBoard, Board.BLACK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Board dimensions do not match");
        }

        @ParameterizedTest(name = "Tọa độ ngoài bàn cờ: ({0}, {1})")
        @CsvSource({
                "-1, 0",
                "0, -1",
                "15, 0",
                "0, 15",
                "20, 20"
        })
        @DisplayName("updateAfterMove với tọa độ ngoài bàn cờ ném IndexOutOfBoundsException")
        void testUpdateAfterMoveOutOfBoundsThrowsException(int x, int y) {
            assertThatThrownBy(() -> zobrist.updateAfterMove(0L, x, y, Board.BLACK))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessageContaining("Position outside board");
        }

        @ParameterizedTest(name = "Player không hợp lệ trong updateAfterMove: {0}")
        @ValueSource(bytes = {Board.EMPTY, 3, -1, 99})
        @DisplayName("updateAfterMove với player không hợp lệ ném IllegalArgumentException")
        void testUpdateAfterMoveInvalidPlayerThrowsException(byte invalidPlayer) {
            assertThatThrownBy(() -> zobrist.updateAfterMove(0L, 7, 7, invalidPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid player");
        }
    }

    @Nested
    @DisplayName("Tính toán Hash đầy đủ (Full Computation)")
    class ComputeHashTests {

        @Test
        @DisplayName("Bàn cờ rỗng khi lượt của Black có hash = 0L")
        void testEmptyBoardBlackTurnIsZero() {
            long hash = zobrist.computeHash(board, Board.BLACK);
            assertThat(hash).isZero();
        }

        @Test
        @DisplayName("Bàn cờ rỗng khi lượt của White có hash khác 0L (chứa sideToMoveKey)")
        void testEmptyBoardWhiteTurnIsNotZero() {
            long hashWhite = zobrist.computeHash(board, Board.WHITE);
            assertThat(hashWhite).isNotZero();

            long hashBlack = zobrist.computeHash(board, Board.BLACK);
            assertThat(hashWhite).isNotEqualTo(hashBlack);
        }

        @Test
        @DisplayName("Cùng một thế cờ và lượt đi luôn cho ra cùng một giá trị hash")
        void testSameBoardAndTurnProducesSameHash() {
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            long hash1 = zobrist.computeHash(board, Board.BLACK);
            long hash2 = zobrist.computeHash(board, Board.BLACK);

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Cùng thế cờ nhưng khác lượt người chơi cho ra 2 hash khác nhau")
        void testSameBoardDifferentTurnProducesDifferentHash() {
            board.placeStone(7, 7, Board.BLACK);

            long blackTurnHash = zobrist.computeHash(board, Board.BLACK);
            long whiteTurnHash = zobrist.computeHash(board, Board.WHITE);

            assertThat(blackTurnHash).isNotEqualTo(whiteTurnHash);
        }

        @Test
        @DisplayName("Khác vị trí quân cờ cho ra các giá trị hash khác nhau")
        void testDifferentPiecePositionsProduceDifferentHash() {
            Board b1 = new Board(15, 15);
            b1.placeStone(7, 7, Board.BLACK);

            Board b2 = new Board(15, 15);
            b2.placeStone(7, 8, Board.BLACK);

            assertThat(zobrist.computeHash(b1, Board.WHITE))
                    .isNotEqualTo(zobrist.computeHash(b2, Board.WHITE));
        }

        @Test
        @DisplayName("Cùng vị trí nhưng khác màu quân cờ cho ra hash khác nhau")
        void testDifferentPieceColorProducesDifferentHash() {
            Board bBlack = new Board(15, 15);
            bBlack.placeStone(7, 7, Board.BLACK);

            Board bWhite = new Board(15, 15);
            bWhite.placeStone(7, 7, Board.WHITE);

            assertThat(zobrist.computeHash(bBlack, Board.WHITE))
                    .isNotEqualTo(zobrist.computeHash(bWhite, Board.WHITE));
        }
    }

    @Nested
    @DisplayName("Cập nhật tăng dần (Incremental Update)")
    class IncrementalUpdateTests {

        @Test
        @DisplayName("Hash cập nhật tăng dần sau 1 nước đi khớp hoàn toàn với computeHash")
        void testIncrementalUpdateMatchesFullComputeSingleMove() {
            long initialHash = zobrist.computeHash(board, Board.BLACK);

            // Đen đánh vào (7, 7)
            board.placeStone(7, 7, Board.BLACK);
            long expectedHash = zobrist.computeHash(board, Board.WHITE);

            long incrementalHash = zobrist.updateAfterMove(initialHash, 7, 7, Board.BLACK);

            assertThat(incrementalHash).isEqualTo(expectedHash);
        }

        @Test
        @DisplayName("Hash cập nhật tăng dần qua nhiều nước đi liên tiếp khớp hoàn toàn với computeHash")
        void testIncrementalUpdateMatchesFullComputeMultipleMoves() {
            long currentHash = zobrist.computeHash(board, Board.BLACK);

            Move[] moves = {
                    new Move(7, 7), // Đen
                    new Move(7, 8), // Trắng
                    new Move(8, 7), // Đen
                    new Move(8, 8), // Trắng
                    new Move(6, 6)  // Đen
            };

            byte currentTurn = Board.BLACK;

            for (Move move : moves) {
                // Đặt quân trên board
                board.placeStone(move.x(), move.y(), currentTurn);

                // Cập nhật tăng dần
                currentHash = zobrist.updateAfterMove(currentHash, move.x(), move.y(), currentTurn);

                // Chuyển lượt
                currentTurn = (currentTurn == Board.BLACK) ? Board.WHITE : Board.BLACK;

                // Kiểm tra tính nhất quán với computeHash tại mỗi bước
                long expectedHash = zobrist.computeHash(board, currentTurn);
                assertThat(currentHash).isEqualTo(expectedHash);
            }
        }
    }

    @Nested
    @DisplayName("Hoàn tác nước đi (Undo Reversibility)")
    class UndoReversibilityTests {

        @Test
        @DisplayName("updateAfterUndo khôi phục lại chính xác hash trước khi đánh nước đi")
        void testUndoRestoresPreviousHash() {
            long hashBefore = zobrist.computeHash(board, Board.BLACK);

            // Đánh nước đi
            long hashAfterMove = zobrist.updateAfterMove(hashBefore, 7, 7, Board.BLACK);
            assertThat(hashAfterMove).isNotEqualTo(hashBefore);

            // Hoàn tác nước đi
            long hashAfterUndo = zobrist.updateAfterUndo(hashAfterMove, 7, 7, Board.BLACK);

            assertThat(hashAfterUndo).isEqualTo(hashBefore);
        }

        @Test
        @DisplayName("Undo nhiều nước đi liên tiếp khôi phục về trạng thái ban đầu")
        void testUndoMultipleMovesSequentially() {
            long initialHash = zobrist.computeHash(board, Board.BLACK);

            long h1 = zobrist.updateAfterMove(initialHash, 7, 7, Board.BLACK);
            long h2 = zobrist.updateAfterMove(h1, 7, 8, Board.WHITE);
            long h3 = zobrist.updateAfterMove(h2, 8, 8, Board.BLACK);

            // Undo ngược lại
            long u3 = zobrist.updateAfterUndo(h3, 8, 8, Board.BLACK);
            assertThat(u3).isEqualTo(h2);

            long u2 = zobrist.updateAfterUndo(u3, 7, 8, Board.WHITE);
            assertThat(u2).isEqualTo(h1);

            long u1 = zobrist.updateAfterUndo(u2, 7, 7, Board.BLACK);
            assertThat(u1).isEqualTo(initialHash);
        }
    }

    @Nested
    @DisplayName("Tính chất hoán vị thế cờ (Transposition Property)")
    class TranspositionTests {

        @Test
        @DisplayName("Hai chuỗi nước đi khác thứ tự nhưng tạo ra cùng một thế cờ có cùng Hash")
        void testTranspositionHashesMatch() {
            // Chuỗi 1:
            // Đen (7, 7) -> Trắng (7, 8) -> Đen (8, 8) -> Trắng (8, 9)
            long hash1 = 0L;
            hash1 = zobrist.updateAfterMove(hash1, 7, 7, Board.BLACK);
            hash1 = zobrist.updateAfterMove(hash1, 7, 8, Board.WHITE);
            hash1 = zobrist.updateAfterMove(hash1, 8, 8, Board.BLACK);
            hash1 = zobrist.updateAfterMove(hash1, 8, 9, Board.WHITE);

            // Chuỗi 2:
            // Đen (8, 8) -> Trắng (8, 9) -> Đen (7, 7) -> Trắng (7, 8)
            long hash2 = 0L;
            hash2 = zobrist.updateAfterMove(hash2, 8, 8, Board.BLACK);
            hash2 = zobrist.updateAfterMove(hash2, 8, 9, Board.WHITE);
            hash2 = zobrist.updateAfterMove(hash2, 7, 7, Board.BLACK);
            hash2 = zobrist.updateAfterMove(hash2, 7, 8, Board.WHITE);

            // Cả 2 chuỗi đều đưa về:
            // - Đen tại (7,7) và (8,8)
            // - Trắng tại (7,8) và (8,9)
            // - Lượt đi tiếp theo là BLACK
            assertThat(hash1).isEqualTo(hash2);
        }
    }

    @Nested
    @DisplayName("Khả năng kháng xung đột và hỗ trợ bàn cờ chữ nhật")
    class CollisionAndRectangularTests {

        @Test
        @DisplayName("Hỗ trợ bàn cờ chữ nhật 10x20 hoạt động chính xác")
        void testRectangularBoardSupport() {
            ZobristHash rectZobrist = new ZobristHash(10, 20);
            Board rectBoard = new Board(10, 20);

            rectBoard.placeStone(5, 18, Board.BLACK);
            rectBoard.placeStone(9, 19, Board.WHITE);

            long computed = rectZobrist.computeHash(rectBoard, Board.BLACK);

            long incremental = 0L;
            incremental = rectZobrist.updateAfterMove(incremental, 5, 18, Board.BLACK);
            incremental = rectZobrist.updateAfterMove(incremental, 9, 19, Board.WHITE);

            assertThat(incremental).isEqualTo(computed);
        }

        @Test
        @DisplayName("Tất cả các ô trên bàn cờ sinh ra các key độc nhất (không trùng lặp)")
        void testUniqueKeysForDifferentCellsAndPlayers() {
            Set<Long> generatedHashes = new HashSet<>();

            for (int y = 0; y < board.getHeight(); y++) {
                for (int x = 0; x < board.getWidth(); x++) {
                    long blackMoveHash = zobrist.updateAfterMove(0L, x, y, Board.BLACK);
                    long whiteMoveHash = zobrist.updateAfterMove(0L, x, y, Board.WHITE);

                    assertThat(generatedHashes.add(blackMoveHash)).isTrue();
                    assertThat(generatedHashes.add(whiteMoveHash)).isTrue();
                }
            }
        }
    }
}

