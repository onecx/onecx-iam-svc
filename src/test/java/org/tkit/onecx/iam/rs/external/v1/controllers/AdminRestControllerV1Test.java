//package org.tkit.onecx.iam.rs.external.v1.controllers;
//
//import static io.restassured.RestAssured.given;
//import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
//
//import java.io.IOException;
//import java.util.Base64;
//
//import jakarta.ws.rs.core.Response;
//
//import org.jose4j.json.internal.json_simple.JSONObject;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.tkit.onecx.iam.test.AbstractTest;
//import org.tkit.quarkus.security.test.GenerateKeycloakClient;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import gen.org.tkit.onecx.iam.v1.model.UserRolesResponseDTOV1;
//import gen.org.tkit.onecx.iam.v1.model.UserRolesSearchRequestDTOV1;
//import io.quarkus.test.common.http.TestHTTPEndpoint;
//import io.quarkus.test.junit.QuarkusTest;
//import io.quarkus.test.keycloak.client.KeycloakTestClient;
//
//@QuarkusTest
//@TestHTTPEndpoint(AdminRestControllerV1.class)
//@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ia:read", "ocx-ia:write" })
//class AdminRestControllerV1Test extends AbstractTest {
//
//    private static final KeycloakTestClient keycloakAuthClient = new KeycloakTestClient();
//    KeycloakTestClient keycloakClient = createClient();
//    KeycloakTestClient keycloakClient1 = createClient1();
//
//    @Test
//    void getUserRolesTest() throws IOException {
//        var tokens = this.getTokens(keycloakClient, USER_ALICE);
//        var aliceToken = tokens.getIdToken();
//        ObjectMapper mapper = new ObjectMapper();
//        Base64.Decoder decoder = Base64.getUrlDecoder();
//        String[] chunks = aliceToken.split("\\.");
//        String body = new String(decoder.decode(chunks[1]));
//        JSONObject jwt = mapper.readValue(body, JSONObject.class);
//
//        String id = jwt.get("sub").toString();
//
//        var tokens1 = this.getTokens(keycloakClient1, USER_ALICE);
//        var aliceToken1 = tokens1.getIdToken();
//        String[] chunks1 = aliceToken1.split("\\.");
//        String body1 = new String(decoder.decode(chunks1[1]));
//        JSONObject jwt1 = mapper.readValue(body1, JSONObject.class);
//
//        var result = given()
//                .auth().oauth2(keycloakAuthClient.getClientAccessToken("testClient"))
//                .header(APM_HEADER_TOKEN, aliceToken)
//                .pathParam("userId", id)
//                .body(new UserRolesSearchRequestDTOV1().issuer(jwt.get("iss").toString()))
//                .contentType(APPLICATION_JSON)
//                .post()
//                .then().statusCode(Response.Status.OK.getStatusCode())
//                .extract().as(UserRolesResponseDTOV1.class);
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals(2, result.getRoles().size());
//
//        //user not found:
//        given()
//                .auth().oauth2(keycloakAuthClient.getClientAccessToken("testClient"))
//                .header(APM_HEADER_TOKEN, aliceToken)
//                .body(new UserRolesSearchRequestDTOV1().issuer(jwt1.get("iss").toString().replace("quarkus", "master")))
//                .pathParam("userId", id)
//                .contentType(APPLICATION_JSON).post()
//                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode());
//
//        //no token test
//        given()
//                .auth().oauth2(keycloakAuthClient.getClientAccessToken("testClient"))
//                .body(new UserRolesSearchRequestDTOV1().issuer(""))
//                .pathParam("userId", id)
//                .contentType(APPLICATION_JSON).post()
//                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode());
//    }
//
//}
