package com.veeva.vault.iaas.apigeeintegration;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApigeeAuthUtil {
    private static Logger log = Logger.getLogger(ApigeeAuthUtil.class);

    // only for test
    public static void main(String[] args) {
        ApigeeConfigVO config = new ApigeeConfigVO();

        ApigeeAuthUtil authUtil = new ApigeeAuthUtil();
        String xApiKey = authUtil.xApiKeyByApigee(config);
        String accessToken = authUtil.veevaTokenByLdap(xApiKey, config);
        String sessionId = authUtil.sessionIdByVeeva(xApiKey, accessToken, config);
        System.out.println("SessionId: " + sessionId);
    }

    public Map<String, String> getAuthHeaderProperties(ApigeeConfigVO apigeeConfig) {
        String xApiKey = xApiKeyByApigee(apigeeConfig);
        String veevaToken = veevaTokenByLdap(xApiKey, apigeeConfig);
        String veevaSessionId = sessionIdByVeeva(xApiKey, veevaToken, apigeeConfig);

        // add to each api header
        Map<String, String> headerParamMap = new LinkedHashMap<>();
        headerParamMap.put("x-apikey", xApiKey);
        headerParamMap.put("VeevaToken", veevaToken);
        headerParamMap.put("VeevaSessionId", veevaSessionId);

        return headerParamMap;
    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + new String(encodedAuth);
        return authHeaderValue;
    }


    private String xApiKeyByApigee(ApigeeConfigVO config) {

        String apiKey = null;
        String authHeaderValue = getBasicAuthenticationHeader(config.apigeeClientId, config.apigeeClientSecret);

        Map<String, String> headerProperties = new LinkedHashMap<>();
        headerProperties.put("Authorization", authHeaderValue);
        try {

            String responseString = sendPOST(config.apigeeHostUrl + config.apigeeAccessPath, headerProperties, null);

            int startPos = responseString.indexOf("access_token") + 15;
            int endPos = responseString.indexOf("\"", startPos);
            apiKey = responseString.substring(startPos, endPos);

            log.debug("X Api Key: " + apiKey);
        } catch (IOException ex) {
            log.error("Failed to do Basic Auth", ex);
            throw new RuntimeException(ex);
        }
        return apiKey;
    }

    private String veevaTokenByLdap(String apiKey, ApigeeConfigVO config) {

        String accessToken = null;

        Map<String, String> headerProperties = new LinkedHashMap<>();
        headerProperties.put("x-apikey", apiKey);
        headerProperties.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> postParams = new LinkedHashMap<>();
        postParams.put("client_id", config.ldapClientId);
        postParams.put("client_secret", config.ldapClientSecret);
        postParams.put("grant_type", config.ldapGrantType);
        postParams.put("scope", config.apigeeClientId + "/.default");

        try {

            String responseString = sendPOST(config.apigeeHostUrl + config.apigeeApiPrefixPath + config.ldapAccessPath, headerProperties, postParams);

            int startPos = responseString.indexOf("access_token") + 15;
            int endPos = responseString.indexOf("\"", startPos);
            accessToken = responseString.substring(startPos, endPos);

            log.debug("Access Token: " + accessToken);
        } catch (IOException ex) {
            log.error("Failed to do Ldap Auth", ex);
            throw new RuntimeException(ex);
        }
        return accessToken;
    }

    private String sessionIdByVeeva(String apiKey, String veevaToken, ApigeeConfigVO config) {

        String sessionId = null;

        Map<String, String> headerProperties = new LinkedHashMap<>();
        headerProperties.put("x-apikey", apiKey);
        headerProperties.put("VeevaToken", veevaToken);
        headerProperties.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> postParams = new LinkedHashMap<>();
        postParams.put("client_id", config.ldapClientId);

        try {

            String responseString = sendPOST(config.apigeeHostUrl + config.apigeeApiPrefixPath + config.ldapAccessPath, headerProperties, postParams);

            int startPos = responseString.indexOf("sessionId") + 13;
            int endPos = responseString.indexOf("\"", startPos);
            sessionId = responseString.substring(startPos, endPos);

            log.debug("Session Id: " + sessionId);
        } catch (IOException ex) {
            log.error("Failed to do Veeva Auth", ex);
            throw new RuntimeException(ex);
        }
        return sessionId;
    }

    private String sendPOST(String url, Map<String, String> headerProperties, Map<String, String> postParams) throws IOException {
        StringBuilder postParamSb = new StringBuilder();

        if (postParams != null) {
            postParams.forEach((k, v) -> postParamSb.append(k + "=" + v).append("\n"));
        }

        String responseString = null;

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");

        if (headerProperties != null) {
            headerProperties.forEach((k, v) -> con.setRequestProperty(k, v));
        }
        con.setRequestProperty("Content-Length", String.valueOf(postParamSb.toString().length()));

        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(postParamSb.toString().getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int responseCode = con.getResponseCode();
        log.debug("POST Response Code: " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer response = new StringBuffer();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        responseString = response.toString();

        // print result
        log.debug("Post response: " + responseString);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.error("POST request did not work.");
            throw new RuntimeException("POST request did not work, response was: " + responseCode);
        }

        return responseString;
    }

}
