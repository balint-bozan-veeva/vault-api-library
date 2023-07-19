package com.veeva.vault.iaas.apigeeintegration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author balint.bozan
 * @version 1.0
 * <p>
 * Value object for ApiGee configuration values
 * </p>
 */
public class ApigeeConfigVO {

    @JsonProperty("apigeeHostUrl")
    public String apigeeHostUrl;
    @JsonProperty("apigeeApiPrefixPath")
    public String apigeeApiPrefixPath;
    @JsonProperty("apigeeAccessPath")
    public String apigeeAccessPath;
    @JsonProperty("ldapAccessPath")
    public String ldapAccessPath;
    @JsonProperty("veevaAccessPath")
    public String veevaAccessPath;
    @JsonProperty("apigeeClientId")
    public String apigeeClientId;
    @JsonProperty("apigeeClientSecret")
    public String apigeeClientSecret;
    @JsonProperty("ldapClientId")
    public String ldapClientId;
    @JsonProperty("ldapClientSecret")
    public String ldapClientSecret;
    @JsonProperty("ldapGrantType")
    public String ldapGrantType;
}
