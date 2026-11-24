package com.hexagon.api_gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The gateway validates the JWT issued by the user-service (HS256, shared JWT_SECRET)
 * and applies the role matrix. Ownership checks ("only your own loans") live in the
 * services that own the data, using the X-User-Id / X-User-Role headers set by UserHeadersFilter.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    static final int MIN_SECRET_LENGTH = 32;
    private static final String LIBRARIAN = "LIBRARIAN";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveJwtDecoder jwtDecoder,
                                                         JsonSecurityErrorHandler errors) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // stateless: every request carries its token
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                // CORS here (not in the gateway routes), so 401/403 responses also carry CORS headers
                .cors(cors -> {})
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // internal endpoint, called only by the loan-service through Eureka
                        .pathMatchers(HttpMethod.PATCH, "/books/*/availability").denyAll()
                        .pathMatchers(HttpMethod.POST, "/auth/login", "/auth/register/**").permitAll()

                        .pathMatchers(HttpMethod.GET, "/books", "/books/**").authenticated()
                        .pathMatchers("/books", "/books/**").hasRole(LIBRARIAN)

                        // /users/me and /users/{id}: the user-service checks "only your own"
                        .pathMatchers(HttpMethod.GET, "/users/*").authenticated()
                        .pathMatchers("/users", "/users/**").hasRole(LIBRARIAN)

                        .pathMatchers(HttpMethod.GET, "/loans/me", "/loans/me/pending", "/fines/me").authenticated()
                        // the loan-service checks the owner of the loan / of the user id
                        .pathMatchers(HttpMethod.POST, "/loans/*/renew").authenticated()
                        .pathMatchers(HttpMethod.GET, "/loans/*", "/loans/user/**", "/fines/user/*").authenticated()
                        .pathMatchers(HttpMethod.POST, "/loans", "/loans/*/return", "/fines/*/pay").hasRole(LIBRARIAN)

                        .anyExchange().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(authenticationConverter()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and have at least " + MIN_SECRET_LENGTH + " characters");
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        // default validators: signature, exp and nbf
        return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** "role": "LIBRARIAN" → authority ROLE_LIBRARIAN */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> authenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
