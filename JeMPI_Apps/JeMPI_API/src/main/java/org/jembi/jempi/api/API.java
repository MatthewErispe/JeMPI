package org.jembi.jempi.api;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;

public final class API {

   private static final Logger LOGGER = LogManager.getLogger(API.class);

   private HttpServer httpServer;

   private API() {
      LOGGER.info("API started.");
   }

   public static void main(String[] args) {
      try {
         new API().run();
      } catch (Exception e) {
         LOGGER.error(e.getLocalizedMessage(), e);
      }
   }

   public Behavior<Void> create() {
      return Behaviors.setup(
            context -> {
               ActorRef<BackEnd.Event> backEnd = context.spawn(BackEnd.create(), "BackEnd");
               context.watch(backEnd);
               httpServer = new HttpServer();
               httpServer.open(context.getSystem(), backEnd);
               return Behaviors.receive(Void.class)
                               .onSignal(Terminated.class,
                                         sig -> {
                                            httpServer.close(context.getSystem());
                                            return Behaviors.stopped();
                                         })
                               .build();
            });
   }

   private void run() {
      LOGGER.info("interface:port {}:{}", AppConfig.HTTP_SERVER_HOST, AppConfig.HTTP_SERVER_PORT);
      ActorSystem.create(this.create(), "API-App");
   }

}
