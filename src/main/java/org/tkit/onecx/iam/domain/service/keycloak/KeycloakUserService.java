package org.tkit.onecx.iam.domain.service.keycloak;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.tkit.onecx.iam.domain.config.KcConfig;
import org.tkit.quarkus.context.ApplicationContext;
import org.tkit.quarkus.log.cdi.LogExclude;

import gen.org.tkit.onecx.iam.internal.model.DomainDTO;
import gen.org.tkit.onecx.iam.internal.model.ProviderDTO;
import gen.org.tkit.onecx.iam.internal.model.ProvidersResponseDTO;
import io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfig;

@ApplicationScoped
public class KeycloakUserService {
    @Inject
    KeycloakClientFactory keycloakClientFactory;

    @Inject
    KcConfig kcConfig;

    @Inject
    KeycloakUtil keycloakUtil;

    private Keycloak keycloakClient;

    public void createClient() {
        var config = kcConfig.keycloaks().get(getCurrentProviderKey());
        keycloakClient = keycloakClientFactory.createKeycloakClient(
                config.url(),
                config.realm(),
                config.clientId(),
                config.clientSecret(),
                config.username(),
                config.password());
    }

    public DomainDTO getCurrentDomain() {
        var principalToken = principalToken();
        return new DomainDTO().name(keycloakUtil.getDomainFromIssuer(principalToken.getIssuer()));
    }

    public String getCurrentIssuer() {
        var principalToken = principalToken();
        return principalToken.getIssuer();
    }

    public String getCurrentProviderKey() {
        var issuerHost = getCurrentIssuer();
        return kcConfig.keycloaks().entrySet().stream()
                .filter(entry -> issuerHost.startsWith(entry.getValue().issuerHost()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public ProvidersResponseDTO getCurrentProviderAndRealm() {
        var currentProviderKey = getCurrentProviderKey();
        ProvidersResponseDTO providersResponseDTO = new ProvidersResponseDTO();
        ProviderDTO providerDTO = new ProviderDTO();
        providerDTO.setName(currentProviderKey);
        providerDTO.setDescription(kcConfig.keycloaks().get(currentProviderKey).description().orElse(null));
        providerDTO.setFromToken(true);
        providerDTO.setDomains(List.of(getCurrentDomain()));
        providerDTO.setDescription(kcConfig.keycloaks().get(currentProviderKey).displayName());
        providersResponseDTO.setProviders(List.of(providerDTO));
        return providersResponseDTO;
    }

    public void resetPassword(@LogExclude(mask = "***") String value) {
        createClient();
        var realm = getCurrentDomain().getName();
        var principal = ApplicationContext.get().getPrincipal();

        CredentialRepresentation resetPassword = new CredentialRepresentation();
        resetPassword.setValue(value);
        resetPassword.setType(KeycloakAdminClientConfig.GrantType.PASSWORD.asString());
        resetPassword.setTemporary(false);
        keycloakClient.realm(realm).users().get(principal).resetPassword(resetPassword);
    }

    private JsonWebToken principalToken() {
        var context = ApplicationContext.get();
        var principalToken = context.getPrincipalToken();
        if (principalToken == null) {
            throw new KeycloakException("Principal token is required");
        }
        return principalToken;
    }
}
