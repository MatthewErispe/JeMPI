package org.jembi.jempi.etl;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;

public final class Main {
   private static final Logger LOGGER = LogManager.getLogger(Main.class);

   public static void main(final String[] args) {
      new Main().run();
   }

   public Behavior<Void> create() {
      return Behaviors.setup(
            context -> {
               final ActorRef<CustomETLBackEnd.Event> backEnd = context.spawn(CustomETLBackEnd.create(), "BackEnd");
//               final var customSourceRecordStream = new CustomSourceRecordStream();
//               customSourceRecordStream.open();
               final var customFHIRsyncReceiver = new CustomETLSyncFrontEnd();
               customFHIRsyncReceiver.open(context.getSystem(), backEnd);
               return Behaviors.receive(Void.class)
                               .onSignal(Terminated.class,
                                         sig -> Behaviors.stopped())
                               .build();
            });
   }

   private void run() {
      LOGGER.info("ETL");
      LOGGER.info("KAFKA: {} {} {}",
                  AppConfig.KAFKA_BOOTSTRAP_SERVERS,
                  AppConfig.KAFKA_APPLICATION_ID,
                  AppConfig.KAFKA_CLIENT_ID);
      ActorSystem.create(this.create(), "ETL");
   }
}
