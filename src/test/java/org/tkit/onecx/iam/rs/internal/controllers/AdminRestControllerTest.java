package org.tkit.onecx.iam.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.tkit.onecx.iam.rs.internal.mappers.ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS;
import static org.tkit.quarkus.rs.context.token.TokenParserService.ErrorKeys.ERROR_PARSE_TOKEN;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.jose4j.json.internal.json_simple.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.iam.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.iam.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gen.org.tkit.onecx.iam.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(AdminRestController.class)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ia:read", "ocx-ia:write", "ocx-ia:all" })
class AdminRestControllerTest extends AbstractTest {

    @Test
    void getAllKeycloaksAndRealms_Test() {
        var kc0Token = this.getTokens(keycloakClient, USER_ALICE).getIdToken();
        var res = given().when()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .header(APM_HEADER_TOKEN, kc0Token)
                .contentType(ContentType.JSON)
                .get("/providers")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ProvidersResponseDTO.class);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getProviders().size());
        kc0Token = this.getTokens(keycloakClient1, USER_ALICE).getIdToken();

        //test different kc client
        res = given().when()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .header(APM_HEADER_TOKEN, kc0Token)
                .contentType(ContentType.JSON)
                .get("/providers")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ProvidersResponseDTO.class);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getProviders().size());

        //test without token
        given().when()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(ContentType.JSON)
                .get("/providers")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

    }

    @Test
    void roleSearchAllTest() throws JsonProcessingException {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        ObjectMapper mapper = new ObjectMapper();
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = aliceToken.split("\\.");
        String body = new String(decoder.decode(chunks[1]));
        JSONObject jwt = mapper.readValue(body, JSONObject.class);
        var result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(new RoleSearchCriteriaDTO().issuer(jwt.get("iss").toString()))
                .post("/roles/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(RolePageResultDTO.class);
        assertThat(result).isNotNull();
        assertThat(result.getStream()).isNotNull().isNotEmpty().hasSize(5);
        //different client
        result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient1, USER_ALICE).getIdToken())
                .body(new RoleSearchCriteriaDTO().issuer(jwt.get("iss").toString()))
                .post("/roles/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(RolePageResultDTO.class);

        assertThat(result).isNotNull();
        assertThat(result.getStream()).isNotNull().isNotEmpty().hasSize(5);
    }

    @Test
    void roleSearchTest() throws JsonProcessingException {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        ObjectMapper mapper = new ObjectMapper();
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = aliceToken.split("\\.");
        String body = new String(decoder.decode(chunks[1]));
        JSONObject jwt = mapper.readValue(body, JSONObject.class);
        var iss = jwt.get("iss").toString();
        iss = iss.replace("quarkus", "master");
        var result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(new RoleSearchCriteriaDTO().name("default-roles-master").issuer(iss))
                .post("/roles/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(RolePageResultDTO.class);

        assertThat(result).isNotNull();
        assertThat(result.getStream()).isNotNull()
                .hasSize(1);

        //search in wrong kc and realm
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(new RoleSearchCriteriaDTO().name("default-roles-master").issuer("wrongkc"))
                .post("/roles/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void createRoleAndAssignToUserTest() throws JsonProcessingException {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        ObjectMapper mapper = new ObjectMapper();
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = aliceToken.split("\\.");
        String body = new String(decoder.decode(chunks[1]));
        JSONObject jwt = mapper.readValue(body, JSONObject.class);
        var iss = jwt.get("iss").toString();

        var requestDTO = new CreateRoleRequestDTO().issuer(iss).name("newRole").description("myNewRole");
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(requestDTO)
                .post("/roles")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode());

        //search for new role
        var result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(new RoleSearchCriteriaDTO().name("newRole").issuer(iss))
                .post("/roles/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(RolePageResultDTO.class);

        assertThat(result).isNotNull();
        assertThat(result.getStream()).isNotNull()
                .hasSize(1);

        RoleAssignmentRequestDTO roleAssignmentRequestDTO = new RoleAssignmentRequestDTO();
        roleAssignmentRequestDTO.setIssuer(iss);
        roleAssignmentRequestDTO.setNames(List.of("newRole"));

        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(roleAssignmentRequestDTO)
                .post("/" + jwt.get("sub").toString() + "/roles/assign")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // get user roles and check for new assigned role
        var userRolesResult = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .header(APM_HEADER_TOKEN, aliceToken)
                .contentType(APPLICATION_JSON)
                .pathParam("userId", jwt.get("sub").toString())
                .body(new UserRolesSearchRequestDTO().issuer(jwt.get("iss").toString()))
                .post("/{userId}/roles")
                .then().statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserRolesResponseDTO.class);
        Assertions.assertNotNull(userRolesResult);
        Assertions.assertEquals(3, userRolesResult.getRoles().size());
        Assertions.assertNotNull(userRolesResult.getRoles().stream()
                .filter(roleDTO -> roleDTO.getName().equals(roleAssignmentRequestDTO.getNames().get(0))));

        roleAssignmentRequestDTO.setNames(List.of("not-existing-role"));
        // try to assign not existing role, should be ignored
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(roleAssignmentRequestDTO)
                .post("/" + jwt.get("sub").toString() + "/roles/assign")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void getUserRolesTest() throws IOException {

        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        ObjectMapper mapper = new ObjectMapper();
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = aliceToken.split("\\.");
        String body = new String(decoder.decode(chunks[1]));
        JSONObject jwt = mapper.readValue(body, JSONObject.class);

        String id = jwt.get("sub").toString();

        var result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .header(APM_HEADER_TOKEN, aliceToken)
                .contentType(APPLICATION_JSON)
                .pathParam("userId", id)
                .body(new UserRolesSearchRequestDTO().issuer(jwt.get("iss").toString()))
                .post("/{userId}/roles")
                .then().statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserRolesResponseDTO.class);
        Assertions.assertNotNull(result);

        //not existing kc and realm
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .header(APM_HEADER_TOKEN, aliceToken)
                .contentType(APPLICATION_JSON)
                .pathParam("userId", id)
                .body(new UserRolesSearchRequestDTO().issuer("notExisting"))
                .post("/{userId}/roles")
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
    }

    @Test
    void roleSearchNoBodyTest() {

        var exception = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .post("/roles/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .body().as(ProblemDetailResponseDTO.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isNotNull().isEqualTo(ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS.name());
        assertThat(exception.getDetail()).isNotNull()
                .isEqualTo("searchRolesByCriteria.roleSearchCriteriaDTO: must not be null");
        assertThat(exception.getInvalidParams()).isNotNull().isNotEmpty();
    }

    @Test
    void roleSearchEmptyResultTest() throws JsonProcessingException {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        ObjectMapper mapper = new ObjectMapper();
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = aliceToken.split("\\.");
        String body = new String(decoder.decode(chunks[1]));
        JSONObject jwt = mapper.readValue(body, JSONObject.class);

        var result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, aliceToken)
                .body(new RoleSearchCriteriaDTO().name("does-not-exists").issuer(jwt.get("iss").toString()))
                .post("/roles/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(RolePageResultDTO.class);

        assertThat(result).isNotNull();
        assertThat(result.getStream()).isNotNull().isEmpty();
    }

    @Test
    void searchUsersRequest() throws JsonProcessingException {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        ObjectMapper mapper = new ObjectMapper();
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = aliceToken.split("\\.");
        String body = new String(decoder.decode(chunks[1]));
        JSONObject jwt = mapper.readValue(body, JSONObject.class);

        var tokensFromSecondKc = this.getTokens(keycloakClient1, USER_ALICE);
        var aliceToken1 = tokensFromSecondKc.getIdToken();
        String[] chunks1 = aliceToken1.split("\\.");
        String body1 = new String(decoder.decode(chunks1[1]));
        JSONObject jwt1 = mapper.readValue(body1, JSONObject.class);

        UserSearchCriteriaDTO dto = new UserSearchCriteriaDTO();
        dto.setUserName("alice");
        dto.setIssuer(jwt.get("iss").toString());

        var result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(UserPageResultDTO.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertNotNull(result.getStream());
        Assertions.assertEquals(1, result.getStream().size());
        Assertions.assertEquals("alice", result.getStream().get(0).getUsername());
        dto.setUserId(result.getStream().get(0).getId());

        result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(UserPageResultDTO.class);
        Assertions.assertEquals(1, result.getTotalElements());
        dto.setUserId("");

        result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(UserPageResultDTO.class);
        Assertions.assertEquals(1, result.getTotalElements());

        //search by id in correct kc

        dto.setUserId(jwt.get("sub").toString());

        result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(UserPageResultDTO.class);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertNotNull(result.getStream().get(0).getDomain());
        Assertions.assertNotNull(result.getStream().get(0).getProvider());

        //search in wrong kc => user with given id does not exist
        dto.setIssuer(jwt1.get("iss").toString());
        result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(UserPageResultDTO.class);
        Assertions.assertEquals(0, result.getTotalElements());

        dto.setIssuer("notExisting");
        //search in not existing kc and realm
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void searchUsersEmptyToken() {

        UserSearchCriteriaDTO dto = new UserSearchCriteriaDTO();
        dto.setIssuer("");
        var exception = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, " ")
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .body().as(ProblemDetailResponseDTO.class);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(ERROR_PARSE_TOKEN.name(), exception.getErrorCode());
        Assertions.assertEquals(
                "Error parse raw token",
                exception.getDetail());
        assertThat(exception.getInvalidParams()).isNotNull().isEmpty();

    }

    @Test
    void searchUsersNoRequest() {

        var exception = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .body().as(ProblemDetailResponseDTO.class);

        Assertions.assertNotNull(exception);
        Assertions.assertEquals(CONSTRAINT_VIOLATIONS.name(), exception.getErrorCode());
        Assertions.assertEquals(
                "searchUsersByCriteria.userSearchCriteriaDTO: must not be null",
                exception.getDetail());
        Assertions.assertNotNull(exception.getInvalidParams());
    }

    @Test
    void createAndUpdateUserTest() throws JsonProcessingException {
        var tokens = this.getTokens(keycloakClient, USER_ALICE);
        var aliceToken = tokens.getIdToken();
        ObjectMapper mapper = new ObjectMapper();
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = aliceToken.split("\\.");
        String body = new String(decoder.decode(chunks[1]));
        JSONObject jwt = mapper.readValue(body, JSONObject.class);

        CreateUserRequestDTO requestDTO = new CreateUserRequestDTO().email("test@test.com").username("newUser").firstName("new")
                .lastName("user").issuer(jwt.get("iss").toString());
        requestDTO.setTemporaryPassword("testPassword");
        requestDTO.setAttributes(Map.of("testAttribute", List.of("attr1")));
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(requestDTO)
                .post("/users")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode());

        //search for new created user
        UserSearchCriteriaDTO dto = new UserSearchCriteriaDTO();
        dto.setUserName("newUser");
        dto.setIssuer(jwt.get("iss").toString());

        var result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(UserPageResultDTO.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(requestDTO.getEmail(), result.getStream().get(0).getEmail());
        Assertions.assertEquals(requestDTO.getAttributes().get("testAttribute").get(0),
                result.getStream().get(0).getAttributes().get("testAttribute").get(0));

        var updateDTO = new UpdateUserRequestDTO().email("updated@email.com").firstName("updatedFirstName")
                .lastName("updatedLastName").issuer(jwt.get("iss").toString());
        var attributes = new HashMap<String, List<String>>();
        attributes.put("testAttribute", List.of("updatedAttribute"));
        attributes.put("newAttribute", List.of("new1", "new2"));
        updateDTO.setAttributes(attributes);
        // update the user
        given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(updateDTO)
                .put("/users/" + result.getStream().get(0).getId())
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // search for user
        result = given()
                .auth().oauth2(authClient.getClientAccessToken("testClient"))
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_TOKEN, this.getTokens(keycloakClient, USER_ALICE).getIdToken())
                .body(dto)
                .post("/users/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(UserPageResultDTO.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(updateDTO.getEmail(), result.getStream().get(0).getEmail());
        Assertions.assertEquals(updateDTO.getFirstName(), result.getStream().get(0).getFirstName());
        Assertions.assertEquals(updateDTO.getLastName(), result.getStream().get(0).getLastName());
        Assertions.assertEquals(updateDTO.getAttributes().get("testAttribute").get(0),
                result.getStream().get(0).getAttributes().get("testAttribute").get(0));
    }
}
