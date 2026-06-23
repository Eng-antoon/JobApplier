create or replace function public.assign_generated_content_version()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  perform pg_advisory_xact_lock(
    hashtextextended(new.job_id::text || ':' || new.content_type, 0)
  );

  if new.version is null or exists (
    select 1
    from public.generated_content
    where job_id = new.job_id
      and content_type = new.content_type
      and version = new.version
  ) then
    select (coalesce(max(version), 0) + 1)::smallint
      into new.version
    from public.generated_content
    where job_id = new.job_id
      and content_type = new.content_type;
  end if;

  return new;
end;
$$;

drop trigger if exists set_generated_content_version on public.generated_content;

create trigger set_generated_content_version
before insert on public.generated_content
for each row
execute function public.assign_generated_content_version();
