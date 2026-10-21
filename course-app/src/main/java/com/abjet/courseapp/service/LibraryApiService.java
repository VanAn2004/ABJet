package com.abjet.courseapp.service;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LibraryApiService {
    private final RestClient client;

    public LibraryApiService(OAuth2AuthorizedClientManager manager,
            @Value("${app.other-app-url:http://localhost:8082}") String libraryUrl) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(request -> "keycloak");
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(libraryUrl)
            .requestFactory(factory).requestInterceptor(interceptor).build();
    }

    public String callDemo() {
        return client.get().uri("/api/demo").retrieve().body(String.class);
    }
}
