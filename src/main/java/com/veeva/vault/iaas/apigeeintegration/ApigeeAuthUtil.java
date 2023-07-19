package com.veeva.vault.iaas.apigeeintegration;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author balint.bozan
 * @version 1.0
 * <p>
 * Authenticates against ApiGee and Ldap. It gets the Vault sessionId as well and collects the tokens in a map.
 * </p>
 */
public class ApigeeAuthUtil {
    private static final Logger log = Logger.getLogger(ApigeeAuthUtil.class);

    /**
     * Main method to test the authentication.
     *
     * @param args not in used
     */
    //
    public static void main(String[] args) {
        ApigeeConfigVO config = new ApigeeConfigVO();

        ApigeeAuthUtil authUtil = new ApigeeAuthUtil();
        String bearerToken = authUtil.bearerTokenByApigee(config);
        String accessToken = authUtil.veevaTokenByLdap(bearerToken, config);
        String sessionId = authUtil.sessionIdByVeeva(bearerToken, accessToken, config);
        System.out.println("SessionId: " + sessionId);
    }

    /**
     * Public authentication request endpoint
     *
     * @param apigeeConfig section of the VAPIL json file
     * @return a map with the auth token values
     */
    public Map<String, String> getAuthHeaderProperties(ApigeeConfigVO apigeeConfig) {
        String bearerToken = bearerTokenByApigee(apigeeConfig);
        String veevaToken = veevaTokenByLdap(bearerToken, apigeeConfig);
        String veevaSessionId = sessionIdByVeeva(bearerToken, veevaToken, apigeeConfig);

        // add to each api header
        Map<String, String> headerParamMap = new LinkedHashMap<>();
        headerParamMap.put("x-apikey", apigeeConfig.apigeeClientId);
        headerParamMap.put("Authorization", "Bearer " + bearerToken);
        headerParamMap.put("VeevaSessionID", veevaSessionId);

        return headerParamMap;
    }

    /**
     * Generates Basic Authentication header value
     *
     * @param username to be set
     * @param password to be set
     * @return Base64 encoded username and password
     */
    private static String getBasicAuthenticationHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + new String(encodedAuth);
        return authHeaderValue;
    }

    /**
     * Authenticate against ApiGee
     *
     * @param config section of the VAPIL json file
     * @return token value
     */
    private String bearerTokenByApigee(ApigeeConfigVO config) {

        String apiKey;
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

    /**
     * Ldap authentication
     *
     * @param apiKey for the request header
     * @param config section of the VAPIL json file
     * @return token value
     */
    private String veevaTokenByLdap(String apiKey, ApigeeConfigVO config) {

        String accessToken;

        Map<String, String> headerProperties = new LinkedHashMap<>();
        headerProperties.put("x-apikey", config.apigeeClientId);
        headerProperties.put("Authorization", "Bearer " + apiKey);
        headerProperties.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> postParams = new LinkedHashMap<>();
        postParams.put("client_id", config.ldapClientId);
        postParams.put("client_secret", config.ldapClientSecret);
        postParams.put("grant_type", config.ldapGrantType);
        postParams.put("scope", config.ldapClientId + "/.default");

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

    /**
     * Gets the Veeva Vault session id
     *
     * @param bearerToken for the request header
     * @param veevaToken  for the request header
     * @param config      section of the VAPIL json file
     * @return Vault session id
     */
    private String sessionIdByVeeva(String bearerToken, String veevaToken, ApigeeConfigVO config) {

        String sessionId;

        Map<String, String> headerProperties = new LinkedHashMap<>();
        headerProperties.put("x-apikey", config.apigeeClientId);
        headerProperties.put("Authorization", "Bearer " + bearerToken);
        headerProperties.put("VeevaToken", veevaToken);
        headerProperties.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> postParams = new LinkedHashMap<>();
        postParams.put("client_id", config.ldapClientId);

        try {

            String responseString = sendPOST(config.apigeeHostUrl + config.apigeeApiPrefixPath + config.veevaAccessPath, headerProperties, postParams);

            int startPos = responseString.indexOf("sessionId") + 12;
            int endPos = responseString.indexOf("\"", startPos);
            sessionId = responseString.substring(startPos, endPos);

            log.debug("Session Id: " + sessionId);
        } catch (IOException ex) {
            log.error("Failed to do Veeva Auth", ex);
            throw new RuntimeException(ex);
        }
        return sessionId;
    }

    /**
     * HTTP communication
     *
     * @param url              to the request
     * @param headerProperties of the request
     * @param postParams       of the request
     * @return response string
     * @throws IOException in case of error
     */
    private String sendPOST(String url, Map<String, String> headerProperties, Map<String, String> postParams) throws IOException {
        StringBuilder postParamSb = new StringBuilder();

        if (postParams != null) {
            postParams.forEach((k, v) -> postParamSb.append(k + "=" + v.replace(' ', '+')).append("&"));
        }

        String responseString;

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

        if (responseCode == HttpURLConnection.HTTP_OK) {
            responseString = readResponse(con.getInputStream());

            // print result
            log.debug("Post response: " + responseString);

        } else {
            responseString = readResponse(con.getErrorStream());

            log.debug("POST Header: " + headerProperties);
            log.debug("POST Body: " + postParams);
            log.error("POST request did not work, " + responseString + " on " + url);
            throw new RuntimeException("POST request did not work, response was: " + responseCode + ", " + responseString);
        }

        return responseString;
    }

    /**
     * Read InputStream to a String
     *
     * @param inputStream to be set
     * @return the string was streamed
     * @throws IOException in case of error
     */
    @NotNull
    private static String readResponse(InputStream inputStream) throws IOException {
        String responseString;
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        responseString = response.toString();
        return responseString;
    }
}
