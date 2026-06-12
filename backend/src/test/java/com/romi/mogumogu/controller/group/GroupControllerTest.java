package com.romi.mogumogu.controller.group;

import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.Response.GroupMemberResponse;
import com.romi.mogumogu.Response.GroupProfileResponse;
import com.romi.mogumogu.dto.AddGroupMemberDto;
import com.romi.mogumogu.dto.TransferGroupAdminDto;
import com.romi.mogumogu.dto.UpdateGroupNameDto;
import com.romi.mogumogu.exception.GlobalExceptionHandler;
import com.romi.mogumogu.service.group.GroupService;
import com.romi.mogumogu.testsupport.TestSecurityConfig;

@WebMvcTest(controllers = GroupController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class GroupControllerTest {
    private static final String GROUPS_MY_PATH = "/groups/my";
    private static final String GROUPS_MY_MEMBERS_PATH = "/groups/my/members";
    private static final String GROUPS_MY_TRANSFER_ADMIN_PATH = "/groups/my/transfer-admin";
    private static final String GROUPS_MY_LEAVE_PATH = "/groups/my/leave";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    private static final String CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String CODE_BAD_REQUEST = "BAD_REQUEST";
    private static final String TRUNCATED_ADD_MEMBER_JSON = "{\"email\":\"new@example.com";
    private static final String TRUNCATED_TRANSFER_ADMIN_JSON = "{\"targetUserId\":20";
    private static final String TRUNCATED_UPDATE_GROUP_NAME_JSON = "{\"groupName\":\"abc\"";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupService groupService;

    @Nested
    class GetMyGroupMembers {
        @Test
        void success_returnsMembers() throws Exception {
            when(groupService.getMyGroupMembers()).thenReturn(List.of(
                    buildMember(1, 1, 1, 1, "admin"),
                    buildMember(2, 1, 2, 2, "user")));

            mockMvc.perform(get(GROUPS_MY_MEMBERS_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].userId").value(1))
                    .andExpect(jsonPath("$[0].groupId").value(1))
                    .andExpect(jsonPath("$[0].displayOrderId").value(1))
                    .andExpect(jsonPath("$[0].role").value(1))
                    .andExpect(jsonPath("$[0].username").value("admin"))
                    .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                    .andExpect(jsonPath("$[1].userId").value(2))
                    .andExpect(jsonPath("$[1].role").value(2));

            verify(groupService).getMyGroupMembers();
        }

        @Test
        void success_returnsEmptyList() throws Exception {
            when(groupService.getMyGroupMembers()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(GROUPS_MY_MEMBERS_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(groupService).getMyGroupMembers();
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(groupService.getMyGroupMembers())
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(mockMvc.perform(get(GROUPS_MY_MEMBERS_PATH)),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_MEMBERS_PATH, "User is not in a group");

            verify(groupService).getMyGroupMembers();
        }
    }

    @Nested
    class AddGroupMember {
        @Test
        void success_returns201WithMember() throws Exception {
            AddGroupMemberDto request = AddGroupMemberDto.builder().email("new-user@example.com").build();
            when(groupService.addGroupMember(any(AddGroupMemberDto.class)))
                    .thenReturn(buildMember(10, 1, 3, 2, "new-user"));

            performPostMembers(request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(10))
                    .andExpect(jsonPath("$.groupId").value(1))
                    .andExpect(jsonPath("$.displayOrderId").value(3))
                    .andExpect(jsonPath("$.role").value(2))
                    .andExpect(jsonPath("$.username").value("new-user"))
                    .andExpect(jsonPath("$.email").value("new-user@example.com"));

            verify(groupService).addGroupMember(any(AddGroupMemberDto.class));
        }

        @Test
        void validationFailed_returns400AndSkipsService() throws Exception {
            AddGroupMemberDto invalid = AddGroupMemberDto.builder().email("").build();
            assertErrorResponseContains(performPostMembers(invalid),
                    400, CODE_BAD_REQUEST, GROUPS_MY_MEMBERS_PATH,
                    "email is required");
            verifyNoInteractions(groupService);
        }

        @Test
        void invalidEmailFormat_returns400AndSkipsService() throws Exception {
            AddGroupMemberDto invalid = AddGroupMemberDto.builder().email("not-an-email").build();
            assertBadRequestValidation(performPostMembers(invalid), GROUPS_MY_MEMBERS_PATH,
                    "email must be a valid email address");
            verifyNoInteractions(groupService);
        }

        @Test
        void nullEmailViaRawJson_returns400AndSkipsService() throws Exception {
            assertBadRequestValidation(performPostMembersRaw("{\"email\":null}"),
                    GROUPS_MY_MEMBERS_PATH, "email is required");
            verifyNoInteractions(groupService);
        }

        @Test
        void missingBody_returns500AndSkipsService() throws Exception {
            assertMissingJsonBodyError(GROUPS_MY_MEMBERS_PATH);
        }

        @Test
        void invalidJson_returns500AndSkipsService() throws Exception {
            assertJsonParseError(GROUPS_MY_MEMBERS_PATH,
                    performPostMembersRaw(TRUNCATED_ADD_MEMBER_JSON));
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(groupService.addGroupMember(any(AddGroupMemberDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(performPostMembers(AddGroupMemberDto.builder().email("new-user@example.com").build()),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_MEMBERS_PATH, "User is not in a group");

            verify(groupService).addGroupMember(any(AddGroupMemberDto.class));
        }

        @Test
        void forbidden_returns403() throws Exception {
            when(groupService.addGroupMember(any(AddGroupMemberDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin can perform this action"));

            assertErrorResponse(performPostMembers(AddGroupMemberDto.builder().email("new-user@example.com").build()),
                    HttpStatus.FORBIDDEN, GROUPS_MY_MEMBERS_PATH, "Only group admin can perform this action");

            verify(groupService).addGroupMember(any(AddGroupMemberDto.class));
        }

        @Test
        void targetNotFound_returns404() throws Exception {
            when(groupService.addGroupMember(any(AddGroupMemberDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

            assertErrorResponse(performPostMembers(AddGroupMemberDto.builder().email("unknown@example.com").build()),
                    HttpStatus.NOT_FOUND, GROUPS_MY_MEMBERS_PATH, "Target user not found");

            verify(groupService).addGroupMember(any(AddGroupMemberDto.class));
        }

        @Test
        void alreadyInGroup_returns400() throws Exception {
            when(groupService.addGroupMember(any(AddGroupMemberDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already in this group"));

            assertErrorResponse(performPostMembers(AddGroupMemberDto.builder().email("admin@example.com").build()),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_MEMBERS_PATH, "You are already in this group");

            verify(groupService).addGroupMember(any(AddGroupMemberDto.class));
        }

        @Test
        void targetAlreadyInGroup_returns409() throws Exception {
            when(groupService.addGroupMember(any(AddGroupMemberDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Target user already belongs to a group"));

            assertErrorResponse(performPostMembers(AddGroupMemberDto.builder().email("member@example.com").build()),
                    HttpStatus.CONFLICT, GROUPS_MY_MEMBERS_PATH, "Target user already belongs to a group");

            verify(groupService).addGroupMember(any(AddGroupMemberDto.class));
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(groupService.addGroupMember(any(AddGroupMemberDto.class)))
                    .thenThrow(new RuntimeException("Add group member failed"));

            assertErrorResponse(performPostMembers(AddGroupMemberDto.builder().email("new-user@example.com").build()),
                    HttpStatus.INTERNAL_SERVER_ERROR, GROUPS_MY_MEMBERS_PATH, "Add group member failed");

            verify(groupService).addGroupMember(any(AddGroupMemberDto.class));
        }
    }

    @Nested
    class RemoveGroupMember {
        @Test
        void success_returns204() throws Exception {
            mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 9))
                    .andExpect(status().isNoContent());

            verify(groupService).removeGroupMember(9);
        }

        @Test
        void notInGroup_returns400() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"))
                    .when(groupService).removeGroupMember(9);

            assertErrorResponse(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 9)),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_MEMBERS_PATH + "/9", "User is not in a group");

            verify(groupService).removeGroupMember(9);
        }

        @Test
        void forbidden_returns403() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin can perform this action"))
                    .when(groupService).removeGroupMember(9);

            assertErrorResponse(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 9)),
                    HttpStatus.FORBIDDEN, GROUPS_MY_MEMBERS_PATH + "/9", "Only group admin can perform this action");

            verify(groupService).removeGroupMember(9);
        }

        @Test
        void cannotRemoveSelf_returns400() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove yourself via this endpoint"))
                    .when(groupService).removeGroupMember(1);

            assertErrorResponse(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 1)),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_MEMBERS_PATH + "/1", "Cannot remove yourself via this endpoint");

            verify(groupService).removeGroupMember(1);
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"))
                    .when(groupService).removeGroupMember(99);

            assertErrorResponse(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 99)),
                    HttpStatus.NOT_FOUND, GROUPS_MY_MEMBERS_PATH + "/99", "Target user not found");

            verify(groupService).removeGroupMember(99);
        }

        @Test
        void targetNotInGroup_returns403() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Target user is not in your group"))
                    .when(groupService).removeGroupMember(9);

            assertErrorResponse(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 9)),
                    HttpStatus.FORBIDDEN, GROUPS_MY_MEMBERS_PATH + "/9", "Target user is not in your group");

            verify(groupService).removeGroupMember(9);
        }

        @Test
        void cannotRemoveAdmin_returns400() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove current group admin"))
                    .when(groupService).removeGroupMember(2);

            assertErrorResponse(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 2)),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_MEMBERS_PATH + "/2", "Cannot remove current group admin");

            verify(groupService).removeGroupMember(2);
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", "bad-id")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, GROUPS_MY_MEMBERS_PATH + "/bad-id",
                    "Failed to convert value of type");
            verifyNoInteractions(groupService);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            doThrow(new RuntimeException("Remove group member failed"))
                    .when(groupService).removeGroupMember(9);

            assertErrorResponse(mockMvc.perform(delete(GROUPS_MY_MEMBERS_PATH + "/{userId}", 9)),
                    HttpStatus.INTERNAL_SERVER_ERROR, GROUPS_MY_MEMBERS_PATH + "/9", "Remove group member failed");

            verify(groupService).removeGroupMember(9);
        }
    }

    @Nested
    class TransferGroupAdmin {
        @Test
        void success_returns200WithTargetAsAdmin() throws Exception {
            TransferGroupAdminDto request = TransferGroupAdminDto.builder().targetUserId(20).build();
            when(groupService.transferGroupAdmin(any(TransferGroupAdminDto.class)))
                    .thenReturn(buildMember(20, 1, 2, 1, "new-admin"));

            performTransferAdmin(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(20))
                    .andExpect(jsonPath("$.groupId").value(1))
                    .andExpect(jsonPath("$.role").value(1))
                    .andExpect(jsonPath("$.username").value("new-admin"));

            verify(groupService).transferGroupAdmin(any(TransferGroupAdminDto.class));
        }

        @Test
        void validationFailed_returns400AndSkipsService() throws Exception {
            assertErrorResponseContains(
                    performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(0).build()),
                    400, CODE_BAD_REQUEST, GROUPS_MY_TRANSFER_ADMIN_PATH, "targetUserId must be greater than 0");
            verifyNoInteractions(groupService);
        }

        @Test
        void negativeTargetUserId_returns400AndSkipsService() throws Exception {
            assertBadRequestValidation(
                    performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(-1).build()),
                    GROUPS_MY_TRANSFER_ADMIN_PATH, "targetUserId must be greater than 0");
            verifyNoInteractions(groupService);
        }

        @Test
        void nullTargetUserIdViaRawJson_returns400AndSkipsService() throws Exception {
            assertBadRequestValidation(performTransferAdminRaw("{\"targetUserId\":null}"),
                    GROUPS_MY_TRANSFER_ADMIN_PATH, "targetUserId is required");
            verifyNoInteractions(groupService);
        }

        @Test
        void missingBody_returns500AndSkipsService() throws Exception {
            assertMissingJsonBodyError(GROUPS_MY_TRANSFER_ADMIN_PATH);
        }

        @Test
        void invalidJson_returns500AndSkipsService() throws Exception {
            assertJsonParseError(GROUPS_MY_TRANSFER_ADMIN_PATH,
                    performTransferAdminRaw(TRUNCATED_TRANSFER_ADMIN_JSON));
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(groupService.transferGroupAdmin(any(TransferGroupAdminDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(20).build()),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_TRANSFER_ADMIN_PATH, "User is not in a group");

            verify(groupService).transferGroupAdmin(any(TransferGroupAdminDto.class));
        }

        @Test
        void forbidden_returns403() throws Exception {
            when(groupService.transferGroupAdmin(any(TransferGroupAdminDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin can perform this action"));

            assertErrorResponse(performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(20).build()),
                    HttpStatus.FORBIDDEN, GROUPS_MY_TRANSFER_ADMIN_PATH, "Only group admin can perform this action");

            verify(groupService).transferGroupAdmin(any(TransferGroupAdminDto.class));
        }

        @Test
        void targetNotFound_returns404() throws Exception {
            when(groupService.transferGroupAdmin(any(TransferGroupAdminDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

            assertErrorResponse(performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(999).build()),
                    HttpStatus.NOT_FOUND, GROUPS_MY_TRANSFER_ADMIN_PATH, "Target user not found");

            verify(groupService).transferGroupAdmin(any(TransferGroupAdminDto.class));
        }

        @Test
        void targetNotInGroup_returns403() throws Exception {
            when(groupService.transferGroupAdmin(any(TransferGroupAdminDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Target user is not in your group"));

            assertErrorResponse(performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(20).build()),
                    HttpStatus.FORBIDDEN, GROUPS_MY_TRANSFER_ADMIN_PATH, "Target user is not in your group");

            verify(groupService).transferGroupAdmin(any(TransferGroupAdminDto.class));
        }

        @Test
        void alreadyAdmin_returns400() throws Exception {
            when(groupService.transferGroupAdmin(any(TransferGroupAdminDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is already group admin"));

            assertErrorResponse(performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(1).build()),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_TRANSFER_ADMIN_PATH, "Target user is already group admin");

            verify(groupService).transferGroupAdmin(any(TransferGroupAdminDto.class));
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(groupService.transferGroupAdmin(any(TransferGroupAdminDto.class)))
                    .thenThrow(new RuntimeException("Transfer group admin failed"));

            assertErrorResponse(performTransferAdmin(TransferGroupAdminDto.builder().targetUserId(20).build()),
                    HttpStatus.INTERNAL_SERVER_ERROR, GROUPS_MY_TRANSFER_ADMIN_PATH, "Transfer group admin failed");

            verify(groupService).transferGroupAdmin(any(TransferGroupAdminDto.class));
        }
    }

    @Nested
    class GetGroupProfile {
        @Test
        void success_returnsProfile() throws Exception {
            when(groupService.getMyGroupProfile())
                    .thenReturn(GroupProfileResponse.builder().groupId(1).groupName("午餐小隊").build());

            mockMvc.perform(get(GROUPS_MY_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupId").value(1))
                    .andExpect(jsonPath("$.groupName").value("午餐小隊"));

            verify(groupService).getMyGroupProfile();
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(groupService.getMyGroupProfile())
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(mockMvc.perform(get(GROUPS_MY_PATH)),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_PATH, "User is not in a group");

            verify(groupService).getMyGroupProfile();
        }

        @Test
        void groupNotFound_returns404() throws Exception {
            when(groupService.getMyGroupProfile())
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

            assertErrorResponse(mockMvc.perform(get(GROUPS_MY_PATH)),
                    HttpStatus.NOT_FOUND, GROUPS_MY_PATH, "Group not found");

            verify(groupService).getMyGroupProfile();
        }
    }

    @Nested
    class UpdateGroupProfile {
        @Test
        void success_returnsUpdatedProfile() throws Exception {
            UpdateGroupNameDto request = UpdateGroupNameDto.builder().groupName("新群組名").build();
            when(groupService.updateMyGroupName(any(UpdateGroupNameDto.class)))
                    .thenReturn(GroupProfileResponse.builder().groupId(1).groupName("新群組名").build());

            performPatchProfile(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupId").value(1))
                    .andExpect(jsonPath("$.groupName").value("新群組名"));

            verify(groupService).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void blankName_returns400AndSkipsService() throws Exception {
            UpdateGroupNameDto request = UpdateGroupNameDto.builder().groupName(" ").build();
            assertErrorResponseContains(performPatchProfile(request),
                    400, CODE_BAD_REQUEST, GROUPS_MY_PATH, "groupName is required");
            verify(groupService, never()).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void emptyName_returns400AndSkipsService() throws Exception {
            assertBadRequestValidation(performPatchProfile(UpdateGroupNameDto.builder().groupName("").build()),
                    GROUPS_MY_PATH, "groupName is required");
            verify(groupService, never()).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void nullGroupNameViaRawJson_returns400AndSkipsService() throws Exception {
            assertBadRequestValidation(performPatchProfileRaw("{\"groupName\":null}"),
                    GROUPS_MY_PATH, "groupName is required");
            verify(groupService, never()).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void groupNameTooLong_returns400AndSkipsService() throws Exception {
            UpdateGroupNameDto request = UpdateGroupNameDto.builder().groupName("x".repeat(65)).build();
            assertBadRequestValidation(performPatchProfile(request),
                    GROUPS_MY_PATH, "groupName size is out of allowed range");
            verify(groupService, never()).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void missingBody_returns500AndSkipsService() throws Exception {
            assertErrorResponseContains(mockMvc.perform(patch(GROUPS_MY_PATH).contentType(CONTENT_TYPE_JSON)),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, GROUPS_MY_PATH,
                    "Required request body is missing");
            verify(groupService, never()).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void invalidJson_returns500AndSkipsService() throws Exception {
            assertJsonParseError(GROUPS_MY_PATH,
                    performPatchProfileRaw(TRUNCATED_UPDATE_GROUP_NAME_JSON));
            verify(groupService, never()).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(groupService.updateMyGroupName(any(UpdateGroupNameDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(performPatchProfile(UpdateGroupNameDto.builder().groupName("新名稱").build()),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_PATH, "User is not in a group");

            verify(groupService).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void forbidden_returns403() throws Exception {
            when(groupService.updateMyGroupName(any(UpdateGroupNameDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin can perform this action"));

            assertErrorResponse(performPatchProfile(UpdateGroupNameDto.builder().groupName("新名稱").build()),
                    HttpStatus.FORBIDDEN, GROUPS_MY_PATH, "Only group admin can perform this action");

            verify(groupService).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void groupNotFound_returns404() throws Exception {
            when(groupService.updateMyGroupName(any(UpdateGroupNameDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

            assertErrorResponse(performPatchProfile(UpdateGroupNameDto.builder().groupName("新名稱").build()),
                    HttpStatus.NOT_FOUND, GROUPS_MY_PATH, "Group not found");

            verify(groupService).updateMyGroupName(any(UpdateGroupNameDto.class));
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(groupService.updateMyGroupName(any(UpdateGroupNameDto.class)))
                    .thenThrow(new RuntimeException("Update group name failed"));

            assertErrorResponse(performPatchProfile(UpdateGroupNameDto.builder().groupName("新名稱").build()),
                    HttpStatus.INTERNAL_SERVER_ERROR, GROUPS_MY_PATH, "Update group name failed");

            verify(groupService).updateMyGroupName(any(UpdateGroupNameDto.class));
        }
    }

    @Nested
    class LeaveGroup {
        @Test
        void success_returns204() throws Exception {
            mockMvc.perform(post(GROUPS_MY_LEAVE_PATH))
                    .andExpect(status().isNoContent());
            verify(groupService).leaveMyGroup();
        }

        @Test
        void notInGroup_returns400() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"))
                    .when(groupService).leaveMyGroup();

            assertErrorResponse(mockMvc.perform(post(GROUPS_MY_LEAVE_PATH)),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_LEAVE_PATH, "User is not in a group");
            verify(groupService).leaveMyGroup();
        }

        @Test
        void adminNeedTransfer_returns400() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please transfer group admin before leaving"))
                    .when(groupService).leaveMyGroup();

            assertErrorResponse(mockMvc.perform(post(GROUPS_MY_LEAVE_PATH)),
                    HttpStatus.BAD_REQUEST, GROUPS_MY_LEAVE_PATH, "Please transfer group admin before leaving");
            verify(groupService).leaveMyGroup();
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            doThrow(new RuntimeException("Leave group failed"))
                    .when(groupService).leaveMyGroup();

            assertErrorResponse(mockMvc.perform(post(GROUPS_MY_LEAVE_PATH)),
                    HttpStatus.INTERNAL_SERVER_ERROR, GROUPS_MY_LEAVE_PATH, "Leave group failed");
            verify(groupService).leaveMyGroup();
        }
    }

    private GroupMemberResponse buildMember(Integer userId, Integer groupId, Integer orderId, Integer role,
            String username) {
        Date now = new Date();
        return GroupMemberResponse.builder()
                .userId(userId)
                .groupId(groupId)
                .displayOrderId(orderId)
                .role(role)
                .username(username)
                .email(username + "@example.com")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void assertBadRequestValidation(ResultActions resultActions, String path, String... messageParts)
            throws Exception {
        String responseJson = resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(CODE_BAD_REQUEST))
                .andExpect(jsonPath("$.path").value(path))
                .andReturn()
                .getResponse()
                .getContentAsString();
        for (String part : messageParts) {
            assertMessageContains(responseJson, part);
        }
    }

    private void assertMessageContains(String responseJson, String messagePart) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode messageNode = rootNode.get("message");
        assertNotNull(messageNode, "message field should exist");
        assertTrue(messageNode.asText().contains(messagePart),
                "message should contain: " + messagePart + " but was: " + messageNode.asText());
    }

    private void assertMissingJsonBodyError(@NonNull String path) throws Exception {
        assertErrorResponseContains(
                mockMvc.perform(post(path).contentType(CONTENT_TYPE_JSON)),
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                path,
                "Required request body is missing");
        verifyNoInteractions(groupService);
    }

    private void assertJsonParseError(@NonNull String path, ResultActions truncatedJsonPost) throws Exception {
        assertErrorResponseContains(
                truncatedJsonPost,
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                path,
                "JSON parse error");
        verifyNoInteractions(groupService);
    }

    private ResultActions performPostMembers(AddGroupMemberDto request) throws Exception {
        return mockMvc.perform(post(GROUPS_MY_MEMBERS_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performPostMembersRaw(String payload) throws Exception {
        return mockMvc.perform(post(GROUPS_MY_MEMBERS_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(payload)));
    }

    private ResultActions performTransferAdmin(TransferGroupAdminDto request) throws Exception {
        return mockMvc.perform(post(GROUPS_MY_TRANSFER_ADMIN_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performTransferAdminRaw(String payload) throws Exception {
        return mockMvc.perform(post(GROUPS_MY_TRANSFER_ADMIN_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(payload)));
    }

    private ResultActions performPatchProfile(UpdateGroupNameDto request) throws Exception {
        return mockMvc.perform(patch(GROUPS_MY_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performPatchProfileRaw(String payload) throws Exception {
        return mockMvc.perform(patch(GROUPS_MY_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(payload)));
    }
}
