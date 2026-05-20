package com.romi.mogumogu.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

@DisplayName("RestaurantSort")
class RestaurantSortTest {

    @ParameterizedTest
    @ValueSource(strings = {"DESC", "desc", " Desc "})
    @DisplayName("SortOrder.from 辨識 DESC")
    void sortOrderFrom_recognizesDesc(String value) {
        assertThat(RestaurantSort.SortOrder.from(value)).isEqualTo(RestaurantSort.SortOrder.DESC);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ASC", "asc", "invalid"})
    @DisplayName("SortOrder.from 預設或回傳 ASC")
    void sortOrderFrom_defaultsToAsc(String value) {
        assertThat(RestaurantSort.SortOrder.from(value)).isEqualTo(RestaurantSort.SortOrder.ASC);
    }

    @Test
    @DisplayName("SortBy 對應正確 JPA 屬性名稱")
    void sortBy_hasExpectedSortProperty() {
        assertThat(RestaurantSort.SortBy.RESTAURANT_ID.getSortProperty()).isEqualTo("restaurantId");
        assertThat(RestaurantSort.SortBy.SELECTED_COUNT.getSortProperty()).isEqualTo("selectedCount");
        assertThat(RestaurantSort.SortBy.DISPLAY_ORDER_ID.getSortProperty()).isEqualTo("displayOrderId");
    }

    @Test
    @DisplayName("SortOrder 對應 Spring Sort.Direction")
    void sortOrder_hasExpectedDirection() {
        assertThat(RestaurantSort.SortOrder.ASC.getSortDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(RestaurantSort.SortOrder.DESC.getSortDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
