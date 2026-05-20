package com.romi.mogumogu.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.config.JwtTokenProvider;
import com.romi.mogumogu.enums.UserRole;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

        /** 未登入或權限不足時，回傳與 API 相同格式的 JSON 錯誤 */
        @Bean
        RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
                return new RestAuthenticationEntryPoint(objectMapper);
        }

        /** 定義請求如何被驗證、授權與過濾 */
        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtTokenProvider jwtTokenProvider,
                        RestAuthenticationEntryPoint restAuthenticationEntryPoint)
                        throws Exception {
                // 從 Authorization header 解析 JWT，寫入 SecurityContext
                JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider);
                http
                                // 關閉 CSRF：REST API 以 Bearer token 驗證，不用表單與 Session
                                .csrf(AbstractHttpConfigurer::disable)
                                // 不建立 Session；身分只靠每次請求帶來的 JWT
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // 401（未認證）、403（無權限）改回 JSON，不用預設登入頁或 HTML
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(restAuthenticationEntryPoint)
                                                .accessDeniedHandler(restAuthenticationEntryPoint))
                                // 依路徑決定誰可以存取
                                .authorizeHttpRequests(auth -> auth
                                                // API 文件與 Swagger UI
                                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                // permitAll：略過認證，匿名即可存取
                                                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login")
                                                .permitAll()
                                                // hasRole：須具指定角色（依路徑與 HTTP 方法匹配）
                                                .requestMatchers(HttpMethod.GET, "/restaurants")
                                                .hasRole(UserRole.SYSTEM_ADMIN.name())

                                                // anyRequest：前述規則未涵蓋的請求，一律須已認證
                                                .anyRequest()
                                                .authenticated())
                                // 在預設登入過濾器前執行，先還原 JWT 身分
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}
