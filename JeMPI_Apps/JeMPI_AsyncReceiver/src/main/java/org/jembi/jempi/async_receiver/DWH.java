package org.jembi.jempi.async_receiver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.util.PGobject;
import scala.Tuple2;

import java.sql.*;

final class DWH {
   private static final String SQL_INSERT = """
                                            INSERT INTO dwh(clinical_data)
                                            VALUES (?)
                                            """;

   private static final String SQL_UPDATE = """
                                            UPDATE dwh
                                            SET golden_id = ?, encounter_id = ?
                                            WHERE dwh_id1 = ? AND dwh_id2 = ?
                                            """;
   private static final Logger LOGGER = LogManager.getLogger(DWH.class);
   private static final String URL = "jdbc:postgresql://postgresql:5432/notifications";
   private static final String USER = "postgres";
   private Connection conn;

   DWH() {
   }

   private boolean open() {
      try {
         if (conn == null || !conn.isValid(0)) {
            if (conn != null) {
               conn.close();
            }
            conn = DriverManager.getConnection(URL, USER, null);
            conn.setAutoCommit(true);
            return conn.isValid(0);
         }
         return true;
      } catch (SQLException e) {
         LOGGER.error(e.getLocalizedMessage(), e);
      }
      return false;
   }

   void backPatchKeys(
         final String dwhId1,
         final String dwhId2,
         final String goldenId,
         final String encounterId) {
      if (open()) {
         try {
            try (PreparedStatement pStmt = conn.prepareStatement(SQL_UPDATE, Statement.RETURN_GENERATED_KEYS)) {
               final PGobject uuid1 = new PGobject();
               uuid1.setType("uuid");
               uuid1.setValue(dwhId1);
               final PGobject uuid2 = new PGobject();
               uuid2.setType("uuid");
               uuid2.setValue(dwhId2);
               pStmt.setString(1, goldenId);
               pStmt.setString(2, encounterId);
               pStmt.setObject(3, uuid1);
               pStmt.setObject(4, uuid2);
               pStmt.executeUpdate();
            }
         } catch (SQLException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
         }
      } else {
         LOGGER.error("NO SQL SERVER");
      }
   }

   Tuple2<String, String> insertClinicalData(final String clinicalData) {
      String dwhId1 = null;
      String dwhId2 = null;
      if (open()) {
         try {
            if (conn == null || !conn.isValid(0)) {
               if (conn != null) {
                  conn.close();
               }
               open();
            }
            try (PreparedStatement pStmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
               pStmt.setString(1, clinicalData);
               int affectedRows = pStmt.executeUpdate();
               if (affectedRows > 0) {
                  final var rs = pStmt.getGeneratedKeys();
                  if (rs.next()) {
                     dwhId1 = rs.getString(1);
                     dwhId2 = rs.getString(2);
                  }
               }
            }
         } catch (SQLException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
         }
      }
      return new Tuple2<>(dwhId1, dwhId2);
   }

}
