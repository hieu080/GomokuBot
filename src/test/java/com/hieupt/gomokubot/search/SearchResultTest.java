package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchResult Tests")
class SearchResultTest {

    @Nested
    @DisplayName("Khởi tạo và truy xuất thuộc tính")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo kết quả tìm kiếm hoàn tất thành công")
        void testCompletedSearchResult() {
            Move bestMove = new Move(7, 7);
            SearchResult result = new SearchResult(
                    bestMove,
                    1500,
                    6,
                    50_000L,
                    1000L,
                    true
            );

            assertThat(result.bestMove()).isEqualTo(bestMove);
            assertThat(result.score()).isEqualTo(1500);
            assertThat(result.completedDepth()).isEqualTo(6);
            assertThat(result.nodesSearched()).isEqualTo(50_000L);
            assertThat(result.elapsedTimeMillis()).isEqualTo(1000L);
            assertThat(result.completed()).isTrue();
            assertThat(result.hasBestMove()).isTrue();
        }

        @Test
        @DisplayName("Khởi tạo kết quả tìm kiếm chưa hoàn tất khi bestMove là null (ví dụ timeout ngay lập tức)")
        void testIncompleteSearchResultWithoutBestMove() {
            SearchResult result = new SearchResult(
                    null,
                    0,
                    0,
                    100L,
                    50L,
                    false
            );

            assertThat(result.bestMove()).isNull();
            assertThat(result.completed()).isFalse();
            assertThat(result.hasBestMove()).isFalse();
        }

        @Test
        @DisplayName("Khởi tạo kết quả tìm kiếm chưa hoàn tất nhưng đã có bestMove tạm thời")
        void testIncompleteSearchResultWithPartialBestMove() {
            Move partialMove = new Move(6, 6);
            SearchResult result = new SearchResult(
                    partialMove,
                    200,
                    3,
                    15_000L,
                    1500L,
                    false
            );

            assertThat(result.bestMove()).isEqualTo(partialMove);
            assertThat(result.completed()).isFalse();
            assertThat(result.hasBestMove()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tính toán tốc độ duyệt nốt (Nodes Per Second - NPS)")
    class NodesPerSecondTests {

        @Test
        @DisplayName("Tính chính xác NPS khi elapsed time > 0")
        void testNodesPerSecondCalculation() {
            // 50,000 nodes trong 1,000 ms -> 50,000 * 1000.0 / 1000 = 50,000.0 NPS
            SearchResult r1 = new SearchResult(new Move(7, 7), 0, 4, 50_000L, 1000L, true);
            assertThat(r1.getNodesPerSecond()).isEqualTo(50_000.0);

            // 10,000 nodes trong 500 ms -> 20,000.0 NPS
            SearchResult r2 = new SearchResult(new Move(7, 7), 0, 4, 10_000L, 500L, true);
            assertThat(r2.getNodesPerSecond()).isEqualTo(20_000.0);
        }

        @Test
        @DisplayName("Khi elapsed time = 0ms thì trả về 0.0 NPS để tránh chia cho 0")
        void testNodesPerSecondWhenElapsedTimeIsZero() {
            SearchResult result = new SearchResult(new Move(7, 7), 0, 1, 100L, 0L, true);
            assertThat(result.getNodesPerSecond()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Khi không duyệt nốt nào (0 nodes) thì trả về 0.0 NPS")
        void testNodesPerSecondWhenNodesSearchedIsZero() {
            SearchResult result = new SearchResult(new Move(7, 7), 0, 0, 0L, 500L, true);
            assertThat(result.getNodesPerSecond()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Kiểm tra tham số đầu vào và xử lý ngoại lệ")
    class ValidationTests {

        @ParameterizedTest(name = "completedDepth âm: {0}")
        @ValueSource(ints = {-1, -5})
        @DisplayName("completedDepth âm ném IllegalArgumentException")
        void testNegativeCompletedDepthThrowsException(int depth) {
            assertThatThrownBy(() -> new SearchResult(new Move(7, 7), 0, depth, 100L, 100L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Completed depth must not be negative");
        }

        @ParameterizedTest(name = "nodesSearched âm: {0}")
        @ValueSource(longs = {-1L, -100L})
        @DisplayName("nodesSearched âm ném IllegalArgumentException")
        void testNegativeNodesSearchedThrowsException(long nodes) {
            assertThatThrownBy(() -> new SearchResult(new Move(7, 7), 0, 3, nodes, 100L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nodes searched must not be negative");
        }

        @ParameterizedTest(name = "elapsedTimeMillis âm: {0}")
        @ValueSource(longs = {-1L, -100L})
        @DisplayName("elapsedTimeMillis âm ném IllegalArgumentException")
        void testNegativeElapsedTimeThrowsException(long time) {
            assertThatThrownBy(() -> new SearchResult(new Move(7, 7), 0, 3, 100L, time, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Elapsed time must not be negative");
        }

        @Test
        @DisplayName("completed = true nhưng bestMove = null vẫn hoàn toàn hợp lệ khi bàn cờ kết thúc hoặc hết nước đi")
        void testCompletedTrueWithoutBestMoveIsValid() {
            SearchResult result = new SearchResult(null, 0, 4, 10_000L, 500L, true);

            assertThat(result.completed()).isTrue();
            assertThat(result.bestMove()).isNull();
            assertThat(result.hasBestMove()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality, HashCode và ToString")
    class RecordContractTests {

        @Test
        @DisplayName("Hai đối tượng cùng giá trị có equals và hashCode bằng nhau")
        void testEqualsAndHashCode() {
            Move move = new Move(7, 7);
            SearchResult r1 = new SearchResult(move, 100, 4, 1000L, 500L, true);
            SearchResult r2 = new SearchResult(move, 100, 4, 1000L, 500L, true);
            SearchResult r3 = new SearchResult(move, 200, 4, 1000L, 500L, true);

            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
            assertThat(r1).isNotEqualTo(r3);
        }

        @Test
        @DisplayName("toString biểu diễn đầy đủ các thông tin của SearchResult")
        void testToString() {
            SearchResult result = new SearchResult(new Move(7, 7), 100, 4, 1000L, 500L, true);
            assertThat(result.toString())
                    .contains("score=100")
                    .contains("completedDepth=4")
                    .contains("completed=true");
        }
    }
}

