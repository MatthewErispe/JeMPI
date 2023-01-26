package org.jembi.jempi.libmpi.dgraph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import org.jembi.jempi.shared.models.CustomGoldenRecord;

@JsonInclude(JsonInclude.Include.NON_NULL)
record CustomLibMPIGoldenRecord (@JsonProperty("uid") String uid,
                                 @JsonProperty("GoldenRecord.source_id") List<LibMPISourceId> sourceId,
                                 @JsonProperty("GoldenRecord.aux_id") String auxId,
                                 @JsonProperty("GoldenRecord.nat_fingerprint_code") String natFingerprintCode,
                                 @JsonProperty("GoldenRecord.emr_fingerprint_code") String emrFingerprintCode,
                                 @JsonProperty("GoldenRecord.gender") String gender,
                                 @JsonProperty("GoldenRecord.dob") String dob,
                                 @JsonProperty("GoldenRecord.city") String city) {

   CustomLibMPIGoldenRecord(final CustomLibMPIDGraphEntity dgraphEntity) {
      this(null,
           List.of(dgraphEntity.sourceId()),
           dgraphEntity.auxId(),
           dgraphEntity.natFingerprintCode(),
           dgraphEntity.emrFingerprintCode(),
           dgraphEntity.gender(),
           dgraphEntity.dob(),
           dgraphEntity.city());
   }

   CustomGoldenRecord toCustomGoldenRecord() {
      return new CustomGoldenRecord(this.uid(),
                                    this.sourceId() != null
                                      ? this.sourceId().stream().map(LibMPISourceId::toSourceId).toList()
                                      : List.of(),
                                    this.auxId(),
                                    this.natFingerprintCode(),
                                    this.emrFingerprintCode(),
                                    this.gender(),
                                    this.dob(),
                                    this.city());
   }

}
