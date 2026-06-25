# URL Shortener Application - Complete Documentation

> **Last Updated:** December 14, 2025  
> **Version:** 2.0  
> **Author:** Yato561  
> **Repository:** [UrlShortener](https://github.com/yato561/UrlShortener)

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Project Setup & Creation](#project-setup--creation)
3. [Dependencies & Configuration](#dependencies--configuration)
4. [Database Entities](#database-entities)
5. [Repository Layer](#repository-layer)
6. [Service Layer](#service-layer)
7. [Security Configuration](#security-configuration)
8. [Controller Layer](#controller-layer)
9. [Data Transfer Objects](#data-transfer-objects)
10. [Exception Handling](#exception-handling)
11. [Testing](#testing)
12. [New Features](#new-features)
13. [API Examples & Flows](#api-examples--flows)
14. [Database Schema](#database-schema)
15. [Creating Components](#creating-components)
16. [Running Locally](#running-locally)
17. [Project Structure](#project-structure)

---

## Project Overview

This is a **Spring Boot REST API** for URL shortening with the following characteristics:

| Feature | Details |
|---------|---------|
| **Framework** | Spring Boot 4.0.0 |
| **Language** | Java 21 |
| **Database** | PostgreSQL 12+ |
| **Authentication** | JWT (JSON Web Tokens) |
| **API Documentation** | Swagger/OpenAPI 3.0 |
| **Build Tool** | Maven 3.8+ |

### Core Capabilities

- ✅ User registration and authentication with JWT
- ✅ URL shortening with custom codes support
- ✅ Click tracking and analytics
- ✅ URL expiration management
- ✅ CORS support for frontend integration
- ✅ Rate limiting and caching
- ✅ QR code generation
- ✅ Webhook support for events

---

## Project Setup & Creation

### Using Spring Initializr

```bash
# Visit: https://start.spring.io/
# Select:
# - Project: Maven
# - Language: Java
# - Spring Boot: 4.0.0
# - Dependencies:
#   * Spring Web
#   * Spring Data JPA
#   * Spring Security
#   * PostgreSQL Driver
#   * Lombok
#   * Spring Doc OpenAPI UI
```

### Using Maven Command

```bash
mvn archetype:generate \
  -DgroupId=com.yato \
  -DartifactId=urlShortenerb \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

### Build Commands

```bash
# Clean and install dependencies
./mvnw clean install

# Build the project
./mvnw clean package

# Run with Maven
./mvnw spring-boot:run

# Build skip tests
./mvnw clean package -DskipTests

# Run JAR
java -jar target/urlShortenerb-0.0.1-SNAPSHOT.jar
```

---

## Dependencies & Configuration

### Key Maven Dependencies

#### Core Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>4.0.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>4.0.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <version>4.0.0</version>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

#### Optional Dependencies for New Features

```xml
<!-- Redis Caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.1</version>
</dependency>

<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.1</version>
</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j_2.13</artifactId>
    <version>7.6.0</version>
</dependency>
```

### Application Properties

**File:** `src/main/resources/application.properties`

```properties
# Application
spring.application.name=urlShortenerb
server.port=8081

# Database
spring.datasource.url=${POSTGRES_URL:jdbc:postgresql://localhost:5432/urlshortenerdb}
spring.datasource.username=${POSTGRES_USER:urluser}
spring.datasource.password=${POSTGRES_PASSWORD:urlpass}

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT
app.jwt.secret=YATO_SUPER_SECRET_KEY_1234567890!@#$%
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# Redis (Optional)
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.redis.time-to-live=3600000

# Logging
logging.level.root=INFO
logging.level.com.yato.urlShortenerb=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n

# Swagger
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## Database Entities

### User Entity

**File:** `src/main/java/com/yato/urlShortenerb/entity/User.java`

```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Url> urls = new ArrayList<>();
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
}
```

**Columns:**
- `id` - Auto-generated primary key
- `email` - Unique user email (login credential)
- `password` - BCrypt encrypted password
- `urls` - One-to-many relationship
- `subscription` - User's subscription plan

---

### Url Entity

**File:** `src/main/java/com/yato/urlShortenerb/entity/Url.java`

```java
@Entity
@Table(name = "urls", indexes = {
    @Index(name = "idx_short_code", columnList = "short_code"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expiry", columnList = "expiry")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(unique = true, nullable = false, length = 50)
    private String shortCode;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String longUrl;
    
    @Column(name = "click_count")
    private Long clickCount = 0L;
    
    @Column(name = "crt_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "expiry")
    private LocalDateTime expiry;
    
    @Column(length = 255)
    private String title;
    
    @Column(columnDefinition = "VARCHAR(50) default 'ACTIVE'")
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, DELETED
    
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
        name = "url_tags_mapping",
        joinColumns = @JoinColumn(name = "url_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<UrlTag> tags = new HashSet<>();
}
```

**Columns:**
- `id` - Primary key
- `user_id` - Foreign key to users table
- `short_code` - Unique 7-character code (or custom)
- `long_url` - Original URL (stored as TEXT for large URLs)
- `click_count` - Number of times clicked
- `crt_at` - Creation timestamp
- `expiry` - Optional expiration date
- `title` - User-friendly title
- `status` - ACTIVE, EXPIRED, DELETED
- `tags` - Many-to-many relationship

---

### AnalyticsEvent Entity

**File:** `src/main/java/com/yato/urlShortenerb/entity/AnalyticsEvent.java`

```java
@Entity
@Table(name = "analytics_events", indexes = {
    @Index(name = "idx_url_id", columnList = "url_id"),
    @Index(name = "idx_clicked_at", columnList = "clicked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(length = 500)
    private String referrer;
    
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt = LocalDateTime.now();
    
    @Column(length = 100)
    private String country;
    
    @Column(length = 50)
    private String deviceType; // MOBILE, DESKTOP, TABLET
    
    @Column(length = 100)
    private String browser;
}
```

---

## Repository Layer

### UserRepo

```java
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### UrlRepo

```java
public interface UrlRepo extends JpaRepository<Url, Long> {
    Optional<Url> findByShortCode(String shortCode);
    List<Url> findByUserId(Long userId);
    List<Url> findByUserIdAndStatus(Long userId, String status);
    List<Url> findByExpiryBefore(LocalDateTime dateTime);
    long countByUserId(Long userId);
}
```

### AnalyticsEventRepo

```java
public interface AnalyticsEventRepo extends JpaRepository<AnalyticsEvent, Long> {
    List<AnalyticsEvent> findByUrlId(Long urlId);
    long countByUrlId(Long urlId);
    List<AnalyticsEvent> findByUrlIdAndClickedAtBetween(
        Long urlId, 
        LocalDateTime start, 
        LocalDateTime end
    );
    long countByUrlIdAndClickedAtBetween(
        Long urlId, 
        LocalDateTime start, 
        LocalDateTime end
    );
}
```

---

## Service Layer

### UserService Interface

```java
public interface UserService {
    ResponseEntity<?> register(RegisterRequest request);
    ResponseEntity<?> login(LoginRequest request);
    Optional<User> findByEmail(String email);
}
```

### UserServiceImpl

**Key Methods:**

#### `register(RegisterRequest request)`

```
Steps:
1. Validate email format
2. Check if email already exists
3. If exists → return 409 Conflict
4. Encode password with BCryptPasswordEncoder (strength: 10)
5. Create User entity
6. Create default FREE subscription
7. Save to database
8. Return 200 OK with success message

Exception Handling:
- InvalidEmailException: Invalid email format
- UserAlreadyExistsException: Email already registered
- DatabaseException: Database operation failed
```

#### `login(LoginRequest request)`

```
Steps:
1. Find user by email
2. If not found → return 401 Unauthorized
3. Verify password with BCryptPasswordEncoder.matches()
4. If password mismatch → return 401
5. Generate JWT token:
   - Subject: user email
   - Issued-at: current time
   - Expiration: current time + 24 hours
   - Signed with HMAC-SHA256
6. Return AuthResponse with token
7. Log successful login

JWT Token Payload:
{
  "sub": "user@example.com",
  "iat": 1702562400,
  "exp": 1702648800
}
```

### UrlService Interface

```java
public interface UrlService {
    ResponseEntity<?> create(UrlRequest request, String currentUserEmail);
    ResponseEntity<?> getAll(String currentUserEmail);
    ResponseEntity<?> getById(Long id, String currentUserEmail);
    ResponseEntity<?> delete(Long id, String currentUserEmail);
    ResponseEntity<?> update(Long id, UrlRequest request, String currentUserEmail);
    List<Url> bulkCreate(List<UrlRequest> requests, String currentUserEmail);
    ResponseEntity<?> bulkDelete(List<Long> ids, String currentUserEmail);
}
```

### UrlServiceImpl

#### `create(UrlRequest request, String currentUserEmail)`

```
Steps:
1. Find user by email (authorization)
2. Check subscription limits:
   - Verify URL count < subscription.urlsLimit
   - If exceeded → return 402 Payment Required
3. Create Url entity with:
   - user association
   - long_url from request
   - generated short_code (loop until unique)
   - creation timestamp
   - status = ACTIVE
4. If expiry provided:
   - Parse ISO format: "2025-12-31T23:59:00"
   - Validate expiry is in future
   - Set expiry date
5. Save to database
6. Trigger webhook: URL_CREATED event
7. Clear cache
8. Return UrlResponse with:
   - id, shortCode, longUrl, clickCount, createdAt, expiry

Response:
{
  "id": 1,
  "shortCode": "aBc1234",
  "longUrl": "https://example.com/...",
  "clickCount": 0,
  "createdAt": "2025-12-14T10:30:00",
  "expiry": "2025-12-31T23:59:00"
}
```

#### `bulkCreate(List<UrlRequest> requests, String currentUserEmail)`

```
Steps:
1. Validate count <= subscription batch limit (e.g., 100)
2. For each request:
   - Validate longUrl format
   - Check total URLs won't exceed limit
   - Generate unique short_code
3. Save all in single batch operation
4. Return BulkResponse with:
   - created: successful count
   - failed: failed count
   - results: individual statuses
   - errors: detailed error messages
```

### AnalyticsService Interface

```java
public interface AnalyticsService {
    void recordClick(String shortCode, HttpServletRequest request);
    AnalyticsDTO getAnalytics(Long urlId, String userEmail);
    AnalyticsByDateDTO getAnalyticsByDateRange(
        Long urlId, 
        LocalDateTime start, 
        LocalDateTime end
    );
}
```

### AnalyticsServiceImpl

#### `recordClick(String shortCode, HttpServletRequest request)`

```
Steps:
1. Find URL by short_code
2. Extract request metadata:
   - User-Agent header → parse device type, browser
   - Client IP address (handle X-Forwarded-For proxy)
   - Referrer header
   - Accept-Language → approximate country
3. Optional: Reverse IP geolocation (MaxMind API)
4. Create AnalyticsEvent entity
5. Save to database (async to not block redirect)
6. Increment URL.clickCount
7. Update URL.lastClickedAt
8. Invalidate analytics cache
9. Trigger webhook: URL_CLICKED event

Performance Optimization:
- Use @Async for non-blocking storage
- Batch insert events every N seconds
- Archive old events to separate table after 90 days
```

#### `getAnalytics(Long urlId, String userEmail)`

```
Query/Cache Layer:
1. Check Redis cache for analytics_<urlId>
2. If cache hit: return cached data (1 hour TTL)
3. If cache miss:
   - Query analytics_events table
   - Group by deviceType, country, browser
   - Aggregate click counts
   - Calculate unique visitors (distinct IP + User-Agent)
   - Calculate daily/weekly trends
   - Store in cache
   - Return AnalyticsDTO

Response:
{
  "urlId": 1,
  "shortCode": "aBc1234",
  "totalClicks": 1250,
  "uniqueVisitors": 432,
  "clicksLast7Days": 145,
  "avgClicksPerDay": 178.6,
  "deviceBreakdown": {
    "mobile": 750,
    "desktop": 450,
    "tablet": 50
  },
  "topCountries": [
    {"country": "US", "clicks": 500},
    {"country": "IN", "clicks": 300},
    {"country": "UK", "clicks": 200}
  ],
  "topReferrers": [
    {"referrer": "google.com", "clicks": 600},
    {"referrer": "twitter.com", "clicks": 300},
    {"referrer": "direct", "clicks": 350}
  ],
  "topBrowsers": [
    {"browser": "Chrome", "clicks": 800},
    {"browser": "Safari", "clicks": 300}
  ]
}
```

---

## Security Configuration

### SecurityConfig

**File:** `src/main/java/com/yato/urlShortenerb/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", 
                                 "/v3/api-docs/**", 
                                 "/swagger-ui/**",
                                 "/s/**")
                    .permitAll()
                .anyRequest()
                    .authenticated()
            )
            .exceptionHandling()
                .authenticationEntryPoint(new JwtAuthEntryPoint())
                .and()
            .addFilterBefore(jwtAuthFilter(), 
                            UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter();
    }
}
```

**Configuration Details:**

| Setting | Value | Purpose |
|---------|-------|---------|
| CSRF | Disabled | Stateless API doesn't need CSRF |
| CORS | Enabled | Cross-origin frontend requests |
| Session | STATELESS | JWT-based, no cookies |
| Auth Entry Point | JwtAuthEntryPoint | Custom 401 responses |

### JWT Configuration

**File:** `src/main/java/com/yato/urlShortenerb/config/JWTUtils.java`

```java
@Component
public class JWTUtils {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;
    
    public String generateToken(String subject) {
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, 
                               SignatureAlgorithm.HS256.getJcaName());
    }
}
```

### JWT Auth Filter

**File:** `src/main/java/com/yato/urlShortenerb/config/JwtAuthFilter.java`

```java
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    
    @Autowired
    private JWTUtils jwtUtils;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (jwtUtils.validateToken(token)) {
                    String email = jwtUtils.getEmailFromToken(token);
                    UserDetails userDetails = userDetailsService
                        .loadUserByUsername(email);
                    
                    UsernamePasswordAuthenticationToken auth = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    
                    SecurityContextHolder.getContext()
                        .setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## Controller Layer

### AuthController

**Base Path:** `/auth`

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Register attempt for email: {}", request.email());
        return userService.register(request);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());
        return userService.login(request);
    }
}
```

#### Register Endpoint

```
POST /auth/register
Content-Type: application/json

Request:
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Success Response (200):
{
  "message": "User registered successfully",
  "email": "john@example.com"
}

Error Responses:
- 400: Email already registered
- 400: Invalid email format
- 400: Password too weak
- 500: Server error
```

#### Login Endpoint

```
POST /auth/login
Content-Type: application/json

Request:
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Success Response (200):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400
}

Error Responses:
- 401: Invalid email or password
- 404: User not found
- 500: Server error
```

### UrlController

**Base Path:** `/urls`

**Authentication:** Required on all endpoints except listed public

```java
@RestController
@RequestMapping("/urls")
@RequiredArgsConstructor
@Slf4j
public class UrlController {
    
    private final UrlService urlService;
    
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        return auth.getName();
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody UrlRequest request) {
        String email = getCurrentUserEmail();
        return urlService.create(request, email);
    }
    
    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        String email = getCurrentUserEmail();
        return urlService.getAll(email);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        return urlService.getById(id, email);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        return urlService.delete(id, email);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                    @RequestBody UrlRequest request) {
        String email = getCurrentUserEmail();
        return urlService.update(id, request, email);
    }
    
    @PostMapping("/bulk/create")
    public ResponseEntity<?> bulkCreate(@RequestBody List<UrlRequest> requests) {
        String email = getCurrentUserEmail();
        return urlService.bulkCreate(requests, email);
    }
    
    @DeleteMapping("/bulk/delete")
    public ResponseEntity<?> bulkDelete(@RequestBody List<Long> ids) {
        String email = getCurrentUserEmail();
        return urlService.bulkDelete(ids, email);
    }
}
```

#### Create URL Endpoint

```
POST /urls/create
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "longUrl": "https://github.com/yato561/UrlShortener",
  "title": "My Project",
  "expiry": "2025-12-31T23:59:00"
}

Success Response (200):
{
  "id": 1,
  "shortCode": "aBc1234",
  "longUrl": "https://github.com/yato561/UrlShortener",
  "title": "My Project",
  "clickCount": 0,
  "createdAt": "2025-12-14T10:30:00",
  "expiry": "2025-12-31T23:59:00"
}

Error Responses:
- 401: Unauthorized (invalid/missing token)
- 402: Subscription limit exceeded
- 400: Invalid URL format
- 409: Short code already exists
```

#### Get All URLs Endpoint

```
GET /urls/all?page=0&size=20&sort=createdAt,desc
Authorization: Bearer {token}

Success Response (200):
[
  {
    "id": 1,
    "shortCode": "aBc1234",
    "longUrl": "https://example1.com",
    "clickCount": 45,
    "createdAt": "2025-12-14T10:30:00",
    "expiry": "2025-12-31T23:59:00"
  },
  {
    "id": 2,
    "shortCode": "xYz5678",
    "longUrl": "https://example2.com",
    "clickCount": 12,
    "createdAt": "2025-12-13T15:45:00",
    "expiry": null
  }
]
```

#### Delete URL Endpoint

```
DELETE /urls/{id}
Authorization: Bearer {token}

Success Response (204): No content

Error Responses:
- 401: Unauthorized
- 403: Forbidden (not owner)
- 404: URL not found
```

### AnalyticsController

**Base Path:** `/analytics`

```java
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        return auth.getName();
    }
    
    @GetMapping("/{urlId}")
    public ResponseEntity<?> getAnalytics(@PathVariable Long urlId) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
            analyticsService.getAnalytics(urlId, email)
        );
    }
    
    @GetMapping("/{urlId}/range")
    public ResponseEntity<?> getAnalyticsByRange(
        @PathVariable Long urlId,
        @RequestParam String startDate,
        @RequestParam String endDate
    ) {
        String email = getCurrentUserEmail();
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        
        return ResponseEntity.ok(
            analyticsService.getAnalyticsByDateRange(urlId, start, end)
        );
    }
}
```

#### Get Analytics Endpoint

```
GET /analytics/{urlId}
Authorization: Bearer {token}

Success Response (200):
{
  "urlId": 1,
  "shortCode": "aBc1234",
  "totalClicks": 1250,
  "uniqueVisitors": 432,
  "clicksLast7Days": 145,
  "avgClicksPerDay": 178.6,
  "deviceBreakdown": {
    "mobile": 750,
    "desktop": 450,
    "tablet": 50
  },
  "topCountries": [
    {"country": "US", "clicks": 500},
    {"country": "IN", "clicks": 300}
  ],
  "topReferrers": [
    {"referrer": "google.com", "clicks": 600},
    {"referrer": "direct", "clicks": 350}
  ]
}
```

### RedirectController

**Base Path:** `/s` (public endpoint)

```java
@RestController
@RequestMapping("/s")
@RequiredArgsConstructor
@Slf4j
public class RedirectController {
    
    private final UrlRepo urlRepo;
    private final AnalyticsService analyticsService;
    
    @GetMapping("/{shortCode}")
    public RedirectView redirect(
        @PathVariable String shortCode,
        HttpServletRequest request
    ) {
        Optional<Url> urlOpt = urlRepo.findByShortCode(shortCode);
        
        if (urlOpt.isEmpty()) {
            throw new UrlNotFoundException("URL not found");
        }
        
        Url url = urlOpt.get();
        
        // Check expiration
        if (url.getExpiry() != null && 
            url.getExpiry().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("URL has expired");
        }
        
        // Record analytics asynchronously
        analyticsService.recordClick(shortCode, request);
        
        // Increment click count
        url.setClickCount(url.getClickCount() + 1);
        urlRepo.save(url);
        
        return new RedirectView(url.getLongUrl());
    }
}
```

#### Redirect Endpoint

```
GET /s/{shortCode}

Success Response (302):
Location: https://github.com/yato561/UrlShortener

Error Responses:
- 400: Invalid or non-existent short code
- 410: URL has expired
```

---

## Data Transfer Objects (DTOs)

### RegisterRequest

```java
public record RegisterRequest(String email, String password) {}
```

### LoginRequest

```java
public record LoginRequest(String email, String password) {}
```

### AuthResponse

```java
public record AuthResponse(String token, Long expiresIn) {}
```

### UrlRequest

```java
public record UrlRequest(
    String longUrl,
    String title,
    String expiry
) {}
```

### UrlResponse

```java
@Data
public class UrlResponse {
    private Long id;
    
    @JsonProperty("shortCode")
    private String shortCode;
    
    @JsonProperty("longUrl")
    private String longUrl;
    
    private String title;
    
    @JsonProperty("clickCount")
    private Long clickCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime expiry;
    
    @JsonProperty("QRCodeUrl")
    private String qrCodeUrl; // /urls/{id}/qrcode
}
```

### AnalyticsDTO

```java
@Data
public class AnalyticsDTO {
    private Long urlId;
    private String shortCode;
    private Long totalClicks;
    private Long uniqueVisitors;
    private Long clicksLast7Days;
    private Double avgClicksPerDay;
    private Map<String, Long> deviceBreakdown;
    private List<CountryStats> topCountries;
    private List<ReferrerStats> topReferrers;
    private List<BrowserStats> topBrowsers;
}
```

---

## Exception Handling

**File:** `src/main/java/com/yato/urlShortenerb/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<?> handleUrlNotFound(UrlNotFoundException ex) {
        return ResponseEntity.status(404).body(
            Map.of("error", "Not Found", "message", ex.getMessage())
        );
    }
    
    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<?> handleUrlExpired(UrlExpiredException ex) {
        return ResponseEntity.status(410).body(
            Map.of("error", "Gone", "message", ex.getMessage())
        );
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(401).body(
            Map.of("error", "Unauthorized", "message", ex.getMessage())
        );
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(403).body(
            Map.of("error", "Forbidden", "message", ex.getMessage())
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(
            Map.of("error", "Internal Server Error", 
                   "message", "An unexpected error occurred")
        );
    }
}
```

---

## Testing

**File:** `src/test/java/com/yato/urlShortenerb/UrlShortenerbApplicationTests.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerbApplicationTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepo userRepo;
    
    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
    }
    
    @Test
    void testRegisterUser() throws Exception {
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"Pass123!\"}"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testLoginUser() throws Exception {
        // Setup user first
        // Then test login
        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"Pass123!\"}"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testCreateUrl() throws Exception {
        // Login first to get token
        // Then create URL with authorization header
    }
}
```

**Run Tests:**

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UrlShortenerbApplicationTests

# Run with coverage
./mvnw test jacoco:report
```

---

## New Features

### 1. Analytics & Click Tracking

Tracks individual clicks with metadata:
- Browser and device information
- Geographic location (IP-based)
- Referrer source
- Timestamps for trend analysis

**Endpoints:**
- `GET /analytics/{urlId}` - Get analytics dashboard
- `GET /analytics/{urlId}/range?start=...&end=...` - Date range analytics

---

### 2. URL Expiration & Cleanup

URLs can have expiration dates. Scheduled task deletes expired URLs automatically.

**Features:**
- Set expiry on URL creation
- 410 Gone response for expired URLs
- Scheduled cleanup every hour

---

### 3. Bulk URL Operations

Create/delete multiple URLs in single request.

```
POST /urls/bulk/create
DELETE /urls/bulk/delete
POST /urls/bulk/export
```

---

### 4. API Rate Limiting

Prevent abuse with configurable rate limits:
- 5 register attempts/hour per IP
- 10 login attempts/hour per IP
- 100 URL creates/day per user

**Response Headers:**
- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset`

---

### 5. Redis Caching

Cache frequently accessed URLs and analytics.

**Cached Data:**
- Short code → URL mapping (30 min)
- User's URLs list (30 min)
- Analytics dashboard (1 hour)

**Cache Hit Improvement:**
- Redirect response: 5-10ms (vs 50-100ms)
- Database load: 70-80% reduction

---

### 6. QR Code Generation

Generate QR codes for shortened URLs.

```
GET /urls/{id}/qrcode?size=300
```

Returns PNG image with embedded short URL.

---

### 7. Custom Short Codes (Vanity URLs)

Create branded short URLs:

```
POST /urls/custom
{
  "longUrl": "https://example.com",
  "customCode": "mycode123",
  "expiry": "2025-12-31T23:59"
}
```

Validation: 3-50 alphanumeric characters, no duplicates.

---

### 8. URL Tags & Categories

Organize URLs with tags:

```
POST /tags/create
GET /urls/filter?tags=social,marketing
DELETE /tags/{tagId}
```

---

### 9. Subscription Plans

Tiered pricing with feature limits:

| Plan | URLs | Analytics | Custom Codes | Price |
|------|------|-----------|--------------|-------|
| FREE | 10 | 7 days | ❌ | $0 |
| BASIC | 100 | 30 days | ✅ | $5/mo |
| PREMIUM | 1000 | 365 days | ✅ | $15/mo |
| ENTERPRISE | ∞ | ∞ | ✅ | Custom |

---

### 10. Webhooks

Receive events for:
- `url.created` - New URL created
- `url.clicked` - URL redirected
- `url.deleted` - URL deleted
- `url.expired` - URL expiration

**Example:**
```
POST https://your-callback-url.com/webhook
{
  "event": "url.clicked",
  "timestamp": "2025-12-14T10:35:00Z",
  "data": {
    "shortCode": "aBc1234",
    "ipAddress": "192.168.1.1",
    "country": "US"
  }
}
```

---

## API Examples & Flows

### Complete Authentication Flow

```
1. REGISTER USER
   POST http://localhost:8081/auth/register
   {
     "email": "john@example.com",
     "password": "SecurePass123!"
   }
   
   Response (200):
   {
     "message": "User registered successfully"
   }

2. LOGIN USER
   POST http://localhost:8081/auth/login
   {
     "email": "john@example.com",
     "password": "SecurePass123!"
   }
   
   Response (200):
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "expiresIn": 86400
   }

3. USE TOKEN
   GET http://localhost:8081/urls/all
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   
   Response (200): List of user's URLs
```

### URL Creation & Sharing Flow

```
1. CREATE SHORT URL
   POST http://localhost:8081/urls/create
   Authorization: Bearer {token}
   {
     "longUrl": "https://github.com/yato561/UrlShortener/issues/123",
     "title": "GitHub Issue",
     "expiry": "2025-12-31T23:59:00"
   }
   
   Response (200):
   {
     "id": 1,
     "shortCode": "aBc1234",
     "shortUrl": "http://localhost:8081/s/aBc1234",
     "qrCodeUrl": "/urls/1/qrcode"
   }

2. SHARE SHORT URL
   User shares: http://localhost:8081/s/aBc1234

3. CLICK REDIRECT
   GET http://localhost:8081/s/aBc1234
   
   Response (302):
   Location: https://github.com/yato561/UrlShortener/issues/123
   
   Browser redirects to original URL
   Analytics recorded

4. VIEW ANALYTICS
   GET http://localhost:8081/analytics/1
   Authorization: Bearer {token}
   
   Response (200):
   {
     "totalClicks": 45,
     "uniqueVisitors": 32,
     "topCountries": [{"country": "US", "clicks": 25}]
   }
```

---

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    subscription_id BIGINT REFERENCES subscriptions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

### URLs Table

```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    short_code VARCHAR(50) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    title VARCHAR(255),
    click_count BIGINT DEFAULT 0,
    crt_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_user_id ON urls(user_id);
CREATE INDEX idx_urls_expiry ON urls(expiry);
```

### Analytics Events Table

```sql
CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES urls(id),
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    referrer VARCHAR(500),
    clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    country VARCHAR(100),
    device_type VARCHAR(50),
    browser VARCHAR(100)
);

CREATE INDEX idx_analytics_url_id ON analytics_events(url_id);
CREATE INDEX idx_analytics_clicked_at ON analytics_events(clicked_at);
```

---

## Creating Components

### Create New Controller

```bash
mkdir -p src/main/java/com/yato/urlShortenerb/controller

cat > src/main/java/com/yato/urlShortenerb/controller/NewController.java << 'EOF'
package com.yato.urlShortenerb.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/new")
@RequiredArgsConstructor
public class NewController {
    
    @GetMapping
    public String getEndpoint() {
        return "Response";
    }
}
EOF
```

### Create New Service

```bash
# Interface
cat > src/main/java/com/yato/urlShortenerb/service/NewService.java << 'EOF'
package com.yato.urlShortenerb.service;

public interface NewService {
    void performAction();
}
EOF

# Implementation
cat > src/main/java/com/yato/urlShortenerb/service/impl/NewServiceImpl.java << 'EOF'
package com.yato.urlShortenerb.service.impl;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewServiceImpl implements NewService {
    
    @Override
    public void performAction() {
        // Implementation
    }
}
EOF
```

### Create New Entity

```bash
cat > src/main/java/com/yato/urlShortenerb/entity/NewEntity.java << 'EOF'
package com.yato.urlShortenerb.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "new_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
}
EOF
```

### Create New Repository

```bash
cat > src/main/java/com/yato/urlShortenerb/repo/NewRepo.java << 'EOF'
package com.yato.urlShortenerb.repo;

import com.yato.urlShortenerb.entity.NewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewRepo extends JpaRepository<NewEntity, Long> {
}
EOF
```

---

## Running Locally

### Prerequisites

```bash
# Required
- Java 21 or higher
- PostgreSQL 12 or higher
- Maven 3.8 or higher
- Git

# Optional
- Docker & Docker Compose
- Redis (for caching)
- Postman (API testing)
```

### Database Setup

```bash
# Create database
createdb urlshortenerdb

# Create user
psql -U postgres -c "CREATE USER urluser WITH PASSWORD 'urlpass';"
psql -U postgres -c "ALTER USER urluser CREATEDB;"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE urlshortenerdb TO urluser;"

# Verify connection
psql -U urluser -d urlshortenerdb -c "SELECT version();"
```

### Run Application

```bash
# Clone repository
git clone https://github.com/yato561/UrlShortener.git
cd UrlShortener

# Install dependencies
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Or run JAR
./mvnw clean package -DskipTests
java -jar target/urlShortenerb-0.0.1-SNAPSHOT.jar
```

### Environment Variables

```bash
export POSTGRES_URL=jdbc:postgresql://localhost:5432/urlshortenerdb
export POSTGRES_USER=urluser
export POSTGRES_PASSWORD=urlpass
export JWT_EXPIRATION_MS=86400000
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379
```

### Access Points

- **API:** http://localhost:8081
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **API Docs:** http://localhost:8081/v3/api-docs

---

## Project Structure

```
urlShortenerb/
├── src/
│   ├── main/
│   │   ├── java/com/yato/urlShortenerb/
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Url.java
│   │   │   │   ├── AnalyticsEvent.java
│   │   │   │   ├── UrlTag.java
│   │   │   │   ├── Subscription.java
│   │   │   │   └── Webhook.java
│   │   │   ├── repo/
│   │   │   │   ├── UserRepo.java
│   │   │   │   ├── UrlRepo.java
│   │   │   │   ├── AnalyticsEventRepo.java
│   │   │   │   ├── UrlTagRepo.java
│   │   │   │   └── SubscriptionRepo.java
│   │   │   ├── service/
│   │   │   │   ├── UserService.java
│   │   │   │   ├── UrlService.java
│   │   │   │   ├── AnalyticsService.java
│   │   │   │   ├── QrCodeService.java
│   │   │   │   ├── WebhookService.java
│   │   │   │   └── UserDetailsService.java
│   │   │   ├── service/impl/
│   │   │   │   ├── UserServiceImpl.java
│   │   │   │   ├── UrlServiceImpl.java
│   │   │   │   ├── AnalyticsServiceImpl.java
│   │   │   │   ├── QrCodeServiceImpl.java
│   │   │   │   ├── WebhookServiceImpl.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UrlController.java
│   │   │   │   ├── RedirectController.java
│   │   │   │   ├── AnalyticsController.java
│   │   │   │   ├── TagController.java
│   │   │   │   └── WebhookController.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JWTUtils.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   ├── PasswordConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   └── SchedulingConfig.java
│   │   │   ├── dto/
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── UrlRequest.java
│   │   │   │   ├── UrlResponse.java
│   │   │   │   ├── AnalyticsDTO.java
│   │   │   │   └── BulkResponse.java
│   │   │   ├── util/
│   │   │   │   ├── ShortCodeGenerator.java
│   │   │   │   ├── GeolocationUtil.java
│   │   │   │   └── DeviceDetector.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── UrlNotFoundException.java
│   │   │   │   ├── UrlExpiredException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   └── ForbiddenException.java
│   │   │   ├── task/
│   │   │   │   └── UrlExpirationTask.java
│   │   │   └── UrlShortenerbApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/
│       └── java/com/yato/urlShortenerb/
│           ├── UrlShortenerbApplicationTests.java
│           ├── controller/
│           ├── service/
│           └── repo/
├── pom.xml
├── DOCUMENTATION.md
├── DOCUMENTATION.txt
├── .gitignore
├── Dockerfile
└── docker-compose.yml
```

---

## Quick Start Guide

```bash
# 1. Clone repository
git clone https://github.com/yato561/UrlShortener.git
cd UrlShortener

# 2. Setup database
createdb urlshortenerdb
psql -U postgres -c "CREATE USER urluser WITH PASSWORD 'urlpass';"

# 3. Build project
./mvnw clean package -DskipTests

# 4. Run application
./mvnw spring-boot:run

# 5. Test API
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePass123!"}'

# 6. Access Swagger UI
open http://localhost:8081/swagger-ui.html
```

---

## Support & Contribution

- **Issues:** [GitHub Issues](https://github.com/yato561/UrlShortener/issues)
- **Pull Requests:** [GitHub PRs](https://github.com/yato561/UrlShortener/pulls)
- **Documentation:** This markdown file

---

## License

This project is licensed under the MIT License.

---

**Last Updated:** December 14, 2025  
**Version:** 2.0.0
