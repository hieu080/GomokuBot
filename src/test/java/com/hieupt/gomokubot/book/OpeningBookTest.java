package com.hieupt.gomokubot.book;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpeningBook Tests")
class OpeningBookTest {

    private OpeningBook book;
    private Board board;

    @BeforeEach
    void setUp() {
        book = new OpeningBook();
        board = new Board(15, 15);
    }

    @Test
    @DisplayName("Bàn cờ rỗng trả về ô trung tâm (7, 7)")
    void testEmptyBoardReturnsCenter() {
        Optional<Move> move = book.getMove(board, Board.BLACK);

        assertThat(move).isPresent();
        assertThat(move.get()).isEqualTo(new Move(7, 7));
    }

    @Test
    @DisplayName("Đen đi (7, 7), Trắng trả về nước khai cuộc hợp lệ kề cạnh hoặc đường chéo")
    void testMove2DirectOrIndirect() {
        board.placeStone(7, 7, Board.BLACK);

        Optional<Move> move = book.getMove(board, Board.WHITE);

        assertThat(move).isPresent();
        assertThat(move.get()).isIn(
                new Move(8, 7),
                new Move(8, 8),
                new Move(7, 8),
                new Move(6, 8)
        );
    }

    @Test
    @DisplayName("Khai cuộc Trực chỉ (7, 7) và (8, 7), Đen chọn khai cuộc kinh điển như Kagetsu (8, 8)")
    void testMove3KagetsuOpening() {
        board.placeStone(7, 7, Board.BLACK);
        board.placeStone(8, 7, Board.WHITE);

        Optional<Move> move = book.getMove(board, Board.BLACK);

        assertThat(move).isPresent();
        assertThat(move.get()).isIn(
                new Move(8, 8), // Kagetsu
                new Move(9, 7), // Matsu
                new Move(8, 6), // Hokuto
                new Move(6, 7)  // Koun
        );
    }

    @Test
    @DisplayName("Thế cờ vượt quá 6 nước hoặc không có trong sách trả về Optional.empty()")
    void testOutOfBookReturnsEmpty() {
        // Đặt 7 quân ngẫu nhiên ở góc bàn cờ
        for (int i = 0; i < 7; i++) {
            board.placeStone(i, 0, i % 2 == 0 ? Board.BLACK : Board.WHITE);
        }

        Optional<Move> move = book.getMove(board, Board.BLACK);

        assertThat(move).isEmpty();
    }
}

