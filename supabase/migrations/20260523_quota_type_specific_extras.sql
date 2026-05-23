-- Make approved extra quota type-specific.
-- Weekly approvals grant 10 weekly AI actions.
-- Resume approvals grant 2 resume parses.

ALTER TABLE user_quotas
  ADD COLUMN IF NOT EXISTS extra_resume_parse_remaining INTEGER NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION on_quota_request_approved()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status IN ('approved', 'denied') AND OLD.status = 'pending' THEN
    NEW.resolved_at = now();
  END IF;

  IF NEW.status = 'approved' AND OLD.status = 'pending' THEN
    IF NEW.request_type = 'resume' THEN
      UPDATE user_quotas
      SET extra_resume_parse_remaining = extra_resume_parse_remaining + 2,
          updated_at = now()
      WHERE user_id = NEW.user_id;
    ELSE
      UPDATE user_quotas
      SET extra_quota_remaining = extra_quota_remaining + 10,
          updated_at = now()
      WHERE user_id = NEW.user_id;
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
