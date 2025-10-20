package org.tkit.onecx.iam.test;

import static io.restassured.RestAssured.given;
import static org.tkit.onecx.iam.test.RealmFactory.createRealm;

import java.io.IOException;
import java.util.*;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.server.KeycloakContainer;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager,
        DevServicesContext.ContextAware {

    private String containerNetworkId = null;

    public static final String KC0 = "kc0";
    public static final String KC1 = "kc1";
    private static final Logger log = LoggerFactory.getLogger(KeycloakTestResource.class);
    private static final String KEYCLOAK_REALM = "quarkus";
    private final List<TestKeycloakContainer> containers = new ArrayList<>();

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        containerNetworkId = context.containerNetworkId().orElse(null);
    }

    public static String authServerUrlProp(String name) {
        return name + ".auth-server-url";
    }

    public static String urlProp(String name) {
        return name + ".url";
    }

    public static String urlClientProp(String name) {
        return name + ".client.url";
    }

    private static void postRealm(String url, RealmRepresentation realm) {
        try {
            var token = getAdminAccessToken(url);
            given().auth().oauth2(token)
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsString(realm))
                    .when()
                    .post(url + "/admin/realms")
                    .then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getAdminAccessToken(String url) {
        return given()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(url + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    @Override
    public Map<String, String> start() {
        var n = new JoinNetwork();
        var container0 = new TestKeycloakContainer(n, KC0, "26.2.0");
        containers.add(container0);

        var container1 = new TestKeycloakContainer(n, KC1, "18.0.0");
        containers.add(container1);

        containers.forEach(this::startContainer);

        Map<String, String> result = new HashMap<>();
        containers.forEach(c -> {
            result.put(urlClientProp(c.getName()), c.getServerUrl());
            result.put(urlProp(c.getName()), containerNetworkId == null ? c.getServerUrl() : c.getInternalUrl());
        });
        return result;
    }

    private void startContainer(TestKeycloakContainer container) {
        log.info("Start container. Name: '{}'", container.getName());
        container.start();

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM, "quarkus-app", "secret",
                Map.of("alice", List.of("user", "admin"), "bob", List.of("user")));

        log.info("Create realm '{}' for container '{}'", realm.getRealm(), container.getName());
        postRealm(container.getServerUrl(), realm);

        log.info("Container started. Name: '{}'", container.getName());
    }

    @Override
    public void stop() {
        containers.forEach(k -> {
            try {
                k.stop();
            } catch (Exception ex) {
                log.error("Error stopping container {}", k, ex);
            }
        });
    }

    public static class TestKeycloakContainer extends KeycloakContainer {

        private final String name;

        public TestKeycloakContainer(Network n, String name, String version) {
            super(DockerImageName.parse("quay.io/keycloak/keycloak:" + version));
            this.withEnv("KEYCLOAK_ADMIN", "admin");
            this.withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin");
            this.name = name;
            this.withNetwork(n);
            this.setNetworkAliases(List.of(this.name));
        }

        public String getName() {
            return name;
        }

        @Override
        public String getInternalUrl() {
            boolean useHttps = false;
            return String.format("%s://%s:%d",
                    useHttps ? "https" : "http", name, getPort());
        }
    }

    private class JoinNetwork implements Network {
        @Override
        public String getId() {
            return containerNetworkId;
        }

        @SuppressWarnings("java:S1186")
        @Override
        public void close() {
            // ignore
        }

        @Override
        public Statement apply(Statement var1, Description var2) {
            return null;
        }
    }
}
