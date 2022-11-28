package org.jembi.jempi.staging;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.codec.language.RefinedSoundex;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.shared.models.*;
import org.jembi.jempi.shared.serdes.JsonPojoDeserializer;
import org.jembi.jempi.shared.serdes.JsonPojoSerializer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomFrontEndStream {

   private static final Logger LOGGER = LogManager.getLogger(CustomFrontEndStream.class);
   private final KafkaStreams patientKafkaStreams;
   private final List<CustomEntity> entityList = new ArrayList<>();
   private final Random random = new Random(1234);
   List<String> FACILITY = Arrays.asList("CLINIC", "PHARMACY", "LABORATORY");
   ExecutorService executorService = Executors.newFixedThreadPool(1);

   public CustomFrontEndStream() {

      final Properties props = loadConfig();

      final Serde<String> stringSerde = Serdes.String();

      final Serializer<CustomTesterPatientRecord> testerPatientRecordSerializer = new JsonPojoSerializer<>();
      final Deserializer<CustomTesterPatientRecord> testerPatientRecordDeserializer =
            new JsonPojoDeserializer<>(CustomTesterPatientRecord.class);

      final Serializer<BatchEntity> batchEntitySerializer = new JsonPojoSerializer<>();
      final Deserializer<BatchEntity> batchEntityDeserializer = new JsonPojoDeserializer<>(BatchEntity.class);

      final Serde<CustomTesterPatientRecord> testerPatientRecordSerde =
            Serdes.serdeFrom(testerPatientRecordSerializer,
                             testerPatientRecordDeserializer);
      final Serde<BatchEntity> batchEntitySerde = Serdes.serdeFrom(batchEntitySerializer, batchEntityDeserializer);

      final StreamsBuilder streamsBuilder = new StreamsBuilder();
      final KStream<String, CustomTesterPatientRecord> patientKStream = streamsBuilder.stream(
            GlobalConstants.TOPIC_PATIENT_STAGING_01,
            Consumed.with(stringSerde, testerPatientRecordSerde));
      patientKStream
            .map((key, rec) -> {
               var k = rec.familyName();
               if (StringUtils.isBlank(k)) {
                  k = "anon";
               }
               k = getEncodedMF(k, OperationType.OPERATION_TYPE_DOUBLE_METAPHONE);
               LOGGER.info("{} : {}", k, rec);
               var entityType = switch (rec.recordType().type) {
                  case CustomTesterPatientRecord.RecordType.BATCH_START_VALUE -> BatchEntity.EntityType.BATCH_START;
                  case CustomTesterPatientRecord.RecordType.BATCH_END_VALUE -> BatchEntity.EntityType.BATCH_END;
                  default -> BatchEntity.EntityType.BATCH_RECORD;
               };
               var entity = new BatchEntity(
                     entityType,
                     rec.stan(),
                     new CustomEntity(null,
                                      new SourceId(null,
                                                   FACILITY.get(random.nextInt(FACILITY.size())),
                                                   StringUtils.isNotBlank(rec.nationalID()) ? rec.nationalID() : "ANON"),
                                      rec.auxId(),
                                      rec.givenName(),
                                      rec.familyName(),
                                      rec.gender(),
                                      rec.dob(),
                                      rec.city(),
                                      rec.phoneNumber(),
                                      rec.nationalID()));
               return KeyValue.pair(k, entity);
            })
            .filter((key, value) -> !(value.entityType() == BatchEntity.EntityType.BATCH_RECORD && StringUtils.isBlank(
                  value.entity().auxId())))
            .to(GlobalConstants.TOPIC_PATIENT_CONTROLLER, Produced.with(stringSerde, batchEntitySerde));
      patientKafkaStreams = new KafkaStreams(streamsBuilder.build(), props);
      patientKafkaStreams.cleanUp();
      patientKafkaStreams.start();
   }

   private String getEncodedMF(String value, OperationType algorithmType) {
      return switch (algorithmType) {
         case OPERATION_TYPE_METAPHONE -> (new Metaphone()).metaphone(value);
         case OPERATION_TYPE_DOUBLE_METAPHONE -> (new DoubleMetaphone()).doubleMetaphone(value);
         case OPERATION_TYPE_SOUNDEX -> (new Soundex()).encode(value);
         case OPERATION_TYPE_REFINED_SOUNDEX -> (new RefinedSoundex()).encode(value);
      };
   }

   public void close() {
      patientKafkaStreams.close();
   }

   private Properties loadConfig() {
      final Properties props = new Properties();
      props.put(StreamsConfig.APPLICATION_ID_CONFIG, AppConfig.KAFKA_APPLICATION_ID);
      props.put(StreamsConfig.CLIENT_ID_CONFIG, AppConfig.KAFKA_CLIENT_ID);
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.KAFKA_BOOTSTRAP_SERVERS);
      props.put(StreamsConfig.POLL_MS_CONFIG, 10);
      return props;
   }

   public enum OperationType {
      OPERATION_TYPE_METAPHONE,
      OPERATION_TYPE_DOUBLE_METAPHONE,
      OPERATION_TYPE_SOUNDEX,
      OPERATION_TYPE_REFINED_SOUNDEX
   }

}