# Cấu hình Keycloak cho ABJet

Đây là hướng dẫn cấu hình local; mã nguồn không tự tạo realm/client hoặc thay đổi Keycloak đang chạy.

## 1. Realm và tài khoản

Khởi động Keycloak ở `http://localhost:8080`. Đăng nhập Admin Console bằng tài khoản quản trị local của bạn.

Tạo hoặc chọn realm `abjet`. Issuer phải là:

```text
http://localhost:8080/realms/abjet
```

Tạo user demo trong realm; điền username, first name, last name và email. Đặt password trong tab Credentials, bỏ Temporary nếu không muốn đổi mật khẩu lúc demo. Không lưu password trong repo.

## 2. Hai client OpenID Connect

| Thuộc tính | Course | Library |
|---|---|---|
| Client type | OpenID Connect | OpenID Connect |
| Client ID | `course-app` | `library-app` |
| Client authentication | On | On |
| Standard flow | On | On |
| Implicit flow | Off | Off |
| Direct access grants | Off | Off |
| Service accounts | Off | Off |
| Root / Home URL | `http://localhost:8081` | `http://localhost:8082` |
| Valid redirect URIs | `http://localhost:8081/login/oauth2/code/keycloak` | `http://localhost:8082/login/oauth2/code/keycloak` |

Giữ các client scope `profile` và `email` sẵn có. Lấy secret riêng của mỗi client ở tab Credentials và cấp bằng biến môi trường mô tả trong README gốc.

Các trang server-rendered dùng redirect và gọi API từ backend nên không cần cấu hình CORS/Web Origins. Không mở wildcard `*` cho callback.

Logout hiện tại chỉ cục bộ; chưa cần post-logout redirect URI hoặc back-channel logout URL.

## 3. Audience mapper cho Course gọi Library

Đây là bước bắt buộc để `GET /api/demo` chấp nhận access token của Course.

Trong client `course-app`:

1. Mở Client scopes, chọn dedicated scope của client (thường là `course-app-dedicated`).
2. Mở Mappers → Configure a new mapper → Audience.
3. Name: `library-api-audience`.
4. Included Client Audience: `library-app`.
5. Add to access token: On.
6. Add to ID token: Off.
7. Lưu.

Tên/vị trí nút có thể khác giữa các phiên bản Keycloak. Có thể tạo một client scope riêng chứa Audience mapper rồi gán làm Default scope cho Course để đạt cùng kết quả.

Dùng Client scopes → Evaluate, chọn user demo để kiểm tra access token có `aud` chứa `library-app`. ID token của Course vẫn có audience `course-app`.

Sau khi thêm/sửa mapper, đăng xuất Course rồi đăng nhập lại để nhận token mới.

Library API xác minh chữ ký RSA qua JWKS của realm, issuer, thời hạn, audience `library-app` và claim `typ=Bearer` của access token Keycloak. Token ID của Keycloak có loại khác, không được dùng thay access token. Demo này không bật lightweight access token; giữ token JWT đầy đủ với các claim chuẩn.

Việc dùng Library vừa làm OIDC client vừa làm resource server là giới hạn có chủ đích của demo hai app; hệ thống lớn có thể tách client/API audience riêng.

Tài liệu tham khảo: [Keycloak hardcoded audience](https://www.keycloak.org/docs/26.7.5/server_admin/index.html#_audience), [Spring Security JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## 4. Kiểm tra nhanh và xử lý lỗi

- Realm discovery: mở `http://localhost:8080/realms/abjet/.well-known/openid-configuration`.
- `invalid_redirect_uri`: kiểm tra port, callback và dùng thống nhất `localhost`, không trộn `127.0.0.1`.
- `invalid_client`: kiểm tra client ID, Client authentication và secret riêng.
- API 401: kiểm tra audience mapper, issuer, thời hạn và đăng nhập lại để nhận token mới.
- Không có tên/email: điền profile của user và giữ client scope profile/email.
- Sau logout bấm login không hỏi password: đây là logout cục bộ; phiên Keycloak vẫn còn.
- Để trình diễn từ đầu, dùng một cửa sổ ẩn danh mới cho cả hai app.
