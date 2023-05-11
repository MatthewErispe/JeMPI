package org.jembi.jempi.api;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.libmpi.MpiGeneralError;
import org.jembi.jempi.libmpi.MpiServiceError;
import org.jembi.jempi.shared.models.*;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Pattern;

import static akka.http.javadsl.server.PathMatchers.segment;

public final class HttpServer extends AllDirectives {

   private static final Logger LOGGER = LogManager.getLogger(HttpServer.class);

   // in-memory refresh token storage
   private static final Function<Entry<String, String>, String> PARAM_STRING = Entry::getValue;
   private CompletionStage<ServerBinding> binding = null;
   private Http http = null;

   private HttpServer() {
   }

   static HttpServer create() {
      return new HttpServer();
   }

   void close(final ActorSystem<Void> actorSystem) {
      binding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
             .thenAccept(unbound -> actorSystem.terminate()); // and shutdown when done
   }

   void open(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Event> backEnd) {
      http = Http.get(actorSystem);
      binding = http.newServerAt(AppConfig.HTTP_SERVER_HOST, AppConfig.HTTP_SERVER_PORT)
                    .bind(this.createFhirRoutes(actorSystem, backEnd));
      LOGGER.info("Server online at http://{}:{}", AppConfig.HTTP_SERVER_HOST, AppConfig.HTTP_SERVER_PORT);
   }

   /*
    *************************** ASK BACKEND ***************************
    */

   private Route mapError(final MpiGeneralError obj) {
      LOGGER.debug("{}", obj);
      return switch (obj) {
         case MpiServiceError.PatientIdDoesNotExistError e -> complete(StatusCodes.BAD_REQUEST, e, Jackson.marshaller());
         case MpiServiceError.GoldenIdDoesNotExistError e -> complete(StatusCodes.BAD_REQUEST, e, Jackson.marshaller());
         case MpiServiceError.GoldenIdPatientConflictError e -> complete(StatusCodes.BAD_REQUEST, e, Jackson.marshaller());
         case MpiServiceError.DeletePredicateError e -> complete(StatusCodes.BAD_REQUEST, e, Jackson.marshaller());
         default -> complete(StatusCodes.INTERNAL_SERVER_ERROR);
      };
   }

   private Route createFhirRoutes(
           final ActorSystem<Void> actorSystem,
           final ActorRef<BackEnd.Event> backEnd) {
              return pathPrefix("fhir", () -> concat(
                          get(() -> concat(path(segment(GlobalConstants.SEGMENT_FHIR_PATIENT).slash(segment(Pattern.compile("^[A-z0-9]+$"))),
                                                (patientResourceId) -> routeGetPatientResource(actorSystem, backEnd, patientResourceId)))))

              );
   }

   private Route routeGetPatientResource(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Event> backEnd,
         final String patientResourceId) {
      return onComplete(askFindPatientResource(actorSystem, backEnd, patientResourceId),
                        result -> result.isSuccess()
                              ? result.get()
                                      .patientResource()
                                      .mapLeft(this::mapError)
                                      .fold(error -> error,
                                            patientResource -> complete(StatusCodes.OK,
                                                                        patientResource
                                                                       ))
                              : complete(StatusCodes.IM_A_TEAPOT));
   }

   private CompletionStage<BackEnd.GetPatientResourceResponse> askFindPatientResource(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Event> backEnd,
         final String patientResourceId) {
      LOGGER.debug("findPatientRecordById : " + patientResourceId);
      final CompletionStage<BackEnd.GetPatientResourceResponse> stage = AskPattern
            .ask(backEnd,
                 replyTo -> new BackEnd.GetPatientResourceRequest(replyTo, patientResourceId),
                 java.time.Duration.ofSeconds(5),
                 actorSystem.scheduler());
      return stage.thenApply(response -> response);
   }

   private interface ApiPaginatedResultSet {
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   private record ApiPagination(@JsonProperty("total") Integer total) {
      static ApiPagination fromLibMPIPagination(final LibMPIPagination pagination) {
         return new ApiPagination(pagination.total());
      }
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   private record ApiGoldenRecordWithScore(
         ApiGoldenRecord goldenRecord,
         Float score) {

      static ApiGoldenRecordWithScore fromGoldenRecordWithScore(final GoldenRecordWithScore goldenRecordWithScore) {
         return new ApiGoldenRecordWithScore(ApiGoldenRecord.fromGoldenRecord(goldenRecordWithScore.goldenRecord()),
                                             goldenRecordWithScore.score());
      }

   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   private record ApiGoldenRecord(
         String uid,
         List<SourceId> sourceId,
         CustomDemographicData demographicData) {

      static ApiGoldenRecord fromGoldenRecord(final GoldenRecord goldenRecord) {
         return new ApiGoldenRecord(goldenRecord.goldenId(), goldenRecord.sourceId(), goldenRecord.demographicData());
      }

   }

   private record ApiExpandedGoldenRecordsPaginatedResultSet(
         List<ApiExpandedGoldenRecord> data,
         ApiPagination pagination) implements ApiPaginatedResultSet {

      static ApiExpandedGoldenRecordsPaginatedResultSet fromLibMPIPaginatedResultSet(
            final LibMPIPaginatedResultSet<ExpandedGoldenRecord> resultSet) {
         final var data = resultSet.data()
                                   .stream()
                                   .map(ApiExpandedGoldenRecord::fromExpandedGoldenRecord)
                                   .toList();
         return new ApiExpandedGoldenRecordsPaginatedResultSet(data, ApiPagination.fromLibMPIPagination(resultSet.pagination()));
      }

   }

   private record ApiPatientRecordsPaginatedResultSet(
         List<ApiPatientRecord> data,
         ApiPagination pagination) implements ApiPaginatedResultSet {

      static ApiPatientRecordsPaginatedResultSet fromLibMPIPaginatedResultSet(
            final LibMPIPaginatedResultSet<PatientRecord> resultSet) {
         final var data = resultSet.data()
                                   .stream()
                                   .map(ApiPatientRecord::fromPatientRecord)
                                   .toList();
         return new ApiPatientRecordsPaginatedResultSet(data, ApiPagination.fromLibMPIPagination(resultSet.pagination()));
      }

   }

   private record ApiGoldenRecordCount(Long count) {
   }

   private record ApiPatientCount(Long count) {
   }

   private record ApiExpandedGoldenRecord(
         ApiGoldenRecord goldenRecord,
         List<ApiPatientRecordWithScore> mpiPatientRecords) {

      static ApiExpandedGoldenRecord fromExpandedGoldenRecord(final ExpandedGoldenRecord expandedGoldenRecord) {
         return new ApiExpandedGoldenRecord(ApiGoldenRecord.fromGoldenRecord(expandedGoldenRecord.goldenRecord()),
                                            expandedGoldenRecord.patientRecordsWithScore()
                                                                .stream()
                                                                .map(ApiPatientRecordWithScore::fromPatientRecordWithScore)
                                                                .toList());
      }

   }

   private record ApiExpandedPatientRecord(
         ApiPatientRecord patientRecord,
         List<ApiGoldenRecordWithScore> goldenRecordsWithScore) {

      static ApiExpandedPatientRecord fromExpandedPatientRecord(final ExpandedPatientRecord expandedPatientRecord) {
         return new ApiExpandedPatientRecord(ApiPatientRecord.fromPatientRecord(expandedPatientRecord.patientRecord()),
                                             expandedPatientRecord.goldenRecordsWithScore()
                                                                  .stream()
                                                                  .map(ApiGoldenRecordWithScore::fromGoldenRecordWithScore)
                                                                  .toList());
      }

   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   private record ApiPatientRecord(
         String uid,
         SourceId sourceId,
         CustomDemographicData demographicData) {

      static ApiPatientRecord fromPatientRecord(final PatientRecord patientRecord) {
         return new ApiPatientRecord(patientRecord.patientId(), patientRecord.sourceId(), patientRecord.demographicData());
      }

   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public record ApiPatientRecordWithScore(
         ApiPatientRecord patientRecord,
         Float score) {

      static ApiPatientRecordWithScore fromPatientRecordWithScore(final PatientRecordWithScore patientRecordWithScore) {
         return new ApiPatientRecordWithScore(ApiPatientRecord.fromPatientRecord(patientRecordWithScore.patientRecord()),
                                              patientRecordWithScore.score());
      }
   }

   private record ApiNumberOfRecords(
         Long goldenRecords,
         Long patientRecords) {
   }

}
