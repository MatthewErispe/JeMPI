package org.jembi.jempi.libmpi.dgraph;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jembi.jempi.shared.models.CustomEntity;

import static org.jembi.jempi.libmpi.dgraph.Queries.runGoldenRecordsQuery;

class CustomLibMPIQueries {

   private CustomLibMPIQueries() {}
   static final String QUERY_DETERMINISTIC_GOLDEN_RECORD_CANDIDATES =
      """
      query query_deterministic_golden_record_candidates($nat_fingerprint_code: string) {
         all(func: eq(GoldenRecord.nat_fingerprint_code, $nat_fingerprint_code)) {
            uid
            GoldenRecord.source_id {
               uid
            }
            GoldenRecord.aux_id
            GoldenRecord.nat_fingerprint_code
            GoldenRecord.emr_fingerprint_code
            GoldenRecord.gender
            GoldenRecord.dob
            GoldenRecord.city
         }
      }
      """;
      

   static LibMPIGoldenRecordList queryDeterministicGoldenRecordCandidates(final CustomEntity customEntity) {
      if (StringUtils.isBlank(customEntity.natFingerprintCode())) {
         return new LibMPIGoldenRecordList(List.of());
      }
      final Map<String, String> map = Map.of("$nat_fingerprint_code", customEntity.natFingerprintCode());
      return runGoldenRecordsQuery(QUERY_DETERMINISTIC_GOLDEN_RECORD_CANDIDATES, map);
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
      return result;
   }

}
