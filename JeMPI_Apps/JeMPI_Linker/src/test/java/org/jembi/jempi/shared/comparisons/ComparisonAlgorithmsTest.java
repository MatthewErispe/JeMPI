package org.jembi.jempi.shared.comparisons;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComparisonAlgorithmsTest {

    @Test
    void testJaroWinklerScore(){
        assert (ComparisonAlgorithms.JaroWinklerScore("muhammet", "muhammed")) == 0.95;
        // online says differently - https://tilores.io/jaro-similarity-algorithm-online-tool?t1=muhammed&t2=muhammet
    }
    @Test
    void testCompareJaroWinkler() {
        assert (ComparisonAlgorithms.compareJaroWinkler("muhammed", "muhammet"));
        assert (ComparisonAlgorithms.compareJaroWinkler("bryan", "bruce", 0.55f));
        assert (ComparisonAlgorithms.compareJaroWinkler("henry", "harry", 0.7f));
    }

    @Test
    void testCompareLevenshtein() {
    }
}