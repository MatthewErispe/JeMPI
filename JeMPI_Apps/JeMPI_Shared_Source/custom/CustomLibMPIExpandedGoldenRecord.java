package org.jembi.jempi.libmpi.dgraph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import org.jembi.jempi.shared.models.CustomGoldenRecord;
import org.jembi.jempi.libmpi.MpiExpandedGoldenRecord;

@JsonInclude(JsonInclude.Include.NON_NULL)
record CustomLibMPIExpandedGoldenRecord(@JsonProperty("uid") String uid,
                                        @JsonProperty("GoldenRecord.source_id") List<LibMPISourceId> sourceId,
                                        @JsonProperty("GoldenRecord.aux_id") String auxId,
                                        @JsonProperty("GoldenRecord.nat_fingerprint_code") String natFingerprintCode,
                                        @JsonProperty("GoldenRecord.emr_fingerprint_code") String emrFingerprintCode,
                                        @JsonProperty("GoldenRecord.gender") String gender,
                                        @JsonProperty("GoldenRecord.dob") String dob,
                                        @JsonProperty("GoldenRecord.city") String city,
                                        @JsonProperty("GoldenRecord.entity_list") List<CustomLibMPIDGraphEntity> dgraphEntityList) {


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

   MpiExpandedGoldenRecord toMpiExpandedGoldenRecord() {
      return new MpiExpandedGoldenRecord(this.toCustomGoldenRecord(),
                                         this.dgraphEntityList()
                                             .stream()
                                             .map(CustomLibMPIDGraphEntity::toMpiEntity)
                                             .toList());
   }

}
