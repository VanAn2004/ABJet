package com.abjet.courseapp.controller;

import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final String otherAppUrl;
    public HomeController(@Value("${app.other-app-url:http://localhost:8082}") String otherAppUrl) {
        this.otherAppUrl = otherAppUrl;
    }
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("otherAppUrl", otherAppUrl);
        return "home";
    }
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OidcUser user, Model model) {
        String name = user.getFullName();
        if (name == null || name.isBlank()) name = user.getPreferredUsername();
        model.addAttribute("name", name == null || name.isBlank() ? user.getSubject() : name);
        model.addAttribute("email", user.getEmail() == null ? "Chưa có email" : user.getEmail());
        var claims = new LinkedHashMap<String, Object>();
        for (String key : new String[] {"iss", "sub", "aud", "iat", "exp"}) {
            Object value = user.getIdToken().getClaims().get(key);
            claims.put(key, value == null ? "Không có" : value);
        }
        model.addAttribute("idTokenClaims", claims);
        model.addAttribute("otherAppUrl", otherAppUrl);
        return "dashboard";
    }
}
