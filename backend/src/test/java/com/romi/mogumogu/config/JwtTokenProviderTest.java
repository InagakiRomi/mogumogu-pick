package com.romi.mogumogu.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private static final String VALID_SECRET_32_BYTES = "a".repeat(32);

    private static final SecretKey DEFAULT_SIGNING_KEY = hmacKeyFrom(VALID_SECRET_32_BYTES);

    private static final long TTL_DEFAULT_MS = 60_000L;
    private static final long TTL_ONE_HOUR_MS = 3_600_000L;
    private static final long TTL_ONE_DAY_MS = 86_400_000L;

    private static final UserEntity BASE_USER = user(1, "a@b.c", 1, UserRole.USER);

    private static SecretKey hmacKeyFrom(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static UserEntity user(
            Integer userId, String email, Integer groupId, UserRole role) {
        return UserEntity.builder()
                .userId(userId)
                .groupId(groupId)
                .roles(role)
                .email(email)
                .build();
    }

    private static JwtTokenProvider provider(String secret, long expirationTimeMs) {
        return new JwtTokenProvider(secret, expirationTimeMs);
    }

    private static JwtTokenProvider provider(long expirationTimeMs) {
        return provider(VALID_SECRET_32_BYTES, expirationTimeMs);
    }

    private static Claims parseAndVerify(String jwt, SecretKey key) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
    }

    private static Claims parseAndVerifyWithSkew(String jwt, SecretKey key, long clockSkewSeconds) {
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(clockSkewSeconds)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    private static Claims parseClaims(String jwt) {
        return parseAndVerify(jwt, DEFAULT_SIGNING_KEY);
    }

    private static Claims parseClaimsWithSkew(String jwt, long clockSkewSeconds) {
        return parseAndVerifyWithSkew(jwt, DEFAULT_SIGNING_KEY, clockSkewSeconds);
    }

    private static String accessToken(long ttlMs, UserEntity user) {
        return provider(ttlMs).generateAccessToken(user);
    }

    private static Claims claimsFrom(long ttlMs, UserEntity user) {
        return parseClaims(accessToken(ttlMs, user));
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Accepts a 32-byte HMAC secret")
        void acceptsMinimumHmacKeyLength() {
            assertThat(provider(VALID_SECRET_32_BYTES, TTL_ONE_HOUR_MS)).isNotNull();
        }

        @Test
        @DisplayName("Accepts a secret longer than 32 bytes")
        void acceptsLongerSecret() {
            assertThat(provider("b".repeat(64), 1L)).isNotNull();
        }

        @Test
        @DisplayName("Rejects a secret shorter than 32 bytes with WeakKeyException")
        void rejectsSecretShorterThan256Bits() {
            String weak = "a".repeat(31);
            assertThatThrownBy(() -> provider(weak, TTL_ONE_HOUR_MS)).isInstanceOf(WeakKeyException.class);
        }

        @Test
        @DisplayName("Rejects an empty secret with WeakKeyException")
        void rejectsEmptySecret() {
            assertThatThrownBy(() -> provider("", TTL_ONE_HOUR_MS)).isInstanceOf(WeakKeyException.class);
        }

        @Test
        @DisplayName("UTF-8 multi-byte secret: 32 ASCII characters satisfy the bit-length requirement")
        void unicodeSecret_bytesNotChars() {
            String emoji32Bytes = "😀".repeat(8);
            assertThat(emoji32Bytes.getBytes(StandardCharsets.UTF_8)).hasSize(32);
            assertThat(provider(emoji32Bytes, 1L)).isNotNull();
        }
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("Generated JWT verifies with the same key and matches subject, claims, and user")
        void tokenIsVerifiableAndClaimsMatchUser() {
            UserEntity u = user(42, "test@example.com", 7, UserRole.GROUP_ADMIN);
            Claims claims = claimsFrom(TTL_ONE_DAY_MS, u);

            assertThat(claims.getSubject()).isEqualTo("42");
            assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
            assertThat(claims.get("groupId", Integer.class)).isEqualTo(7);
            assertThat(claims.get("role", String.class)).isEqualTo("GROUP_ADMIN");
        }

        @ParameterizedTest(name = "role {0} writes claim role = {1}")
        @MethodSource("allRoles")
        @DisplayName("Each UserRole name() is written into the role claim")
        void roleClaimReflectsEnumName(UserRole role, String expectedClaim) {
            UserEntity u = user(1, "a@b.c", 1, role);
            Claims claims = claimsFrom(TTL_DEFAULT_MS, u);
            assertThat(claims.get("role", String.class)).isEqualTo(expectedClaim);
        }

        static Stream<Arguments> allRoles() {
            return Stream.of(
                    Arguments.of(UserRole.GROUP_ADMIN, "GROUP_ADMIN"),
                    Arguments.of(UserRole.USER, "USER"));
        }

        @Test
        @DisplayName("exp - iat equals the configured TTL in milliseconds when it is a multiple of 1000")
        void expirationEqualsIssuedAtPlusConfiguredMillis_whenTtlMultipleOfSecond() {
            UserEntity u = user(99, "x@y.z", 3, UserRole.USER);

            Instant before = Instant.now();
            String jwt = accessToken(TTL_ONE_DAY_MS, u);
            Instant after = Instant.now();

            Claims claims = parseClaims(jwt);
            Date iat = claims.getIssuedAt();
            Date exp = claims.getExpiration();

            assertThat(iat).isNotNull();
            assertThat(exp).isNotNull();
            assertThat(exp.getTime() - iat.getTime()).isEqualTo(TTL_ONE_DAY_MS);

            assertThat(iat.toInstant())
                    .isBetween(before.minus(2, ChronoUnit.SECONDS), after.plus(2, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Non-second TTL is stored in seconds, so exp-iat may differ by at most about 1 second")
        void expirationTruncatesToJwtSecondPrecision() {
            long ttlMs = 12_345L;
            Claims claims = claimsFrom(ttlMs, BASE_USER);
            long delta = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
            assertThat(Math.abs(delta - ttlMs)).isLessThanOrEqualTo(1000L);
        }

        @Test
        @DisplayName("Zero expirationTime puts exp and iat in the same second (clock skew needed to verify)")
        void zeroTtl_expEqualsIatSameSecond() {
            UserEntity u = user(1, "a@a.a", 1, UserRole.USER);
            Claims claims = parseClaimsWithSkew(accessToken(0L, u), 120);
            assertThat(claims.getExpiration().getTime()).isEqualTo(claims.getIssuedAt().getTime());
        }

        @Test
        @DisplayName("Different users produce different token strings")
        void differentUsersYieldDifferentCompactJws() {
            String t1 = accessToken(TTL_ONE_HOUR_MS, user(1, "a@a.a", 1, UserRole.USER));
            String t2 = accessToken(TTL_ONE_HOUR_MS, user(2, "a@a.a", 1, UserRole.USER));
            assertThat(t1).isNotEqualTo(t2);
        }

        @Test
        @DisplayName("Consecutive tokens differ after the second boundary; they may match within the same second")
        void consecutiveGenerationsDistinctAfterSecondBoundary() throws Exception {
            UserEntity u = user(5, "same@same.com", 1, UserRole.USER);
            JwtTokenProvider p = provider(TTL_ONE_HOUR_MS);
            String first = p.generateAccessToken(u);
            Thread.sleep(1100);
            String second = p.generateAccessToken(u);
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("Emails with special characters such as + and . are written into claims")
        void emailWithSpecialCharacters() {
            String email = "user.name+tag@sub.example.co.jp";
            Claims claims = claimsFrom(TTL_DEFAULT_MS, user(10, email, 2, UserRole.USER));
            assertThat(claims.get("email", String.class)).isEqualTo(email);
        }

        @Test
        @DisplayName("A wrong key cannot verify the signature")
        void wrongKeyFailsVerification() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            SecretKey otherKey = hmacKeyFrom("b".repeat(32));
            assertThatThrownBy(() -> parseAndVerify(jwt, otherKey)).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("Null user throws NullPointerException")
        void nullUserThrowsNpe() {
            assertThatThrownBy(() -> provider(TTL_DEFAULT_MS).generateAccessToken(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Null roles throw NullPointerException when name() is called")
        void nullRoleThrowsNpe() {
            UserEntity u = user(1, "a@b.c", 1, null);
            assertThatThrownBy(() -> accessToken(TTL_DEFAULT_MS, u)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Null userId becomes the literal subject string null via String.valueOf")
        void nullUserId_subjectIsLiteralNullString() {
            UserEntity u = user(null, "a@b.c", 1, UserRole.USER);
            Claims claims = claimsFrom(TTL_DEFAULT_MS, u);
            assertThat(claims.getSubject()).isEqualTo("null");
        }

        @Test
        @DisplayName("Very large expirationTime may differ from exp-iat by at most about 1 second due to JWT second precision")
        void veryLargeTtl_secondPrecisionTruncation() {
            long ttl = Integer.MAX_VALUE;
            Claims claims = claimsFrom(ttl, BASE_USER);
            long delta = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
            assertThat(Math.abs(delta - ttl)).isLessThanOrEqualTo(1000L);
        }

        @Test
        @DisplayName("groupId 0 is still written as claim 0")
        void groupIdZero() {
            Claims claims = claimsFrom(TTL_DEFAULT_MS, user(1, "a@b.c", 0, UserRole.USER));
            assertThat(claims.get("groupId", Integer.class)).isZero();
        }

        @Test
        @DisplayName("Negative userId is written into subject as-is")
        void negativeUserIdInSubject() {
            Claims claims = claimsFrom(TTL_DEFAULT_MS, user(-1, "a@b.c", 1, UserRole.USER));
            assertThat(claims.getSubject()).isEqualTo("-1");
        }
    }

    @Nested
    @DisplayName("parseValidClaims")
    class ParseValidClaimsTests {

        @Test
        @DisplayName("A valid token returns Claims")
        void validToken_returnsClaims() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(provider(TTL_DEFAULT_MS).parseValidClaims(jwt)).isPresent();
        }

        @Test
        @DisplayName("Null, blank, or non-JWT strings return empty")
        void invalidInput_returnsEmpty() {
            JwtTokenProvider provider = provider(TTL_DEFAULT_MS);
            assertThat(provider.parseValidClaims(null)).isEmpty();
            assertThat(provider.parseValidClaims("   ")).isEmpty();
            assertThat(provider.parseValidClaims("not-a-jwt")).isEmpty();
        }

        @Test
        @DisplayName("An expired token returns empty")
        void expiredToken_returnsEmpty() {
            Date issuedAt = new Date(System.currentTimeMillis() - 300_000L);
            Date expiredAt = new Date(System.currentTimeMillis() - 180_000L);
            String jwt = Jwts.builder()
                    .subject("1")
                    .claim("email", "a@b.c")
                    .claim("groupId", 1)
                    .claim("role", "USER")
                    .issuedAt(issuedAt)
                    .expiration(expiredAt)
                    .signWith(DEFAULT_SIGNING_KEY)
                    .compact();

            assertThat(provider(TTL_DEFAULT_MS).parseValidClaims(jwt)).isEmpty();
        }

        @Test
        @DisplayName("A token signed with a different key returns empty")
        void wrongKey_returnsEmpty() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            JwtTokenProvider other = provider("b".repeat(32), TTL_DEFAULT_MS);
            assertThat(other.parseValidClaims(jwt)).isEmpty();
        }

        @Test
        @DisplayName("Leading and trailing whitespace is trimmed before parsing")
        void trimsWhitespace() {
            String jwt = "  " + accessToken(TTL_DEFAULT_MS, BASE_USER) + "  ";
            assertThat(provider(TTL_DEFAULT_MS).parseValidClaims(jwt)).isPresent();
        }
    }

    @Nested
    @DisplayName("resolveAuthentication")
    class ResolveAuthenticationTests {

        @Test
        @DisplayName("A Bearer token returns Authentication with a ROLE_ prefix")
        void bearerToken_returnsAuthenticationWithRole() {
            UserEntity admin = user(7, "admin@example.com", 1, UserRole.GROUP_ADMIN);
            String header = "Bearer " + accessToken(TTL_DEFAULT_MS, admin);

            var authOpt = provider(TTL_DEFAULT_MS).resolveAuthentication(header);
            assertThat(authOpt).isPresent();
            assertThat(authOpt.get().getPrincipal()).isEqualTo("7");
            assertThat(authOpt.get().getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_GROUP_ADMIN");
        }

        @Test
        @DisplayName("The bearer prefix is case-insensitive")
        void bearerPrefix_isCaseInsensitive() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(provider(TTL_DEFAULT_MS).resolveAuthentication("bearer " + jwt)).isPresent();
        }

        @Test
        @DisplayName("A missing Bearer prefix returns empty")
        void missingBearerPrefix_returnsEmpty() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(provider(TTL_DEFAULT_MS).resolveAuthentication(jwt)).isEmpty();
        }

        @Test
        @DisplayName("An invalid role claim returns empty")
        void invalidRoleClaim_returnsEmpty() {
            String jwt = Jwts.builder()
                    .subject("1")
                    .claim("role", "NOT_A_REAL_ROLE")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + TTL_DEFAULT_MS))
                    .signWith(DEFAULT_SIGNING_KEY)
                    .compact();

            assertThat(provider(TTL_DEFAULT_MS).resolveAuthentication("Bearer " + jwt)).isEmpty();
        }

        @Test
        @DisplayName("A null header returns empty")
        void nullHeader_returnsEmpty() {
            assertThat(provider(TTL_DEFAULT_MS).resolveAuthentication(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Boundaries: JWT structure and verification")
    class TokenStructureTests {

        @Test
        @DisplayName("Compact JWS has three base64url parts")
        void compactJwsHasThreeParts() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(jwt.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("A non-JWS string cannot be parsed")
        void malformedStringFailsParse() {
            assertThatThrownBy(() -> parseClaims("not-a-jwt")).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("Tampering with the payload segment fails signature verification")
        void tamperedPayloadFailsSignature() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            String[] parts = jwt.split("\\.");
            assertThat(parts).hasSize(3);
            String tampered = parts[0] + "." + parts[1].replace('e', 'f') + "." + parts[2];
            assertThatThrownBy(() -> parseClaims(tampered)).isInstanceOf(JwtException.class);
        }
    }
}
