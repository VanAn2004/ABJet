package com.abjet.courseapp;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
class LibraryDemoTests {
    static final AtomicReference<String> AUTHORIZATION = new AtomicReference<>();
    static final AtomicInteger RESPONSE_STATUS = new AtomicInteger(200);
    static final HttpServer API;
    static {
        try {
            API = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            API.createContext("/api/demo", exchange -> {
                AUTHORIZATION.set(exchange.getRequestHeaders().getFirst("Authorization"));
                String body = RESPONSE_STATUS.get() == 200
                    ? "{\"application\":\"library-app\",\"subject\":\"demo-user\",\"message\":\"API accepted access token\"}"
                    : "private-server-detail";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(RESPONSE_STATUS.get(), bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            API.start();
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.other-app-url", () -> "http://127.0.0.1:" + API.getAddress().getPort());
    }
    @AfterAll static void stop() { API.stop(0); }
    @Autowired MockMvc mvc;
    @Autowired ClientRegistrationRepository registrations;
    @MockitoBean OAuth2AuthorizedClientManager manager;

    @BeforeEach void setup() {
        AUTHORIZATION.set(null);
        RESPONSE_STATUS.set(200);
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-access-token",
            Instant.now(), Instant.now().plusSeconds(300));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(
            registrations.findByRegistrationId("keycloak"), "user", token));
    }
    @Test void sendsAccessTokenFromBackendAndShowsResult() throws Exception {
        mvc.perform(post("/library-demo").with(oidcLogin()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("API accepted access token")))
            .andExpect(content().string(not(containsString("test-access-token"))));
        assertEquals("Bearer test-access-token", AUTHORIZATION.get());
    }
    @Test void apiRejectionShowsSafeExplanation() throws Exception {
        RESPONSE_STATUS.set(401);
        mvc.perform(post("/library-demo").with(oidcLogin()).with(csrf()))
            .andExpect(status().isOk()).andExpect(model().attributeExists("apiError"))
            .andExpect(content().string(not(containsString("private-server-detail"))));
    }
    @Test void serverFailureShowsSafeExplanation() throws Exception {
        RESPONSE_STATUS.set(500);
        mvc.perform(post("/library-demo").with(oidcLogin()).with(csrf()))
            .andExpect(status().isOk()).andExpect(model().attributeExists("apiError"));
    }
    @Test void callRequiresLoginAndCsrf() throws Exception {
        mvc.perform(post("/library-demo").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/library-demo").with(oidcLogin())).andExpect(status().isForbidden());
    }
}
