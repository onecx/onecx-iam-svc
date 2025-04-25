package org.tkit.onecx.iam.domain.service.keycloak;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.iam.domain.config.KcConfig;

@ApplicationScoped
public class KeycloakUtil {
    @Inject
    KcConfig kcConfig;

    String getDomainFromIssuer(String issuer) {
        int index = issuer.lastIndexOf("/");
        if (index >= 0) {
            return issuer.substring(index + 1);
        }
        throw new KeycloakException("Wrong issuer format");
    }

    String getProviderFromIssuer(String issuer) {
        return kcConfig.keycloaks().entrySet().stream()
                .filter(entry -> issuer.startsWith(entry.getValue().issuerHost()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    String buildIssuerFromHostAndDomain(String issuerHost, String domain) {
        return issuerHost + "/realms/" + domain;
    }
}
