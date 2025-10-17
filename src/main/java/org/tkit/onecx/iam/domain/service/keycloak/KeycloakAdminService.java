package org.tkit.onecx.iam.domain.service.keycloak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.*;
import org.tkit.onecx.iam.domain.config.KcConfig;
import org.tkit.onecx.iam.domain.model.Page;
import org.tkit.onecx.iam.domain.model.PageResult;
import org.tkit.onecx.iam.domain.model.RoleSearchCriteria;
import org.tkit.onecx.iam.domain.model.UserSearchCriteria;
import org.tkit.onecx.iam.rs.internal.mappers.AdminMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.iam.internal.model.CreateRoleRequestDTO;
import gen.org.tkit.onecx.iam.internal.model.CreateUserRequestDTO;
import gen.org.tkit.onecx.iam.internal.model.RoleAssignmentRequestDTO;
import gen.org.tkit.onecx.iam.internal.model.UpdateUserRequestDTO;
import io.quarkus.logging.Log;

@LogService
@ApplicationScoped
public class KeycloakAdminService {

    @Inject
    KeycloakClientFactory keycloakClientFactory;

    @Inject
    KcConfig kcConfig;

    @Inject
    AdminMapper adminMapper;

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
        kcConfig.keycloaks().forEach((s, clientConfig) -> {
            try {
                realms.put(s, keycloakClients.get(s).realms().findAll());
            } catch (Exception ex) {
                Log.error("Provider: " + s + " not reachable. Skipped.");
                realms.put(s, null);
            }
        });
        return realms;
    }

    public String getProviderFromIssuer(String issuer) {
        return kcConfig.keycloaks().entrySet().stream()
                .filter(entry -> issuer.startsWith(entry.getValue().issuerHost()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public Response createUser(CreateUserRequestDTO createUserRequestDTO) {
        var targetProviderKey = getProviderFromIssuer(createUserRequestDTO.getIssuer());
        var userToCreate = adminMapper.createUser(createUserRequestDTO);
        var credentials = new CredentialRepresentation();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(createUserRequestDTO.getTemporaryPassword());
        credentials.setTemporary(true);
        userToCreate.setCredentials(List.of(credentials));
        try (Response res = keycloakClients.get(targetProviderKey)
                .realm(KeycloakUtil.getDomainFromIssuer(createUserRequestDTO.getIssuer())).users().create(userToCreate)) {
            return Response.status(res.getStatus()).build();
        }
    }

    public void updateUser(String userId, UpdateUserRequestDTO updateUserRequestDTO) {
        var targetProviderKey = getProviderFromIssuer(updateUserRequestDTO.getIssuer());
        var targetDomain = KeycloakUtil.getDomainFromIssuer(updateUserRequestDTO.getIssuer());
        var targetUser = adminMapper.updateUser(updateUserRequestDTO);
        keycloakClients.get(targetProviderKey).realm(targetDomain).users().get(userId).update(targetUser);
    }

    public void createRole(CreateRoleRequestDTO createRoleRequestDTO) {
        var targetProviderKey = getProviderFromIssuer(createRoleRequestDTO.getIssuer());
        var targetDomain = KeycloakUtil.getDomainFromIssuer(createRoleRequestDTO.getIssuer());
        var role = adminMapper.createRole(createRoleRequestDTO);
        keycloakClients.get(targetProviderKey).realm(targetDomain).roles().create(role);
    }

    public void assignRole(String userId, RoleAssignmentRequestDTO roleAssignmentRequestDTO) {
        var targetProviderKey = getProviderFromIssuer(roleAssignmentRequestDTO.getIssuer());
        var targetDomain = KeycloakUtil.getDomainFromIssuer(roleAssignmentRequestDTO.getIssuer());
        var availableRoles = keycloakClients.get(targetProviderKey).realm(targetDomain).roles().list();
        List<RoleRepresentation> targetRoles = availableRoles.stream()
                .filter(role -> roleAssignmentRequestDTO.getNames().contains(role.getName())).toList();
        if (!targetRoles.isEmpty()) {
            keycloakClients.get(targetProviderKey).realm(targetDomain).users().get(userId).roles()
                    .realmLevel().add(targetRoles);
        }
    }
}
