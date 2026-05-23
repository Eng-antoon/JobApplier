-- Add composite index on quota_requests for query performance
-- Queries filter on (user_id, status, request_type) in edge function and Android app
CREATE INDEX IF NOT EXISTS idx_quota_requests_lookup
  ON quota_requests(user_id, status, request_type);
