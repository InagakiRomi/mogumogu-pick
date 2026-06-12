INSERT INTO restaurant_category (group_id, display_order_id, category_name, created_at)
SELECT g.group_id, 1, '主食', CURRENT_TIMESTAMP
FROM user_group g
WHERE NOT EXISTS (SELECT 1 FROM restaurant_category c WHERE c.group_id = g.group_id)
UNION ALL
SELECT g.group_id, 2, '輕食', CURRENT_TIMESTAMP
FROM user_group g
WHERE NOT EXISTS (SELECT 1 FROM restaurant_category c WHERE c.group_id = g.group_id)
UNION ALL
SELECT g.group_id, 3, '飲料', CURRENT_TIMESTAMP
FROM user_group g
WHERE NOT EXISTS (SELECT 1 FROM restaurant_category c WHERE c.group_id = g.group_id);
