package com.romi.mogumogu.repository.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.romi.mogumogu.entity.group.GroupEntity;

public interface GroupRepository extends JpaRepository<GroupEntity, Integer> {

    /** 查詢最大群組 ID */
    @Query("SELECT COALESCE(MAX(g.groupId), 0) FROM GroupEntity g")
    Integer findMaxGroupId();
}
