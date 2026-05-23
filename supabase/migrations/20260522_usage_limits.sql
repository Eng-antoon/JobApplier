-- Usage Limits: enums, tables, triggers, cron

-- Enums
CREATE TYPE quota_request_status AS ENUM ('pending', 'approved', 'denied');
CREATE TYPE quota_request_type AS ENUM ('weekly', 'resume');

-- User quotas table
CREATE TABLE user_quotas (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  weekly_ai_limit INTEGER NOT NULL DEFAULT 15,
  resume_parse_limit INTEGER NOT NULL DEFAULT 2,
  extra_quota_remaining INTEGER NOT NULL DEFAULT 0,
  week_start TIMESTAMPTZ NOT NULL DEFAULT now(),
  weekly_usage_count INTEGER NOT NULL DEFAULT 0,
  resume_parse_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE user_quotas ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own quota"
  ON user_quotas FOR SELECT
  USING (auth.uid() = user_id);

-- Quota requests table
CREATE TABLE quota_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  status quota_request_status NOT NULL DEFAULT 'pending',
  request_type quota_request_type NOT NULL DEFAULT 'weekly',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ
);

ALTER TABLE quota_requests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own requests"
  ON quota_requests FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own requests"
  ON quota_requests FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- Auto-create quota row on user signup
CREATE OR REPLACE FUNCTION create_user_quota()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO user_quotas (user_id) VALUES (NEW.id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created_quota
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION create_user_quota();

-- Grant extra quota when request approved
CREATE OR REPLACE FUNCTION on_quota_request_approved()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'approved' AND OLD.status = 'pending' THEN
    UPDATE user_quotas
    SET extra_quota_remaining = extra_quota_remaining + 10,
        updated_at = now()
    WHERE user_id = NEW.user_id;
    NEW.resolved_at = now();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_quota_approved
  BEFORE UPDATE ON quota_requests
  FOR EACH ROW EXECUTE FUNCTION on_quota_request_approved();

-- Weekly reset cron (every Monday at 00:00 UTC)
SELECT cron.schedule(
  'reset-weekly-quotas',
  '0 0 * * 1',
  $$
    UPDATE user_quotas
    SET weekly_usage_count = 0,
        week_start = now(),
        updated_at = now();
  $$
);

-- Backfill existing users
INSERT INTO user_quotas (user_id)
SELECT id FROM auth.users
WHERE id NOT IN (SELECT user_id FROM user_quotas)
ON CONFLICT DO NOTHING;
