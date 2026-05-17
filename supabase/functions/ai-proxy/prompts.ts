export function buildPrompt(action: string, payload: Record<string, unknown>): string {
  switch (action) {
    case "analyze_jd":
      return analyzeJdPrompt(payload);
    case "generate_cover_letter":
      return coverLetterPrompt(payload);
    case "generate_cover_email":
      return coverEmailPrompt(payload);
    case "answer_question":
      return answerQuestionPrompt(payload);
    case "detect_new_data":
      return detectNewDataPrompt(payload);
    case "parse_resume":
      return parseResumePrompt(payload);
    case "generate_suggestions":
      return generateSuggestionsPrompt(payload);
    default:
      throw new Error(`Unknown action: ${action}`);
  }
}

function analyzeJdPrompt(p: Record<string, unknown>): string {
  return `You are a job application analyst. Analyze this job description against the candidate's profile.

JOB DESCRIPTION:
${(p.job_description as string || "").slice(0, 4000)}

CANDIDATE PROFILE:
${p.user_profile as string || ""}

Respond with ONLY valid JSON (no markdown):
{
  "requirements": ["req1", "req2", ...],
  "match_score": 0-100,
  "matched": ["skill/experience that matches", ...],
  "gaps": ["missing requirement", ...],
  "partial": ["partially matching area", ...],
  "suggestions": ["how to strengthen application", ...]
}`;
}

function coverLetterPrompt(p: Record<string, unknown>): string {
  const tone = (p.tone as string) || "professional";
  const extra = (p.additional_instructions as string) || "";
  return `Write a cover letter for this job application. Tone: ${tone}. Length: 250-350 words.
${extra ? `Additional instructions: ${extra}\n` : ""}
JOB DESCRIPTION:
${(p.job_description as string || "").slice(0, 4000)}

CANDIDATE PROFILE:
${p.user_profile as string || ""}

Write the cover letter directly, no preamble or labels. Make it compelling and specific to the role.`;
}

function coverEmailPrompt(p: Record<string, unknown>): string {
  const tone = (p.tone as string) || "professional";
  return `Write a cover email for this job application. Tone: ${tone}. Keep it concise (150-200 words). Include a subject line on the first line prefixed with "Subject: ".

JOB DESCRIPTION:
${(p.job_description as string || "").slice(0, 4000)}

CANDIDATE PROFILE:
${p.user_profile as string || ""}

Write the email directly. Be concise and professional.`;
}

function answerQuestionPrompt(p: Record<string, unknown>): string {
  return `Answer this job application question based on the candidate's profile. Write in first person as the candidate. Keep the answer to 2-3 paragraphs.

QUESTION: ${p.question as string || ""}
${p.question_type ? `QUESTION TYPE: ${p.question_type}` : ""}

JOB DESCRIPTION:
${(p.job_description as string || "").slice(0, 4000)}

CANDIDATE PROFILE:
${p.user_profile as string || ""}

Write the answer directly, no preamble.`;
}

function detectNewDataPrompt(p: Record<string, unknown>): string {
  return `Analyze this text and extract any skills, work experiences, or certifications mentioned that are NOT already in the existing profile.

TEXT TO ANALYZE:
${p.text as string || ""}

EXISTING PROFILE:
${p.existing_profile as string || ""}

Respond with ONLY valid JSON (no markdown):
{
  "detected_skills": [{ "name": "skill", "category": "programming|tool|soft_skill", "proficiency": "beginner|intermediate|advanced|expert" }],
  "detected_experiences": [{ "company": "", "title": "", "description": "" }],
  "detected_certifications": [{ "name": "", "issuing_org": "" }]
}

Only include items NOT already in the existing profile. Return empty arrays if nothing new found.`;
}

function parseResumePrompt(p: Record<string, unknown>): string {
  return `You are a professional resume parser. Extract ALL structured data from this resume text.

RESUME TEXT:
${(p.resume_text as string || "").slice(0, 12000)}

Extract every piece of information and respond with ONLY valid JSON (no markdown, no explanation):
{
  "full_name": "string or null",
  "email": "string or null",
  "phone": "string or null",
  "location": "city, country or null",
  "linkedin_url": "string or null",
  "summary": "professional summary text or null",
  "desired_role": "inferred desired role or null",
  "skills": [
    { "name": "skill name", "category": "programming|framework|tool|database|cloud|soft_skill|language|other", "proficiency": "beginner|intermediate|advanced|expert", "years_experience": number_or_null }
  ],
  "experiences": [
    { "company": "company name", "title": "job title", "location": "city or null", "start_date": "YYYY-MM or YYYY", "end_date": "YYYY-MM or YYYY or null", "is_current": boolean, "description": "role description", "achievements": ["achievement 1", ...], "technologies_used": ["tech1", ...] }
  ],
  "education": [
    { "institution": "university name", "degree": "degree type", "field_of_study": "major or null", "start_date": "YYYY or null", "end_date": "YYYY or null", "gpa": "string or null", "description": "null" }
  ],
  "certifications": [
    { "name": "cert name", "issuing_org": "org or null", "issue_date": "YYYY-MM or null", "expiry_date": "null", "credential_url": "null" }
  ],
  "languages": [
    { "name": "language", "proficiency": "native|fluent|advanced|intermediate|basic" }
  ]
}

CRITICAL RULES:
- Extract ALL skills mentioned ANYWHERE in the resume (in experience descriptions, skill sections, summary, education)
- Include technologies_used from each job experience
- Extract achievements as separate bullet points from job descriptions
- Infer proficiency from context (years used, seniority of role)
- For dates, use YYYY-MM format. If only year available, use YYYY
- Set is_current=true for the most recent job if no end date
- If information is not available, use null
- Return empty arrays [] if a section has no data`;
}

function generateSuggestionsPrompt(p: Record<string, unknown>): string {
  const hasJobs = !!(p.job_descriptions as string);
  return `You are a career advisor AI. Based on the user's profile${hasJobs ? " and their saved job descriptions" : ""}, provide actionable suggestions to improve their job application success.

USER PROFILE:
${p.user_profile as string || ""}
${hasJobs ? `\nSAVED JOB DESCRIPTIONS:\n${(p.job_descriptions as string).slice(0, 4000)}` : ""}

Respond with ONLY valid JSON (no markdown):
{
  "headline_suggestions": ["3-5 professional headline options tailored to their target roles"],
  "summary_rewrites": ["2-3 improved versions of their professional summary"],
  "skill_gaps": [
    { "skill": "skill name", "reason": "why they should add it based on target jobs or industry trends", "priority": "high|medium|low" }
  ],
  "cover_email_templates": [
    { "job_id": "general", "template": "a reusable cover email template" }
  ],
  "general_tips": ["3-5 actionable tips to improve their profile and applications"]
}

Be specific and actionable. Reference actual data from their profile. Do not be generic.`;
}
