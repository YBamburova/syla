package com.syla.core.identity.internal;

import java.io.IOException;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
class BrowserSecurity {
    static final String CSRF_RAW = "syla.csrf.raw";
    static final String LOGIN_AT = "syla.login-at";

    @Bean
    SecurityFilterChain browser(HttpSecurity http, ClientRegistrationRepository registrations,
                                AccountRegistration accounts, ObjectMapper mapper) throws Exception {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        var logout = new OidcClientInitiatedLogoutSuccessHandler(registrations);
        logout.setPostLogoutRedirectUri("{baseUrl}/swagger-ui/index.html");
        http.authorizeHttpRequests(access -> access
                .requestMatchers("/api/v1/health", "/api/v1/csrf", "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs/**", "/openapi/**", "/oauth2/**", "/login/**", "/error").permitAll()
                .anyRequest().authenticated());
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfHandler()).ignoringRequestMatchers("/api/v1/health"));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, failure) -> problem(response, mapper, 401, "Sign in first."))
                .accessDeniedHandler((request, response, failure) -> problem(response, mapper,
                        SecurityContextHolder.getContext().getAuthentication() == null
                                || SecurityContextHolder.getContext().getAuthentication() instanceof org.springframework.security.authentication.AnonymousAuthenticationToken ? 401 : 403, "Access denied or CSRF token missing.")));
        http.oauth2Login(login -> login.authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                .successHandler((request, response, authentication) -> {
                    try {
                        accounts.afterVerifiedLogin((OidcUser) authentication.getPrincipal());
                        request.getSession().setAttribute(LOGIN_AT, System.currentTimeMillis());
                        response.sendRedirect("/swagger-ui/index.html");
                    } catch (com.syla.core.ApiFailure failure) {
                        SecurityContextHolder.clearContext();
                        request.getSession().invalidate();
                        problem(response, mapper, 403, "This account cannot sign in.");
                    }
                }));
        http.logout(config -> config.logoutSuccessHandler(logout));
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                var session = request.getSession(false);
                if (session != null && session.getAttribute(LOGIN_AT) instanceof Long loginAt
                        && System.currentTimeMillis() - loginAt >= Duration.ofHours(12).toMillis()) {
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                    problem(response, mapper, 401, "Session expired; sign in again.");
                    return;
                }
                chain.doFilter(request, response);
            }
        }, AuthorizationFilter.class);
        return http.build();
    }

    private static void problem(HttpServletResponse response, ObjectMapper mapper, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.valueOf(status), detail));
    }

    private static class SpaCsrfHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           java.util.function.Supplier<CsrfToken> token) {
            xor.handle(request, response, token);
            request.setAttribute(CSRF_RAW, token.get());
        }
        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
            return (StringUtils.hasText(request.getHeader(token.getHeaderName())) ? plain : xor)
                    .resolveCsrfTokenValue(request, token);
        }
    }
}
