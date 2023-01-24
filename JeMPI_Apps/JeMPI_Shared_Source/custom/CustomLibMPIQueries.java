package org.jembi.jempi.libmpi.dgraph;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jembi.jempi.shared.models.CustomEntity;

import static org.jembi.jempi.libmpi.dgraph.Queries.runGoldenRecordQuery;

class CustomLibMPIQueries {

   private CustomLibMPIQueries() {}
   static final String QUERY_DETERMINISTIC_GOLDEN_RECORD_CANDIDATES =
      """
      query query_deterministic_golden_record_candidates($given_name: string, $family_name: string, $phone_number: string, $national_id: string, $nat_fingerprint_code: string) {
         var(func: eq(GoldenRecord.given_name, $given_name)) {
            A as uid
         }
         var(func: eq(GoldenRecord.family_name, $family_name)) {
            B as uid
         }
         var(func: eq(GoldenRecord.phone_number, $phone_number)) {
            C as uid
         }
         var(func: eq(GoldenRecord.national_id, $national_id)) {
            D as uid
         }
         var(func: eq(GoldenRecord.nat_fingerprint_code, $nat_fingerprint_code)) {
            E as uid
         }
         all(func: uid(C,E,B,D,A)) @filter (uid(E) OR (uid(D) AND uid(A) AND uid(B) AND uid(C))) {
            uid
            GoldenRecord.source_id {
               uid
            }
            GoldenRecord.aux_id
            GoldenRecord.nat_fingerprint_code
            GoldenRecord.emr_fingerprint_code
            GoldenRecord.given_name
            GoldenRecord.family_name
            GoldenRecord.gender
            GoldenRecord.dob
            GoldenRecord.city
            GoldenRecord.phone_number
            GoldenRecord.national_id
         }
      }
      """;
      
   static final String QUERY_MATCH_GOLDEN_RECORD_CANDIDATES_BY_DISTANCE =
      """
      query query_match_golden_record_candidates_by_distance($given_name: string, $family_name: string, $city: string) {
         var(func: match(GoldenRecord.given_name, $given_name, 3)) {
            A as uid
         }
         var(func: match(GoldenRecord.family_name, $family_name, 3)) {
            B as uid
         }
         var(func: match(GoldenRecord.city, $city, 3)) {
            C as uid
         }
         all(func: uid(A,B,C)) @filter ((uid(A) AND uid(B)) OR (uid(A) AND uid(C)) OR (uid(B) AND uid(C))) {
            uid
            GoldenRecord.source_id {
               uid
            }
            GoldenRecord.aux_id
            GoldenRecord.nat_fingerprint_code
            GoldenRecord.emr_fingerprint_code
            GoldenRecord.given_name
            GoldenRecord.family_name
            GoldenRecord.gender
            GoldenRecord.dob
            GoldenRecord.city
            GoldenRecord.phone_number
            GoldenRecord.national_id
         }
      }
      """;
      
   static final String QUERY_MATCH_GOLDEN_RECORD_CANDIDATES_BY_PHONE_NUMBER =
      """
      query query_match_golden_record_candidates_by_phone_number($phone_number: string) {
         all(func: eq(GoldenRecord.phone_number, $phone_number)) {
            uid
            GoldenRecord.source_id {
               uid
            }
            GoldenRecord.aux_id
            GoldenRecord.nat_fingerprint_code
            GoldenRecord.emr_fingerprint_code
            GoldenRecord.given_name
            GoldenRecord.family_name
            GoldenRecord.gender
            GoldenRecord.dob
            GoldenRecord.city
            GoldenRecord.phone_number
            GoldenRecord.national_id
         }
      }
      """;
      

   static LibMPIGoldenRecordList queryDeterministicGoldenRecordCandidates(final CustomEntity customEntity) {
      final var givenName = customEntity.givenName();
      final var familyName = customEntity.familyName();
      final var phoneNumber = customEntity.phoneNumber();
      final var nationalId = customEntity.nationalId();
      final var natFingerprintCode = customEntity.natFingerprintCode();
      final var givenNameIsBlank = StringUtils.isBlank(givenName);
      final var familyNameIsBlank = StringUtils.isBlank(familyName);
      final var phoneNumberIsBlank = StringUtils.isBlank(phoneNumber);
      final var nationalIdIsBlank = StringUtils.isBlank(nationalId);
      final var natFingerprintCodeIsBlank = StringUtils.isBlank(natFingerprintCode);
      if ((natFingerprintCodeIsBlank && (nationalIdIsBlank || givenNameIsBlank || familyNameIsBlank || phoneNumberIsBlank))) {
         return new LibMPIGoldenRecordList(List.of());
      }
      final var map = Map.of(
         "$given_name",
         StringUtils.isNotBlank(givenName)
            ? givenName
            : Queries.EMPTY_FIELD_SENTINEL,
         "$family_name",
         StringUtils.isNotBlank(familyName)
            ? familyName
            : Queries.EMPTY_FIELD_SENTINEL,
         "$phone_number",
         StringUtils.isNotBlank(phoneNumber)
            ? phoneNumber
            : Queries.EMPTY_FIELD_SENTINEL,
         "$national_id",
         StringUtils.isNotBlank(nationalId)
            ? nationalId
            : Queries.EMPTY_FIELD_SENTINEL,
         "$nat_fingerprint_code",
         StringUtils.isNotBlank(natFingerprintCode)
            ? natFingerprintCode
            : Queries.EMPTY_FIELD_SENTINEL);
      return runGoldenRecordQuery(QUERY_DETERMINISTIC_GOLDEN_RECORD_CANDIDATES, map);
   }

   static LibMPIGoldenRecordList queryMatchGoldenRecordCandidatesByDistance(final CustomEntity customEntity) {
      final var givenName = customEntity.givenName();
      final var familyName = customEntity.familyName();
      final var city = customEntity.city();
      final var givenNameIsBlank = StringUtils.isBlank(givenName);
      final var familyNameIsBlank = StringUtils.isBlank(familyName);
      final var cityIsBlank = StringUtils.isBlank(city);
      if (((givenNameIsBlank || familyNameIsBlank) && (givenNameIsBlank || cityIsBlank) && (familyNameIsBlank || cityIsBlank))) {
         return new LibMPIGoldenRecordList(List.of());
      }
      final var map = Map.of(
         "$given_name",
         StringUtils.isNotBlank(givenName)
            ? givenName
            : Queries.EMPTY_FIELD_SENTINEL,
         "$family_name",
         StringUtils.isNotBlank(familyName)
            ? familyName
            : Queries.EMPTY_FIELD_SENTINEL,
         "$city",
         StringUtils.isNotBlank(city)
            ? city
            : Queries.EMPTY_FIELD_SENTINEL);
      return runGoldenRecordQuery(QUERY_MATCH_GOLDEN_RECORD_CANDIDATES_BY_DISTANCE, map);
   }

   static LibMPIGoldenRecordList queryMatchGoldenRecordCandidatesByPhoneNumber(final CustomEntity customEntity) {
      if (StringUtils.isBlank(customEntity.phoneNumber())) {
         return new LibMPIGoldenRecordList(List.of());
      }
      final Map<String, String> map = Map.of("$phone_number", customEntity.phoneNumber());
      return runGoldenRecordQuery(QUERY_MATCH_GOLDEN_RECORD_CANDIDATES_BY_PHONE_NUMBER, map);
   }

   private static void updateCandidates(final List<CustomLibMPIGoldenRecord> goldenRecords,
                                        final LibMPIGoldenRecordList block) {
      final var candidates = block.all();
      if (!candidates.isEmpty()) {
         candidates.forEach(candidate -> {
            var found = false;
            for (CustomLibMPIGoldenRecord goldenRecord : goldenRecords) {
               if (candidate.uid().equals(goldenRecord.uid())) {
                  found = true;
                  break;
               }
            }
            if (!found) {
               goldenRecords.add(candidate);
            }
         });
      }
   }

   static List<CustomLibMPIGoldenRecord> getCandidates(final CustomEntity dgraphEntity,
                                                       final boolean applyDeterministicFilter) {

      if (applyDeterministicFilter) {
         final var result = Queries.deterministicFilter(dgraphEntity);
         if (!result.isEmpty()) {
            return result;
         }
      }
      var result = new LinkedList<CustomLibMPIGoldenRecord>();
      updateCandidates(result, queryMatchGoldenRecordCandidatesByDistance(dgraphEntity));
      updateCandidates(result, queryMatchGoldenRecordCandidatesByPhoneNumber(dgraphEntity));
      return result;
   }

}
