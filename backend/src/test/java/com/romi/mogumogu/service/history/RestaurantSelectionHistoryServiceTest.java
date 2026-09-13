package com.romi.mogumogu.service.history;

import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.history.RestaurantSelectionHistoryRepository;
import com.romi.mogumogu.repository.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantSelectionHistoryServiceTest {

    @Mock
    private RestaurantSelectionHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RestaurantSelectionHistoryService service;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clearMyGroupSelectionHistory_groupAdmin_deletesAndResetsSequence() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buildUser(10, UserRole.GROUP_ADMIN)));

        service.clearMyGroupSelectionHistory();

        InOrder inOrder = inOrder(historyRepository);
        inOrder.verify(historyRepository).deleteByGroupId(10);
        inOrder.verify(historyRepository).flush();
        inOrder.verify(historyRepository).resetHistoryIdSequence();
    }

    @Test
    void clearMyGroupSelectionHistory_regularUser_returns403WithoutDeleting() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buildUser(10, UserRole.USER)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                service::clearMyGroupSelectionHistory);

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(historyRepository, never()).deleteByGroupId(10);
        verify(historyRepository, never()).resetHistoryIdSequence();
    }

    private UserEntity buildUser(Integer groupId, UserRole role) {
        return UserEntity.builder()
                .userId(1)
                .groupId(groupId)
                .roles(role)
                .build();
    }
}
