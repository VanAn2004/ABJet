package com.abjet.courseapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityTests {
    @Autowired MockMvc mvc;

    @Test void homeLinksToLibrary() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(containsString("http://localhost:8082")));
    }

    @Test void dashboardRequiresLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test void dashboardShowsIdTokenClaimsWithoutRawToken() throws Exception {
        mvc.perform(get("/dashboard").with(oidcLogin().idToken(token -> token
            .subject("demo-user").claim("name", "Demo User").claim("email", "demo@example.com")
            .claim("iss", "http://localhost:8080/realms/abjet").claim("aud", java.util.List.of("course-app")))))
            .andExpect(status().isOk()).andExpect(model().attributeExists("idTokenClaims"))
            .andExpect(content().string(containsString("demo@example.com")));
    }

    @Test void logoutRequiresCsrf() throws Exception {
        mvc.perform(post("/logout").with(oidcLogin())).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(oidcLogin()).with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    }
}
