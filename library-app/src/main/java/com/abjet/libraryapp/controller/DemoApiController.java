package com.abjet.libraryapp.controller;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoApiController {
    @GetMapping("/api/demo")
    public Map<String, String> demo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "application", "library-app",
            "subject", jwt.getSubject() == null ? "Không có subject" : jwt.getSubject(),
            "message", "Library API đã chấp nhận access token hợp lệ.");
    }
}
