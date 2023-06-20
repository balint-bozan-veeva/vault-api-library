package com.veeva.vault.vapil;

import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ApigeeTest {
    private static final Logger log = LoggerFactory.getLogger(ApigeeTest.class);

    public static void main(String[] args){
        ApigeeTest apigeeTest = new ApigeeTest();
        apigeeTest.testApigeeQuery();
    }

    private void testApigeeQuery() {

        File settingsFile = new File("c:\\temp\\haleon\\IntelliJ-wokspace\\Quality-PrintService-App (Helion)\\src\\main\\external-resources\\Haleon_VAPIL_settings.json");
        VaultClient vc = VaultClient
                .newClientBuilderFromSettings(settingsFile)
                .build();

        QueryResponse resp = vc.newRequest(QueryRequest.class).query("select id, username__sys from user__sys");

        if (resp.hasErrors()) {
            log.error("API Errors:  " + resp.getErrors());
        } else
        if (!resp.isSuccessful()) {
            log.warn("API Warning:  " + resp.getErrors());
        } else {
            log.info("API response:  " + resp.getResponse());
        }

    }


}
