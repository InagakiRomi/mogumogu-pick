package com.romi.mogumogu.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HealthResponse {
    /** 服務狀態 */
    private String status;
}
