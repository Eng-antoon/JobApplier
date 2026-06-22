alter table public.generated_content
  drop constraint generated_content_content_type_check;

alter table public.generated_content
  add constraint generated_content_content_type_check
  check (
    content_type = any (
      array[
        'cover_letter'::text,
        'cover_email'::text,
        'headline'::text,
        'why_work_here'::text,
        'strengths'::text,
        'weaknesses'::text,
        'motivation'::text,
        'custom_question'::text
      ]
    )
  );
