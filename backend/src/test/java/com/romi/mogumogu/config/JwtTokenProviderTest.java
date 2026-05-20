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

    /** HS256 至少需要 256 bits（32 bytes）金鑰，與 Keys.hmacShaKeyFor 要求一致 */
    private static final String VALID_SECRET_32_BYTES = "a".repeat(32);

    private static final SecretKey DEFAULT_SIGNING_KEY = hmacKeyFrom(VALID_SECRET_32_BYTES);

    private static final long TTL_DEFAULT_MS = 60_000L;
    private static final long TTL_ONE_HOUR_MS = 3_600_000L;
    private static final long TTL_ONE_DAY_MS = 86_400_000L;

    /** 多數案例共用的使用者雛形（依測試覆寫欄位） */
    private static final UserEntity BASE_USER = user(1, "a@b.c", 1, UserRole.USER);

    private static SecretKey hmacKeyFrom(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static UserEntity user(
            Integer userId, String email, Integer groupId, UserRole role) {
        return UserEntity.builder()
                .userId(userId)
                .email(email)
                .groupId(groupId)
                .roles(role)
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

    /** JWT NumericDate 為秒精度；零 TTL 時 exp≈iat，驗證需容忍時鐘誤差。 */
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
    @DisplayName("建構子")
    class ConstructorTests {

        @Test
        @DisplayName("32 bytes 金鑰可成功建立")
        void acceptsMinimumHmacKeyLength() {
            assertThat(provider(VALID_SECRET_32_BYTES, TTL_ONE_HOUR_MS)).isNotNull();
        }

        @Test
        @DisplayName("長於 32 bytes 的金鑰可成功建立")
        void acceptsLongerSecret() {
            assertThat(provider("b".repeat(64), 1L)).isNotNull();
        }

        @Test
        @DisplayName("金鑰不足 32 bytes 時拋出 WeakKeyException")
        void rejectsSecretShorterThan256Bits() {
            String weak = "a".repeat(31);
            assertThatThrownBy(() -> provider(weak, TTL_ONE_HOUR_MS)).isInstanceOf(WeakKeyException.class);
        }

        @Test
        @DisplayName("空字串金鑰拋出 WeakKeyException")
        void rejectsEmptySecret() {
            assertThatThrownBy(() -> provider("", TTL_ONE_HOUR_MS)).isInstanceOf(WeakKeyException.class);
        }

        @Test
        @DisplayName("UTF-8 多字元金鑰：以字元數計需至少 32 字元（ASCII）才滿足位元長度")
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
        @DisplayName("產生的 JWT 可被同一支金鑰驗簽，且 subject、claims 與使用者一致")
        void tokenIsVerifiableAndClaimsMatchUser() {
            UserEntity u = user(42, "test@example.com", 7, UserRole.GROUP_ADMIN);
            Claims claims = claimsFrom(TTL_ONE_DAY_MS, u);

            assertThat(claims.getSubject()).isEqualTo("42");
            assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
            assertThat(claims.get("groupId", Integer.class)).isEqualTo(7);
            assertThat(claims.get("role", String.class)).isEqualTo("GROUP_ADMIN");
        }

        @ParameterizedTest(name = "角色 {0} 寫入 claim role = {1}")
        @MethodSource("allRoles")
        @DisplayName("各 UserRole 的 name() 會寫入 role claim")
        void roleClaimReflectsEnumName(UserRole role, String expectedClaim) {
            UserEntity u = user(1, "a@b.c", 1, role);
            Claims claims = claimsFrom(TTL_DEFAULT_MS, u);
            assertThat(claims.get("role", String.class)).isEqualTo(expectedClaim);
        }

        static Stream<Arguments> allRoles() {
            return Stream.of(
                    Arguments.of(UserRole.SYSTEM_ADMIN, "SYSTEM_ADMIN"),
                    Arguments.of(UserRole.GROUP_ADMIN, "GROUP_ADMIN"),
                    Arguments.of(UserRole.USER, "USER"));
        }

        @Test
        @DisplayName("exp - iat 等於設定的 TTL（毫秒），且為 1000 的倍數時與 JWT 秒精度一致")
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
        @DisplayName("TTL 非整秒時，payload 以秒儲存會四捨五入到整秒，exp-iat 與設定值相差至多約 1 秒")
        void expirationTruncatesToJwtSecondPrecision() {
            long ttlMs = 12_345L;
            Claims claims = claimsFrom(ttlMs, BASE_USER);
            long delta = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
            assertThat(Math.abs(delta - ttlMs)).isLessThanOrEqualTo(1000L);
        }

        @Test
        @DisplayName("expirationTime 為 0 時，exp 與 iat 落在同一秒（驗證時需 clock skew）")
        void zeroTtl_expEqualsIatSameSecond() {
            UserEntity u = user(1, "a@a.a", 1, UserRole.USER);
            Claims claims = parseClaimsWithSkew(accessToken(0L, u), 120);
            assertThat(claims.getExpiration().getTime()).isEqualTo(claims.getIssuedAt().getTime());
        }

        @Test
        @DisplayName("不同使用者產生的 token 字串不同")
        void differentUsersYieldDifferentCompactJws() {
            String t1 = accessToken(TTL_ONE_HOUR_MS, user(1, "a@a.a", 1, UserRole.USER));
            String t2 = accessToken(TTL_ONE_HOUR_MS, user(2, "a@a.a", 1, UserRole.USER));
            assertThat(t1).isNotEqualTo(t2);
        }

        @Test
        @DisplayName("連續兩次產生：跨秒後 iat 不同則 compact JWS 不同（同秒內可能完全相同）")
        void consecutiveGenerationsDistinctAfterSecondBoundary() throws Exception {
            UserEntity u = user(5, "same@same.com", 1, UserRole.USER);
            JwtTokenProvider p = provider(TTL_ONE_HOUR_MS);
            String first = p.generateAccessToken(u);
            Thread.sleep(1100);
            String second = p.generateAccessToken(u);
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("email 含 +、. 等特殊字元仍正確寫入 claim")
        void emailWithSpecialCharacters() {
            String email = "user.name+tag@sub.example.co.jp";
            Claims claims = claimsFrom(TTL_DEFAULT_MS, user(10, email, 2, UserRole.USER));
            assertThat(claims.get("email", String.class)).isEqualTo(email);
        }

        @Test
        @DisplayName("錯誤金鑰無法驗簽")
        void wrongKeyFailsVerification() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            SecretKey otherKey = hmacKeyFrom("b".repeat(32));
            assertThatThrownBy(() -> parseAndVerify(jwt, otherKey)).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("null 使用者拋出 NullPointerException")
        void nullUserThrowsNpe() {
            assertThatThrownBy(() -> provider(TTL_DEFAULT_MS).generateAccessToken(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("roles 為 null 時呼叫 name() 拋出 NullPointerException")
        void nullRoleThrowsNpe() {
            UserEntity u = user(1, "a@b.c", 1, null);
            assertThatThrownBy(() -> accessToken(TTL_DEFAULT_MS, u)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("userId 為 null 時 subject 為字串 \"null\"（String.valueOf 行為）")
        void nullUserId_subjectIsLiteralNullString() {
            UserEntity u = user(null, "a@b.c", 1, UserRole.USER);
            Claims claims = claimsFrom(TTL_DEFAULT_MS, u);
            assertThat(claims.getSubject()).isEqualTo("null");
        }

        @Test
        @DisplayName("極大 expirationTime：JWT 秒精度使 exp-iat 與設定值最多相差約 1 秒")
        void veryLargeTtl_secondPrecisionTruncation() {
            long ttl = Integer.MAX_VALUE;
            Claims claims = claimsFrom(ttl, BASE_USER);
            long delta = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
            assertThat(Math.abs(delta - ttl)).isLessThanOrEqualTo(1000L);
        }

        @Test
        @DisplayName("groupId 為 0 時 claim 仍為 0")
        void groupIdZero() {
            Claims claims = claimsFrom(TTL_DEFAULT_MS, user(1, "a@b.c", 0, UserRole.USER));
            assertThat(claims.get("groupId", Integer.class)).isZero();
        }

        @Test
        @DisplayName("userId 為負數仍如實寫入 subject")
        void negativeUserIdInSubject() {
            Claims claims = claimsFrom(TTL_DEFAULT_MS, user(-1, "a@b.c", 1, UserRole.USER));
            assertThat(claims.getSubject()).isEqualTo("-1");
        }
    }

    @Nested
    @DisplayName("parseValidClaims")
    class ParseValidClaimsTests {

        @Test
        @DisplayName("合法 token 回傳 Claims")
        void validToken_returnsClaims() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(provider(TTL_DEFAULT_MS).parseValidClaims(jwt)).isPresent();
        }

        @Test
        @DisplayName("null、空白、非 JWT 字串回傳 empty")
        void invalidInput_returnsEmpty() {
            JwtTokenProvider provider = provider(TTL_DEFAULT_MS);
            assertThat(provider.parseValidClaims(null)).isEmpty();
            assertThat(provider.parseValidClaims("   ")).isEmpty();
            assertThat(provider.parseValidClaims("not-a-jwt")).isEmpty();
        }

        @Test
        @DisplayName("過期 token 回傳 empty")
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
        @DisplayName("不同金鑰簽署的 token 回傳 empty")
        void wrongKey_returnsEmpty() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            JwtTokenProvider other = provider("b".repeat(32), TTL_DEFAULT_MS);
            assertThat(other.parseValidClaims(jwt)).isEmpty();
        }

        @Test
        @DisplayName("前後空白會 trim 後解析")
        void trimsWhitespace() {
            String jwt = "  " + accessToken(TTL_DEFAULT_MS, BASE_USER) + "  ";
            assertThat(provider(TTL_DEFAULT_MS).parseValidClaims(jwt)).isPresent();
        }
    }

    @Nested
    @DisplayName("resolveAuthentication")
    class ResolveAuthenticationTests {

        @Test
        @DisplayName("Bearer token 回傳 Authentication 與 ROLE_ 前綴")
        void bearerToken_returnsAuthenticationWithRole() {
            UserEntity admin = user(7, "admin@example.com", 1, UserRole.SYSTEM_ADMIN);
            String header = "Bearer " + accessToken(TTL_DEFAULT_MS, admin);

            var authOpt = provider(TTL_DEFAULT_MS).resolveAuthentication(header);
            assertThat(authOpt).isPresent();
            assertThat(authOpt.get().getPrincipal()).isEqualTo("7");
            assertThat(authOpt.get().getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_SYSTEM_ADMIN");
        }

        @Test
        @DisplayName("bearer 前綴大小寫不敏感")
        void bearerPrefix_isCaseInsensitive() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(provider(TTL_DEFAULT_MS).resolveAuthentication("bearer " + jwt)).isPresent();
        }

        @Test
        @DisplayName("缺少 Bearer 前綴回傳 empty")
        void missingBearerPrefix_returnsEmpty() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(provider(TTL_DEFAULT_MS).resolveAuthentication(jwt)).isEmpty();
        }

        @Test
        @DisplayName("無效 role claim 回傳 empty")
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
        @DisplayName("null header 回傳 empty")
        void nullHeader_returnsEmpty() {
            assertThat(provider(TTL_DEFAULT_MS).resolveAuthentication(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("邊界：JWT 結構與驗證")
    class TokenStructureTests {

        @Test
        @DisplayName("compact JWS 為三段 base64url")
        void compactJwsHasThreeParts() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            assertThat(jwt.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("非 JWS 字串無法解析")
        void malformedStringFailsParse() {
            assertThatThrownBy(() -> parseClaims("not-a-jwt")).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("竄改 payload 段後簽章驗證失敗")
        void tamperedPayloadFailsSignature() {
            String jwt = accessToken(TTL_DEFAULT_MS, BASE_USER);
            String[] parts = jwt.split("\\.");
            assertThat(parts).hasSize(3);
            String tampered = parts[0] + "." + parts[1].replace('e', 'f') + "." + parts[2];
            assertThatThrownBy(() -> parseClaims(tampered)).isInstanceOf(JwtException.class);
        }
    }
}
