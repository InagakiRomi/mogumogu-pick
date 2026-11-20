package com.romi.mogumogu.response;

import com.romi.mogumogu.constant.DateTimePatternConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    @Schema(description = "使用者 ID")
    private Integer userId;

    @Schema(description = "群組 ID")
    private Integer groupId;

    @Schema(description = "群組內排序 ID")
    private Integer displayOrderId;

    @Schema(description = "角色代碼")
    private Integer role;

    @Schema(description = "使用者名稱")
    private String username;

    @Schema(description = "電子郵件")
    private String email;

    @Schema(description = "建立時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    private Date createdAt;

    @Schema(description = "更新時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    private Date updatedAt;
}
