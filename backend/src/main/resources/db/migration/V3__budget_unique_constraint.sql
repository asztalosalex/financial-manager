UPDATE budgets
SET month_value = date_trunc('month', month_value)::date
WHERE month_value <> date_trunc('month', month_value)::date;

WITH grouped AS (
    SELECT user_id, category_id, month_value, MIN(id) AS keep_id, SUM(amount) AS total_amount
    FROM budgets
    GROUP BY user_id, category_id, month_value
    HAVING COUNT(*) > 1
)
UPDATE budgets b
SET amount = g.total_amount
FROM grouped g
WHERE b.id = g.keep_id;

WITH grouped AS (
    SELECT user_id, category_id, month_value, MIN(id) AS keep_id
    FROM budgets
    GROUP BY user_id, category_id, month_value
    HAVING COUNT(*) > 1
)
DELETE FROM budgets b
USING grouped g
WHERE b.user_id = g.user_id
    AND b.category_id = g.category_id
    AND b.month_value = g.month_value
    AND b.id <> g.keep_id;

ALTER TABLE budgets
    ADD CONSTRAINT uk_budgets_user_id_category_id_month_value UNIQUE (user_id, category_id, month_value);
