package org.jembi.jempi.api;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.libmpi.LibMPI;
import org.jembi.jempi.libmpi.MpiGeneralError;
import org.jembi.jempi.libmpi.MpiServiceError;
import org.jembi.jempi.shared.mapper.JsonToFhir;
import org.jembi.jempi.shared.models.*;
import java.util.*;

public final class BackEnd extends AbstractBehavior<BackEnd.Event> {

   private static final Logger LOGGER = LogManager.getLogger(BackEnd.class);
   private static LibMPI libMPI = null;
   private BackEnd(final ActorContext<Event> context) {
      super(context);
      if (libMPI == null) {
         openMPI();
      }
   }

   private BackEnd(
         final ActorContext<Event> context,
         final LibMPI libMPI) {
      super(context);
      BackEnd.libMPI = libMPI;
   }

   public static Behavior<BackEnd.Event> create() {
      return Behaviors.setup(BackEnd::new);
   }

   public static Behavior<Event> create(final LibMPI lib) {
      return Behaviors.setup(context -> new BackEnd(context, lib));
   }

   private static void openMPI() {
      final var host = new String[]{AppConfig.DGRAPH_ALPHA1_HOST, AppConfig.DGRAPH_ALPHA2_HOST,
                                    AppConfig.DGRAPH_ALPHA3_HOST};
      final var port = new int[]{AppConfig.DGRAPH_ALPHA1_PORT, AppConfig.DGRAPH_ALPHA2_PORT,
                                 AppConfig.DGRAPH_ALPHA3_PORT};
      libMPI = new LibMPI(host, port);
   }

   @Override
   public Receive<Event> createReceive() {
      return actor();
   }

   public Receive<Event> actor() {
      ReceiveBuilder<Event> builder = newReceiveBuilder();
      return builder
            .onMessage(GetPatientResourceRequest.class, this::getPatientResourceHandler)
            .build();
   }

   private Behavior<Event> getPatientResourceHandler(final GetPatientResourceRequest request) {
      List<ExpandedPatientRecord> expandedPatientRecords = null;
      ExpandedGoldenRecord expandedGoldenRecord = null;
      String patientResource = "";
      LOGGER.debug("getPatientResource");

      try {
         libMPI.startTransaction();
         expandedPatientRecords = libMPI.findExpandedPatientRecords(List.of(request.patientResourceId));
         libMPI.closeTransaction();
      } catch (Exception exception) {
         LOGGER.error("libMPI.findExpandedPatientRecords failed for patientIds: {} with error: {}",
                      request.patientResourceId,
                      exception.getMessage());
      }

      try {
         libMPI.startTransaction();
         expandedGoldenRecord = libMPI.findExpandedGoldenRecord(request.patientResourceId);
         libMPI.closeTransaction();
      } catch (Exception exception) {
         LOGGER.error("libMPI.findExpandedGoldenRecord failed for goldenId: {} with error: {}",
                      request.patientResourceId,
                      exception.getMessage());
      }

      if (expandedGoldenRecord != null) {
         patientResource = JsonToFhir.mapGoldenRecordToFhirFormat(
               expandedGoldenRecord.goldenRecord(),
               expandedGoldenRecord.patientRecordsWithScore());
         request.replyTo.tell(new GetPatientResourceResponse(Either.right(patientResource)));
      } else if (expandedPatientRecords != null) {
         patientResource = JsonToFhir.mapPatientRecordToFhirFormat(
               expandedPatientRecords.get(0).patientRecord(),
               expandedPatientRecords.get(0).goldenRecordsWithScore());
         request.replyTo.tell(new GetPatientResourceResponse(Either.right(patientResource)));
      } else {
         request.replyTo.tell(new GetPatientResourceResponse(Either.left(new MpiServiceError.PatientIdDoesNotExistError(
               "Record not found for {}",
               request.patientResourceId))));
      }

      return Behaviors.same();
   }

   interface Event {
   }

   interface EventResponse {
   }

   public record GetPatientResourceRequest(
         ActorRef<GetPatientResourceResponse> replyTo,
         String patientResourceId) implements Event {
   }

   public record GetPatientResourceResponse(Either<MpiGeneralError, String> patientResource)
         implements EventResponse {
   }
}
