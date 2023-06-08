package com.veeva.vault.iaas.apigeeintegration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApigeeConfigVO {

    @JsonProperty("apigeeHostUrl")
    public String apigeeHostUrl = "https://ch-api-dev.apigee.net";
    @JsonProperty("apigeeApiPrefixPath")
    public String apigeeApiPrefixPath = "/eap-veeva-system-api/v1";
    @JsonProperty("apigeeAccessPath")
    public String apigeeAccessPath = "/oauth/v2/client_credential/accesstoken?grant_type=client_credentials";
    @JsonProperty("ldapAccessPath")
    public String ldapAccessPath = "/oauth/token";
    @JsonProperty("veevaAccessPath")
    public String veevaAccessPath = "/auth";
    @JsonProperty("apigeeClientId")
    public String apigeeClientId = "sGsp4Cc0d0YsMjKOGz6PpfbcoznIDGXT";
    @JsonProperty("apigeeClientSecret")
    public String apigeeClientSecret = "3xDsueTFNiEugFlx";
    @JsonProperty("ldapClientId")
    public String ldapClientId = "55f91c09-f406-40f5-b9d8-2a3414b5858a";
    @JsonProperty("ldapClientSecret")
    public String ldapClientSecret = "ZMb8Q~J5dmCxbKXUbOMW47W-myJqxHXKUBpGddrp";
    @JsonProperty("ldapGrantType")
    public String ldapGrantType = "client_credentials";
}
