ALTER TABLE app_users
  ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN DEFAULT false;
