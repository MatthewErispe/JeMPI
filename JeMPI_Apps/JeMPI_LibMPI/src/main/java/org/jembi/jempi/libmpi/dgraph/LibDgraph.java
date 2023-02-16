package org.jembi.jempi.libmpi.dgraph;

import io.dgraph.DgraphProto;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.libmpi.LibMPIClientInterface;
import org.jembi.jempi.libmpi.MpiGeneralError;
import org.jembi.jempi.libmpi.MpiServiceError;
import org.jembi.jempi.shared.models.*;
import org.jembi.jempi.shared.utils.LibMPIPaginatedResultSet;
import org.jembi.jempi.shared.utils.SimpleSearchRequestPayload;

import java.util.List;

import static io.dgraph.DgraphProto.Operation.DropOp.DATA;

public class LibDgraph implements LibMPIClientInterface {

   private static final Logger LOGGER = LogManager.getLogger(LibDgraph.class);

   public LibDgraph(
         final String[] host,
         final int[] port) {
      LOGGER.info("{}", "LibDgraph Constructor");

      Client.getInstance().config(host, port);
   }

   /*
    * *******************************************************
    * QUERIES
    * *******************************************************
    *
    */

   public long countPatientRecords() {
      return Queries.countPatients();
   }

   public long countGoldenRecords() {
      return Queries.countGoldenRecords();
   }

   public PatientRecord getPatientRecord(final String uid) {
      return Queries.getDGraphPatientRecord(uid);
   }

   public GoldenRecord getGoldenRecord(final String uid) {
      final var rec = Queries.getGoldenRecordByUid(uid);
      if (rec == null) {
         return null;
      }
      return rec.toGoldenRecord();
   }

   public List<GoldenRecord> getCandidates(
         final CustomDemographicData demographicData,
         final boolean applyDeterministicFilter) {
      final var candidates = CustomLibMPIQueries.getCandidates(demographicData, applyDeterministicFilter);
      return candidates.stream().map(CustomLibMPIGoldenRecord::toGoldenRecord).toList();
   }

   public List<ExpandedPatientRecord> getExpandedPatients(final List<String> ids) {
      final var list = Queries.getExpandedPatientRecords(ids);
      return list.stream().map(CustomLibMPIExpandedPatientRecord::toExpandedPatientRecord).toList();
   }

   public List<ExpandedGoldenRecord> getExpandedGoldenRecords(final List<String> GoldenIds) {
      final var list = Queries.getExpandedGoldenRecordList(GoldenIds);
      return list.stream().map(CustomLibMPIExpandedGoldenRecord::toExpandedGoldenRecord).toList();
   }

   public List<String> getGoldenIds() {
      return Queries.getGoldenIds();
   }

   public LibMPIPaginatedResultSet<ExpandedGoldenRecord> simpleSearchGoldenRecords(
         List<SimpleSearchRequestPayload.SearchParameter> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                                  ) {
      final var list = Queries.simpleSearchGoldenRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIExpandedGoldenRecord::toExpandedGoldenRecord).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   public LibMPIPaginatedResultSet<ExpandedGoldenRecord> customSearchGoldenRecords(
         List<SimpleSearchRequestPayload> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                                  ) {
      final var list = Queries.customSearchGoldenRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIExpandedGoldenRecord::toExpandedGoldenRecord).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   public LibMPIPaginatedResultSet<PatientRecord> simpleSearchPatientRecords(
         List<SimpleSearchRequestPayload.SearchParameter> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                            ) {
      final var list = Queries.simpleSearchPatientRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIDGraphPatientRecord::toPatientRecord).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   public LibMPIPaginatedResultSet<PatientRecord> customSearchPatientRecords(
         List<SimpleSearchRequestPayload> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                            ) {
      final var list = Queries.customSearchPatientRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIDGraphPatientRecord::toPatientRecord).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   /*
    * *******************************************************
    * MUTATIONS
    * *******************************************************
    */

   public boolean updateGoldenRecordField(
         final String uid,
         final String fieldName,
         final String val) {
      return Mutations.updateGoldenRecordField(uid, fieldName, val);
   }

   public Either<MpiGeneralError, LinkInfo> unLink(
         final String goldenUID,
         final String patientUID,
         final float score) {
      return Mutations.unLink(goldenUID, patientUID, score);
   }

   public Either<MpiGeneralError, LinkInfo> updateLink(
         final String goldenUID,
         final String newGoldenUID,
         final String patientUID,
         final float score) {
      return Mutations.updateLink(goldenUID, newGoldenUID, patientUID, score);
   }

   public LinkInfo createPatientAndLinkToExistingGoldenRecord(
         final PatientRecord patientRecord,
         final GoldenUIDScore goldenUIDScore) {
      return Mutations.linkDGraphPatient(patientRecord, goldenUIDScore);
   }

   public LinkInfo createPatientAndLinkToClonedGoldenRecord(
         final PatientRecord patientRecord,
         float score) {
      return Mutations.addNewDGraphPatient(patientRecord);
   }

   public void startTransaction() {
      Client.getInstance().startTransaction();
   }

   public void closeTransaction() {
      Client.getInstance().closeTransaction();
   }

   /*
    * *******************************************************
    * DATABASE
    * *******************************************************
    */

   public Option<MpiGeneralError> dropAll() {
      try {
         Client.getInstance().alter(DgraphProto.Operation.newBuilder().setDropAll(true).build());
         return Option.none();
      } catch (RuntimeException e) {
         LOGGER.error(e.getMessage(), e);
         return Option.of(new MpiServiceError.GeneralError("Drop All Error"));
      }
   }

   public Option<MpiGeneralError> dropAllData() {
      try {
         Client.getInstance().alter(DgraphProto.Operation.newBuilder().setDropOp(DATA).build());
         return Option.none();
      } catch (RuntimeException e) {
         LOGGER.error(e.getMessage());
         return Option.of(new MpiServiceError.GeneralError("Drop All Data Error"));
      }
   }

   public Option<MpiGeneralError> createSchema() {
      return Mutations.createSchema();
   }

}
