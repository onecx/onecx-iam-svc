package org.tkit.onecx.iam.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakException;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakUserService;
import org.tkit.onecx.iam.rs.internal.mappers.ExceptionMapper;
import org.tkit.quarkus.log.cdi.LogService;
import org.tkit.quarkus.rs.context.token.TokenException;

import gen.org.tkit.onecx.iam.internal.UserInternalApi;
import gen.org.tkit.onecx.iam.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.iam.internal.model.UserResetPasswordRequestDTO;

@LogService
@ApplicationScoped
public class UserRestController implements UserInternalApi {
    @Inject
    KeycloakUserService userService;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response getUserProvider() {
        var providerResponse = userService.getCurrentProviderAndRealm();
        return Response.status(Response.Status.OK).entity(providerResponse).build();
    }

    @Override
    public Response resetPassword(UserResetPasswordRequestDTO userResetPasswordRequestDTO) {
        userService.resetPassword(userResetPasswordRequestDTO.getPassword());
        return Response.noContent().build();
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
