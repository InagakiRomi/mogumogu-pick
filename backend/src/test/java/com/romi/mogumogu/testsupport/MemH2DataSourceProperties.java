package com.romi.mogumogu.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;

/** 整合測試使用獨立記憶體 H2，避免本機檔案庫 Flyway 校驗或種子資料衝突。 */
public final class MemH2DataSourceProperties {

    private MemH2DataSourceProperties() {}

    public static void register(DynamicPropertyRegistry registry, String memDbName) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:" + memDbName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    }
}
