CREATE INDEX idx_transactions_user_id_date ON transactions (user_id, date);

CREATE INDEX idx_budgets_user_id_month_value ON budgets (user_id, month_value);
