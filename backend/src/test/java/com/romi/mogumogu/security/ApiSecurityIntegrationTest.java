package com.romi.mogumogu.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.config.JwtTokenProvider;
import com.romi.mogumogu.dto.RegisterRequest;
import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;
import com.romi.mogumogu.repository.user.UserRepository;
import com.romi.mogumogu.testsupport.IntegrationTestFixtures;
import com.romi.mogumogu.testsupport.MemH2DataSourceProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class ApiSecurityIntegrationTest {

    @DynamicPropertySource
    static void memH2(DynamicPropertyRegistry registry) {
        MemH2DataSourceProperties.register(registry, "mogu-api-security-it");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestaurantCategoryRepository restaurantCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    private String bearerToken(UserRole role) {
        UserEntity user = UserEntity.builder()
                .userId(99)
                .groupId(1)
                .roles(role)
                .email("security-it@example.com")
                .build();
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    @Test
    void getRestaurants_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getRestaurants_asUser_returns401WhenUserNotInDatabase() throws Exception {
        mockMvc.perform(get("/restaurants").header(HttpHeaders.AUTHORIZATION, bearerToken(UserRole.USER)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRestaurants_asGroupAdmin_returns401WhenUserNotInDatabase() throws Exception {
        mockMvc.perform(get("/restaurants").header(HttpHeaders.AUTHORIZATION, bearerToken(UserRole.GROUP_ADMIN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRestaurants_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/restaurants").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRestaurants_withMalformedAuthorization_returns401() throws Exception {
        mockMvc.perform(get("/restaurants").header(HttpHeaders.AUTHORIZATION, jwtTokenProvider.generateAccessToken(
                        UserEntity.builder().userId(1).groupId(1).roles(UserRole.USER).email("x@y.z").build())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withoutToken_isPermitted() throws Exception {
        RegisterRequest body = new RegisterRequest();
        body.setUsername("anon");
        body.setEmail("anon-security@example.com");
        body.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(body))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anon-security@example.com"));
    }

    @Test
    void postRestaurants_withoutToken_returns401() throws Exception {
        IntegrationTestFixtures.seedCategoryGroup1(restaurantCategoryRepository);

        String payload = """
                {"groupId":1,"categoryId":1,"restaurantName":"安全測試餐廳"}
                """;

        mockMvc.perform(post("/restaurants")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postRestaurants_asAuthenticatedUser_returns201() throws Exception {
        RestaurantCategoryEntity category = IntegrationTestFixtures.seedCategoryGroup1(restaurantCategoryRepository);

        String payload = """
                {"groupId":1,"categoryId":%d,"restaurantName":"已認證新增"}
                """.formatted(category.getCategoryId());

        mockMvc.perform(post("/restaurants")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(UserRole.USER))
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantName").value("已認證新增"));
    }

    @Test
    void swaggerUi_withoutToken_isPermitted() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @Test
    void getRestaurantCategories_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/restaurant-categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_asGroupAdmin_seedsDefaultCategories() throws Exception {
        RegisterRequest body = new RegisterRequest();
        body.setUsername("admin-seed");
        body.setEmail("admin-seed-categories@example.com");
        body.setPassword("password123");
        body.setRole(UserRole.GROUP_ADMIN.ordinal());

        mockMvc.perform(post("/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(body))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").isNumber());

        var savedUser = userRepository.findByEmailIgnoreCase("admin-seed-categories@example.com").orElseThrow();
        var categories = restaurantCategoryRepository.findByGroupIdOrderByDisplayOrderIdAsc(savedUser.getGroupId());

        assertThat(categories).hasSize(3);
        assertThat(categories.get(0).getCategoryName()).isEqualTo("主食");
        assertThat(categories.get(1).getCategoryName()).isEqualTo("輕食");
        assertThat(categories.get(2).getCategoryName()).isEqualTo("飲料");
    }
}
