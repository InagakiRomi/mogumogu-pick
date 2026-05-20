package com.romi.mogumogu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.romi.mogumogu.testsupport.MemH2DataSourceProperties;

@SpringBootTest
@ActiveProfiles("h2")
class MogumoguApplicationTests {

    @DynamicPropertySource
    static void memH2(DynamicPropertyRegistry registry) {
        MemH2DataSourceProperties.register(registry, "mogu-ctx-loads");
    }

    @Test
    void contextLoads() {}
}
