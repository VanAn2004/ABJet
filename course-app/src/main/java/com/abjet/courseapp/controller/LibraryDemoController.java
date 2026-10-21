package com.abjet.courseapp.controller;

import com.abjet.courseapp.service.LibraryApiService;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Controller
public class LibraryDemoController {
    private final LibraryApiService libraryApi;

    public LibraryDemoController(LibraryApiService libraryApi) {
        this.libraryApi = libraryApi;
    }

    @PostMapping("/library-demo")
    public String demo(Model model) {
        try {
            model.addAttribute("apiResult", libraryApi.callDemo());
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String message = switch (status) {
                case 401 -> "Library API từ chối token (401). Kiểm tra issuer, audience library-app và thời hạn; đăng nhập lại sau khi sửa cấu hình Keycloak.";
                case 403 -> "Library API từ chối quyền truy cập (403).";
                default -> "Library API trả lỗi HTTP " + status + ". Hãy thử lại khi Library App sẵn sàng.";
            };
            model.addAttribute("apiError", message);
        } catch (OAuth2AuthorizationException e) {
            model.addAttribute("apiError", "Không thể cấp hoặc làm mới access token. Hãy đăng xuất Course App rồi đăng nhập lại.");
        } catch (RestClientException e) {
            model.addAttribute("apiError", "Không thể kết nối Library API. Kiểm tra Library App đang chạy tại cổng 8082.");
        }
        return "api-result";
    }
}
