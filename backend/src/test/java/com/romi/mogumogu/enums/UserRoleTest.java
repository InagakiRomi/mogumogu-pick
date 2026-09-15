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
    @ValueSource(strings = {"GROUP_ADMIN", "USER"})
    @DisplayName("Valid names can be parsed")
    void fromName_validNames(String name) {
        assertThat(UserRole.fromName(name)).isPresent();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "UNKNOWN", "ADMIN"})
    @DisplayName("Invalid names return empty")
    void fromName_invalidNames(String name) {
        assertThat(UserRole.fromName(name)).isEmpty();
    }

    @Test
    @DisplayName("fromName is case-sensitive and requires an exact enum name")
    void fromName_isCaseSensitiveForEnumNames() {
        assertThat(UserRole.fromName("user")).isEmpty();
        assertThat(UserRole.fromName("USER")).contains(UserRole.USER);
    }
}
