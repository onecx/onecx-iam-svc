package org.tkit.onecx.iam.domain.service.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@ApplicationScoped
public class KeycloakClientFactory {

    public Keycloak createKeycloakClient(String serverUrl, String realm, String clientId, String clientSecret, String userName,
            String password) {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .username(userName)
                .password(password)
                .build();
    }
}
