package org.jembi.jempi.libmpi.dgraph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jembi.jempi.shared.models.LibMPIPagination;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record DgraphExpandedPatientRecords(
      @JsonProperty("all") List<CustomDgraphExpandedPatientRecord> all,
      @JsonProperty("pagination") List<LibMPIPagination> pagination) {

   DgraphExpandedPatientRecords(@JsonProperty("all") final List<CustomDgraphExpandedPatientRecord> all) {
      this(all, List.of(new LibMPIPagination(all.size())));
   }

}
