package com.syla.core.identity.internal;

import com.syla.core.platform.ApiProblem;
import com.syla.core.identity.IdentityLifecycle;
import org.springframework.security.core.Authentication;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class KeycloakAccountDeletion implements IdentityLifecycle {
    private final String baseUrl;
    private final String secret;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public KeycloakAccountDeletion(@Value("${syla.identity-backchannel:http://localhost:8081}") String baseUrl,
                                   @Value("${syla.lifecycle-secret:}") String secret, ObjectMapper mapper) {
        this.baseUrl = baseUrl; this.secret = secret; this.mapper = mapper;
    }

    public void deleteProviderAccount(Authentication authentication) {
        OidcUser user = (OidcUser) authentication.getPrincipal();
        if (secret.isBlank()) throw new ApiProblem(503, "Account cleanup is not configured. Your data is blocked; retry after configuration is restored.");
        try {
            String form = "grant_type=client_credentials&client_id=syla-lifecycle&client_secret=" + java.net.URLEncoder.encode(secret, java.nio.charset.StandardCharsets.UTF_8);
            var tokenResponse = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/realms/syla/protocol/openid-connect/token"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() != 200) throw new IllegalStateException("Provider authentication unavailable");
            String token = mapper.readTree(tokenResponse.body()).path("access_token").asText();
            String url = UriComponentsBuilder.fromUriString(baseUrl).pathSegment("admin", "realms", "syla", "users", user.getSubject()).build().encode().toUriString();
            var result = client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token).DELETE().build(), HttpResponse.BodyHandlers.discarding());
            if (result.statusCode() != 204 && result.statusCode() != 404) throw new IllegalStateException("Provider cleanup unavailable");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ApiProblem(503, "Identity cleanup was interrupted. Retry account deletion.");
        } catch (Exception failure) {
            throw new ApiProblem(503, "Identity cleanup is temporarily unavailable. Training access is blocked; retry account deletion.");
        }
    }
}
