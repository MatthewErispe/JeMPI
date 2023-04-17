package org.jembi.jempi.libmpi.dgraph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jembi.jempi.shared.models.CustomDemographicData;
import org.jembi.jempi.shared.models.GoldenRecord;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record CustomDgraphGoldenRecord(
      @JsonProperty("uid") String goldenId,
      @JsonProperty("GoldenRecord.source_id") List<DgraphSourceId> sourceId,
      @JsonProperty("GoldenRecord.aux_id") String auxId,
      @JsonProperty("GoldenRecord.aux_dwh_id1") String auxDwhId1,
      @JsonProperty("GoldenRecord.aux_dwh_id2") String auxDwhId2,
      @JsonProperty("GoldenRecord.phonetic_given_name") String phoneticGivenName,
      @JsonProperty("GoldenRecord.phonetic_family_name") String phoneticFamilyName,
      @JsonProperty("GoldenRecord.gender") String gender,
      @JsonProperty("GoldenRecord.dob") String dob,
      @JsonProperty("GoldenRecord.nupi") String nupi) {

   CustomDgraphGoldenRecord(final CustomDgraphPatientRecord rec) {
      this(null,
           List.of(rec.sourceId()),
           rec.auxId(),
           rec.auxDwhId1(),
           rec.auxDwhId2(),
           rec.phoneticGivenName(),
           rec.phoneticFamilyName(),
           rec.gender(),
           rec.dob(),
           rec.nupi());
   }

   GoldenRecord toGoldenRecord() {
      return new GoldenRecord(this.goldenId(),
                              this.sourceId() != null
                                    ? this.sourceId().stream().map(DgraphSourceId::toSourceId).toList()
                                    : List.of(),
                              new CustomDemographicData(this.auxId(),
                                                        this.auxDwhId1(),
                                                        this.auxDwhId2(),
                                                        this.phoneticGivenName(),
                                                        this.phoneticFamilyName(),
                                                        this.gender(),
                                                        this.dob(),
                                                        this.nupi()));
   }

}
