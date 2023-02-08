package org.jembi.jempi.shared.models;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomEntity(String uid,
                           SourceId sourceId,
                           String auxId,
                           String natFingerprintCode,
                           String emrFingerprintCode,
                           String gender,
                           String dob,
                           String city) {
   public CustomEntity() {
      this(null,
           null,
           null,
           null,
           null,
           null,
           null,
           null);
   }
   public String getNames(final CustomEntity entity) {
      return  "";
   }

}
