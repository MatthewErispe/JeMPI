package org.jembi.jempi.shared.comparisons;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class ComparisonAlgorithms {

    private static final JaroWinklerSimilarity JARO_WINKLER_SIMILARITY = new JaroWinklerSimilarity();
    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = new LevenshteinDistance();

    public static double JaroWinklerScore(
            final String left,
            final String right){
        System.out.println(JARO_WINKLER_SIMILARITY.apply(left, right));
        return JARO_WINKLER_SIMILARITY.apply(left, right);
    }

    public static boolean compareJaroWinkler(
            final String left,
            final String right) {
        return JARO_WINKLER_SIMILARITY.apply(left, right) >= 0.92;
    }

    public static boolean compareJaroWinkler(
            final String left,
            final String right,
            final Float similarity) {
        return JARO_WINKLER_SIMILARITY.apply(left, right) >= similarity;
    }

    public static boolean compareLevenshtein(
            final String left,
            final String right){
        return LEVENSHTEIN_DISTANCE.apply(left, right) <= 2;
    }

    public static boolean compareLevenshtein(
            final String left,
            final String right,
            final Integer distance){
        return LEVENSHTEIN_DISTANCE.apply(left, right) <= distance;
    }

}
