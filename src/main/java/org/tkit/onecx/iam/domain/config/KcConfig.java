package org.tkit.onecx.iam.domain.config;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * Iam svc configuration
 */
@ConfigDocFilename("onecx-iam-svc.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "onecx.iam")
public interface KcConfig {

    /**
     * Keycloak configurations
     */
    @WithName("keycloaks")
    Map<String, KeycloakClientConfig> keycloaks();

    /**
     * general shared client configurations
     */
    interface SharedClientConfig {

        /**
         * Display name for keycloak
         */
        @WithName("display-name")
        String displayName();

        /**
         * Description for keycloak
         */
        @WithName("description")
        Optional<String> description();

        /**
         * url of keycloak
         */
        @WithName("url")
        String url();
    }

    /**
     * Keycloak client configurations
     */
    interface KeycloakClientConfig extends SharedClientConfig {

        /**
         * Baseurl of keycloak
         */
        @WithName("issuerHost")
        String issuerHost();

        /**
         * Keycloak realm
         */
        @WithName("realm")
        String realm();

        /**
         * Client for keylcloak admin api
         */
        @WithName("client")
        String clientId();

        /**
         * Client secret
         */
        @WithName("secret")
        String clientSecret();

        /**
         * Username for keycloak admin api access
         */
        @WithName("username")
        String username();

        /**
         * User password
         */
        @WithName("password")
        String password();
    }
}
