package com.hexagon.api_gateway;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * The three services are replaced by one stub that answers 200 and echoes the
 * X-User-* headers it received, so the tests see exactly what the gateway forwards.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false", "app.jwt.secret=" + GatewaySecurityTests.SECRET})
class GatewaySecurityTests {

    static final String SECRET = "gateway-test-secret-with-at-least-32-chars";
    private static final String FRONTEND = "http://localhost:5173";

    static final HttpServer stub = startStub();

    @DynamicPropertySource
    static void routesToStub(DynamicPropertyRegistry registry) {
        // a list property replaces the whole list, so each route is declared in full (same paths as application.yaml)
        String uri = "http://localhost:" + stub.getAddress().getPort();
        String[][] routes = {
                {"user-service", "Path=/users/**,/auth/**"},
                {"catalog-service", "Path=/books/**"},
                {"loan-service", "Path=/loans/**,/fines/**"},
        };
        for (int i = 0; i < routes.length; i++) {
            String prefix = "spring.cloud.gateway.server.webflux.routes[" + i + "]";
            String[] route = routes[i];
            registry.add(prefix + ".id", () -> route[0]);
            registry.add(prefix + ".uri", () -> uri);
            registry.add(prefix + ".predicates[0]", () -> route[1]);
        }
    }

    @AfterAll
    static void stopStub() {
        stub.stop(0);
    }

    @LocalServerPort
    int port;

    // ---------- authentication ----------

    @Test
    void requestWithoutTokenGets401InServiceErrorFormat() {
        client().get().uri("/books").exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication required")
                .jsonPath("$.details").isArray();
    }

    @Test
    void tokenWithWrongSignatureOrExpiredGets401() {
        String forged = token("1", "LIBRARIAN", Instant.now().plusSeconds(3600), "another-secret-with-at-least-32-characters");
        String expired = token("1", "LIBRARIAN", Instant.now().minusSeconds(60), SECRET);

        client().get().uri("/books").header("Authorization", "Bearer " + forged).exchange()
                .expectStatus().isUnauthorized();
        client().get().uri("/books").header("Authorization", "Bearer " + expired).exchange()
                .expectStatus().isUnauthorized();
        client().get().uri("/books").header("Authorization", "Bearer not-a-jwt").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void loginAndPublicRegistrationNeedNoToken() {
        client().post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isOk();
        client().post().uri("/auth/register/student").contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isOk();
        client().post().uri("/auth/register/external").contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isOk();
    }

    // ---------- role matrix ----------

    @Test
    void memberCanReadCatalogAndOwnDataButNotCounterOperations() {
        String member = token("7", "MEMBER");

        expect(HttpMethod.GET, "/books", member, 200);
        expect(HttpMethod.GET, "/books/abc", member, 200);
        expect(HttpMethod.GET, "/books/search?title=x", member, 200);
        expect(HttpMethod.GET, "/users/me", member, 200);
        expect(HttpMethod.GET, "/users/7", member, 200);
        expect(HttpMethod.GET, "/loans/me", member, 200);
        expect(HttpMethod.GET, "/loans/me/pending", member, 200);
        expect(HttpMethod.GET, "/fines/me", member, 200);
        expect(HttpMethod.GET, "/loans/user/7", member, 200);
        expect(HttpMethod.POST, "/loans/1/renew", member, 200);

        expect(HttpMethod.POST, "/books", member, 403);
        expect(HttpMethod.PUT, "/books/abc", member, 403);
        expect(HttpMethod.DELETE, "/books/abc", member, 403);
        expect(HttpMethod.GET, "/users", member, 403);
        expect(HttpMethod.POST, "/users", member, 403);
        expect(HttpMethod.PUT, "/users/7", member, 403);
        expect(HttpMethod.POST, "/loans", member, 403);
        expect(HttpMethod.POST, "/loans/1/return", member, 403);
        expect(HttpMethod.POST, "/fines/1/pay", member, 403);
    }

    @Test
    void forbiddenResponseUsesServiceErrorFormat() {
        client().get().uri("/users").header("Authorization", "Bearer " + token("7", "MEMBER")).exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access denied");
    }

    @Test
    void librarianCanUseCounterOperations() {
        String librarian = token("1", "LIBRARIAN");

        expect(HttpMethod.GET, "/users", librarian, 200);
        expect(HttpMethod.POST, "/users", librarian, 200);
        expect(HttpMethod.PUT, "/users/7", librarian, 200);
        expect(HttpMethod.POST, "/books", librarian, 200);
        expect(HttpMethod.DELETE, "/books/abc", librarian, 200);
        expect(HttpMethod.POST, "/loans", librarian, 200);
        expect(HttpMethod.POST, "/loans/1/return", librarian, 200);
        expect(HttpMethod.POST, "/loans/1/renew", librarian, 200);
        expect(HttpMethod.POST, "/fines/1/pay", librarian, 200);
    }

    @Test
    void availabilityEndpointIsBlockedForEveryone() {
        expect(HttpMethod.PATCH, "/books/abc/availability", token("1", "LIBRARIAN"), 403);
        expect(HttpMethod.PATCH, "/books/abc/availability", token("7", "MEMBER"), 403);
        client().patch().uri("/books/abc/availability").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void unknownRoutesAreDenied() {
        expect(HttpMethod.GET, "/actuator/env", token("1", "LIBRARIAN"), 403);
    }

    // ---------- identity headers ----------

    @Test
    void gatewaySetsIdentityHeadersFromTheToken() {
        client().get().uri("/loans/me").header("Authorization", "Bearer " + token("7", "MEMBER")).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("7")
                .jsonPath("$.role").isEqualTo("MEMBER");
    }

    @Test
    void forgedIdentityHeadersAreDiscarded() {
        client().get().uri("/loans/me")
                .header("Authorization", "Bearer " + token("7", "MEMBER"))
                .header("X-User-Id", "1")
                .header("X-User-Role", "LIBRARIAN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("7")
                .jsonPath("$.role").isEqualTo("MEMBER");

        // public route: forged headers are removed and nothing is added
        client().post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .header("X-User-Id", "1")
                .header("x-user-role", "LIBRARIAN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("")
                .jsonPath("$.role").isEqualTo("");
    }

    // ---------- CORS ----------

    @Test
    void corsPreflightAndErrorResponsesCarryCorsHeaders() {
        client().options().uri("/books")
                .header("Origin", FRONTEND)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", FRONTEND);

        // without CORS headers on the 401, the browser would report a network error instead
        client().get().uri("/books").header("Origin", FRONTEND).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", FRONTEND);

        client().get().uri("/books").header("Origin", "http://evil.example").exchange()
                .expectStatus().isForbidden();
    }

    // ---------- helpers ----------

    private void expect(HttpMethod method, String uri, String token, int status) {
        client().method(method).uri(uri)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().value(actual -> {
                    if (actual != status) {
                        throw new AssertionError(method + " " + uri + " expected " + status + " but was " + actual);
                    }
                });
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private static String token(String subject, String role) {
        return token(subject, role, Instant.now().plusSeconds(3600), SECRET);
    }

    private static String token(String subject, String role, Instant expiresAt, String secret) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .claim("role", role)
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String userId = exchange.getRequestHeaders().getFirst("X-User-Id");
                String role = exchange.getRequestHeaders().getFirst("X-User-Role");
                byte[] body = ("{\"userId\":\"" + (userId == null ? "" : userId)
                        + "\",\"role\":\"" + (role == null ? "" : role) + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
