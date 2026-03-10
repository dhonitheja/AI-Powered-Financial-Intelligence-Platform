-- Phase 2: Add Stripe integration fields

ALTER TABLE users 
  ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS stripe_default_payment_method VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_users_stripe_customer 
  ON users(stripe_customer_id);

ALTER TABLE autopay_execution_logs 
  ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS stripe_status VARCHAR(50),
  ADD COLUMN IF NOT EXISTS plaid_verification_status VARCHAR(20) DEFAULT 'UNVERIFIED',
  ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0;
