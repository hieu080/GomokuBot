package com.hieupt.gomokubot.core;

import java.util.Arrays;

public class Board {
    public static final byte EMPTY = 0;
    public static final byte BLACK = 1;
    public static final byte WHITE = 2;

    private final int width;
    private final int height;

    private final byte[][] cells;

    private int occupiedCount;

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Board dimensions must be positive"
            );
        }

        this.width = width;
        this.height = height;
        this.cells = new byte[height][width];
        this.occupiedCount = 0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public int getEmptyCount() {
        return (width * height) - occupiedCount;
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < width
                && y >= 0 && y < height;
    }

    public byte getCell(int x, int y) {
        validatePosition(x, y);
        return cells[y][x];
    }

    public boolean isEmpty(int x, int y) {
        return getCell(x, y) == EMPTY;
    }

    public boolean isEmpty() {
        return occupiedCount == 0;
    }

    public boolean isFull() {
        return occupiedCount == width * height;
    }

    public void placeStone(int x, int y, byte player) {
        validatePosition(x, y);
        validatePlayer(player);

        if (cells[y][x] != EMPTY) {
            throw new IllegalStateException(
                    "Cell is already occupied at (" + x + ", " + y + ")"
            );
        }

        cells[y][x] = player;
        occupiedCount++;
    }

    public void removeStone(int x, int y) {
        validatePosition(x, y);

        if (cells[y][x] == EMPTY) {
            throw new IllegalStateException(
                    "Cell is already empty at (" + x + ", " + y + ")"
            );
        }

        cells[y][x] = EMPTY;
        occupiedCount--;
    }

    public void reset() {
        for (byte[] row : cells) {
            Arrays.fill(row, EMPTY);
        }

        occupiedCount = 0;
    }

    public Board copy() {
        Board copy = new Board(width, height);

        for (int y = 0; y < height; y++) {
            System.arraycopy(
                    cells[y],
                    0,
                    copy.cells[y],
                    0,
                    width
            );
        }

        copy.occupiedCount = occupiedCount;

        return copy;
    }

    private void validatePosition(int x, int y) {
        if (!isInside(x, y)) {
            throw new IndexOutOfBoundsException(
                    "Position out of board: (" + x + ", " + y + ")"
            );
        }
    }

    private void validatePlayer(byte player) {
        if (player != BLACK && player != WHITE) {
            throw new IllegalArgumentException(
                    "Invalid player value: " + player
            );
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("  ");

        for (int x = 0; x < width; x++) {
            builder.append(String.format("%2d", x));
        }

        builder.append('\n');

        for (int y = 0; y < height; y++) {
            builder.append(String.format("%2d", y));

            for (int x = 0; x < width; x++) {
                char symbol = switch (cells[y][x]) {
                    case BLACK -> 'X';
                    case WHITE -> 'O';
                    default -> '.';
                };

                builder.append(' ').append(symbol);
            }

            builder.append('\n');
        }

        return builder.toString();
    }
}
