-- 移除已廢止的 SYSTEM_ADMIN 角色與帳號，並重新對齊角色代碼。
-- 舊代碼：0=SYSTEM_ADMIN, 1=GROUP_ADMIN, 2=USER
-- 新代碼：0=GROUP_ADMIN, 1=USER

-- 刪除系統管理員帳號
DELETE FROM "user"
WHERE roles = 0;

-- 角色代碼平移：GROUP_ADMIN 1->0, USER 2->1
UPDATE "user"
SET roles = CASE
    WHEN roles = 1 THEN 0
    WHEN roles = 2 THEN 1
    ELSE roles
END;
