# ABJet implementation plan

Approved scope: two independent Java 21 / Maven / Spring Boot applications, Keycloak realm abjet, Authentication, OIDC ID token claims, SSO, and Course-to-Library bearer-token API demo. No business database or course/book management.

Architecture: Course App (8081, course-app) is an OIDC client. Library App (8082, library-app) is an OIDC client for HTML and a stateless OAuth2 resource server for /api/**. Its API requires a signed, unexpired token with issuer http://localhost:8080/realms/abjet and audience library-app. Each application has its own session cookie. Logout is local and requires POST with CSRF.

Execution: implement inline in the existing workspace, preserving existing user changes; no commits or publishing.

- [x] Rename courseapp to course-app; preserve Java package and Maven wrapper.
- [x] Write security/HTML/API-caller checks before implementing new behavior.
- [x] Implement Course dashboard claims and server-side Library API call using the authorized-client manager (including token refresh), finite HTTP timeouts, and safe error messages. Never expose raw tokens.
- [x] Implement Library HTML and a separate stateless bearer-only filter chain for GET /api/demo. Test missing token, browser-session-only access, invalid signature, expired token, wrong issuer/audience, and a valid signed token.
- [x] Write Keycloak setup instructions including audience mapper, environment secrets, and four-topic demonstration script.
- [x] Run both Maven clean verify commands; report live-Keycloak verification separately from automated checks.

Review focus: distinct localhost cookie names; no session authentication on API; retain CSRF for HTML/logout; escaped token claims and API responses; absent profile claims; unavailable Library server; no real credentials in source.

Verification: both Maven clean verify commands succeeded on JDK 21.0.7. Course: 9 tests; Library: 10 tests; zero failures/errors/skips. Both executable JARs generated. Standard Mockito dynamic-agent warnings were emitted. Keycloak discovery at localhost:8080 was not reachable, so actual browser login/SSO and the local realm/client configuration remain unverified.

Review decision: Library is both an OIDC client and API in this two-app demo. Audience validation alone would also accept its ID token; a red test demonstrated this. Added Keycloak-specific typ=Bearer validation, with the full signed-token suite passing afterward.
