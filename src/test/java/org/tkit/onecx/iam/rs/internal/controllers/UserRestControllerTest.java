package org.tkit.onecx.iam.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.tkit.onecx.iam.rs.internal.mappers.ExceptionMapper.ErrorKeys.TOKEN_ERROR;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.iam.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.iam.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;

import gen.org.tkit.onecx.iam.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.iam.internal.model.ProvidersResponseDTO;
import gen.org.tkit.onecx.iam.internal.model.UserResetPasswordRequestDTO;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(UserRestController.class)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ia:read", "ocx-ia:write", "ocx-ia:all" })
class UserRestControllerTest extends AbstractTest {

    @Test
    void resetPasswordTest() {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        UserResetPasswordRequestDTO dto = new UserResetPasswordRequestDTO();
        dto.setPassword("changedPassword");

        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(dto)
                .put("password")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        aliceToken = getTokens(keycloakClient, USER_ALICE, dto.getPassword()).getIdToken();
        dto.setPassword(USER_ALICE);

        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(dto)
                .put("password")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

    }

    @Test
    void getUsersProviderAndRealm() {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();

        var res = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .get("provider")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ProvidersResponseDTO.class);
        assertThat(res.getProviders()).hasSize(1);
        assertThat(res.getProviders().get(0).getName()).isEqualTo("kc0");
        assertThat(res.getProviders().get(0).getDomains()).hasSize(1);
        assertThat(res.getProviders().get(0).getDomains().get(0).getName()).isEqualTo("quarkus");
    }

    @Test
    void getUsersProviderAndRealm_empty_token_Test() {
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, " ")
                .get("provider")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
    }

    @Test
    void resetPasswordNoTokenTest() {

        UserResetPasswordRequestDTO dto = new UserResetPasswordRequestDTO();
        dto.setPassword("*******");

        var exception = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .put("password")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .body().as(ProblemDetailResponseDTO.class);

        Assertions.assertNotNull(exception);
        Assertions.assertEquals(TOKEN_ERROR.name(), exception.getErrorCode());
        Assertions.assertEquals(
                "Principal token is required",
                exception.getDetail());
        assertThat(exception.getInvalidParams()).isNotNull().isEmpty();
    }

    @Test
    void resetPasswordEmptyRequestTest() {
        UserResetPasswordRequestDTO dto = new UserResetPasswordRequestDTO();

        var exception = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .put("password")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .body().as(ProblemDetailResponseDTO.class);

        Assertions.assertNotNull(exception);
        Assertions.assertEquals(ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS.name(),
                exception.getErrorCode());
        Assertions.assertEquals(
                "resetPassword.userResetPasswordRequestDTO.password: must not be null",
                exception.getDetail());
        Assertions.assertNotNull(exception.getInvalidParams());
    }

    @Test
    void resetPasswordNoRequestTest() {

        var exception = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .put("password")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .body().as(ProblemDetailResponseDTO.class);

        Assertions.assertNotNull(exception);
        Assertions.assertEquals(
                ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS.name(),
                exception.getErrorCode());
        Assertions.assertEquals(
                "resetPassword.userResetPasswordRequestDTO: must not be null",
                exception.getDetail());
        Assertions.assertNotNull(exception.getInvalidParams());
    }
}
