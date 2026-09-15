package com.romi.mogumogu.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupProfileResponse {
    @Schema(description = "群組 ID")
    private Integer groupId;

    @Schema(description = "群組名稱")
    private String groupName;
}
