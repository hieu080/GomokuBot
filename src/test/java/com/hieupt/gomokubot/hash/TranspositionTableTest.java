package com.hieupt.gomokubot.hash;

import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TranspositionTable Tests")
class TranspositionTableTest {

    private TranspositionTable table;
    private TranspositionTable.ProbeResult result;

    @BeforeEach
    void setUp() {
        // Khởi tạo bảng kích thước 15x15 với capacity 1024
        table = new TranspositionTable(15, 15, 1024);
        result = new TranspositionTable.ProbeResult();
    }

    @Nested
    @DisplayName("Khởi tạo và cấu hình Capacity")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo thành công với kích thước và capacity hợp lệ")
        void testValidInitialization() {
            assertThat(table.getWidth()).isEqualTo(15);
            assertThat(table.getHeight()).isEqualTo(15);
            assertThat(table.getCapacity()).isEqualTo(1024);
        }

        @ParameterizedTest(name = "Requested: {0} -> Power of Two: {1}")
        @CsvSource({
                "1, 1",
                "2, 2",
                "3, 4",
                "5, 8",
                "1000, 1024",
                "1024, 1024",
                "1025, 2048"
        })
        @DisplayName("Capacity luôn được làm tròn lên lũy thừa của 2 gần nhất")
        void testCapacityRoundingUpToPowerOfTwo(int requested, int expectedCapacity) {
            TranspositionTable customTable = new TranspositionTable(15, 15, requested);
            assertThat(customTable.getCapacity()).isEqualTo(expectedCapacity);
        }

        @ParameterizedTest(name = "Kích thước không hợp lệ: width={0}, height={1}")
        @CsvSource({
                "0, 15",
                "15, 0",
                "-1, 15",
                "15, -1",
                "0, 0"
        })
        @DisplayName("Kích thước bàn cờ <= 0 ném IllegalArgumentException")
        void testInvalidDimensionsThrowException(int width, int height) {
            assertThatThrownBy(() -> new TranspositionTable(width, height, 1024))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @ParameterizedTest(name = "Capacity không hợp lệ: {0}")
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("Capacity <= 0 ném IllegalArgumentException")
        void testInvalidCapacityThrowsException(int invalidCapacity) {
            assertThatThrownBy(() -> new TranspositionTable(15, 15, invalidCapacity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Capacity must be positive");
        }

        @Test
        @DisplayName("Capacity vượt quá giới hạn ném IllegalArgumentException")
        void testCapacityTooLargeThrowsException() {
            assertThatThrownBy(() -> new TranspositionTable(15, 15, (1 << 30) + 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Capacity is too large");
        }
    }

    @Nested
    @DisplayName("Kiểm tra tham số đầu vào và xử lý ngoại lệ")
    class ValidationTests {

        @Test
        @DisplayName("probe với ProbeResult null ném NullPointerException")
        void testProbeNullResultThrowsException() {
            assertThatThrownBy(() -> table.probe(12345L, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("ProbeResult must not be null");
        }

        @Test
        @DisplayName("store với depth âm ném IllegalArgumentException")
        void testStoreNegativeDepthThrowsException() {
            assertThatThrownBy(() -> table.store(12345L, -1, 100, TranspositionTable.Bound.EXACT, new Move(7, 7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Depth must not be negative");
        }

        @Test
        @DisplayName("store với Bound null ném NullPointerException")
        void testStoreNullBoundThrowsException() {
            assertThatThrownBy(() -> table.store(12345L, 3, 100, null, new Move(7, 7)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Bound must not be null");
        }

        @ParameterizedTest(name = "BestMove ngoài bàn cờ: ({0}, {1})")
        @CsvSource({
                "-1, 0",
                "0, -1",
                "15, 0",
                "0, 15",
                "20, 20"
        })
        @DisplayName("store với BestMove ngoài biên bàn cờ ném IndexOutOfBoundsException")
        void testStoreMoveOutOfBoundsThrowsException(int x, int y) {
            Move outsideMove = new Move(x, y);
            assertThatThrownBy(() -> table.store(12345L, 3, 100, TranspositionTable.Bound.EXACT, outsideMove))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessageContaining("Move outside board");
        }

        @Test
        @DisplayName("Lấy BestMove khi entry không có bestMove ném IllegalStateException")
        void testGetBestMoveWhenNoneThrowsException() {
            table.store(12345L, 2, 50, TranspositionTable.Bound.EXACT, null);

            boolean hit = table.probe(12345L, result);
            assertThat(hit).isTrue();
            assertThat(result.hasBestMove()).isFalse();

            assertThatThrownBy(() -> result.getBestMoveX())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Entry has no best move");

            assertThatThrownBy(() -> result.getBestMoveY())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Entry has no best move");
        }
    }

    @Nested
    @DisplayName("Lưu trữ và truy xuất cơ bản (Store & Probe)")
    class StoreAndProbeTests {

        @Test
        @DisplayName("Probe hash chưa được lưu trả về false")
        void testProbeMissingHashReturnsFalse() {
            boolean hit = table.probe(999999L, result);
            assertThat(hit).isFalse();
        }

        @Test
        @DisplayName("Lưu và truy xuất thành công với đầy đủ thông tin kể cả BestMove")
        void testStoreAndProbeWithBestMove() {
            long hash = 123456789012345L;
            Move bestMove = new Move(7, 8);

            table.store(hash, 4, 1500, TranspositionTable.Bound.EXACT, bestMove);

            boolean hit = table.probe(hash, result);
            assertThat(hit).isTrue();
            assertThat(result.getDepth()).isEqualTo(4);
            assertThat(result.getScore()).isEqualTo(1500);
            assertThat(result.getBound()).isEqualTo(TranspositionTable.Bound.EXACT);
            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.getBestMoveX()).isEqualTo(7);
            assertThat(result.getBestMoveY()).isEqualTo(8);
        }

        @Test
        @DisplayName("Lưu và truy xuất thành công khi BestMove là null")
        void testStoreAndProbeWithoutBestMove() {
            long hash = 987654321L;

            table.store(hash, 2, -300, TranspositionTable.Bound.LOWER_BOUND, null);

            boolean hit = table.probe(hash, result);
            assertThat(hit).isTrue();
            assertThat(result.getDepth()).isEqualTo(2);
            assertThat(result.getScore()).isEqualTo(-300);
            assertThat(result.getBound()).isEqualTo(TranspositionTable.Bound.LOWER_BOUND);
            assertThat(result.hasBestMove()).isFalse();
        }

        @ParameterizedTest(name = "Bound type: {0}")
        @EnumSource(TranspositionTable.Bound.class)
        @DisplayName("Hỗ trợ lưu và giải mã chính xác tất cả các loại Bound")
        void testAllBoundTypes(TranspositionTable.Bound bound) {
            long hash = 55555L;
            table.store(hash, 3, 200, bound, new Move(0, 0));

            boolean hit = table.probe(hash, result);
            assertThat(hit).isTrue();
            assertThat(result.getBound()).isEqualTo(bound);
        }
    }

    @Nested
    @DisplayName("Chính sách thay thế Entry (Replacement Strategy)")
    class ReplacementStrategyTests {

        @Test
        @DisplayName("Cùng một thế cờ: depth lớn hơn sẽ ghi đè depth nhỏ hơn")
        void testSamePositionDeeperDepthOverwritesShallower() {
            long hash = 42L;

            // Lưu trước với depth = 2
            table.store(hash, 2, 100, TranspositionTable.Bound.LOWER_BOUND, new Move(1, 1));

            // Lưu sau với depth = 4 (sâu hơn)
            table.store(hash, 4, 250, TranspositionTable.Bound.EXACT, new Move(2, 2));

            boolean hit = table.probe(hash, result);
            assertThat(hit).isTrue();
            assertThat(result.getDepth()).isEqualTo(4);
            assertThat(result.getScore()).isEqualTo(250);
            assertThat(result.getBound()).isEqualTo(TranspositionTable.Bound.EXACT);
            assertThat(result.getBestMoveX()).isEqualTo(2);
            assertThat(result.getBestMoveY()).isEqualTo(2);
        }

        @Test
        @DisplayName("Cùng một thế cờ: depth bằng nhau vẫn được cập nhật")
        void testSamePositionEqualDepthOverwrites() {
            long hash = 42L;

            table.store(hash, 3, 100, TranspositionTable.Bound.LOWER_BOUND, new Move(1, 1));
            table.store(hash, 3, 120, TranspositionTable.Bound.EXACT, new Move(3, 3));

            boolean hit = table.probe(hash, result);
            assertThat(hit).isTrue();
            assertThat(result.getScore()).isEqualTo(120);
            assertThat(result.getBound()).isEqualTo(TranspositionTable.Bound.EXACT);
            assertThat(result.getBestMoveX()).isEqualTo(3);
        }

        @Test
        @DisplayName("Cùng một thế cờ: depth nhỏ hơn không được phép ghi đè depth sâu hơn")
        void testSamePositionShallowerDepthDoesNotOverwrite() {
            long hash = 42L;

            // Đã có kết quả sâu với depth = 5
            table.store(hash, 5, 500, TranspositionTable.Bound.EXACT, new Move(7, 7));

            // Cố gắng ghi đè bằng depth = 3
            table.store(hash, 3, 200, TranspositionTable.Bound.UPPER_BOUND, new Move(1, 1));

            boolean hit = table.probe(hash, result);
            assertThat(hit).isTrue();
            assertThat(result.getDepth()).isEqualTo(5);
            assertThat(result.getScore()).isEqualTo(500);
            assertThat(result.getBound()).isEqualTo(TranspositionTable.Bound.EXACT);
            assertThat(result.getBestMoveX()).isEqualTo(7);
            assertThat(result.getBestMoveY()).isEqualTo(7);
        }

        @Test
        @DisplayName("Xung đột chỉ mục (Hash Collision): depth lớn hơn hoặc bằng sẽ thay thế entry cũ")
        void testCollisionDeeperDepthOverwritesDifferentHash() {
            // Khởi tạo bảng capacity cực nhỏ để dễ tạo collision (capacity = 1)
            TranspositionTable tinyTable = new TranspositionTable(15, 15, 1);
            assertThat(tinyTable.getCapacity()).isEqualTo(1);

            long hashA = 100L;
            long hashB = 200L;

            // Lưu hashA với depth = 2
            tinyTable.store(hashA, 2, 50, TranspositionTable.Bound.EXACT, new Move(1, 1));

            // Lưu hashB (xung đột cùng index 0) với depth = 4 (sâu hơn)
            tinyTable.store(hashB, 4, 300, TranspositionTable.Bound.EXACT, new Move(2, 2));

            // hashA bị thay thế, probe hashA trả về false
            assertThat(tinyTable.probe(hashA, result)).isFalse();

            // probe hashB trả về true với depth = 4
            assertThat(tinyTable.probe(hashB, result)).isTrue();
            assertThat(result.getDepth()).isEqualTo(4);
            assertThat(result.getScore()).isEqualTo(300);
        }

        @Test
        @DisplayName("Xung đột chỉ mục (Hash Collision): depth nhỏ hơn không thay thế entry cũ sâu hơn")
        void testCollisionShallowerDepthDoesNotOverwriteDifferentHash() {
            TranspositionTable tinyTable = new TranspositionTable(15, 15, 1);

            long hashDeep = 100L;
            long hashShallow = 200L;

            // Lưu hashDeep với depth = 5
            tinyTable.store(hashDeep, 5, 800, TranspositionTable.Bound.EXACT, new Move(5, 5));

            // Thử lưu hashShallow với depth = 2
            tinyTable.store(hashShallow, 2, 100, TranspositionTable.Bound.EXACT, new Move(1, 1));

            // hashDeep vẫn còn nguyên
            assertThat(tinyTable.probe(hashDeep, result)).isTrue();
            assertThat(result.getDepth()).isEqualTo(5);

            // hashShallow bị từ chối
            assertThat(tinyTable.probe(hashShallow, result)).isFalse();
        }
    }

    @Nested
    @DisplayName("Xóa bảng (clear)")
    class ClearTests {

        @Test
        @DisplayName("clear() xóa sạch mọi entry đã lưu trong bảng")
        void testClearRemovesAllEntries() {
            long hash1 = 111L;
            long hash2 = 222L;

            table.store(hash1, 3, 100, TranspositionTable.Bound.EXACT, new Move(5, 5));
            table.store(hash2, 4, 200, TranspositionTable.Bound.EXACT, new Move(6, 6));

            assertThat(table.probe(hash1, result)).isTrue();
            assertThat(table.probe(hash2, result)).isTrue();

            table.clear();

            assertThat(table.probe(hash1, result)).isFalse();
            assertThat(table.probe(hash2, result)).isFalse();
        }
    }

    @Nested
    @DisplayName("Hỗ trợ bàn cờ hình chữ nhật")
    class RectangularBoardTests {

        @Test
        @DisplayName("Mã hóa và giải mã chính xác tọa độ BestMove trên bàn cờ chữ nhật 10x20")
        void testRectangularBoardMoveEncoding() {
            TranspositionTable rectTable = new TranspositionTable(10, 20, 256);
            long hash = 777L;
            Move move = new Move(9, 19); // Ô góc dưới-phải (x=9, y=19)

            rectTable.store(hash, 5, 999, TranspositionTable.Bound.EXACT, move);

            boolean hit = rectTable.probe(hash, result);
            assertThat(hit).isTrue();
            assertThat(result.hasBestMove()).isTrue();
            assertThat(result.getBestMoveX()).isEqualTo(9);
            assertThat(result.getBestMoveY()).isEqualTo(19);
        }
    }
}

