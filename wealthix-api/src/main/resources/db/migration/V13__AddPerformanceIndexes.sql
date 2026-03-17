-- AutoPay performance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_schedules_user_active_due
  ON autopay_schedules(user_id, is_active, next_due_date)
  WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_schedules_due_date_active
  ON autopay_schedules(next_due_date, is_active)
  WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_exec_logs_schedule_status
  ON autopay_execution_logs(schedule_id, status, execution_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_exec_logs_user_date
  ON autopay_execution_logs(user_id, execution_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_user_date
  ON transactions(user_id, transaction_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_plaid_id
  ON transactions(plaid_transaction_id)
  WHERE plaid_transaction_id IS NOT NULL;
