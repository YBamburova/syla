package com.syla.core.platform.internal;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableScheduling
class PlatformConfiguration {
    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info().title("Syla Core API").version("v1")
                .description("Private manual activity logging. Sign in through OIDC, consent, and send the session CSRF token for mutations."))
                .servers(List.of(new Server().url("/")));
    }

    @Bean
    ClientRegistrationRepository registrations(@Value("${syla.oidc-issuer}") String issuer,
                                              @Value("${syla.identity-backchannel}") String backchannel,
                                              @Value("${syla.web-origin}") String origin) {
        // The browser and container use different hosts; issuer validation remains exact.
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("keycloak")
                .clientId("syla-web").clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(origin + "/login/oauth2/code/keycloak").scope("openid", "profile")
                .authorizationUri(issuer + "/protocol/openid-connect/auth")
                .tokenUri(backchannel + "/realms/syla/protocol/openid-connect/token")
                .jwkSetUri(backchannel + "/realms/syla/protocol/openid-connect/certs")
                .userInfoUri(backchannel + "/realms/syla/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub").issuerUri(issuer)
                .providerConfigurationMetadata(Map.of("end_session_endpoint", issuer + "/protocol/openid-connect/logout"))
                .clientName("Syla").build());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clients,
                                            @Value("${syla.web-origin:http://localhost:5173}") String origin) throws Exception {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        var logout = new OidcClientInitiatedLogoutSuccessHandler(clients);
        logout.setPostLogoutRedirectUri(origin);
        return http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/health", "/api/v1/session", "/api/v1/session/csrf",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2Login(login -> login.authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                        .successHandler((request, response, authentication) -> {
                            request.getSession().setAttribute("syla.loginAt", Instant.now().getEpochSecond());
                            response.sendRedirect(origin);
                        }).failureHandler((request, response, exception) -> response.sendRedirect(origin + "/?login=failed")))
                .logout(config -> config.logoutSuccessHandler(logout))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> problem(response, 401, "Sign in to continue."))
                        .accessDeniedHandler((request, response, exception) -> problem(response, 403, "This request was not allowed. Refresh your session and try again.")))
                .addFilterBefore(new SessionLifetimeFilter(), AuthorizationFilter.class)
                .build();
    }

    static void problem(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/problem+json"); response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"type\":\"about:blank\",\"status\":" + status + ",\"detail\":\"" + message + "\"}");
    }

    private static class SessionLifetimeFilter extends OncePerRequestFilter {
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            var session = request.getSession(false);
            if (session != null && session.getAttribute("syla.loginAt") instanceof Long loginAt && Instant.now().getEpochSecond() - loginAt > 43200) {
                session.invalidate();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                problem(response, 401, "Your session expired. Sign in again.");
                return;
            }
            if (request.getRequestURI().startsWith("/api/")) response.setHeader("Cache-Control", "no-store");
            chain.doFilter(request, response);
        }
    }
}
