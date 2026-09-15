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
public class LoginResponse {
    @Schema(description = "使用者 ID")
    private Integer userId;

    @Schema(description = "所屬群組 ID")
    private Integer groupId;

    @Schema(description = "角色")
    private Integer role;

    @Schema(description = "使用者名稱")
    private String username;

    @Schema(description = "電子郵件")
    private String email;

    @Schema(description = "帳號建立時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    private Date createdAt;

    @Schema(description = "帳號最後更新時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    private Date updatedAt;

    @Schema(description = "JWT access token")
    private String token;
}
