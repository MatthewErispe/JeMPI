package org.jembi.jempi.api;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.postgres.PsqlQueries;
import org.jembi.jempi.shared.models.GlobalConstants;
import org.jembi.jempi.shared.models.Notification;
import org.jembi.jempi.shared.serdes.JsonPojoDeserializer;
import org.jembi.jempi.shared.serdes.JsonPojoSerializer;

import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public final class NotificationStreamProcessor {

   private static final Logger LOGGER = LogManager.getLogger(NotificationStreamProcessor.class);
   private KafkaStreams notificationKafkaStreams = null;

   void open(
         final ActorSystem<Void> system,
         final ActorRef<BackEnd.Event> backEnd) {
      LOGGER.info("Stream Processor");

      final Properties props = loadConfig();
      final Serde<String> stringSerde = Serdes.String();
      final Serializer<Notification> notificationSerializer = new JsonPojoSerializer<>();
      final Deserializer<Notification> notificationDeserializer = new JsonPojoDeserializer<>(Notification.class);
      final Serde<Notification> notificationSerde = Serdes.serdeFrom(notificationSerializer, notificationDeserializer);
      final StreamsBuilder streamsBuilder = new StreamsBuilder();
      final KStream<String, Notification> notificationStream = streamsBuilder.stream(
            GlobalConstants.TOPIC_NOTIFICATIONS,
            Consumed.with(stringSerde, notificationSerde));
      notificationStream
            .foreach((key, value) -> {
               try {

                  LOGGER.debug("key:{}, value:{}", key, value);
                  UUID id = UUID.randomUUID();
                  PsqlQueries.insert(id,
                                     value.notificationType().toString(),
                                     value.patientNames(),
                                     value.linkedTo().score(),
                                     value.timeStamp(),
                                     value.linkedTo().gID(),
                                     value.dID());

                  for (int i = 0; i < value.candidates().size(); i++) {
                     PsqlQueries.insertCandidates(id, value.candidates().get(i).score(), value.candidates().get(i).gID());
                  }
               } catch (SQLException e) {
                  LOGGER.debug(e.toString());
               }
               LOGGER.debug("Linked To data : " + value.linkedTo());
               LOGGER.debug("Candidates data : " + value.candidates().get(0).gID());
            });

      notificationKafkaStreams = new KafkaStreams(streamsBuilder.build(), props);
      notificationKafkaStreams.cleanUp();
      notificationKafkaStreams.start();
      Runtime.getRuntime().addShutdownHook(new Thread(notificationKafkaStreams::close));
      LOGGER.info("Notifications started");
   }

   private Properties loadConfig() {
      final Properties props = new Properties();
      props.put(StreamsConfig.APPLICATION_ID_CONFIG, AppConfig.KAFKA_APPLICATION_ID);
      props.put(StreamsConfig.CLIENT_ID_CONFIG, AppConfig.KAFKA_CLIENT_ID);
//        props.put(StreamsConfig.GROUP_ID, AppConfig.KAFKA_GROUP_ID);  TODO check howto set group id
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.KAFKA_BOOTSTRAP_SERVERS);
      props.put(StreamsConfig.POLL_MS_CONFIG, 50);
      return props;
   }


}
