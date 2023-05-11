package org.jembi.jempi.api;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.shared.utils.JsonFieldsConfig;

public final class API {

   private static final Logger LOGGER = LogManager.getLogger(API.class);
   private HttpServer httpServer;

   private final String fileName = "/fhir-field-config.json";
   private JsonFieldsConfig jsonFieldsConfig = new JsonFieldsConfig(fileName);


   private API() {
      LOGGER.info("API started.");
   }

   public static void main(final String[] args) {
      try {
         new API().run();
      } catch (Exception e) {
         LOGGER.error(e.getLocalizedMessage(), e);
      }
   }

   public Behavior<Void> create() {
      return Behaviors.setup(context -> {
         ActorRef<BackEnd.Event> backEnd = context.spawn(BackEnd.create(), "BackEnd");
         context.watch(backEnd);
         httpServer = HttpServer.create();
         httpServer.open(context.getSystem(), backEnd);
         return Behaviors.receive(Void.class).onSignal(Terminated.class, sig -> {
            httpServer.close(context.getSystem());
            return Behaviors.stopped();
         }).build();
      });
   }

   private void run() {
      LOGGER.info("interface:port {}:{}", AppConfig.HTTP_SERVER_HOST, AppConfig.HTTP_SERVER_PORT);
      try {
         LOGGER.info("Loading fields configuration file ");
         jsonFieldsConfig.load(fileName);
         LOGGER.info("Fields configuration file successfully loaded");
         ActorSystem.create(this.create(), "API-App");
      } catch (Exception e) {
         LOGGER.error("Unable to start the API", e);
      }
   }

}
