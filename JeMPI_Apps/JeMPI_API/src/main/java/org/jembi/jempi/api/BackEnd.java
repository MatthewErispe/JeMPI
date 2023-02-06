package org.jembi.jempi.api;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.http.javadsl.server.directives.FileInfo;
import io.vavr.control.Either;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.api.keycloak.AkkaAdapterConfig;
import org.jembi.jempi.api.keycloak.AkkaKeycloakDeploymentBuilder;
import org.jembi.jempi.api.models.OAuthCodeRequestPayload;
import org.jembi.jempi.libmpi.LibMPI;
import org.jembi.jempi.libmpi.MpiExpandedGoldenRecord;
import org.jembi.jempi.libmpi.MpiGeneralError;
import org.jembi.jempi.linker.CustomLinkerProbabilistic;
import org.jembi.jempi.shared.models.*;
import org.jembi.jempi.api.models.User;
import org.jembi.jempi.postgres.PsqlQueries;
import org.jembi.jempi.shared.utils.LibMPIPaginatedResultSet;
import org.jembi.jempi.shared.utils.SimpleSearchRequestPayload;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class BackEnd extends AbstractBehavior<BackEnd.Event> {

    private static final Logger LOGGER = LogManager.getLogger(BackEnd.class);

    private static LibMPI libMPI = null;
    private AkkaAdapterConfig keycloakConfig;
    private KeycloakDeployment keycloak;

    private BackEnd(ActorContext<Event> context) {
        super(context);
        if (libMPI == null) {
            openMPI();
        }
        // Init keycloak
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream keycloakConfigStream = classLoader.getResourceAsStream("/keycloak.json");
        keycloakConfig = AkkaKeycloakDeploymentBuilder.loadAdapterConfig(keycloakConfigStream);
        keycloak = AkkaKeycloakDeploymentBuilder.build(keycloakConfig);
        LOGGER.debug("Keycloak configured, realm : " + keycloak.getRealm());
    }

    public static Behavior<BackEnd.Event> create() {
        return Behaviors.setup(BackEnd::new);
    }

    private static void openMPI() {
        final var host = new String[] { AppConfig.DGRAPH_ALPHA1_HOST, AppConfig.DGRAPH_ALPHA2_HOST,
                AppConfig.DGRAPH_ALPHA3_HOST };
        final var port = new int[] { AppConfig.DGRAPH_ALPHA1_PORT, AppConfig.DGRAPH_ALPHA2_PORT,
                AppConfig.DGRAPH_ALPHA3_PORT };
        libMPI = new LibMPI(host, port);
    }

    @Override
    public Receive<Event> createReceive() {
        return actor();
    }

    public Receive<Event> actor() {
        ReceiveBuilder<Event> builder = newReceiveBuilder();
        return builder
                .onMessage(EventLoginWithKeycloakRequest.class, this::eventLoginWithKeycloakHandler)
                .onMessage(EventGetGoldenRecordCountReq.class, this::eventGetGoldenRecordCountHandler)
                .onMessage(EventGetDocumentCountReq.class, this::eventGetDocumentCountHandler)
                .onMessage(EventGetNumberOfRecordsReq.class, this::eventGetNumberOfRecordsHandler)
                .onMessage(EventGetGoldenIdListByPredicateReq.class, this::eventGetGoldenIdListByPredicateHandler)
                .onMessage(EventGetGoldenIdListReq.class, this::eventGetGoldenIdListHandler)
                .onMessage(EventFindGoldenRecordByUidRequest.class, this::eventGetGoldenRecordHandler)
                .onMessage(EventGetGoldenRecordDocumentsReq.class, this::eventGetGoldenRecordDocumentsHandler)
                .onMessage(EventFindPatientByUidRequest.class, this::findPatientByIdEventHandler)
                .onMessage(EventGetCandidatesReq.class, this::eventGetCandidatesHandler)
                .onMessage(EventPatchGoldenRecordPredicateReq.class, this::eventPatchGoldenRecordPredicateHandler)
                .onMessage(EventPatchLinkReq.class, this::eventPatchLinkHandler)
                .onMessage(EventGetMatchesForReviewReq.class, this::eventGetMatchesForReviewHandler)
                .onMessage(EventPatchUnLinkReq.class, this::eventPatchUnLinkHandler)
                .onMessage(EventNotificationRequestReq.class, this::eventNotificationRequestHandler)
                .onMessage(EventSimpleSearchGoldenRecordsRequest.class, this::eventSimpleSearchGoldenRecordsHandler)
                .onMessage(EventSimpleSearchPatientRecordsRequest.class, this::eventSimpleSearchPatientRecordsHandler)
                .onMessage(EventPostCsvFileRequest.class, this::eventPostCsvFileRequestHandler)
                .build();
    }

    private Behavior < Event > eventSimpleSearchGoldenRecordsHandler(EventSimpleSearchGoldenRecordsRequest request) {
        SimpleSearchRequestPayload payload = request.simpleSearchRequestPayload();
        List<SimpleSearchRequestPayload.SearchParameter> parameters = payload.parameters();
        Integer offset = payload.offset();
        Integer limit = payload.limit();
        String sortBy = payload.sortBy();
        Boolean sortAsc = payload.sortAsc();
        libMPI.startTransaction();
        var recs = libMPI.simpleSearchGoldenRecords(parameters, offset, limit, sortBy, sortAsc);
        libMPI.closeTransaction();
        request.replyTo.tell(new EventSimpleSearchGoldenRecordsResponse(recs));
        return Behaviors.same();
    }
    private Behavior < Event > eventSimpleSearchPatientRecordsHandler(EventSimpleSearchPatientRecordsRequest request) {
        SimpleSearchRequestPayload payload = request.simpleSearchRequestPayload();
        List<SimpleSearchRequestPayload.SearchParameter> parameters = payload.parameters();
        Integer offset = payload.offset();
        Integer limit = payload.limit();
        String sortBy = payload.sortBy();
        Boolean sortAsc = payload.sortAsc();
        libMPI.startTransaction();
        var recs = libMPI.simpleSearchPatientRecords(parameters, offset, limit, sortBy, sortAsc);
        libMPI.closeTransaction();
        request.replyTo.tell(new EventSimpleSearchPatientRecordsResponse(recs));
        return Behaviors.same();
    }

    private Behavior<Event> eventLoginWithKeycloakHandler(final EventLoginWithKeycloakRequest request) {
        LOGGER.debug("loginWithKeycloak");
        LOGGER.debug("Logging in {}", request.payload);
        try {
            // Exchange code for a token from Keycloak
            AccessTokenResponse tokenResponse = ServerRequest.invokeAccessCodeToToken(keycloak, request.payload.code(),
                    keycloakConfig.getRedirectUri(), request.payload.sessionId());
            LOGGER.debug("Token Exchange succeeded!");

            String tokenString = tokenResponse.getToken();
            String idTokenString = tokenResponse.getIdToken();

            AdapterTokenVerifier.VerifiedTokens tokens = AdapterTokenVerifier.verifyTokens(tokenString, idTokenString,
                    keycloak);
            LOGGER.debug("Token Verification succeeded!");
            AccessToken token = tokens.getAccessToken();
            LOGGER.debug("Is user already registered?");
            String email = token.getEmail();
            User user = PsqlQueries.getUserByEmail(email);
            if (user == null) {
                // Register new user
                LOGGER.debug("User registration ... " + email);
                User newUser = User.buildUserFromToken(token);
                user = PsqlQueries.registerUser(newUser);
            }
            LOGGER.debug("User has signed in : " + user.getEmail());
            request.replyTo.tell(new EventLoginWithKeycloakResponse(user));
            return Behaviors.same();
        } catch (SQLException e) {
            LOGGER.error("failed sql query: " + e.getMessage());
        } catch (VerificationException e) {
            LOGGER.error("failed verification of token: " + e.getMessage());
        } catch (ServerRequest.HttpFailure failure) {
            LOGGER.error("failed to turn code into token");
            LOGGER.error("status from server: " + failure.getStatus());
            if (failure.getError() != null && !failure.getError().trim().isEmpty()) {
                LOGGER.error("   " + failure.getError());
            }
        } catch (IOException e) {
            LOGGER.error("failed to turn code into token", e);
        }
        request.replyTo.tell(new EventLoginWithKeycloakResponse(null));
        return Behaviors.same();
    }

    private Behavior<Event> eventGetMatchesForReviewHandler(final EventGetMatchesForReviewReq request) {
        LOGGER.debug("getMatchesForReview");
        var recs = PsqlQueries.getMatchesForReview();
        request.replyTo.tell(new EventGetMatchesForReviewListRsp(recs));
        return Behaviors.same();
    }

    private Behavior<Event> eventGetGoldenRecordCountHandler(final EventGetGoldenRecordCountReq request) {
        LOGGER.debug("getGoldenRecordCount");
        libMPI.startTransaction();
        final var count = libMPI.countGoldenRecords();
        libMPI.closeTransaction();
        request.replyTo.tell(new EventGetGoldenRecordCountRsp(count));
        return Behaviors.same();
    }

    private Behavior<Event> eventGetDocumentCountHandler(final EventGetDocumentCountReq request) {
        LOGGER.debug("getDocumentCount");
        libMPI.startTransaction();
        final var count = libMPI.countEntities();
        libMPI.closeTransaction();
        request.replyTo.tell(new EventGetDocumentCountRsp(count));
        return Behaviors.same();
    }

    private Behavior<Event> eventGetNumberOfRecordsHandler(final EventGetNumberOfRecordsReq request) {
        LOGGER.debug("getNumberOfRecords");
        libMPI.startTransaction();
        var recs = libMPI.countGoldenRecords();
        var docs = libMPI.countEntities();
        libMPI.closeTransaction();
        request.replyTo.tell(new BackEnd.EventGetNumberOfRecordsRsp(recs, docs));
        return Behaviors.same();
    }

    private Behavior<Event> eventGetGoldenIdListByPredicateHandler(EventGetGoldenIdListByPredicateReq request) {
        LOGGER.debug("getGoldenRecordsByPredicate");
        libMPI.startTransaction();
        var recs = libMPI.getGoldenIdListByPredicate(request.predicate, request.val);
        request.replyTo.tell(new EventGetGoldenIdListByPredicateRsp(recs));
        libMPI.closeTransaction();
        return Behaviors.same();
    }

    private Behavior<Event> eventGetGoldenIdListHandler(final EventGetGoldenIdListReq request) {
        LOGGER.debug("getGoldenIdList");
        libMPI.startTransaction();
        var recs = libMPI.getGoldenIdList();
        request.replyTo.tell(new EventGetGoldenIdListRsp(recs));
        libMPI.closeTransaction();
        return Behaviors.same();
    }

    private Behavior<Event> eventGetGoldenRecordHandler(final EventFindGoldenRecordByUidRequest request) {
        LOGGER.debug("getGoldenRecord");
        libMPI.startTransaction();
        final var rec = libMPI.getGoldenRecord(request.uid);
        request.replyTo.tell(new EventFindGoldenRecordByUidResponse(rec));
        libMPI.closeTransaction();
        return Behaviors.same();
    }

    private Behavior<Event> eventGetGoldenRecordDocumentsHandler(final EventGetGoldenRecordDocumentsReq request) {
        LOGGER.debug("getGoldenRecordDocuments");
        libMPI.startTransaction();
        final var mpiExpandedGoldenRecordList = libMPI.getMpiExpandedGoldenRecordList(request.uids);
        request.replyTo.tell(new EventGetGoldenRecordDocumentsRsp(mpiExpandedGoldenRecordList));
        libMPI.closeTransaction();
        return Behaviors.same();
    }

    private Behavior<Event> findPatientByIdEventHandler(final EventFindPatientByUidRequest request) {
        LOGGER.debug("findPatientById");
        libMPI.startTransaction();
        final var patient = libMPI.getDocument(request.uid);
        request.replyTo.tell(new EventFindPatientRecordByUidResponse(patient));
        libMPI.closeTransaction();
        return Behaviors.same();
    }

    private Behavior<Event> eventGetCandidatesHandler(final EventGetCandidatesReq request) {
        LOGGER.debug("getCandidates");
        LOGGER.debug("{} {}", request.docID, request.mu);
        libMPI.startTransaction();
        final var mpiEntity = libMPI.getMpiEntity(request.docID);
        final var recs = libMPI.getCandidates(mpiEntity, true);

        CustomLinkerProbabilistic.updateMU(request.mu);
        CustomLinkerProbabilistic.checkUpdatedMU();
        final var candidates = recs
                .stream()
                .map(candidate -> new EventGetCandidatesRsp.Candidate(candidate,
                        CustomLinkerProbabilistic.probabilisticScore(candidate, mpiEntity)))
                .toList();
        request.replyTo.tell(new EventGetCandidatesRsp(Either.right(candidates)));
        libMPI.closeTransaction();
        return Behaviors.same();
    }

    private Behavior<Event> eventPatchGoldenRecordPredicateHandler(final EventPatchGoldenRecordPredicateReq request) {
        final var result = libMPI.updateGoldenRecordPredicate(request.goldenID, request.predicate, request.value);
        if (result) {
            request.replyTo.tell(new EventPatchGoldenRecordPredicateRsp(0));
        } else {
            request.replyTo.tell(new EventPatchGoldenRecordPredicateRsp(-1));
        }
        return Behaviors.same();
    }

    private Behavior<Event> eventPatchLinkHandler(final EventPatchLinkReq request) {
        var listLinkInfo = libMPI.updateLink(
                request.goldenID, request.newGoldenID, request.docID, request.score);
        request.replyTo.tell(new EventPatchLinkRsp(listLinkInfo));
        return Behaviors.same();
    }

    private Behavior<Event> eventPatchUnLinkHandler(final EventPatchUnLinkReq request) {
        var linkInfo = libMPI.unLink(
                request.goldenID, request.docID, request.score);
        request.replyTo.tell(new EventPatchUnLinkRsp(linkInfo));
        return Behaviors.same();
    }

    private Behavior<Event> eventNotificationRequestHandler(EventNotificationRequestReq request) {
        try {
            PsqlQueries.updateNotificationState(request.notificationId, request.state);
        } catch (SQLException exception) {
            LOGGER.error(exception.getMessage());
        }
        request.replyTo.tell(new EventNotificationRequestRsp());
        return Behaviors.same();
    }

    private Behavior<Event> eventPostCsvFileRequestHandler(EventPostCsvFileRequest request) throws IOException {
        File file = request.file();
        FileInfo info = request.info();
        try {
                Files.copy(file.toPath(), Paths.get("/app/csv/" + file.getName()));
                LOGGER.debug("File moved successfully");
                file.delete();
        }
        catch (IOException e) { LOGGER.error(e); }
        request.replyTo.tell(new EventPostCsvFileResponse());
        return Behaviors.same();
    }

    interface Event {
    }

    interface EventResponse {
    }

    public record EventGetGoldenRecordCountReq(ActorRef<EventGetGoldenRecordCountRsp> replyTo) implements Event {
    }

    public record EventGetGoldenRecordCountRsp(long count) implements EventResponse {
    }

    public record EventGetDocumentCountReq(ActorRef<EventGetDocumentCountRsp> replyTo) implements Event {
    }

    public record EventGetDocumentCountRsp(long count) implements EventResponse {
    }

    public record EventGetNumberOfRecordsReq(ActorRef<EventGetNumberOfRecordsRsp> replyTo) implements Event {
    }

    public record EventGetNumberOfRecordsRsp(long goldenRecords, long documents) implements EventResponse {
    }

    public record EventGetGoldenIdListByPredicateReq(ActorRef<EventGetGoldenIdListByPredicateRsp> replyTo,
            String predicate,
            String val) implements Event {
    }

    public record EventGetGoldenIdListByPredicateRsp(List<String> records) implements EventResponse {
    }

    public record EventGetGoldenIdListReq(ActorRef<EventGetGoldenIdListRsp> replyTo) implements Event {
    }

    public record EventGetGoldenIdListRsp(List<String> records) implements EventResponse {
    }

    public record EventFindGoldenRecordByUidRequest(ActorRef<EventFindGoldenRecordByUidResponse> replyTo, String uid)
            implements Event {
    }

    public record EventFindGoldenRecordByUidResponse(CustomGoldenRecord goldenRecord) implements EventResponse {
    }

    public record EventGetGoldenRecordDocumentsReq(ActorRef<EventGetGoldenRecordDocumentsRsp> replyTo,
            List<String> uids) implements Event {
    }

    public record EventGetGoldenRecordDocumentsRsp(List<MpiExpandedGoldenRecord> goldenRecords)
            implements EventResponse {
    }

    public record EventFindPatientByUidRequest(ActorRef<EventFindPatientRecordByUidResponse> replyTo,
            String uid) implements Event {
    }

    public record EventGetMatchesForReviewReq(ActorRef<EventGetMatchesForReviewListRsp> replyTo) implements Event {
    }

    public record EventGetMatchesForReviewListRsp(List records) implements EventResponse {
    }

    public record EventFindPatientRecordByUidResponse(CustomEntity document)
            implements EventResponse {
    }

    public record EventPatchGoldenRecordPredicateReq(ActorRef<EventPatchGoldenRecordPredicateRsp> replyTo,
            String goldenID,
            String predicate,
            String value) implements Event {
    }

    public record EventPatchGoldenRecordPredicateRsp(Integer result) implements EventResponse {
    }

    public record EventPatchLinkReq(ActorRef<EventPatchLinkRsp> replyTo,
            String goldenID,
            String newGoldenID,
            String docID,
            Float score) implements Event {
    }

    public record EventPatchLinkRsp(Either<MpiGeneralError, LinkInfo> linkInfo)
            implements EventResponse {
    }

    public record EventPatchUnLinkReq(ActorRef<EventPatchUnLinkRsp> replyTo,
            String goldenID,
            String docID,
            float score) implements Event {
    }

    public record EventPatchUnLinkRsp(Either<MpiGeneralError, LinkInfo> linkInfo)
            implements EventResponse {
    }

    public record EventGetCandidatesReq(ActorRef<EventGetCandidatesRsp> replyTo,
            String docID, CustomMU mu) implements Event {
    }

    public record EventGetCandidatesRsp(Either<MpiGeneralError, List<Candidate>> candidates) implements EventResponse {
        record Candidate(CustomGoldenRecord goldenRecord, float score) {
        }
    }

    public record EventNotificationRequestReq(ActorRef<EventNotificationRequestRsp> replyTo,
            String notificationId,
            String state) implements Event {
    }

    public record EventNotificationRequestRsp() implements EventResponse {
    }

    public record EventLoginWithKeycloakRequest(ActorRef<EventLoginWithKeycloakResponse> replyTo,
            OAuthCodeRequestPayload payload) implements Event {
    }

    public record EventLoginWithKeycloakResponse(User user) implements EventResponse {
    }


    public record EventSimpleSearchGoldenRecordsRequest(ActorRef<EventSimpleSearchGoldenRecordsResponse> replyTo,
                                                        SimpleSearchRequestPayload simpleSearchRequestPayload) implements Event {
    }
    public record EventSimpleSearchGoldenRecordsResponse(LibMPIPaginatedResultSet<MpiExpandedGoldenRecord> records) implements EventResponse {
    }
    public record EventSimpleSearchPatientRecordsRequest(ActorRef<EventSimpleSearchPatientRecordsResponse> replyTo,
                                                        SimpleSearchRequestPayload simpleSearchRequestPayload) implements Event {
    }
    public record EventSimpleSearchPatientRecordsResponse(LibMPIPaginatedResultSet<CustomEntity> records) implements EventResponse {
    }
    public record EventPostCsvFileRequest(ActorRef<EventPostCsvFileResponse> replyTo, FileInfo info, File file)
            implements Event {
    }

    public record EventPostCsvFileResponse() implements EventResponse {
    }

}
