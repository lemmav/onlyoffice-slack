package com.onlyoffice.slack.configuration.registry;

import lombok.Setter;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "onlyoffice.registry.client")
@Setter
public class KeycloakConfiguration {
    private String authServer;
    private String clientId;
    private String clientSecret;
    private String authRealm;

    @Bean
    public Keycloak getKeycloakClientInstance() {
        return KeycloakBuilder
                .builder()
                .serverUrl(authServer)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .realm(authRealm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .resteasyClient(
                        new ResteasyClientBuilder()
                                .connectionPoolSize(50).build()
                ).build();
    }
}
