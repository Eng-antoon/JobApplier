-- Harden usage-limit trigger functions.
-- Trigger execution does not require public RPC execute privileges.

ALTER FUNCTION public.create_user_quota()
  SET search_path = public;

ALTER FUNCTION public.on_quota_request_approved()
  SET search_path = public;

REVOKE EXECUTE ON FUNCTION public.create_user_quota() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.on_quota_request_approved() FROM PUBLIC, anon, authenticated;
