# ABJet

ABJet là demo Authentication, OAuth 2.0, ID Token và Single Sign-On (SSO), dùng Java 21, Maven, Spring Boot và Thymeleaf.

| Thành phần | URL | Vai trò |
|---|---|---|
| Keycloak | http://localhost:8080 | Identity Provider, realm `abjet` |
| Course App | http://localhost:8081 | OIDC client `course-app`, gọi API Library bằng access token |
| Library App | http://localhost:8082 | OIDC client `library-app`, OAuth2 Resource Server cho `/api/**` |

## Cấu trúc workspace

```text
ABJet/
├── README.md
├── .gitignore
├── keycloak/README.md
├── docs/
│   ├── implementation-plan.md
│   └── demo-guide.md
├── course-app/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd / .mvn/wrapper/
│   └── src/
│       ├── main/
│       │   ├── java/com/abjet/courseapp/
│       │   │   ├── CourseappApplication.java
│       │   │   ├── config/SecurityConfig.java
│       │   │   ├── controller/HomeController.java
│       │   │   ├── controller/LibraryDemoController.java
│       │   │   └── service/LibraryApiService.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── templates/{home,dashboard,api-result}.html
│       │       └── static/css/style.css
│       └── test/
│           ├── java/com/abjet/courseapp/
│           └── resources/application.yml
└── library-app/
    ├── pom.xml
    ├── mvnw / mvnw.cmd / .mvn/wrapper/
    └── src/
        ├── main/
        │   ├── java/com/abjet/libraryapp/
        │   │   ├── LibraryAppApplication.java
        │   │   ├── config/SecurityConfig.java
        │   │   ├── config/AccessTokenConfig.java
        │   │   ├── controller/HomeController.java
        │   │   └── controller/DemoApiController.java
        │   └── resources/
        │       ├── application.yml
        │       ├── templates/{home,dashboard}.html
        │       └── static/css/style.css
        └── test/
            ├── java/com/abjet/libraryapp/
            └── resources/application.yml
```

- `controller/`: trang HTML, thông tin danh tính, thao tác gọi API hoặc JSON demo.
- `config/`: bảo vệ route, OIDC login, logout cục bộ và xác minh access token.
- `service/` trong Course: HTTP client gọi API bằng token do Spring Security quản lý; có làm mới token khi cần và timeout.
- `templates/`: HTML Thymeleaf, tự escape dữ liệu và tạo CSRF token cho form.
- `static/css/`: CSS responsive đơn giản. Không cần JavaScript.
- `application.yml`: cổng, cookie session riêng, client OIDC và issuer.
- `pom.xml`: Java 21, dependency và plugin tạo executable JAR.
- Chưa cần model/service nghiệp vụ, database hoặc module dùng chung.

## Chạy local

1. Cài JDK 21 và cấu hình `JAVA_HOME`.
2. Khởi động Keycloak tại cổng 8080 và làm theo [hướng dẫn realm/client/audience](keycloak/README.md).
3. Mở hai terminal PowerShell từ workspace.

Terminal Course:

```powershell
cd course-app
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:COURSE_APP_CLIENT_SECRET = '<secret course-app lấy từ Keycloak>'
.\mvnw.cmd spring-boot:run
```

Terminal Library:

```powershell
cd library-app
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:LIBRARY_APP_CLIENT_SECRET = '<secret library-app lấy từ Keycloak>'
.\mvnw.cmd spring-boot:run
```

Các giá trị trong dấu `<...>` là mẫu, phải thay tại máy local. Đường dẫn JDK điều chỉnh theo máy.

Issuer mặc định là `http://localhost:8080/realms/abjet`; có thể ghi đè bằng `KEYCLOAK_ISSUER_URI` trong cả hai terminal. Course vẫn hỗ trợ biến cũ `KEYCLOAK_CLIENT_SECRET` để chuyển tiếp từ cấu hình trước, ưu tiên biến mới `COURSE_APP_CLIENT_SECRET`.

Không commit mật khẩu, secret, token hoặc realm export chứa credentials. Không có secret dùng chung giữa hai client. Ứng dụng không tự nạp file `.env`. Keycloak phải sẵn sàng trước khi khởi động app vì OIDC client dùng discovery.

## Các route

| Route | Course | Library |
|---|---|---|
| `GET /` | Trang chủ công khai | Trang chủ công khai |
| `GET /oauth2/authorization/keycloak` | Bắt đầu OIDC login | Bắt đầu OIDC login |
| `GET /login/oauth2/code/keycloak` | Callback Spring Security xử lý | Callback Spring Security xử lý |
| `GET /dashboard` | Danh tính, ID token claims và nút gọi API | Danh tính và ID token claims |
| `POST /library-demo` | Gọi API từ backend, yêu cầu login + CSRF | Không có |
| `GET /api/demo` | Không có | JSON demo, yêu cầu bearer access token |
| `POST /logout` | Logout cục bộ + CSRF | Logout cục bộ + CSRF |

## Build và kiểm thử

Chạy trong mỗi thư mục app:

```powershell
.\mvnw.cmd clean verify
```

Linux/macOS dùng `sh ./mvnw clean verify`. Maven Wrapper tải Maven/dependency nếu chưa có cache.

JAR: `course-app/target/course-app-1.0-SNAPSHOT.jar` và `library-app/target/library-app-1.0-SNAPSHOT.jar`.

Test không cần Keycloak thật: OIDC principal mô phỏng cho HTML; RSA/JWKS local để kiểm thử xác minh JWT; HTTP server local để kiểm tra header bearer và xử lý lỗi gọi API. Test này không thay thế kiểm tra đăng nhập/SSO trên trình duyệt với Keycloak thật.

Xem [kịch bản trình diễn bốn nội dung](docs/demo-guide.md).

## Phạm vi

Đã có mã nguồn cho đăng nhập OIDC, ID token claims, liên kết SSO, logout cục bộ và API bearer-token demo. Việc chạy SSO thực tế phụ thuộc cấu hình Keycloak local.

Không có quản lý khóa học, quản lý sách, database nghiệp vụ, role nghiệp vụ, global logout hoặc cơ chế back-channel logout.

## Setup và chạy từng bước trên Windows

### 1. Kiểm tra JDK 21

Cài JDK 21 và thay đường dẫn bên dưới bằng thư mục JDK thực tế (không trỏ tới `bin`). Chạy trong từng terminal mới nếu chưa đặt biến môi trường Windows:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
& "$env:JAVA_HOME\bin\javac.exe" -version
```

Cả hai cần hiển thị phiên bản 21. Không cần cài Maven riêng: repo có Maven Wrapper. Lần build đầu cần Internet để tải Maven và dependency. Hai app không cần Node.js hay database nghiệp vụ.

### 2. Setup Keycloak trước khi chạy app

Khởi động Keycloak local theo cách cài đặt của bạn tại `http://localhost:8080`, rồi làm theo [hướng dẫn Keycloak](keycloak/README.md):

1. Tạo realm `abjet` và user demo có mật khẩu.
2. Tạo hai client `course-app` và `library-app`, bật Client authentication và Standard flow.
3. Cấu hình callback lần lượt là `http://localhost:8081/login/oauth2/code/keycloak` và `http://localhost:8082/login/oauth2/code/keycloak`.
4. Lấy secret riêng của mỗi client trong tab Credentials.
5. Thêm Audience mapper cho Course để access token có audience `library-app`.

Kiểm tra realm sẵn sàng:

```powershell
Invoke-RestMethod 'http://localhost:8080/realms/abjet/.well-known/openid-configuration' |
    Select-Object issuer
```

Issuer cần là `http://localhost:8080/realms/abjet`. Repo không tự tạo realm, client hay user.

### 3. Chạy Library trong terminal thứ nhất

```powershell
Set-Location 'C:\Users\vanan\OneDrive\Desktop\ABJet\library-app'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:LIBRARY_APP_CLIENT_SECRET = '<secret của client library-app>'
.\mvnw.cmd spring-boot:run
```

Thay secret và đường dẫn JDK bằng giá trị thật. Giữ terminal chạy, mở `http://localhost:8082` sau khi log báo ứng dụng khởi động thành công.

### 4. Chạy Course trong terminal thứ hai

```powershell
Set-Location 'C:\Users\vanan\OneDrive\Desktop\ABJet\course-app'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:COURSE_APP_CLIENT_SECRET = '<secret của client course-app>'
.\mvnw.cmd spring-boot:run
```

Thay secret và đường dẫn JDK bằng giá trị thật. Mở `http://localhost:8081` sau khi ứng dụng khởi động. Thư mục hiện tại tên là `course-app`, không phải `courseapp`. Nếu chuyển repo sang nơi khác, đổi đường dẫn `Set-Location` tương ứng.

Các biến `$env:...` chỉ áp dụng trong terminal hiện tại. Không dùng chung secret giữa hai client; app không tự đọc file `.env`.

### 5. Kiểm tra chương trình

1. Mở Course, bấm đăng nhập bằng tài khoản demo của realm `abjet`.
2. Vào dashboard, kiểm tra tên người dùng và ID token claims.
3. Gọi Library API từ Course; Library cần đang chạy.
4. Mở Library trong cùng trình duyệt và bấm đăng nhập để kiểm tra SSO.
5. Dừng từng app bằng `Ctrl+C` ở terminal tương ứng.

Logout chỉ xóa phiên của app, phiên Keycloak vẫn còn. Dùng cửa sổ ẩn danh mới để demo lại từ đầu.

### 6. Build và chạy JAR

Chạy trong từng thư mục app, với JDK 21 đã cấu hình:

```powershell
.\mvnw.cmd -v
.\mvnw.cmd clean verify
```

Lệnh đầu giúp xác nhận Java mà Maven dùng. Workspace gốc không có `pom.xml` hay Maven Wrapper chung, nên build từng app riêng.

Sau khi build thành công, có thể chạy JAR thay cho `spring-boot:run`. Dừng app cũ trước để tránh trùng cổng; giữ nguyên biến secret và issuer trong từng terminal.

Trong `course-app`:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\course-app-1.0-SNAPSHOT.jar
```

Trong `library-app`, ở terminal khác:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\library-app-1.0-SNAPSHOT.jar
```

### Xử lý lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Không tìm thấy `mvnw.cmd` | Chuyển vào `course-app` hoặc `library-app`, dùng `.\mvnw.cmd`. |
| `JAVA_HOME` sai / `release version 21 not supported` | Trỏ tới JDK 21, kiểm tra lại bằng `.\mvnw.cmd -v`. |
| Không tải được Maven/dependency | Kiểm tra Internet, proxy và repository Maven; xem lỗi tải cụ thể trong log. |
| Không resolve được issuer / connection refused tới 8080 | Khởi động Keycloak, tạo realm `abjet`, kiểm tra endpoint discovery ở trên. |
| Thiếu `LIBRARY_APP_CLIENT_SECRET` / `invalid_client` | Đặt biến trong đúng terminal, kiểm tra secret riêng và Client authentication. |
| `invalid_redirect_uri` | Kiểm tra callback và cổng; dùng thống nhất `localhost`, không trộn `127.0.0.1`. |
| Cổng 8081 hoặc 8082 đã được sử dụng | Dừng phiên app cũ bằng `Ctrl+C` trước khi chạy lại. |
| Course không gọi được Library | Kiểm tra Library đang chạy ở `http://localhost:8082`. |
| API trả 401 | Kiểm tra audience mapper `library-app`, issuer và hạn token; đăng nhập lại Course sau khi sửa mapper. |

Build/test dùng cấu hình mô phỏng; chạy ứng dụng cần Keycloak thật và secret hợp lệ. Nếu thay issuer mặc định, đặt `$env:KEYCLOAK_ISSUER_URI` trong cả hai terminal trước khi chạy app.