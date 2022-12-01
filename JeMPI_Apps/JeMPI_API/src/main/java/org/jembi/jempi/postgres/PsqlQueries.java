package org.jembi.jempi.postgres;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashMap;
import java.lang.String;
import java.util.UUID;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.String;


public class PsqlQueries {

    private static final String QUERY = " select N.patient_id, N.id, N.names, N.created, N.reason,  NS.state," +
            " NT.type, M.score, M.golden_id from notification N " +
            "JOIN notification_state NS  ON NS.id = N.state_id " +
            "JOIN notification_type NT on N.type_id = NT.id " +
            "JOIN match M ON M.notification_id = N.id";

    private static final Logger LOGGER = LogManager.getLogger(PsqlQueries.class);

    public static List getMatchesForReview() {

        ArrayList list = new ArrayList();
        try (Connection connection = dbConnect.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(QUERY);) {
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            int columns = md.getColumnCount();

            while (rs.next()) {
                HashMap row = new HashMap(columns);
                for (int i = 1; i <= columns; i++) {
                    row.put(md.getColumnName(i), (rs.getObject(i)));
                }
                list.add(row);
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return list;
    }


    public static void insert(UUID id, String type, String patientNames, Float score, Long created, String gID ) throws SQLException {

        Connection conn = dbConnect.connect();
        Statement stmt = conn.createStatement();

        // Set auto-commit to false
        conn.setAutoCommit(false);
        UUID stateId = null;
        UUID someType = null;
        Date res = new Date(created);

        ResultSet rs = stmt.executeQuery( "select * from notification_state");
        while(rs.next()){
            if(rs.getString("state").equals("New"))
                 stateId = UUID.fromString(rs.getString("id"));
        }

        rs = stmt.executeQuery( "select * from notification_type");
        while(rs.next()){
            if(rs.getString("type").equals(type))
                someType = rs.getObject("id", java.util.UUID.class);
        }
        String sql = "INSERT INTO notification (id, type_id, state_id, names, created) " +
                "VALUES ('"+id+"','"+someType+"','"+stateId+"','"+patientNames+"', '"+res+"')";
        stmt.addBatch(sql);

        sql = "INSERT INTO match (notification_id, score, golden_id)" + " VALUES ('"+id+"','"+score+"', '"+gID+"')";
        stmt.addBatch(sql);

        int[] count = stmt.executeBatch();
        conn.commit();


    }
}
