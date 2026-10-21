# Authentication and Application Security: Educational Theory Guide

## Table of Contents
1. [Introduction and Pedagogical Objectives](#1-introduction-and-pedagogical-objectives)
2. [Foundations: Authentication vs. Authorization](#2-foundations-authentication-vs-authorization)
3. [HTTP and REST API Security Fundamentals](#3-http-and-rest-api-security-fundamentals)
4. [Java and Spring Boot Architectural Principles](#4-java-and-spring-boot-architectural-principles)
5. [Password Security and Cryptographic Hashing](#5-password-security-and-cryptographic-hashing)
6. [JSON Web Tokens (JWT) and Asymmetric Cryptography (RS256)](#6-json-web-tokens-jwt-and-asymmetric-cryptography-rs256)
7. [Token Lifecycles: Access Tokens, Refresh Tokens, and Session Governance](#7-token-lifecycles-access-tokens-refresh-tokens-and-session-governance)
8. [Browser Security, Cookies, and Cross-Site Mitigations](#8-browser-security-cookies-and-cross-site-mitigations)
9. [Spring Security Architecture and Filter Chain Internals](#9-spring-security-architecture-and-filter-chain-internals)
10. [Password Reset, OTP Mechanics, and Account State Machines](#10-password-reset-otp-mechanics-and-account-state-machines)
11. [Database Concurrency, Row Locking, and Persistence Theory](#11-database-concurrency-row-locking-and-persistence-theory)
12. [Testing Theory for High-Assurance Security Systems](#12-testing-theory-for-high-assurance-security-systems)
13. [End-to-End Application Workflow Walkthroughs](#13-end-to-end-application-workflow-walkthroughs)
14. [Security Risk Assessment, Limitations, and Hardening Recommendations](#14-security-risk-assessment-limitations-and-hardening-recommendations)
15. [Comprehensive Glossary and Progressive Learning Roadmap](#15-comprehensive-glossary-and-progressive-learning-roadmap)

---

## 1. Introduction and Pedagogical Objectives

### 1.1 Purpose of this Guide
Software security is rarely about memorizing syntax; it is about understanding **threat models, trust boundaries, cryptographic invariants, and failure modes**. 

This guide is an educational textbook and companion to the **Visitor Management System (VMS) Auth/Security Application**. It is designed to teach computer science students, junior engineers, and teammates not only *what* code exists in this repository, but *why* modern software engineers structure security systems this way.

### 1.2 Structure and Reading Philosophy
Every chapter progresses through four clear dimensions:
1. **General Security Theory:** The mathematical or architectural concept independent of any framework.
2. **Spring Security & Java Mechanics:** How the Java ecosystem and Spring framework model this concept.
3. **Actual Application Implementation:** Exactly how the VMS Auth service implements it in code.
4. **Distinction from Future Best Practices:** Honest analysis of existing code versus theoretical ideals.

---

## 2. Foundations: Authentication vs. Authorization

### 2.1 The Conceptual Difference
At the foundation of computer security lie two distinct questions:
- **Authentication (AuthN):** *"Who are you?"* The process of verifying that an entity claiming an identity actually possesses the credentials proving that identity.
- **Authorization (AuthZ):** *"What are you permitted to do?"* The process of evaluating an authenticated entity's permissions against the access policy of a requested resource or operation.

```
Incoming Request
      │
      ▼
[ Authentication Barrier ] ──(Invalid Credentials)──► 401 Unauthorized
      │ (Identity Proven: "I am user USR-001")
      ▼
[ Authorization Barrier ]  ──(Insufficient Privileges)──► 403 Forbidden
      │ (Permission Granted: Has Role ADMIN)
      ▼
[ Resource / Controller Execution ]
```

### 2.2 Terminology: Identities, Credentials, Principals, Authorities, and Roles
- **Identity:** The unique identifier claimed by a user (e.g., an email address `admin@adminvisitor.com` or user ID `USR-001`).
- **Credential:** The secret piece of evidence presented to prove that identity (e.g., a raw password `Admin@123` or an RSA cryptographic signature).
- **Principal:** The actively verified identity representation within the runtime environment. In Spring Security, the principal is held inside the `Authentication` object. In this project's [JwtAuthenticationFilter.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/security/JwtAuthenticationFilter.java), the principal is set as the user ID `String` (e.g., `"USR-001"`).
- **Authority / Granted Authority:** A granular permission string (e.g., `ROLE_ADMIN`, `ROLE_FRONT_DESK`, `ROLE_INITIAL_PASSWORD_CHANGE_REQUIRED`).
- **Role:** A high-level grouping of authorities. In Spring Security, roles are simply granted authorities that follow the `ROLE_` naming convention.

### 2.3 User Account vs. Authenticated Session
A **User Account** is a static, persistent entity stored in a database ([User.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/entity/User.java)) containing an ID, an associated employee, a password hash, a role, and an account status (`ACTIVE` vs. `INACTIVE`).

An **Authenticated Session** is an ephemeral, time-bounded relationship established after authentication. In this application, a session is represented by:
1. A short-lived, signed JWT access token (15-minute validity).
2. A database-backed refresh token row ([RefreshToken.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/entity/RefreshToken.java)) with expiration and revocation timestamps.

### 2.4 Why APIs Must Never Trust Client-Supplied Identity
A fundamental law of web security: **Clients (browsers, mobile apps, curl scripts) run in hostile environments outside the server's control.** 
If an API accepted a JSON payload like `{"userId": "USR-001", "role": "ADMIN"}` without verifying cryptographic proofs, any malicious actor could spoof any identity. The server must verify every request using cryptographically unforgeable tokens (like RS256 JWTs) or secure server-controlled session cookies.

---

## 3. HTTP and REST API Security Fundamentals

### 3.1 HTTP as a Stateless Protocol
HTTP is inherently stateless: each request from a browser to a server is treated as completely independent. The server retains no memory of previous requests unless explicit session tracking (cookies, authorization headers) is used.

In a modern REST architecture:
- State should not reside in server memory (HTTP Session replication across nodes is complex and error-prone).
- State is either carried inside signed tokens presented on each request, or verified through high-speed database checks.

### 3.2 Request/Response Semantics and Status Codes
HTTP status codes carry precise semantic meaning in security workflows:
- `200 OK`: Request succeeded. Used for login, password change, token refresh.
- `201 Created`: A new persistent resource was created. Used by `POST /api/auth/users`.
- `400 Bad Request`: Client sent malformed JSON or violated input validation constraints (e.g. invalid email format, weak password).
- `401 Unauthorized`: Specifically means **Unauthenticated**. Identity is missing, invalid, or expired. Returned on bad credentials, expired tokens, or unauthenticated access to protected routes.
- `403 Forbidden`: Identity is known and authenticated, but **lacks required permissions** (e.g., a `FRONT_DESK` user attempting to provision accounts via `POST /api/auth/users`).
- `404 Not Found`: Target entity does not exist (e.g., attempting to create a user for non-existent employee `EMP-999`).
- `409 Conflict`: Resource state conflict (e.g., employee already has a user account).
- `429 Too Many Requests`: Client exceeded rate limits (e.g., requesting too many OTPs or entering 5 wrong codes).
- `500 Internal Server Error`: Unhandled server-side fault (e.g., SMTP server unreachable).

### 3.3 HTTP Headers and Browser Cookies
- **`Authorization: Bearer <token>`:** The standard RFC 6750 header for API-to-API communication.
- **`Set-Cookie` and `Cookie`:** The browser storage mechanism. A server sends `Set-Cookie` in a response; the browser automatically attaches matching `Cookie` headers to subsequent requests.
- **Why Cookies over Headers for Web Apps?** Storing tokens in browser `localStorage` leaves them completely vulnerable to Cross-Site Scripting (XSS) attacks. If a malicious script runs, it can read `localStorage.getItem('token')`. By contrast, an `HttpOnly` cookie is **inaccessible to JavaScript**, neutralizing token theft via XSS.

---

## 4. Java and Spring Boot Architectural Principles

### 4.1 Layered Architecture and Separation of Concerns
To ensure code maintainability, testability, and security auditability, responsibilities are cleanly isolated:
1. **Controllers ([AuthController.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/controller/AuthController.java)):** Only handle HTTP translation, request validation, cookie headers, and status code selection. Never contain database queries or business rules.
2. **Services ([AuthServiceImpl.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/service/Impl/AuthServiceImpl.java)):** Own business workflows, password policy validation, session counts, token creation orchestration, and transactional boundaries.
3. **Repositories ([UserRepository.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/repository/UserRepository.java)):** Own database access, query construction, and locking semantics.

### 4.2 Inversion of Control (IoC) and Dependency Injection (DI)
Rather than a class constructing its own dependencies with `new`:
```java
// Anti-pattern: Hard-coded dependency, impossible to mock or substitute
public class AuthService {
    private UserRepository repo = new UserRepository(); 
}
```
Spring Boot manages components as **Beans** inside an Application Context and injects them via constructor injection:
```java
// Clean Architecture: Injected interface, easily unit-tested with mocks
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
}
```

### 4.3 DTOs vs. Persistence Entities: The Mass-Assignment Threat
A common security vulnerability in poorly designed applications is **Mass Assignment / Over-Posting**. If an endpoint binds HTTP JSON directly into a JPA Entity (`@RequestBody User user`), an attacker could send:
```json
{
  "email": "attacker@evil.com",
  "role": "ADMIN",
  "mustChangePassword": false,
  "status": "ACTIVE"
}
```
If the entity is saved directly, the attacker escalates their privileges!
In this project:
- Clients submit immutable **Request DTOs** ([CreateUserRequest.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/dto/requestdto/CreateUserRequest.java)) containing only fields clients are allowed to supply (`employeeId`, `role`).
- Controllers return **Response DTOs** ([UserResponse.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/dto/responsedto/UserResponse.java)) mapped via MapStruct ([UserMapper.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/mapper/UserMapper.java)), ensuring internal entity fields (like `passwordHash` or sequence details) are never leaked.

---

## 5. Password Security and Cryptographic Hashing

### 5.1 Why Plaintext Passwords are a Catastrophe
If passwords were saved in plaintext, any database breach, backup exposure, SQL injection vulnerability, or internal administrator misuse would instantly compromise every user's credentials across all systems where they reuse passwords.

### 5.2 Encryption vs. One-Way Cryptographic Hashing
- **Encryption (Symmetric/Asymmetric):** A **two-way** mathematical process. Data is scrambled with a key and can be decrypted back to plaintext with the appropriate key. Passwords must **never** be encrypted, because any party with access to the key can recover all passwords.
- **Hashing:** A **one-way mathematical trapdoor function**. It converts input data into a fixed-length string such that computing the hash from the password is fast, but mathematically impossible to reverse (pre-image resistance).

```
Password ("MySecret123") ──[One-Way Hash Function]──► "$2a$12$e8Y..."
"$2a$12$e8Y..." ──[Cannot be reversed]──► ???
```

### 5.3 Rainbow Tables, Salts, and Work Factors
1. **Rainbow Tables:** Precomputed lookup tables of hashes for billions of common passwords. If two users have the same password, their hashes would be identical.
2. **Cryptographic Salt:** A unique, cryptographically random string generated for every individual user and prepended to the password before hashing. Even if two users share the password `"Password123!"`, their salts ensure their final hashes are completely distinct, neutralizing rainbow tables.
3. **Adaptive Work Factor (Cost Factor):** Moore's Law and GPUs allow attackers to guess billions of hashes per second using fast algorithms like MD5 or SHA-256. Modern password algorithms (BCrypt, Argon2, PBKDF2) are **intentionally slow**. The work factor determines how many computational rounds are executed ($2^{\text{cost}}$ rounds).

### 5.4 BCrypt in the Auth Service
In [SecurityConfig.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/config/SecurityConfig.java):
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```
A BCrypt hash string has three distinct segments:
```
$2a$12$e8YK2nU...v7zO...
├── 2a: BCrypt algorithm identifier
├── 12: Work factor (cost = 12, meaning 2^12 = 4,096 iterations)
├── First 22 chars: Embedded 128-bit salt
└── Remaining 31 chars: The computed hash
```
When `passwordEncoder.matches(rawPassword, storedHash)` is called during login, BCrypt extracts the salt and work factor directly from the stored hash, hashes the raw password with that exact salt and cost, and verifies if the resulting hash matches.

### 5.5 Empirical Latency: BCrypt Benchmark Analysis
From our baseline test suite execution of [BCryptBenchmarkTest.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/test/java/com/adminvisitor/auth/util/BCryptBenchmarkTest.java):
- Cost 10: ~128 ms
- Cost 11: ~218 ms
- Cost 12: ~413 ms
- Cost 13: ~911 ms

Selecting **Cost 12** provides an outstanding defense against offline brute-force attacks (~413 milliseconds per guess on server hardware) while keeping interactive login latency well under half a second.

### 5.6 Password Policy Enforcement
In [AuthServiceImpl.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/service/Impl/AuthServiceImpl.java), `validateNewPasswordPolicy()` strictly enforces:
- Minimum 8 characters.
- At least 1 uppercase letter (`A-Z`).
- At least 1 lowercase letter (`a-z`).
- At least 1 numeric digit (`0-9`).
- At least 1 special character (`!@#$%^&*()_+-=[]{}|;:,.<>?`).
- Cannot match current password.
- Must match confirmation password.

---

## 6. JSON Web Tokens (JWT) and Asymmetric Cryptography (RS256)

### 6.1 Anatomy of a JWT
A JSON Web Token (RFC 7519) is a compact, URL-safe string composed of three Base64URL-encoded parts separated by periods (`.`):
```
Header.Payload.Signature
```

```
┌─────────────────────────┐
│         Header          │ {"alg": "RS256", "typ": "JWT"}
└─────────────────────────┘
             │ (Base64URL)
             ▼
eyJhbGciOiJSUzI1NiJ9 .
┌─────────────────────────┐
│         Payload         │ {"sub": "USR-001", "role": "ADMIN", "state": "AUTHENTICATED", "exp": 1729512345}
└─────────────────────────┘
             │ (Base64URL)
             ▼
eyJzdWIiOiJVU1ItMDAxIn0 .
┌─────────────────────────┐
│        Signature        │ RSA-SHA256( Base64(Header) + "." + Base64(Payload), PrivateKey )
└─────────────────────────┘
```

### 6.2 Signed vs. Encrypted: The Visibility Invariant
**Crucial Principle:** A signed JWT (JWS) is **NOT encrypted**. The payload is merely Base64URL-encoded. Anyone who intercepts the token can decode and read every claim.
- **What it guarantees:** **Integrity and Authenticity**. The recipient can mathematically prove that the payload has not been modified since the issuer signed it, and that only the holder of the private key could have created it.
- **What it does NOT guarantee:** Confidentiality. Never store passwords, social security numbers, or sensitive private data inside JWT claims!

### 6.3 Symmetric (HS256) vs. Asymmetric (RS256)
- **HS256 (HMAC with SHA-256):** Uses a single shared secret key for both signing and verifying.
  - *Risk:* If the API Gateway or another microservice needs to verify the token, they must know the shared secret. If any service is compromised, attackers can forge valid tokens for the entire system!
- **RS256 (RSA Signature with SHA-256):** An asymmetric key pair:
  - **Private Key (Secret):** Kept strictly on the Auth server. Used exclusively to sign newly issued tokens ([JwtService.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/security/JwtService.java)).
  - **Public Key (Public):** Freely distributed to API Gateways, Visitor microservices, and frontends. Used solely to verify signatures. Even if a downstream microservice is breached, an attacker cannot forge tokens because they lack the private key!

### 6.4 Key Loading and Parsing in `JwtService`
In [JwtService.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/security/JwtService.java):
- `loadPrivateKey`: Reads standard PKCS#8 PEM strings (`-----BEGIN PRIVATE KEY-----`), strips header/footer and whitespace, Base64-decodes the bytes, and builds an RSA `PrivateKey` via `KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes))`.
- `loadPublicKey`: Reads X.509 PEM strings (`-----BEGIN PUBLIC KEY-----`) and builds an RSA `PublicKey` via `generatePublic(new X509EncodedKeySpec(keyBytes))`.

### 6.5 The State Claim: Enforcing First-Time Password Changes
In addition to user identity and role, `JwtService` issues tokens with a custom `state` claim:
1. `STATE_AUTHENTICATED`: Issued to normal users who have already set their password.
2. `STATE_INITIAL_PASSWORD_CHANGE_REQUIRED`: Issued when `user.isMustChangePassword() == true`.
When `JwtAuthenticationFilter` inspects a restricted token, it maps it exclusively to:
```java
new SimpleGrantedAuthority("ROLE_INITIAL_PASSWORD_CHANGE_REQUIRED")
```
Because [SecurityConfig.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/config/SecurityConfig.java) only permits this authority on `/api/auth/change-password`, a user with a restricted token is completely locked out of all other business APIs until they change their temporary password!

---

## 7. Token Lifecycles: Access Tokens, Refresh Tokens, and Session Governance

### 7.1 The Access Token Dilemma
A purely stateless access token has an inherent vulnerability: **It cannot be revoked immediately without consulting a database**. If an access token were valid for 24 hours and a user was fired or their laptop was stolen, the token would remain valid for the full 24 hours.

### 7.2 The Dual-Token Pattern
To resolve this dilemma, production systems use a dual-token architecture:
- **Access Token:** Very short-lived (15 minutes). Sent with every API request. If stolen, the attacker's window of opportunity is limited to minutes.
- **Refresh Token:** Long-lived (12 hours). Stored in the database and exchanged periodically for a new access token.

```
Client                             Auth Service                           Database
  │                                     │                                    │
  ├─── POST /api/auth/refresh ─────────►│                                    │
  │    (with Cookie: REFRESH_TOKEN)     ├─── SELECT ... FOR UPDATE ─────────►│
  │                                     │    (Lock token row)                │
  │                                     │◄── Return RefreshToken entity ─────┤
  │                                     │                                    │
  │                                     ├─── Verify Active, Unrevoked ───────┤
  │                                     ├─── Generate New Raw Token ─────────┤
  │                                     ├─── Update Token Hash & Expiration ─┤
  │                                     ├─── UPDATE vms_refresh_token ──────►│
  │                                     │                                    │
  │◄── Set-Cookie: New ACCESS_TOKEN ────┤                                    │
  │◄── Set-Cookie: New REFRESH_TOKEN ───┤                                    │
```

### 7.3 Refresh Token Rotation (RTR)
In older systems, refresh tokens were static and reusable until expiration. This meant if a refresh token was stolen, the attacker could refresh indefinitely.
In this application, **Refresh Token Rotation (RTR)** is strictly implemented:
1. Every time a refresh token is used via `POST /api/auth/refresh`, that specific refresh token string is consumed and cannot be reused.
2. A new opaque random token string is generated via `TokenUtils.generateOpaqueToken()`.
3. The database record is updated in-place with the SHA-256 hash of the new token and extended expiration.
4. The client receives the new token in the `Set-Cookie` header.

### 7.4 Storing Hashes, Not Plaintext Tokens
Just like passwords, **raw refresh tokens are never stored in the database**.
In [TokenUtils.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/util/TokenUtils.java), `TokenUtils.hashToken(rawToken)` calculates a SHA-256 digest. The database table `vms_refresh_token` only stores `token_hash`. If the database is compromised, the attacker cannot use the hashed tokens to impersonate users.

### 7.5 Concurrency and Row-Level Locking (`PESSIMISTIC_WRITE`)
What happens if a client's frontend fires two simultaneous refresh requests (e.g. from two tabs or parallel AJAX calls)?
Without locking, both threads would read the old token, both would consider it valid, and both would try to rotate it—causing race conditions or duplicate sessions.
In [RefreshTokenRepository.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/repository/RefreshTokenRepository.java):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
```
This issues a SQL `SELECT ... FOR UPDATE`, forcing the database engine to acquire an exclusive row lock. The second concurrent thread must block until the first transaction commits, guaranteeing serial execution.

---

## 8. Browser Security, Cookies, and Cross-Site Mitigations

### 8.1 The Cookie Security Triad: `HttpOnly`, `Secure`, and `SameSite`
When transmitting tokens to web browsers, cookies provide critical security controls that raw headers cannot:

| Attribute | Configured in Project | Security Guarantee & Threat Mitigated |
| :--- | :---: | :--- |
| **`HttpOnly`** | **`true`** | **Mitigates XSS (Cross-Site Scripting):** Prevents client-side scripts from accessing `document.cookie`. Even if an attacker injects arbitrary JavaScript, they cannot extract the session tokens. |
| **`Secure`** | `false` (dev) / `true` (prod) | **Mitigates MitM (Man-in-the-Middle):** Tells the browser to only transmit the cookie over encrypted TLS/HTTPS connections. Tokens are never leaked over cleartext HTTP. |
| **`SameSite=Lax`** | **`"Lax"`** | **Mitigates CSRF (Cross-Site Request Forgery):** Prevents the browser from sending the cookie on cross-site subrequests (e.g. images, iframes, malicious cross-site POST forms). Sent only when navigating directly to the origin site. |
| **`Path`** | `/api` vs `/api/auth` | **Principle of Least Privilege:** `ACCESS_TOKEN` is accessible across `/api`. `REFRESH_TOKEN` is strictly scoped to `/api/auth`, preventing non-auth endpoints from even receiving the refresh cookie! |

### 8.2 CSRF Protection for Single-Page Applications
In [SecurityConfig.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/config/SecurityConfig.java):
```java
http.csrf(csrf -> csrf.spa())
```
Spring Security's Single Page Application CSRF helper configures cookie-based CSRF repository handling. The frontend initiates a handshake by calling [CsrfController.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/controller/CsrfController.java) (`GET /api/auth/csrf`), which forces CSRF token evaluation and returns the expected token header.

---

## 9. Spring Security Architecture and Filter Chain Internals

### 9.1 The Filter Chain Pipeline
Spring Security does not execute inside Spring MVC controllers; it sits at the Servlet container boundary as a chain of standard Jakarta Servlet `Filter`s:

```
Incoming Request
       │
       ▼
[ SecurityContextHolderFilter ]       (Initializes empty SecurityContext)
       │
       ▼
[ CorsFilter ]                         (Applies allowed origins & headers)
       │
       ▼
[ CsrfFilter ]                         (Validates CSRF tokens on state-changing methods)
       │
       ▼
[ JwtAuthenticationFilter ]           (Custom Filter: parses cookie, verifies RS256, populates context)
       │
       ▼
[ UsernamePasswordAuthenticationFilter ] (Standard form login filter - skipped)
       │
       ▼
[ AuthorizationFilter ]                (Checks request matchers vs SecurityContext authorities)
       │
       ├─── Denied ──► CustomAuthenticationEntryPoint (401 JSON)
       └─── Allowed ──► DispatcherServlet ──► AuthController
```

### 9.2 Inside `JwtAuthenticationFilter`
[JwtAuthenticationFilter.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/security/JwtAuthenticationFilter.java) extends `OncePerRequestFilter`, guaranteeing exactly one execution per request dispatch.
1. Extracts `ACCESS_TOKEN` from cookies.
2. If token is absent: passes request forward (if the endpoint is public, it succeeds; if protected, `AuthorizationFilter` will reject it).
3. Validates token signature and expiration locally using the RSA public key.
4. Performs a DB sanity check:
   ```java
   if (!userRepository.existsByIdAndStatus(userId, "ACTIVE"))
   ```
   *Why this matters:* If an admin deactivates an employee (`status = INACTIVE`), their 15-minute access token is rejected immediately on the next request, eliminating the standard stateless JWT delay!
5. Populates `SecurityContextHolder.getContext().setAuthentication(authToken)`.

---

## 10. Password Reset, OTP Mechanics, and Account State Machines

### 10.1 The Vulnerability of Password Resets
Password reset flows are the single most targeted component of authentication systems. If an attacker can guess a reset code or bypass verification, they can take over any account without knowing the password.

### 10.2 Defense-in-Depth OTP Design in `AuthServiceImpl`
The password reset flow implements rigorous multi-layer protections:
1. **Cryptographic Randomness:** The OTP is generated using `SecureRandom.nextInt(1_000_000)` formatted as a 6-digit numeric string (`000000` to `999999`).
2. **Short Time-to-Live (TTL):** The OTP expires strictly in **5 minutes** (`now.plusMinutes(5)`).
3. **Cooldown Window:** A user cannot spam OTP requests. If `latestToken.getLastSentAt().plusSeconds(60).isAfter(now)`, the request is rejected with `429 Too Many Requests`.
4. **Volume Throttling:** If a user makes 3 OTP requests within 15 minutes (`countByUserIdAndCreatedAtAfter(...) >= 3`), further requests are locked for 15 minutes.
5. **Brute-Force Lockout:** An attacker attempting to guess the 6-digit code has at most **5 attempts** (`otpAttemptCount >= 5`). On the 5th failed attempt, the token is permanently invalidated (`invalidated = true`) and `OtpRateLimitException` is thrown.
6. **Two-Phase Reset Handshake:** Successfully guessing the OTP does *not* directly reset the password. Instead, OTP verification yields a one-time, 32-byte cryptographically random `resetToken` valid for only 10 minutes. This reset token must be presented in `POST /api/auth/reset-password`.
7. **Session Invalidation on Reset:** When a password is reset, `refreshTokenRepository.revokeAllByUserId(userId, now)` is called, immediately terminating all active sessions across all devices!

---

## 11. Database Concurrency, Row Locking, and Persistence Theory

### 11.1 Concurrency Anomalies in Authentication
In high-throughput environments, multiple requests arrive concurrently:
- Two simultaneous logins from the same user.
- Two simultaneous refreshes from parallel browser tabs.
- Multiple threads generating user IDs concurrently.

### 11.2 Pessimistic Locking (`SELECT ... FOR UPDATE`)
Spring Data JPA supports two locking mechanisms:
- **Optimistic Locking:** Uses a `@Version` column. If a collision occurs, an `OptimisticLockException` is thrown, rolling back the transaction. (Good for low collision rates, poor for high-contention sequential counters).
- **Pessimistic Write Locking (`LockModeType.PESSIMISTIC_WRITE`):** The database engine locks the row at the SQL level (`SELECT ... FOR UPDATE`). Any competing transaction attempting to read or update that row must wait in a queue.

### 11.3 Sequence Counter Implementation
In [IdSequenceRepository.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/repository/IdSequenceRepository.java) and [IdGeneratorServiceImpl.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/service/Impl/IdGeneratorServiceImpl.java):
```java
@Transactional
public String generateId(String sequenceName, String prefix) {
    IdSequence sequence = idSequenceRepository
            .findBySequenceNameForUpdate(sequenceName) // Lock row
            .orElseThrow(...);
            
    long currentValue = sequence.getNextValue();
    sequence.setNextValue(currentValue + 1);
    
    return String.format("%s%03d", prefix, currentValue);
}
```
Because the row is locked with `PESSIMISTIC_WRITE`, parallel user creation requests will never receive duplicate IDs like `USR-001`.

---

## 12. Testing Theory for High-Assurance Security Systems

### 12.1 The Pyramid of Testing in Security
- **Unit Tests with Mockito:** Tests individual business rules in isolation. Guarantees that edge cases (null inputs, expired timestamps, bad passwords) trigger expected exceptions without requiring database setup.
- **Controller Slice Tests (`MockMvc`):** Tests serialization, HTTP status codes, validation filters, and cookie headers.
- **Integration Tests (`@SpringBootTest`):** Bootstraps the full Spring context, real JPA repositories, in-memory H2 database, and filter chain to verify end-to-end integration.

### 12.2 Why Passing Tests Do Not Prove Security
A test only proves that the scenarios written in the test behave as expected. A test suite can pass 100% while the application remains vulnerable if:
- Security headers are not asserted.
- Tests mock away critical security checks (e.g. mocking `jwtService.isTokenValid` to always return true).
- Race conditions under concurrent load are not simulated.

### 12.3 High-Quality Mocking Principles Applied
During our audit:
- Never enable global Mockito leniency. Strict stubbing ensures unused mocks are flagged as errors.
- Never mock away the behavior being evaluated.
- Ensure row-lock queries (`findByIdForUpdate`, `findByTokenHashForUpdate`) are explicitly verified.

---

## 13. End-to-End Application Workflow Walkthroughs

### Walkthrough 1: Successful Login (Normal User)
1. **Client Action:** `POST /api/auth/login` with `{"email": "admin@adminvisitor.com", "password": "Admin@123"}`.
2. **Filter Chain:** `JwtAuthenticationFilter` finds no `ACCESS_TOKEN` cookie. Continues to `SecurityConfig`, where `/api/auth/login` is `permitAll()`.
3. **Aspect:** `ApiLoggingAspect` captures start time.
4. **Controller:** `AuthController.login` receives validated `LoginRequest`. Calls `authService.login(request)`.
5. **Authentication:** `AuthServiceImpl` invokes `authenticationManager.authenticate(...)`. Spring Security's `DaoAuthenticationProvider` calls `CustomUserDetailsService.loadUserByUsername(...)`, queries `userRepository.findByEmployeeEmail`, and verifies BCrypt hash.
6. **Row Lock & Status Check:** `AuthServiceImpl` locks user row via `userRepository.findByIdForUpdate("USR001")`. Verifies `status == "ACTIVE"`.
7. **Session Limits:** Counts unrevoked refresh tokens. If $< 2$, proceeds.
8. **Token Issuance:** `jwtService.generateToken("USR001", "ADMIN")` creates RS256 JWT with `state=AUTHENTICATED` (15 min exp). `TokenUtils.generateOpaqueToken()` generates 32-byte refresh token; SHA-256 hash saved in `vms_refresh_token`.
9. **Cookie Construction:** `AuthController` builds `ACCESS_TOKEN` cookie (path `/api`, max-age 900s) and `REFRESH_TOKEN` cookie (path `/api/auth`, max-age 43200s).
10. **Response:** HTTP 200 OK with `AuthResponse` JSON and two `Set-Cookie` headers.

---

### Walkthrough 2: First-Time Login (Restricted Session)
1. **Context:** User created by admin has `must_change_password = true`.
2. **Authentication:** User logs in with temporary credentials generated by `PasswordGenerator`.
3. **Service Branch:** In `AuthServiceImpl.login`, `user.isMustChangePassword()` evaluates to `true`.
4. **Restricted Token Issuance:** Service invokes `jwtService.generateRestrictedToken("USR002", "FRONT_DESK")`. JWT payload receives `state: "INITIAL_PASSWORD_CHANGE_REQUIRED"`. **No refresh token is saved or issued.**
5. **Cookie Delivery:** `AuthController` sends `ACCESS_TOKEN` cookie with the restricted token, and issues an expired `REFRESH_TOKEN` cookie (`Max-Age=0`) to clear any stale session.
6. **Subsequent API Request:** If user calls `GET /api/visitors`, `JwtAuthenticationFilter` maps restricted token to `ROLE_INITIAL_PASSWORD_CHANGE_REQUIRED`. Spring Security's `AuthorizationFilter` rejects request with 403 Forbidden!
7. **Password Change:** User calls `POST /api/auth/change-password`. Permitted because `SecurityConfig` allows `ROLE_INITIAL_PASSWORD_CHANGE_REQUIRED` on this specific route.
8. **Completion:** Password updated, `mustChangePassword` set to `false`. User can now log in normally.

---

### Walkthrough 3: Refresh Token Rotation
1. **Client Action:** Client makes request to `POST /api/auth/refresh`. Browser automatically attaches `Cookie: REFRESH_TOKEN=<rawToken>`.
2. **Service Lookup:** `AuthServiceImpl.refresh` hashes raw token with SHA-256. Invokes `refreshTokenRepository.findByTokenHashForUpdate(tokenHash)`.
3. **Database Lock:** Row locked with `SELECT ... FOR UPDATE`.
4. **Validation:**
   - If token row missing: `InvalidTokenException("Invalid or unrecognized refresh token")`.
   - If `revokedAt != null`: `InvalidTokenException("Refresh token has been revoked")`.
   - If `expiresAt <= now`: `InvalidTokenException("Refresh token has expired")`.
   - If user not `ACTIVE`: `InvalidTokenException("Associated user account is inactive")`.
5. **In-Place Rotation:** New raw token generated. Existing database entity updated with new hash and new 12-hour expiration. Saved once to database.
6. **New JWT:** New 15-minute access token generated via `jwtService.generateToken`.
7. **Response:** HTTP 200 OK with updated `ACCESS_TOKEN` and `REFRESH_TOKEN` cookies.

---

## 14. Security Risk Assessment, Limitations, and Hardening Recommendations

Based on our architectural audit, here is an objective vulnerability and risk analysis:

| Finding / Risk Area | Observed Code Behavior | Severity | Threat / Practical Risk | Recommended Mitigation |
| :--- | :--- | :---: | :--- | :--- |
| **`auth.cookie.secure = false` by Default** | [application.yaml](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/resources/application.yaml) sets `auth.cookie.secure: false`. | **HIGH** (in Production) | If deployed to production without overriding this property, tokens will transmit over unencrypted HTTP, allowing network eavesdropping. | Enforce `auth.cookie.secure: true` in production profile or fail application startup if non-HTTPS. |
| **Shared Database Entity with Core (`vms_employee`)** | Auth service maps `vms_employee` directly via JPA. | **MEDIUM** (Architectural) | Database-level tight coupling. Schema migrations in VMS Core can break Auth service persistence. | Decouple via asynchronous events (`EmployeeCreatedEvent`) or REST client with local cached read model. |
| **Static Seeder in Production Context** | [TestUserSeeder.java](file:///c:/Users/logeshwaran.j/Downloads/auth/auth/src/main/java/com/adminvisitor/auth/util/TestUserSeeder.java) runs on every startup without profile gate. | **MEDIUM** | Seeds default admin credentials (`admin@adminvisitor.com` / `Admin@123`) on every restart if not present. | Gate with `@Profile("!prod")` or seed only through controlled Flyway/Liquibase database migrations. |
| **In-Memory Rate Limiting Absence on Login** | Login endpoint relies on database row checks after authentication. | **LOW to MEDIUM** | Distributed Denial of Service (DDoS) or brute-force password guessing could strain BCrypt hashing resources. | Introduce Redis-backed IP/Account rate limiting or Gateway bucket throttling (e.g., Bucket4j). |

---

## 15. Comprehensive Glossary and Progressive Learning Roadmap

### 15.1 Technical Glossary
- **Access Token:** A short-lived credential proving authentication to APIs.
- **Asymmetric Encryption / Signing:** Cryptographic systems using paired public and private keys.
- **Authentication:** Proving identity.
- **Authorization:** Proving permission.
- **BCrypt:** A password hashing function based on the Blowfish cipher with adaptive salting and work factor.
- **Claims:** Key-value pairs encoded inside a JWT payload.
- **CORS (Cross-Origin Resource Sharing):** Browser security mechanism restricting cross-origin HTTP requests.
- **CSRF (Cross-Site Request Forgery):** Attack tricking a victim browser into executing unwanted actions on a trusted site.
- **DTO (Data Transfer Object):** An object carrying data between processes without business logic.
- **HttpOnly:** Cookie attribute preventing client-side script access.
- **JWT (JSON Web Token):** RFC 7519 standard for URL-safe signed claims.
- **MapStruct:** Annotation processor generating Java bean mappers at compile time.
- **Nonce / Opaque Token:** Random cryptographically strong string with no internal structure or readable claims.
- **OTP (One-Time Password):** Ephemeral numeric passcode valid for a single verification session.
- **Pessimistic Locking:** Database row lock preventing concurrent modification until transaction completion.
- **Principal:** The currently authenticated user representation.
- **Rainbow Table:** Precomputed database of password hashes used for offline password cracking.
- **Refresh Token:** Long-lived credential used to obtain fresh access tokens.
- **RS256:** RSA Signature with SHA-256 asymmetric cryptographic algorithm.
- **SameSite:** Cookie attribute controlling cross-site transmission policies.
- **Salt:** Random bytes added to plaintext before hashing to ensure hash uniqueness.
- **SecurityContextHolder:** Spring Security's thread-local storage for authentication state.
- **Thymeleaf:** Modern server-side Java template engine.
- **XSS (Cross-Site Scripting):** Attack injecting malicious scripts into trusted websites.

### 15.2 Progressive Learning Roadmap for Developers
To build mastery in enterprise Java application security, follow this 11-step learning progression:

```
[1. Java & OOP Foundations: Records, Streams, Concurrency]
                         │
                         ▼
[2. HTTP Protocol & REST: Headers, Cookies, Status Codes]
                         │
                         ▼
[3. Spring Boot Core: IoC, Dependency Injection, Beans]
                         │
                         ▼
[4. Layered Architecture: Controllers, Services, DTO Boundaries]
                         │
                         ▼
[5. Spring Data JPA & Persistence: Transactions, Row Locking]
                         │
                         ▼
[6. Password Security: One-Way Hashing, Salts, BCrypt Cost Tuning]
                         │
                         ▼
[7. Spring Security Fundamentals: Filter Chains, SecurityContext]
                         │
                         ▼
[8. Cryptographic Tokens: JWT Structure, Claims, RS256 Asymmetric Pairs]
                         │
                         ▼
[9. Session Architectures: Refresh Tokens, In-Place Rotation, Cookie Security]
                         │
                         ▼
[10. Defensive Engineering: OTP Rate Limiting, Brute Force Throttling]
                         │
                         ▼
[11. Automated Security Testing: JUnit 5, Mockito Isolation, MockMvc Validation]
```
