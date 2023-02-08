package org.jembi.jempi.libmpi.dgraph;

public final class CustomLibMPIConstants {

   private CustomLibMPIConstants() {}

   public static final String PREDICATE_GOLDEN_RECORD_AUX_ID = "GoldenRecord.aux_id";
   public static final String PREDICATE_GOLDEN_RECORD_NAT_FINGERPRINT_CODE = "GoldenRecord.nat_fingerprint_code";
   public static final String PREDICATE_GOLDEN_RECORD_EMR_FINGERPRINT_CODE = "GoldenRecord.emr_fingerprint_code";
   public static final String PREDICATE_GOLDEN_RECORD_GENDER = "GoldenRecord.gender";
   public static final String PREDICATE_GOLDEN_RECORD_DOB = "GoldenRecord.dob";
   public static final String PREDICATE_GOLDEN_RECORD_CITY = "GoldenRecord.city";
   public static final String PREDICATE_GOLDEN_RECORD_ENTITY_LIST = "GoldenRecord.entity_list";

   public static final String PREDICATE_ENTITY_AUX_ID = "Entity.aux_id";
   public static final String PREDICATE_ENTITY_NAT_FINGERPRINT_CODE = "Entity.nat_fingerprint_code";
   public static final String PREDICATE_ENTITY_EMR_FINGERPRINT_CODE = "Entity.emr_fingerprint_code";
   public static final String PREDICATE_ENTITY_GENDER = "Entity.gender";
   public static final String PREDICATE_ENTITY_DOB = "Entity.dob";
   public static final String PREDICATE_ENTITY_CITY = "Entity.city";

   static final String QUERY_GET_GOLDEN_RECORD_BY_UID =
      """
      query goldenRecordByUid($uid: string) {
         all(func: uid($uid)) {
            uid
            GoldenRecord.source_id {
               uid
            }
            GoldenRecord.aux_id
            GoldenRecord.nat_fingerprint_code
            GoldenRecord.emr_fingerprint_code
            GoldenRecord.gender
            GoldenRecord.dob
            GoldenRecord.city
         }
      }
      """;

   static final String QUERY_GET_GOLDEN_RECORD_ENTITIES =
      """
      query expandedGoldenRecord() {
         all(func: uid(%s)) {
            uid
            GoldenRecord.source_id {
               uid
            }
            GoldenRecord.aux_id
            GoldenRecord.nat_fingerprint_code
            GoldenRecord.emr_fingerprint_code
            GoldenRecord.gender
            GoldenRecord.dob
            GoldenRecord.city
            GoldenRecord.entity_list @facets(score) {
               uid
               Entity.source_id {
                 uid
                 SourceId.facility
                 SourceId.patient
               }
               Entity.aux_id
               Entity.nat_fingerprint_code
               Entity.emr_fingerprint_code
               Entity.gender
               Entity.dob
               Entity.city
            }
         }
      }
      """;

   static final String QUERY_GET_ENTITY_BY_UID =
      """
      query entityByUid($uid: string) {
         all(func: uid($uid)) {
            uid
            Entity.source_id {
              uid
              SourceId.facility
              SourceId.patient
            }
            Entity.aux_id
            Entity.nat_fingerprint_code
            Entity.emr_fingerprint_code
            Entity.gender
            Entity.dob
            Entity.city
         }
      }
      """;

   static final String MUTATION_CREATE_SOURCE_ID_TYPE =
      """

      type SourceId {
         SourceId.facility
         SourceId.patient
      }
      """;
         
   static final String MUTATION_CREATE_GOLDEN_RECORD_TYPE =
      """

      type GoldenRecord {
         GoldenRecord.source_id:                 [SourceId]
         GoldenRecord.aux_id
         GoldenRecord.nat_fingerprint_code
         GoldenRecord.emr_fingerprint_code
         GoldenRecord.gender
         GoldenRecord.dob
         GoldenRecord.city
         GoldenRecord.entity_list:               [Entity]
         <~Entity.golden_record_list>
      }
      """;
         
   static final String MUTATION_CREATE_ENTITY_TYPE =
      """

      type Entity {
         Entity.source_id:                     SourceId
         Entity.aux_id
         Entity.nat_fingerprint_code
         Entity.emr_fingerprint_code
         Entity.gender
         Entity.dob
         Entity.city
         Entity.golden_record_list:            [GoldenRecord]
      }
      """;

   static final String MUTATION_CREATE_SOURCE_ID_FIELDS =
      """
      SourceId.facility:                     string    @index(exact)                      .
      SourceId.patient:                      string    @index(exact)                      .
      """;
         
   static final String MUTATION_CREATE_GOLDEN_RECORD_FIELDS =
      """
      GoldenRecord.source_id:                [uid]                                        .
      GoldenRecord.aux_id:                   string    @index(exact)                      .
      GoldenRecord.nat_fingerprint_code:     string    @index(exact)                      .
      GoldenRecord.emr_fingerprint_code:     string                                       .
      GoldenRecord.gender:                   string    @index(exact)                      .
      GoldenRecord.dob:                      string    @index(exact)                      .
      GoldenRecord.city:                     string    @index(exact)                      .
      GoldenRecord.entity_list:              [uid]     @reverse                           .
      """;

   static final String MUTATION_CREATE_ENTITY_FIELDS =
      """
      Entity.source_id:                    uid                                          .
      Entity.aux_id:                       string                                       .
      Entity.nat_fingerprint_code:         string                                       .
      Entity.emr_fingerprint_code:         string    @index(exact)                      .
      Entity.gender:                       string    @index(exact)                      .
      Entity.dob:                          string    @index(exact)                      .
      Entity.city:                         string    @index(exact)                      .
      Entity.golden_record_list:           [uid]     @reverse                           .
      """;

}
