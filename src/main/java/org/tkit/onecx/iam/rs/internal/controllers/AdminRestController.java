package org.tkit.onecx.iam.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakAdminService;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakException;
import org.tkit.onecx.iam.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.iam.rs.internal.mappers.RoleMapper;
import org.tkit.onecx.iam.rs.internal.mappers.UserMapper;
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
    UserMapper userMapper;

    @Inject
    RoleMapper roleMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response getAllProviders() {
        return Response.status(Response.Status.OK).entity(adminService.getAllProviderAndDomains()).build();
    }

    @Override
    public Response getUserRoles(String userId, UserRolesSearchRequestDTO userRolesSearchRequestDTO) {
        return Response.ok().entity(roleMapper.map(adminService.getUserRoles(userRolesSearchRequestDTO.getIssuer(), userId)))
                .build();
    }

    @Override
    public Response searchRolesByCriteria(RoleSearchCriteriaDTO roleSearchCriteriaDTO) {
        var criteria = roleMapper.map(roleSearchCriteriaDTO);
        var result = adminService.searchRoles(roleSearchCriteriaDTO.getIssuer(), criteria);
        return Response.ok(roleMapper.map(result)).build();
    }

    @Override
    public Response searchUsersByCriteria(UserSearchCriteriaDTO userSearchCriteriaDTO) {
        var criteria = userMapper.map(userSearchCriteriaDTO);
        var usersPage = adminService.searchUsers(userSearchCriteriaDTO.getIssuer(), criteria);
        return Response.ok(userMapper.map(usersPage, "addRealmHere")).build();
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
