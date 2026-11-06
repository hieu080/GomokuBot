package com.hieupt.gomokubot.time;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.search.SearchLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TimeManager Tests")
class TimeManagerTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(15, 15);
    }

    @Nested
    @DisplayName("Khởi tạo và kiểm tra tham số")
    class InitializationTests {

        @Test
        @DisplayName("Khởi tạo mặc định có thời gian 2000ms và không phải dynamic mode")
        void testDefaultConstructor() {
            TimeManager tm = new TimeManager();
            assertThat(tm.getFixedMoveTimeMs()).isEqualTo(2000L);
            assertThat(tm.isDynamicMode()).isFalse();
        }

        @Test
        @DisplayName("Khởi tạo với thời gian tùy biến thành công")
        void testCustomFixedTime() {
            TimeManager tm = new TimeManager(3500L);
            assertThat(tm.getFixedMoveTimeMs()).isEqualTo(3500L);
            assertThat(tm.isDynamicMode()).isFalse();
        }

        @Test
        @DisplayName("Thời gian âm hoặc 0 ném IllegalArgumentException")
        void testInvalidTimeThrowsException() {
            assertThatThrownBy(() -> new TimeManager(0L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TimeManager(-100L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TimeManager(0L, 100L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TimeManager(1000L, -1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("allocateTime với Board null ném NullPointerException")
        void testNullBoardThrowsException() {
            TimeManager tm = new TimeManager();
            assertThatThrownBy(() -> tm.allocateTime(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Phân bổ thời gian (Time Allocation)")
    class AllocationTests {

        @Test
        @DisplayName("Bàn cờ rỗng phân bổ thời gian rất ngắn (50ms)")
        void testEmptyBoardAllocation() {
            TimeManager tm = new TimeManager(2000L);
            long time = tm.allocateTime(board);
            assertThat(time).isEqualTo(50L);
        }

        @Test
        @DisplayName("Giai đoạn khai cuộc (<= 2 quân) phân bổ thời gian ngắn (<= 500ms)")
        void testOpeningAllocation() {
            TimeManager tm = new TimeManager(2000L);
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);

            long time = tm.allocateTime(board);
            assertThat(time).isLessThanOrEqualTo(500L);
        }

        @Test
        @DisplayName("Giai đoạn trung cuộc phân bổ đủ thời gian cấu hình")
        void testMiddleGameAllocation() {
            TimeManager tm = new TimeManager(2000L);
            for (int i = 0; i < 5; i++) {
                board.placeStone(i, 0, Board.BLACK);
            }

            long time = tm.allocateTime(board);
            assertThat(time).isEqualTo(2000L);
        }

        @Test
        @DisplayName("Dynamic mode phân bổ thời gian hợp lý và cập nhật thời gian còn lại")
        void testDynamicMode() {
            TimeManager tm = new TimeManager(60000L, 1000L); // 60s tổng, 1s increment
            assertThat(tm.isDynamicMode()).isTrue();

            for (int i = 0; i < 10; i++) {
                board.placeStone(i, 0, Board.BLACK);
            }

            long allocated = tm.allocateTime(board);
            assertThat(allocated).isBetween(TimeManager.MIN_MOVE_TIME_MS, TimeManager.MAX_MOVE_TIME_MS);

            tm.updateRemainingTime(1500L);
            // 60000 - 1500 + 1000 = 59500
            assertThat(tm.getTotalRemainingTimeMs()).isEqualTo(59500L);
        }
    }

    @Nested
    @DisplayName("Tạo SearchLimits")
    class SearchLimitsCreationTests {

        @Test
        @DisplayName("createSearchLimits trả về giới hạn depthAndTime chính xác")
        void testCreateSearchLimits() {
            TimeManager tm = new TimeManager(2000L);
            board.placeStone(7, 7, Board.BLACK);
            board.placeStone(7, 8, Board.WHITE);
            board.placeStone(6, 6, Board.BLACK);

            SearchLimits limits = tm.createSearchLimits(board, 8);
            assertThat(limits.hasDepthLimit()).isTrue();
            assertThat(limits.maxDepth()).isEqualTo(8);
            assertThat(limits.hasTimeLimit()).isTrue();
            assertThat(limits.timeLimitMillis()).isEqualTo(2000L);
        }
    }
}

