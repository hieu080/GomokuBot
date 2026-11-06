package com.hieupt.gomokubot.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchLimits Tests")
class SearchLimitsTest {

    @Nested
    @DisplayName("Các Factory Methods tiện ích")
    class FactoryMethodTests {

        @Test
        @DisplayName("depthLimit chỉ thiết lập maxDepth")
        void testDepthLimit() {
            SearchLimits limits = SearchLimits.depthLimit(6);

            assertThat(limits.maxDepth()).isEqualTo(6);
            assertThat(limits.maxNodes()).isZero();
            assertThat(limits.timeLimitMillis()).isZero();

            assertThat(limits.hasDepthLimit()).isTrue();
            assertThat(limits.hasNodeLimit()).isFalse();
            assertThat(limits.hasTimeLimit()).isFalse();
        }

        @Test
        @DisplayName("nodeLimit chỉ thiết lập maxNodes")
        void testNodeLimit() {
            SearchLimits limits = SearchLimits.nodeLimit(500_000L);

            assertThat(limits.maxDepth()).isZero();
            assertThat(limits.maxNodes()).isEqualTo(500_000L);
            assertThat(limits.timeLimitMillis()).isZero();

            assertThat(limits.hasDepthLimit()).isFalse();
            assertThat(limits.hasNodeLimit()).isTrue();
            assertThat(limits.hasTimeLimit()).isFalse();
        }

        @Test
        @DisplayName("timeLimit chỉ thiết lập timeLimitMillis")
        void testTimeLimit() {
            SearchLimits limits = SearchLimits.timeLimit(3000L);

            assertThat(limits.maxDepth()).isZero();
            assertThat(limits.maxNodes()).isZero();
            assertThat(limits.timeLimitMillis()).isEqualTo(3000L);

            assertThat(limits.hasDepthLimit()).isFalse();
            assertThat(limits.hasNodeLimit()).isFalse();
            assertThat(limits.hasTimeLimit()).isTrue();
        }

        @Test
        @DisplayName("depthAndTime thiết lập cả maxDepth và timeLimitMillis")
        void testDepthAndTime() {
            SearchLimits limits = SearchLimits.depthAndTime(8, 5000L);

            assertThat(limits.maxDepth()).isEqualTo(8);
            assertThat(limits.maxNodes()).isZero();
            assertThat(limits.timeLimitMillis()).isEqualTo(5000L);

            assertThat(limits.hasDepthLimit()).isTrue();
            assertThat(limits.hasNodeLimit()).isFalse();
            assertThat(limits.hasTimeLimit()).isTrue();
        }

        @Test
        @DisplayName("unlimited thiết lập maxDepth là Integer.MAX_VALUE")
        void testUnlimited() {
            SearchLimits limits = SearchLimits.unlimited();

            assertThat(limits.maxDepth()).isEqualTo(Integer.MAX_VALUE);
            assertThat(limits.maxNodes()).isZero();
            assertThat(limits.timeLimitMillis()).isZero();

            assertThat(limits.hasDepthLimit()).isTrue();
            assertThat(limits.hasNodeLimit()).isFalse();
            assertThat(limits.hasTimeLimit()).isFalse();
        }

        @Test
        @DisplayName("Khởi tạo thủ công với cả 3 giới hạn")
        void testCustomConstructorWithAllLimits() {
            SearchLimits limits = new SearchLimits(10, 1_000_000L, 2500L);

            assertThat(limits.maxDepth()).isEqualTo(10);
            assertThat(limits.maxNodes()).isEqualTo(1_000_000L);
            assertThat(limits.timeLimitMillis()).isEqualTo(2500L);

            assertThat(limits.hasDepthLimit()).isTrue();
            assertThat(limits.hasNodeLimit()).isTrue();
            assertThat(limits.hasTimeLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("Kiểm tra tham số đầu vào và xử lý ngoại lệ")
    class ValidationTests {

        @ParameterizedTest(name = "maxDepth âm: {0}")
        @ValueSource(ints = {-1, -5, -100})
        @DisplayName("maxDepth âm ném IllegalArgumentException")
        void testNegativeMaxDepthThrowsException(int depth) {
            assertThatThrownBy(() -> new SearchLimits(depth, 1000L, 1000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum depth must not be negative");
        }

        @ParameterizedTest(name = "maxNodes âm: {0}")
        @ValueSource(longs = {-1L, -500L, -100_000L})
        @DisplayName("maxNodes âm ném IllegalArgumentException")
        void testNegativeMaxNodesThrowsException(long nodes) {
            assertThatThrownBy(() -> new SearchLimits(5, nodes, 1000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum nodes must not be negative");
        }

        @ParameterizedTest(name = "timeLimitMillis âm: {0}")
        @ValueSource(longs = {-1L, -1000L})
        @DisplayName("timeLimitMillis âm ném IllegalArgumentException")
        void testNegativeTimeLimitThrowsException(long time) {
            assertThatThrownBy(() -> new SearchLimits(5, 1000L, time))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Time limit must not be negative");
        }

        @Test
        @DisplayName("Cả 3 giới hạn đều bằng 0 ném IllegalArgumentException")
        void testAllZeroLimitsThrowsException() {
            assertThatThrownBy(() -> new SearchLimits(0, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one search limit must be specified");
        }
    }

    @Nested
    @DisplayName("Equality, HashCode và ToString")
    class RecordContractTests {

        @Test
        @DisplayName("Hai đối tượng cùng giá trị có equals và hashCode bằng nhau")
        void testEqualsAndHashCode() {
            SearchLimits limits1 = SearchLimits.depthAndTime(6, 2000L);
            SearchLimits limits2 = SearchLimits.depthAndTime(6, 2000L);
            SearchLimits limits3 = SearchLimits.depthLimit(6);

            assertThat(limits1).isEqualTo(limits2);
            assertThat(limits1.hashCode()).isEqualTo(limits2.hashCode());
            assertThat(limits1).isNotEqualTo(limits3);
        }

        @Test
        @DisplayName("toString biểu diễn đầy đủ các trường của record")
        void testToString() {
            SearchLimits limits = SearchLimits.depthAndTime(5, 1500L);
            assertThat(limits.toString()).contains("maxDepth=5").contains("timeLimitMillis=1500");
        }
    }
}

