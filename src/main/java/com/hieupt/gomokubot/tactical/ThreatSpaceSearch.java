package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;

import java.util.Objects;
import java.util.Optional;

public class ThreatSpaceSearch {

    private final VCFSearch vcfSearch;
    private final VCTSearch vctSearch;

    public ThreatSpaceSearch() {
        this(new VCFSearch(), new VCTSearch());
    }

    public ThreatSpaceSearch(VCFSearch vcfSearch, VCTSearch vctSearch) {
        this.vcfSearch = Objects.requireNonNull(vcfSearch, "VCFSearch must not be null");
        this.vctSearch = Objects.requireNonNull(vctSearch, "VCTSearch must not be null");
    }

    public VCFSearch getVcfSearch() {
        return vcfSearch;
    }

    public VCTSearch getVctSearch() {
        return vctSearch;
    }

    public VCFResult solveVCF(Board board, byte attacker, int maxDepth) {
        return vcfSearch.search(board, attacker, maxDepth);
    }

    public VCTResult solveVCT(Board board, byte attacker, int maxDepth) {
        return vctSearch.search(board, attacker, maxDepth);
    }

    public Optional<Move> findForcedWinningMove(Board board, byte attacker, int maxDepth) {
        VCFResult vcf = solveVCF(board, attacker, maxDepth);
        if (vcf.found()) {
            return Optional.of(vcf.winningMove());
        }

        VCTResult vct = solveVCT(board, attacker, maxDepth);
        if (vct.found()) {
            return Optional.of(vct.winningMove());
        }

        return Optional.empty();
    }
}
