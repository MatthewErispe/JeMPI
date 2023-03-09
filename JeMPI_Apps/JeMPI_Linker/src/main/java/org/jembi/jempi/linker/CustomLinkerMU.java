package org.jembi.jempi.linker;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.shared.comparisons.ComparisonAlgorithms;
import org.jembi.jempi.shared.models.CustomDemographicData;

import java.util.Objects;


public final class CustomLinkerMU {

   private static final Logger LOGGER = LogManager.getLogger(CustomLinkerMU.class);
   private final Fields fields = new Fields();

   CustomLinkerMU() {
      LOGGER.debug("CustomLinkerMU");
   }

   // TODO change from string algorithm to getting the name of the record
   private void updateMatchedPair(
      final Field field,
      final String left,
      final String right,
      final String algorithm) {
      if (Objects.equals(algorithm, "jw")) {
         if (StringUtils.isBlank(left) || StringUtils.isBlank(right) ||
                 ComparisonAlgorithms.compareJaroWinkler(left, right)) {
            field.matchedPairFieldUnmatched += 1;
         } else {
            field.matchedPairFieldMatched += 1;
         }
      } // TODO add the other options here
   }

   private void updateUnMatchedPair(
         final Field field,
         final String left,
         final String right,
         final String algorithm) {
      if (StringUtils.isBlank(left) || StringUtils.isBlank(right) ||
              ComparisonAlgorithms.compareJaroWinkler(left, right)) {
         field.unMatchedPairFieldUnmatched += 1;
      } else {
         field.unMatchedPairFieldMatched += 1;
      }
   }

   void updateMatchSums(
         final CustomDemographicData patient,
         final CustomDemographicData goldenRecord) {
      updateMatchedPair(fields.givenName, patient.givenName(), goldenRecord.givenName(), "jw");
      updateMatchedPair(fields.familyName, patient.familyName(), goldenRecord.familyName(), "jw");
      updateMatchedPair(fields.gender, patient.gender(), goldenRecord.gender(), "exa");
      updateMatchedPair(fields.dob, patient.dob(), goldenRecord.dob(), "exa");
      updateMatchedPair(fields.city, patient.city(), goldenRecord.city(), "jw");
      updateMatchedPair(fields.phoneNumber, patient.phoneNumber(), goldenRecord.phoneNumber(), "lev");
      updateMatchedPair(fields.nationalId, patient.nationalId(), goldenRecord.nationalId(), "lev");
      LOGGER.debug("{}", fields);
   }

   void updateNonMatchSums(
         final CustomDemographicData patient,
         final CustomDemographicData goldenRecord) {
      updateUnMatchedPair(fields.givenName, patient.givenName(), goldenRecord.givenName(), "jw");
      updateUnMatchedPair(fields.familyName, patient.familyName(), goldenRecord.familyName(), "jw");
      updateUnMatchedPair(fields.gender, patient.gender(), goldenRecord.gender(), "exa");
      updateUnMatchedPair(fields.dob, patient.dob(), goldenRecord.dob(), "exa");
      updateUnMatchedPair(fields.city, patient.city(), goldenRecord.city(), "jw");
      updateUnMatchedPair(fields.phoneNumber, patient.phoneNumber(), goldenRecord.phoneNumber(), "lev");
      updateUnMatchedPair(fields.nationalId, patient.nationalId(), goldenRecord.nationalId(), "lev");
      LOGGER.debug("{}", fields);
   }

   // TODO would be nice to change the name of Field to TallyCount
   static class Field {
      // we are counting all the 4 segments of the square, not necessary but works
      long matchedPairFieldMatched = 0L;        // record match AND field match
      long matchedPairFieldUnmatched = 0L;      // record match AND field non-match
      long unMatchedPairFieldMatched = 0L;      // record non-match AND field match
      long unMatchedPairFieldUnmatched = 0L;    // record non-match AND field non-match
   }

   // hard coded the attributes/fields that we are working with
   static class Fields {
      final Field givenName = new Field();
      final Field familyName = new Field();
      final Field gender = new Field();
      final Field dob = new Field();
      final Field city = new Field();
      final Field phoneNumber = new Field();
      final Field nationalId = new Field();

      private float computeM(final Field field) {
         try {
            return (float) (field.matchedPairFieldMatched)
                    / (float) (field.matchedPairFieldMatched + field.matchedPairFieldUnmatched);
         }
         catch (ArithmeticException e) {
            return (float) (field.matchedPairFieldMatched) / 0.000001f;
         }
      }

      private float computeU(final Field field) {
         try {
            return (float) (field.unMatchedPairFieldMatched)
                    / (float) (field.unMatchedPairFieldMatched + field.unMatchedPairFieldUnmatched);
         }
         catch (ArithmeticException e){
            return (float) (field.unMatchedPairFieldMatched) / 0.000001f;
         }
      }

      @Override
      public String toString() {
         return String.format("f1(%f:%f) f2(%f:%f) f3(%f:%f) f4(%f:%f) f5(%f:%f) f6(%f:%f) f7(%f:%f)",
                              computeM(givenName), computeU(givenName),
                              computeM(familyName), computeU(familyName),
                              computeM(gender), computeU(gender),
                              computeM(dob), computeU(dob),
                              computeM(city), computeU(city),
                              computeM(phoneNumber), computeU(phoneNumber),
                              computeM(nationalId), computeU(nationalId));
      }
   }
}
