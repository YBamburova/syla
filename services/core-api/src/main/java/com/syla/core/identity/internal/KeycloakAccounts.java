package com.syla.core.identity.internal;

import java.time.Duration;
import java.net.http.HttpClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
class KeycloakAccounts {
    private final IdentityProperties properties;
    private final RestClient client;

    KeycloakAccounts(IdentityProperties properties) {
        this.properties = properties;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    boolean delete(String issuer, String subject) {
        if (!issuer.equals(properties.issuer().toString()) || properties.adminClientSecret().isBlank()) return false;
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "client_credentials");

            var token = client.post().uri(issuer + "/protocol/openid-connect/token")
                    .headers(headers -> headers.setBasicAuth(properties.adminClientId(), properties.adminClientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Token.class);
            if (token == null || token.accessToken() == null) return false;
            var realm = properties.issuer().getPath().substring("/realms/".length());
            var base = properties.issuer().resolve("/");
            client.delete().uri(builder -> builder.scheme(base.getScheme()).host(base.getHost()).port(base.getPort())
                    .pathSegment("admin", "realms", realm, "users", subject).build())
                    .headers(headers -> headers.setBearerAuth(token.accessToken()))
                    .retrieve().onStatus(status -> status.value() == 404, (request, response) -> {})
                    .toBodilessEntity();
            return true;
        } catch (org.springframework.web.client.RestClientResponseException failure) {
            org.slf4j.LoggerFactory.getLogger(KeycloakAccounts.class).warn("Provider cleanup pending: HTTP {}", failure.getStatusCode().value());
            return false;
        } catch (org.springframework.web.client.RestClientException ignored) {
            org.slf4j.LoggerFactory.getLogger(KeycloakAccounts.class).warn("Provider cleanup pending: {}", ignored.getClass().getSimpleName());
            // Keep the deletion pending. Never log tokens, subjects, provider response bodies, or credentials.
            return false;
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record Token(@JsonProperty("access_token") String accessToken) {}
}
