package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.evaluation.Evaluator;
import com.hieupt.gomokubot.evaluation.PatternRecognizer;
import com.hieupt.gomokubot.hash.TranspositionTable;
import com.hieupt.gomokubot.hash.ZobristHash;
import com.hieupt.gomokubot.movegen.CandidateMoveGenerator;
import com.hieupt.gomokubot.rules.WinChecker;
import com.hieupt.gomokubot.tactical.*;
import org.junit.jupiter.api.Test;

import java.util.List;

public class VCFPhantomTest {

    private int gx(char c) {
        return c - 'A';
    }

    private int gy(int row) {
        return 15 - row;
    }

    private void place(Board b, String pos, byte player) {
        char c = pos.charAt(0);
        int row = Integer.parseInt(pos.substring(1));
        b.placeStone(gx(c), gy(row), player);
    }

    @Test
    void testReproduceVCFOnUserBoard() {
        Board board = new Board(15, 15);
        WinChecker winChecker = new WinChecker(5);
        CandidateMoveGenerator moveGen = new CandidateMoveGenerator(2);
        ThreatDetector detector = new ThreatDetector(winChecker, moveGen);
        ZobristHash zobrist = new ZobristHash(15, 15);
        TranspositionTable tt = new TranspositionTable(15, 15, 100_000);
        Evaluator evaluator = new Evaluator();

        VCFSearch vcfSearch = new VCFSearch(detector, winChecker, zobrist);
        VCTSearch vctSearch = new VCTSearch(detector, vcfSearch, winChecker, zobrist);

        // Black stones (13 stones)
        for (String pos : List.of("I6", "E7", "G7", "F8", "H8", "I8", "J8", "D9", "G9", "B10", "G10", "I10", "D11")) {
            place(board, pos, Board.BLACK);
        }

        // White stones before C7 (12 stones)
        for (String pos : List.of("F7", "H7", "E8", "G8", "E9", "F9", "H9", "I9", "C10", "D10", "E10", "F10")) {
            place(board, pos, Board.WHITE);
        }

        System.out.println("Board before C7:");
        System.out.println("Black diagonal 3: E7(" + gx('E') + "," + gy(7) + "), F8(" + gx('F') + "," + gy(8) + "), G9(" + gx('G') + "," + gy(9) + ")");
        System.out.println("Diagonal endpoints: D6(" + gx('D') + "," + gy(6) + ") and H10(" + gx('H') + "," + gy(10) + ")");

        System.out.println("Black winning moves (immediate 5): " + detector.findWinningMoves(board, Board.BLACK));
        System.out.println("Black four threats: " + detector.findFourThreats(board, Board.BLACK));

        // Test C7 placed
        board.placeStone(gx('C'), gy(7), Board.WHITE);
        System.out.println("After White plays C7, Black four threats: " + detector.findFourThreats(board, Board.BLACK));
        board.removeStone(gx('C'), gy(7));

        // Test D6 placed
        board.placeStone(gx('D'), gy(6), Board.WHITE);
        System.out.println("After White plays D6, Black four threats: " + detector.findFourThreats(board, Board.BLACK));
        board.removeStone(gx('D'), gy(6));

        // Test H10 placed
        board.placeStone(gx('H'), gy(10), Board.WHITE);
        System.out.println("After White plays H10, Black four threats: " + detector.findFourThreats(board, Board.BLACK));
        board.removeStone(gx('H'), gy(10));

        // Check White VCF
        VCFResult whiteVcf = vcfSearch.search(board, Board.WHITE, 12, 2000, 250);
        System.out.println("White VCF found: " + whiteVcf.found());

        // Check White VCT
        VCTResult whiteVct = vctSearch.search(board, Board.WHITE, 8, 2000, 400);
        System.out.println("White VCT found: " + whiteVct.found());
        if (whiteVct.found()) {
            System.out.println("White VCT winning move: " + whiteVct.winningMove() + ", depth: " + whiteVct.depth() + ", sequence: " + whiteVct.winningSequence());
        }

        // Check Full Negamax search WITH VCT
        NegamaxSearch searchWithVct = new NegamaxSearch(evaluator, moveGen, winChecker, zobrist, tt, vcfSearch, vctSearch);
        SearchResult resultWithVct = searchWithVct.search(board, Board.WHITE, SearchLimits.timeLimit(4000));
        System.out.println("WITH VCT, bestMove: " + resultWithVct.bestMove() + ", score: " + resultWithVct.score() + ", depth: " + resultWithVct.completedDepth() + ", nodes: " + resultWithVct.nodesSearched());

        MoveOrdering ordering = new MoveOrdering(moveGen, winChecker);
        List<Move> ordered = ordering.orderMoves(board, Board.WHITE, null, null);
        System.out.println("Top 10 ordered moves for White:");
        for (int i = 0; i < Math.min(10, ordered.size()); i++) {
            Move m = ordered.get(i);
            System.out.println("  " + m + " (" + (char)('A' + m.x()) + (15 - m.y()) + ")");
        }
        System.out.println("--- Pattern breakdown for moves ---");
        PatternRecognizer pr = new PatternRecognizer();
        for (Move m : List.of(new Move(gx('H'), gy(10)), new Move(gx('D'), gy(6)), new Move(gx('E'), gy(11)), new Move(gx('C'), gy(7)))) {
            board.placeStone(m.x(), m.y(), Board.WHITE);
            System.out.println("Move " + m + " (" + (char)('A' + m.x()) + (15 - m.y()) + "):");
            System.out.println("  White patterns: " + pr.recognize(board, Board.WHITE));
            System.out.println("  Black patterns: " + pr.recognize(board, Board.BLACK));
            board.removeStone(m.x(), m.y());
        }

        VCTSearch disabledVctSearch = new VCTSearch(detector, vcfSearch, winChecker, zobrist) {
            @Override
            public VCTResult search(Board b, byte p, int d, long mn, long tm) {
                return VCTResult.failure(0, 0);
            }
        };
        NegamaxSearch searchWithoutVct = new NegamaxSearch(evaluator, moveGen, winChecker, zobrist, tt, vcfSearch, disabledVctSearch);

        System.out.println("--- AlphaBeta score of H10 and D6 at depth 1 to 4 ---");
        for (int d = 1; d <= 4; d++) {
            System.out.println("Depth " + d + ":");
            for (Move m : List.of(new Move(gx('H'), gy(10)), new Move(gx('D'), gy(6)), new Move(gx('E'), gy(11)), new Move(gx('C'), gy(7)))) {
                board.placeStone(m.x(), m.y(), Board.WHITE);
                // Opponent's turn next, so evaluate from Black's perspective and negate
                SearchResult sr = searchWithoutVct.search(board, Board.BLACK, SearchLimits.depthLimit(Math.max(1, d - 1)));
                int scoreForWhite = -sr.score();
                System.out.println("  Move " + (char)('A' + m.x()) + (15 - m.y()) + " -> score: " + scoreForWhite + ", best Black reply: " + sr.bestMove());
                board.removeStone(m.x(), m.y());
            }
        }

        System.out.println("C7 is (" + gx('C') + ", " + gy(7) + ") = Move[x=2, y=8]");
        System.out.println("D6 is (" + gx('D') + ", " + gy(6) + ") = Move[x=3, y=9]");
        System.out.println("H10 is (" + gx('H') + ", " + gy(10) + ") = Move[x=7, y=5]");
    }
}
