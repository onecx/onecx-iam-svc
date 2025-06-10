package org.tkit.onecx.iam.rs.internal.controllers;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.iam.domain.config.KcConfig;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakAdminService;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakException;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakUtil;
import org.tkit.onecx.iam.rs.internal.mappers.AdminMapper;
import org.tkit.onecx.iam.rs.internal.mappers.ExceptionMapper;
import org.tkit.quarkus.context.ApplicationContext;
import org.tkit.quarkus.log.cdi.LogService;
import org.tkit.quarkus.rs.context.token.TokenException;

import gen.org.tkit.onecx.iam.internal.AdminInternalApi;
import gen.org.tkit.onecx.iam.internal.model.*;

@LogService
@ApplicationScoped
public class AdminRestController implements AdminInternalApi {

    @Inject
    KeycloakAdminService adminService;

    @Inject
    AdminMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    KcConfig kcConfig;

    @Override
    public Response getAllProviders() {
        // token information
        var context = ApplicationContext.get();
        var principalToken = context.getPrincipalToken();
        if (principalToken == null) {
            throw new KeycloakException("Principal token is required");
        }
        var issuerHost = principalToken.getIssuer();

        // find current token provider for the user token
        var kcs = kcConfig.keycloaks();
        var tokenProviderKey = kcs.entrySet().stream()
                .filter(entry -> issuerHost.startsWith(entry.getValue().issuerHost()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        // load all realms/domains
        var realms = adminService.findAllRealms();

        return Response.status(Response.Status.OK).entity(mapper.map(kcs, realms, tokenProviderKey)).build();
    }

    @Override
    public Response getUserRoles(String userId, UserRolesSearchRequestDTO userRolesSearchRequestDTO) {
        return Response.ok().entity(mapper.map(adminService.getUserRoles(userRolesSearchRequestDTO.getIssuer(), userId)))
                .build();
    }

    @Override
    public Response searchRolesByCriteria(RoleSearchCriteriaDTO roleSearchCriteriaDTO) {
        var criteria = mapper.map(roleSearchCriteriaDTO);
        var result = adminService.searchRoles(roleSearchCriteriaDTO.getIssuer(), criteria);
        return Response.ok(mapper.map(result)).build();
    }

    @Override
    public Response searchUsersByCriteria(UserSearchCriteriaDTO userSearchCriteriaDTO) {
        var criteria = mapper.map(userSearchCriteriaDTO);
        var usersPage = adminService.searchUsers(userSearchCriteriaDTO.getIssuer(), criteria);
        var domain = KeycloakUtil.getDomainFromIssuer(userSearchCriteriaDTO.getIssuer());
        var provider = adminService.getProviderFromIssuer(userSearchCriteriaDTO.getIssuer());
        return Response.ok(mapper.map(usersPage, domain, provider)).build();
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(TokenException ex) {
        return exceptionMapper.exception(ex.getKey(), ex.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(KeycloakException ex) {
        return exceptionMapper.exception(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }
}
