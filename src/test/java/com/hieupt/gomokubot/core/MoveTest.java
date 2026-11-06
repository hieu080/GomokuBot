package com.hieupt.gomokubot.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Move Tests")
class MoveTest {

    @Test
    @DisplayName("Khởi tạo Move và truy xuất tọa độ x, y")
    void testMoveCreationAndAccessors() {
        Move move = new Move(7, 8);
        assertThat(move.x()).isEqualTo(7);
        assertThat(move.y()).isEqualTo(8);
    }

    @Test
    @DisplayName("Kiểm tra tính bằng nhau (equality) và hashCode")
    void testMoveEqualityAndHashCode() {
        Move move1 = new Move(3, 4);
        Move move2 = new Move(3, 4);
        Move move3 = new Move(4, 3);

        assertThat(move1).isEqualTo(move2);
        assertThat(move1.hashCode()).isEqualTo(move2.hashCode());

        assertThat(move1).isNotEqualTo(move3);
        assertThat(move1).isNotEqualTo(null);
        assertThat(move1).isNotEqualTo("Move[x=3, y=4]");
    }

    @Test
    @DisplayName("Kiểm tra định dạng toString của record Move")
    void testMoveToString() {
        Move move = new Move(10, 12);
        assertThat(move.toString()).contains("10").contains("12");
    }
}

