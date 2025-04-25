package org.tkit.onecx.iam.domain.service.keycloak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.tkit.onecx.iam.domain.config.KcConfig;
import org.tkit.onecx.iam.domain.model.Page;
import org.tkit.onecx.iam.domain.model.PageResult;
import org.tkit.onecx.iam.domain.model.RoleSearchCriteria;
import org.tkit.onecx.iam.domain.model.UserSearchCriteria;
import org.tkit.quarkus.context.ApplicationContext;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.iam.internal.model.DomainDTO;
import gen.org.tkit.onecx.iam.internal.model.ProviderDTO;
import gen.org.tkit.onecx.iam.internal.model.ProvidersResponseDTO;

@LogService
@ApplicationScoped
public class KeycloakAdminService {

    @Inject
    KeycloakClientFactory keycloakClientFactory;

    @Inject
    KeycloakUtil keycloakUtil;

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
            var provider = keycloakUtil.getProviderFromIssuer(issuer);
            var domain = keycloakUtil.getDomainFromIssuer(issuer);
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
            var provider = keycloakUtil.getProviderFromIssuer(issuer);
            var domain = keycloakUtil.getDomainFromIssuer(issuer);
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
            var provider = keycloakUtil.getProviderFromIssuer(issuer);
            var domain = keycloakUtil.getDomainFromIssuer(issuer);
            MappingsRepresentation roles = keycloakClients.get(provider).realm(domain)
                    .users().get(userId).roles().getAll();
            return roles.getRealmMappings();
        } catch (Exception ex) {
            throw new KeycloakException(ex.getMessage());
        }
    }

    public String getCurrentProviderKey() {
        var principalToken = principalToken();
        var issuerHost = principalToken.getIssuer();
        return kcConfig.keycloaks().entrySet().stream()
                .filter(entry -> issuerHost.startsWith(entry.getValue().issuerHost()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public ProvidersResponseDTO getAllProviderAndDomains() {
        var tokenProviderKey = getCurrentProviderKey();
        ProvidersResponseDTO providersResponseDTO = new ProvidersResponseDTO();

        kcConfig.keycloaks().forEach((s, clientConfig) -> {
            ProviderDTO provider = new ProviderDTO();
            provider.setName(s);
            provider.setDescription(clientConfig.description().orElse(null));
            provider.setFromToken(tokenProviderKey.equals(s));
            provider.setDomains(getDomains(s, clientConfig.issuerHost()));
            provider.setDescription(clientConfig.displayName());
            providersResponseDTO.addProvidersItem(provider);
        });
        return providersResponseDTO;
    }

    public List<DomainDTO> getDomains(String provider, String issuerHost) {
        var domains = keycloakClients.get(provider).realms().findAll();
        List<DomainDTO> domainDtos = new ArrayList<>();
        domains.forEach(realmRepresentation -> {
            var domain = new DomainDTO();
            domain.setName(realmRepresentation.getRealm());
            domain.setDisplayName(realmRepresentation.getDisplayName());
            domain.setIssuer(keycloakUtil.buildIssuerFromHostAndDomain(issuerHost, realmRepresentation.getRealm()));
            domainDtos.add(domain);
        });
        return domainDtos;
    }

    public String validateIssuer(String issuer) {
        return keycloakUtil.getProviderFromIssuer(issuer);
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
