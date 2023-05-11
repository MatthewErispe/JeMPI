package org.jembi.jempi.fhir;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Main {

   private static final Logger LOGGER = LogManager.getLogger(Main.class);

   private Main() {
   }

   public static void main(final String[] args) {
      new Main().run();
   }

   public Behavior<Void> create() {
      return Behaviors.setup(
            context -> {
               final var backEndActor = context.spawn(BackEnd.create(), "BackEnd");
               context.watch(backEndActor);
               var frontEnd = new FrontEnd();
               frontEnd.open(context.getSystem(), backEndActor);
               return Behaviors.receive(Void.class)
                               .onSignal(Terminated.class,
                                         sig -> {
                                            frontEnd.close(context.getSystem());
                                            return Behaviors.stopped();
                                         })
                               .build();
            });
   }

   private void run() {
      ActorSystem.create(this.create(), "FhirApp");
   }
}
