package org.jembi.jempi.api;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.Behaviors;
import akka.dispatch.MessageDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;

public final class API {

    private static final Logger LOGGER = LogManager.getLogger(API.class);

    private HttpServer httpServer;

    private JsonFieldsConfig jsonFieldsConfig = new JsonFieldsConfig();

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
                    final var notificationsSteam = new NotificationStreamProcessor();
<<<<<<< HEAD
                    ActorSystem system = context.getSystem();
                    notificationsSteam.open(system, backEnd);
                    DispatcherSelector selector = DispatcherSelector.fromConfig("akka.actor.default-dispatcher");
                    MessageDispatcher dispatcher = (MessageDispatcher) system.dispatchers().lookup(selector);
                    httpServer = new HttpServer(dispatcher);
                    httpServer.open(context.getSystem(), backEnd);
=======
                    notificationsSteam.open(context.getSystem(), backEnd);
                    httpServer = new HttpServer();
                    httpServer.open(context.getSystem(), backEnd, jsonFieldsConfig.fields);
>>>>>>> main
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
        try {
            LOGGER.info("Loading fields configuration file ");
            jsonFieldsConfig.load();
            LOGGER.info("Fields configuration file successfully loaded");
            ActorSystem.create(this.create(), "API-App");
        } catch (Exception e) {
            LOGGER.error("Unable to start the API", e);
        }
    }

}
