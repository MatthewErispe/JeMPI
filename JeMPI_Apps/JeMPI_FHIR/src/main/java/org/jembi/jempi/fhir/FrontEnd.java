package org.jembi.jempi.fhir;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.shared.models.*;
import java.util.regex.Pattern;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.segment;

public final class FrontEnd extends AllDirectives {

   private static final Logger LOGGER = LogManager.getLogger(FrontEnd.class);

   private CompletionStage<ServerBinding> binding = null;
   private Http http = null;

   void close(final ActorSystem<Void> system) {
      binding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
             .thenAccept(unbound -> system.terminate()); // and shutdown when done
   }

   void open(
         final ActorSystem<Void> system,
         final ActorRef<BackEnd.Event> backEnd) {
      http = Http.get(system);
      binding = http.newServerAt(AppConfig.HTTP_SERVER_HOST,
                                 AppConfig.HTTP_SERVER_PORT)
                    .bind(this.createRoute(system, backEnd));
      LOGGER.info("Server online at http://{}:{}", AppConfig.HTTP_SERVER_HOST, AppConfig.HTTP_SERVER_PORT);
   }

   private CompletionStage<HttpResponse> findExpandedPatientRecords(final String patientResourceId) {
      final var request = HttpRequest
            .create("http://api:50000/JeMPI/expanded-patient-records?uidList=" + patientResourceId)
            .withMethod(HttpMethods.GET);
      final var stage = http.singleRequest(request);
      return stage.thenApply(response -> response);
   }

   private Route routeFindExpandedPatientRecords(final String patientResourceId) {
      return onComplete(findExpandedPatientRecords(patientResourceId),
                        response -> response.isSuccess()
                              ? complete(response.get())
                              : complete(StatusCodes.IM_A_TEAPOT));
   }

   private Route createRoute(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Event> backEnd) {
      return pathPrefix("fhir",
                        () -> concat(
                              get(() -> path(segment(GlobalConstants.SEGMENT_FHIR_PATIENT).slash(segment(Pattern.compile("^[A-z0-9]+$"))),
                                             (patientResourceId) -> routeFindExpandedPatientRecords(patientResourceId))
                                 )));
   }

}
