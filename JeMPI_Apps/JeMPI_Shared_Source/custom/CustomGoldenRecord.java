package org.jembi.jempi.shared.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomGoldenRecord(String uid,
                                 List<SourceId> sourceId,
                                 String auxId,
                                 String natFingerprintCode,
                                 String emrFingerprintCode,
                                 String gender,
                                 String dob,
                                 String city) {
   public CustomGoldenRecord() {
      this(null,
           null,
           null,
           null,
           null,
           null,
           null,
           null);
   }

   public CustomGoldenRecord(final CustomEntity entity) {
      this(null,
           List.of(entity.sourceId()),
           entity.auxId(),
           entity.natFingerprintCode(),
           entity.emrFingerprintCode(),
           entity.gender(),
           entity.dob(),
           entity.city());
   }

}
