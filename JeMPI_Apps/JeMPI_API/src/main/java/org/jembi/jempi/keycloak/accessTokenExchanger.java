package org.jembi.jempi.keycloak;

import org.jembi.jempi.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.JsonNode;
import kong.unirest.*;
import java.util.Base64;
import kong.unirest.json.JSONObject;

public class accessTokenExchanger {

    private static final Logger LOGGER = LogManager.getLogger(accessTokenExchanger.class);

    private static String decode(String encodedString) {
        return new String(Base64.getUrlDecoder().decode(encodedString));
    }
    public static void exchangeToken(String auth_code){
            try{
                HttpResponse<JsonNode> response = Unirest.post("http://DELL-MAHAO:8089/auth/realms/demo/protocol/openid-connect/token")
                        .header("content-type", "application/x-www-form-urlencoded")
                        .body("grant_type=authorization_code&client_id=myclient&client_secret=5dc715bf-ca03-442d-9564-1638519da9e2&code="+ auth_code)
                        .asJson();

                LOGGER.debug(response.getBody());

                final JSONObject object = response.getBody().getObject();
                String[] parts = (object.get("access_token").toString()).split("\\.");
                JSONObject header = new JSONObject(decode(parts[0]));
                JSONObject payload = new JSONObject(decode(parts[1]));
                String signature = decode(parts[2]);
                String given_name = payload.getString("given_name");
                String family_name = payload.getString("family_name");
                String email = payload.getString("email");
                String user_id = payload.getString("sub");
                LOGGER.debug(payload);
                LOGGER.debug(given_name);
                LOGGER.debug(family_name);
                LOGGER.debug(email);
                LOGGER.debug(user_id);
            }catch(Exception e){
                LOGGER.error(e.toString());
            }

    }
}
