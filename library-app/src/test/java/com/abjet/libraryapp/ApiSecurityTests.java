package com.abjet.libraryapp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityTests {
    static final String ISSUER = "http://localhost:8080/realms/abjet";
    static final RSAKey KEY;
    static final HttpServer JWKS;
    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("demo-test-key").generate();
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            JWKS.createContext("/certs", exchange -> {
                byte[] bytes = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            JWKS.start();
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/certs");
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences[0]", () -> "library-app");
    }
    @AfterAll static void stop() { JWKS.stop(0); }
    @Autowired MockMvc mvc;

    String token(String issuer, String audience, Instant expires, RSAKey key) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).subject("demo-user")
            .audience(audience).issueTime(Date.from(Instant.now().minusSeconds(600)))
            .expirationTime(Date.from(expires)).claim("typ", "Bearer").claim("scope", "openid profile email").build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
    @Test void missingTokenReturns401WithoutLoginRedirect() throws Exception {
        mvc.perform(get("/api/demo")).andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Location"));
    }
    @Test void browserSessionCannotAuthenticateApi() throws Exception {
        var webSession = (org.springframework.mock.web.MockHttpSession) mvc
            .perform(get("/dashboard").with(oidcLogin())).andReturn().getRequest().getSession();
        mvc.perform(get("/api/demo").session(webSession)).andExpect(status().isUnauthorized());
    }
    @Test void validSignedTokenReturnsDemo() throws Exception {
        mvc.perform(get("/api/demo").header("Authorization", "Bearer " + token(ISSUER, "library-app", Instant.now().plusSeconds(300), KEY)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subject").value("demo-user"))
            .andExpect(jsonPath("$.application").value("library-app"));
    }
    @Test void wrongAudienceIsRejected() throws Exception {
        mvc.perform(get("/api/demo").header("Authorization", "Bearer " + token(ISSUER, "course-app", Instant.now().plusSeconds(300), KEY)))
            .andExpect(status().isUnauthorized());
    }
    @Test void wrongIssuerIsRejected() throws Exception {
        mvc.perform(get("/api/demo").header("Authorization", "Bearer " + token("http://untrusted.example", "library-app", Instant.now().plusSeconds(300), KEY)))
            .andExpect(status().isUnauthorized());
    }
    @Test void expiredTokenIsRejected() throws Exception {
        mvc.perform(get("/api/demo").header("Authorization", "Bearer " + token(ISSUER, "library-app", Instant.now().minusSeconds(300), KEY)))
            .andExpect(status().isUnauthorized());
    }
    @Test void wrongSignatureIsRejected() throws Exception {
        mvc.perform(get("/api/demo").header("Authorization", "Bearer " + token(ISSUER, "library-app", Instant.now().plusSeconds(300), new RSAKeyGenerator(2048).generate())))
            .andExpect(status().isUnauthorized());
    }
    @Test void malformedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/demo").header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }
    @Test void idTokenCannotReplaceAccessToken() throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject("demo-user")
            .audience("library-app").issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("typ", "ID").build();
        var idToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), claims);
        idToken.sign(new RSASSASigner(KEY));
        mvc.perform(get("/api/demo").header("Authorization", "Bearer " + idToken.serialize()))
            .andExpect(status().isUnauthorized());
    }
    @Test void htmlRemainsOidcProtected() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(containsString("http://localhost:8081")));
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/dashboard").with(oidcLogin())).andExpect(status().isOk());
        mvc.perform(post("/logout").with(oidcLogin())).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(oidcLogin()).with(csrf())).andExpect(redirectedUrl("/"));
    }
}
