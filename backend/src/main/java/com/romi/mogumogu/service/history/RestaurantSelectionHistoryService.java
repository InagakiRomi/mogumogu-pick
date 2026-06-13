package com.romi.mogumogu.service.history;

import com.romi.mogumogu.Response.RestaurantListResponse;
import com.romi.mogumogu.Response.SelectionHistoryResponse;
import com.romi.mogumogu.dto.GetSelectionHistoryQuery;
import com.romi.mogumogu.entity.history.RestaurantSelectionHistoryEntity;
import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.RestaurantSort;
import com.romi.mogumogu.repository.history.RestaurantSelectionHistoryRepository;
import com.romi.mogumogu.repository.user.UserRepository;
import com.romi.mogumogu.security.SecurityUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RestaurantSelectionHistoryService {

    private final RestaurantSelectionHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public RestaurantSelectionHistoryService(
            RestaurantSelectionHistoryRepository historyRepository,
            UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    /** 記錄餐廳選取歷史 */
    @Transactional
    public void recordSelection(Integer groupId, RestaurantEntity restaurant, Date selectedAt) {
        RestaurantSelectionHistoryEntity entity = Objects.requireNonNull(RestaurantSelectionHistoryEntity.builder()
                .groupId(groupId)
                .restaurant(restaurant)
                .selectedAt(selectedAt)
                .build());
        historyRepository.save(entity);
    }

    /** 刪除餐廳底下的所有選取歷史 */
    @Transactional
    public void deleteByRestaurantId(Integer restaurantId) {
        historyRepository.deleteByRestaurant_RestaurantId(restaurantId);
    }

    /** 取得自己所屬群組的餐廳抽選歷史紀錄 */
    public RestaurantListResponse<SelectionHistoryResponse> getMyGroupSelectionHistory(
            GetSelectionHistoryQuery queryParams) {
        Integer groupId = resolveCurrentUserGroupId();
        Integer page = queryParams.getPage();
        Integer limit = queryParams.getLimit();

        RestaurantSort.SortOrder safeSort = queryParams.getSort() != null
                ? queryParams.getSort()
                : RestaurantSort.SortOrder.DESC;

        Sort.Direction sortDirection = Objects.requireNonNull(safeSort.getSortDirection());
        Sort jpaSort = Sort.by(sortDirection, "selectedAt");

        Specification<RestaurantSelectionHistoryEntity> spec = (root, query, cb) ->
                cb.equal(root.get("groupId"), groupId);

        // 建立分頁規則
        Pageable pageable = PageRequest.of(page - 1, limit, jpaSort);

        // 取得餐廳抽選歷史紀錄
        Page<RestaurantSelectionHistoryEntity> pageResult = historyRepository.findAll(spec, pageable);

        // 轉換為回傳 DTO
        List<SelectionHistoryResponse> data = pageResult.getContent().stream()
                .map(SelectionHistoryResponse::from)
                .toList();

        return RestaurantListResponse.of(data, page, limit, pageResult.getTotalElements());
    }

    /** 取得目前登入使用者的群組 ID */
    private Integer resolveCurrentUserGroupId() {
        // 取得目前登入使用者的 ID
        Integer currentUserId = Objects.requireNonNull(SecurityUtils.getCurrentUserId());

        // 取得使用者資料
        Optional<UserEntity> userOpt = userRepository.findById(currentUserId);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        // 取得使用者資料
        UserEntity user = userOpt.get();

        // 檢查使用者是否屬於群組
        if (user.getGroupId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group");
        }

        return user.getGroupId();
    }
}
