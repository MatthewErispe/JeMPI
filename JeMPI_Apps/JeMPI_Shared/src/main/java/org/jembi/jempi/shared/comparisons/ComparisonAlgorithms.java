package org.jembi.jempi.shared.comparisons;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class ComparisonAlgorithms {

    private static final JaroWinklerSimilarity JARO_WINKLER_SIMILARITY = new JaroWinklerSimilarity();

    public static boolean compareJaroWinkler(
            final String left,
            final String right) {
        return JARO_WINKLER_SIMILARITY.apply(left, right) <= 0.92;
    }

    public static boolean compareJaroWinkler(
            final String left,
            final String right,
            final Float similarity) {
        return JARO_WINKLER_SIMILARITY.apply(left, right) <= similarity;
    }

    public static boolean compareLevenshtein(
            final String left,
            final String right){
        return true;
    }

    public static boolean compareLevenshtein(
            final String left,
            final String right,
            final Integer distance){
        return true;
    }

}
