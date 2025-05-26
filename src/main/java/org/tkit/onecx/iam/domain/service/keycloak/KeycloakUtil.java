package org.tkit.onecx.iam.domain.service.keycloak;

public interface KeycloakUtil {

    static String getDomainFromIssuer(String issuer) {
        int index = issuer.lastIndexOf("/");
        if (index >= 0) {
            return issuer.substring(index + 1);
        }
        throw new KeycloakException("Wrong issuer format");
    }

    static String buildIssuerFromHostAndDomain(String issuerHost, String domain) {
        return issuerHost + "/realms/" + domain;
    }
}
