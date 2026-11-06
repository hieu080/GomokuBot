package com.hieupt.gomokubot.book;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;

import java.util.*;

/**
 * Sách khai cuộc chuẩn cho cờ Gomoku (Opening Book).
 * Cung cấp các nước khai cuộc kinh điển (Kagetsu, Matsu, Hokuto, etc.)
 * giúp AI ra quân nhanh (0ms) và chiếm các vị trí chiến lược kiểm soát trung tâm.
 */
public class OpeningBook {

    private final Map<String, List<Move>> bookMoves = new HashMap<>();
    private final Random random;

    public OpeningBook() {
        this(new Random(42));
    }

    public OpeningBook(Random random) {
        this.random = Objects.requireNonNull(random, "Random must not be null");
        initializeBook();
    }

    public Optional<Move> getMove(Board board, byte currentPlayer) {
        Objects.requireNonNull(board, "Board must not be null");

        int occupied = board.getOccupiedCount();
        if (occupied > 6) {
            return Optional.empty(); // Sau nước thứ 6, chuyển sang Engine tính toán
        }

        // Bàn cờ rỗng: nước đầu tiên luôn là trung tâm
        if (occupied == 0) {
            return Optional.of(new Move(board.getWidth() / 2, board.getHeight() / 2));
        }

        // Tạo chuỗi biểu diễn trạng thái các quân cờ hiện tại
        String boardKey = generateBoardKey(board);
        List<Move> candidates = bookMoves.get(boardKey);

        if (candidates != null && !candidates.isEmpty()) {
            Move selected = candidates.get(random.nextInt(candidates.size()));
            if (board.isEmpty(selected.x(), selected.y())) {
                return Optional.of(selected);
            }
        }

        return Optional.empty();
    }

    private void initializeBook() {
        int cx = 7;
        int cy = 7;

        // --- Move 2 (White đáp trả Black tại trung tâm (7, 7)) ---
        // Black: (7, 7)
        // White có thể chọn:
        // 1. Trực chỉ (Direct): (8, 7), (7, 8), (6, 7), (7, 6)
        // 2. Gián chỉ (Indirect): (8, 8), (6, 6), (8, 6), (6, 8)
        registerMoves(
                List.of(new Stone(cx, cy, Board.BLACK)),
                List.of(
                        new Move(cx + 1, cy),     // (8, 7) Trực chỉ
                        new Move(cx + 1, cy + 1), // (8, 8) Gián chỉ
                        new Move(cx, cy + 1),     // (7, 8)
                        new Move(cx - 1, cy + 1)  // (6, 8)
                )
        );

        // --- Move 3 (Black đi tiếp sau khi White đã đặt quân thứ 2) ---
        // Thế 1: Black (7, 7), White (8, 7) [Trực chỉ]
        // Black chọn: Kagetsu (8, 8) - mạnh nhất, Matsu (9, 7), Hokuto (8, 6)
        registerMoves(
                List.of(
                        new Stone(cx, cy, Board.BLACK),
                        new Stone(cx + 1, cy, Board.WHITE)
                ),
                List.of(
                        new Move(cx + 1, cy + 1), // Kagetsu (8, 8)
                        new Move(cx + 2, cy),     // Matsu (9, 7)
                        new Move(cx + 1, cy - 1), // Hokuto (8, 6)
                        new Move(cx - 1, cy)      // Koun (6, 7)
                )
        );

        // Thế 2: Black (7, 7), White (7, 8)
        registerMoves(
                List.of(
                        new Stone(cx, cy, Board.BLACK),
                        new Stone(cx, cy + 1, Board.WHITE)
                ),
                List.of(
                        new Move(cx + 1, cy + 1), // (8, 8)
                        new Move(cx - 1, cy + 1), // (6, 8)
                        new Move(cx, cy + 2)      // (7, 9)
                )
        );

        // Thế 3: Black (7, 7), White (6, 7)
        registerMoves(
                List.of(
                        new Stone(cx, cy, Board.BLACK),
                        new Stone(cx - 1, cy, Board.WHITE)
                ),
                List.of(
                        new Move(cx - 1, cy - 1), // (6, 6)
                        new Move(cx - 1, cy + 1), // (6, 8)
                        new Move(cx - 2, cy)      // (5, 7)
                )
        );

        // Thế 4: Black (7, 7), White (7, 6)
        registerMoves(
                List.of(
                        new Stone(cx, cy, Board.BLACK),
                        new Stone(cx, cy - 1, Board.WHITE)
                ),
                List.of(
                        new Move(cx + 1, cy - 1), // (8, 6)
                        new Move(cx - 1, cy - 1), // (6, 6)
                        new Move(cx, cy - 2)      // (7, 5)
                )
        );

        // Thế Gián chỉ 1: Black (7, 7), White (8, 8)
        registerMoves(
                List.of(
                        new Stone(cx, cy, Board.BLACK),
                        new Stone(cx + 1, cy + 1, Board.WHITE)
                ),
                List.of(
                        new Move(cx + 1, cy),     // Urano (8, 7)
                        new Move(cx, cy + 1),     // Suigetsu (7, 8)
                        new Move(cx + 2, cy + 2), // Keigetsu (9, 9)
                        new Move(cx - 1, cy - 1)  // (6, 6)
                )
        );

        // Thế Gián chỉ 2: Black (7, 7), White (6, 6)
        registerMoves(
                List.of(
                        new Stone(cx, cy, Board.BLACK),
                        new Stone(cx - 1, cy - 1, Board.WHITE)
                ),
                List.of(
                        new Move(cx - 1, cy), // (6, 7)
                        new Move(cx, cy - 1), // (7, 6)
                        new Move(cx + 1, cy + 1) // (8, 8)
                )
        );

        // --- Move 4 (White đối phó với Kagetsu: Black (7, 7) và (8, 8), White (8, 7)) ---
        registerMoves(
                List.of(
                        new Stone(cx, cy, Board.BLACK),
                        new Stone(cx + 1, cy, Board.WHITE),
                        new Stone(cx + 1, cy + 1, Board.BLACK)
                ),
                List.of(
                        new Move(cx, cy + 1), // Chặn (7, 8)
                        new Move(cx - 1, cy), // Phản kích (6, 7)
                        new Move(cx + 2, cy)  // Chặn (9, 7)
                )
        );
    }

    private void registerMoves(List<Stone> stones, List<Move> nextMoves) {
        String key = stonesToKey(stones);
        bookMoves.put(key, nextMoves);
    }

    private String generateBoardKey(Board board) {
        List<Stone> stones = new ArrayList<>();
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                byte p = board.getCell(x, y);
                if (p != Board.EMPTY) {
                    stones.add(new Stone(x, y, p));
                }
            }
        }
        return stonesToKey(stones);
    }

    private String stonesToKey(List<Stone> stones) {
        StringBuilder sb = new StringBuilder();
        stones.stream()
                .sorted(Comparator.comparingInt(Stone::y).thenComparingInt(Stone::x))
                .forEach(s -> sb.append(s.x()).append(',').append(s.y()).append(':').append(s.player()).append(';'));
        return sb.toString();
    }

    private record Stone(int x, int y, byte player) {
    }
}

