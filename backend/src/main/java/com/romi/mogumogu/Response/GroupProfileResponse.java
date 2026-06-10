package com.romi.mogumogu.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupProfileResponse {
    /** 群組 ID */
    private Integer groupId;

    /** 群組名稱 */
    private String groupName;
}
