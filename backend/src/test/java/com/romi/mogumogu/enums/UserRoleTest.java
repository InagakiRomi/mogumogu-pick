package com.romi.mogumogu.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UserRole")
class UserRoleTest {

    @ParameterizedTest
    @ValueSource(strings = {"SYSTEM_ADMIN", "GROUP_ADMIN", "USER"})
    @DisplayName("合法名稱可解析")
    void fromName_validNames(String name) {
        assertThat(UserRole.fromName(name)).isPresent();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "UNKNOWN", "ADMIN"})
    @DisplayName("無效名稱回傳 empty")
    void fromName_invalidNames(String name) {
        assertThat(UserRole.fromName(name)).isEmpty();
    }

    @Test
    @DisplayName("fromName 區分大小寫（enum 名稱須完全匹配）")
    void fromName_isCaseSensitiveForEnumNames() {
        assertThat(UserRole.fromName("user")).isEmpty();
        assertThat(UserRole.fromName("USER")).contains(UserRole.USER);
    }
}
