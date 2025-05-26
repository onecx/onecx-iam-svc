package org.tkit.onecx.iam.domain.service.keycloak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.tkit.onecx.iam.domain.config.KcConfig;
import org.tkit.onecx.iam.domain.model.Page;
import org.tkit.onecx.iam.domain.model.PageResult;
import org.tkit.onecx.iam.domain.model.RoleSearchCriteria;
import org.tkit.onecx.iam.domain.model.UserSearchCriteria;
import org.tkit.quarkus.log.cdi.LogService;

@LogService
@ApplicationScoped
public class KeycloakAdminService {

    @Inject
    KeycloakClientFactory keycloakClientFactory;

    @Inject
    KcConfig kcConfig;

    private Map<String, Keycloak> keycloakClients = new HashMap<>();

    @PostConstruct
    public void init() {
        kcConfig.keycloaks().forEach((key, config) -> {
            Keycloak keycloak = keycloakClientFactory.createKeycloakClient(
                    config.url(),
                    config.realm(),
                    config.clientId(),
                    config.clientSecret(),
                    config.username(),
                    config.password());
            keycloakClients.put(key, keycloak);
        });
    }

    public PageResult<RoleRepresentation> searchRoles(String issuer, RoleSearchCriteria criteria) {
        try {
            var provider = getProviderFromIssuer(issuer);
            var domain = KeycloakUtil.getDomainFromIssuer(issuer);
            var first = criteria.getPageNumber() * criteria.getPageSize();
            var count = 0;

            List<RoleRepresentation> roles = keycloakClients.get(provider).realm(domain)
                    .roles().list(criteria.getName(), first, criteria.getPageSize(), true);

            return new PageResult<>(count, roles, Page.of(criteria.getPageNumber(), criteria.getPageSize()));
        } catch (Exception ex) {
            throw new KeycloakException(ex.getMessage());
        }
    }

    public PageResult<UserRepresentation> searchUsers(String issuer, UserSearchCriteria criteria) {
        try {
            var provider = getProviderFromIssuer(issuer);
            var domain = KeycloakUtil.getDomainFromIssuer(issuer);
            if (criteria.getUserId() != null && !criteria.getUserId().isBlank()) {
                var user = getUserById(criteria.getUserId(), provider, domain);
                return new PageResult<>(user.size(), user, Page.of(0, 1));
            }

            var first = criteria.getPageNumber() * criteria.getPageSize();
            var count = keycloakClients.get(provider).realm(domain).users().count(criteria.getLastName(),
                    criteria.getFirstName(),
                    criteria.getEmail(),
                    criteria.getUserName());

            List<UserRepresentation> users = keycloakClients.get(provider).realm(domain)
                    .users()
                    .search(criteria.getUserName(), criteria.getFirstName(), criteria.getLastName(), criteria.getEmail(), first,
                            criteria.getPageSize(), null, false);

            return new PageResult<>(count, users, Page.of(criteria.getPageNumber(), criteria.getPageSize()));
        } catch (Exception ex) {
            throw new KeycloakException(ex.getMessage());
        }
    }

    public List<UserRepresentation> getUserById(String userId, String provider, String realm) {
        List<UserRepresentation> users;
        try {
            users = List.of(keycloakClients.get(provider).realm(realm).users().get(userId).toRepresentation());
        } catch (ClientWebApplicationException ex) {
            users = new ArrayList<>();
        }
        return users;
    }

    public List<RoleRepresentation> getUserRoles(String issuer, String userId) {
        try {
            var provider = getProviderFromIssuer(issuer);
            var domain = KeycloakUtil.getDomainFromIssuer(issuer);
            MappingsRepresentation roles = keycloakClients.get(provider).realm(domain)
                    .users().get(userId).roles().getAll();
            return roles.getRealmMappings();
        } catch (Exception ex) {
            throw new KeycloakException(ex.getMessage());
        }
    }

    public Map<String, List<RealmRepresentation>> findAllRealms() {
        Map<String, List<RealmRepresentation>> realms = new HashMap<>();
        kcConfig.keycloaks().forEach((s, clientConfig) -> realms.put(s, keycloakClients.get(s).realms().findAll()));
        return realms;
    }

    String getProviderFromIssuer(String issuer) {
        return kcConfig.keycloaks().entrySet().stream()
                .filter(entry -> issuer.startsWith(entry.getValue().issuerHost()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
