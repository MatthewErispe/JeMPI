package org.jembi.jempi.libmpi.dgraph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jembi.jempi.shared.models.CustomEntity;
import org.jembi.jempi.libmpi.MpiEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
record CustomLibMPIDGraphEntity(@JsonProperty("uid") String uid,
                                @JsonProperty("Entity.source_id") LibMPISourceId sourceId,
                                @JsonProperty("Entity.aux_id") String auxId,
                                @JsonProperty("Entity.nat_fingerprint_code") String natFingerprintCode,
                                @JsonProperty("Entity.emr_fingerprint_code") String emrFingerprintCode,
                                @JsonProperty("Entity.gender") String gender,
                                @JsonProperty("Entity.dob") String dob,
                                @JsonProperty("Entity.city") String city,
                                @JsonProperty("GoldenRecord.entity_list|score") Float score) {
   CustomLibMPIDGraphEntity(final CustomEntity entity, final Float score) {
      this(entity.uid(),
           new LibMPISourceId(entity.sourceId()),
           entity.auxId(),
           entity.natFingerprintCode(),
           entity.emrFingerprintCode(),
           entity.gender(),
           entity.dob(),
           entity.city(),
           score);
   }

   private CustomEntity toCustomEntity() {
      return new CustomEntity(this.uid(),
                              this.sourceId() != null
                                 ? this.sourceId().toSourceId()
                                 : null,
                              this.auxId(),
                              this.natFingerprintCode(),
                              this.emrFingerprintCode(),
                              this.gender(),
                              this.dob(),
                              this.city());
   }

   MpiEntity toMpiEntity() {
      return new MpiEntity(toCustomEntity(), this.score());
   }

}
