package org.jembi.jempi.linker;

import org.jembi.jempi.libmpi.LibMPI;
import org.jembi.jempi.shared.models.CustomEntity;

import java.util.List;

public final class CustomLinkerBackEnd {

   private CustomLinkerBackEnd() {}

   static void updateGoldenRecordFields(final LibMPI libMPI, final String uid) {
      final var expandedGoldenRecord = libMPI.getMpiExpandedGoldenRecordList(List.of(uid)).get(0);


   }

}
