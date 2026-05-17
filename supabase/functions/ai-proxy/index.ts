import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

interface ActionRequest {
  action: string;
  payload: Record<string, unknown>;
}

type ActionResponse = Record<string, unknown>;

interface GeminiResponse {
  candidates?: Array<{
    content?: {
      parts?: Array<{ text?: string }>;
    };
  }>;
  usageMetadata?: {
    promptTokenCount?: number;
    candidatesTokenCount?: number;
    cachedContentTokenCount?: number;
  };
}

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const MAX_TOKENS_MAP: Record<string, number> = {
  analyze_jd: 2000,
  generate_cover_letter: 1000,
  generate_cover_email: 600,
  answer_question: 800,
  detect_new_data: 1000,
  parse_resume: 8192,
  generate_suggestions: 4000,
  fetch_job_url: 2000,
};

async function callGemini(prompt: string, action: string): Promise<GeminiResponse> {
  const maxTokens = MAX_TOKENS_MAP[action] ?? 500;

  const response = await fetch(`${GEMINI_URL}?key=${GEMINI_API_KEY}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: {
        maxOutputTokens: maxTokens,
        temperature: action === "analyze_jd" || action === "detect_new_data" || action === "parse_resume" ? 0.2 : 0.7,
      },
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error(`Gemini API error for action=${action}: status=${response.status}, body=${errorText.slice(0, 500)}`);
    throw new Error(`Gemini API error (${response.status}): ${errorText}`);
  }

  return await response.json();
}

function buildPrompt(action: string, payload: Record<string, unknown>): string {
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

function calculateCost(inputTokens: number, outputTokens: number): number {
  return (inputTokens * 0.10 + outputTokens * 0.40) / 1_000_000;
}

function cleanJsonResponse(text: string): string {
  let cleaned = text.trim();
  if (cleaned.startsWith("```json")) {
    cleaned = cleaned.slice(7);
  } else if (cleaned.startsWith("```")) {
    cleaned = cleaned.slice(3);
  }
  if (cleaned.endsWith("```")) {
    cleaned = cleaned.slice(0, -3);
  }
  cleaned = cleaned.trim();
  // Extract JSON object if surrounded by extra text
  const firstBrace = cleaned.indexOf("{");
  const lastBrace = cleaned.lastIndexOf("}");
  if (firstBrace !== -1 && lastBrace !== -1 && firstBrace < lastBrace) {
    cleaned = cleaned.slice(firstBrace, lastBrace + 1);
  }
  return cleaned;
}

async function handleFetchJobUrl(payload: Record<string, unknown>): Promise<Record<string, unknown>> {
  const url = payload.url as string;
  if (!url) return { success: false, reason: "url is required" };

  try {
    const response = await fetch(url, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
      },
      redirect: "follow",
    });

    if (!response.ok) {
      return { success: false, reason: `HTTP ${response.status}` };
    }

    const html = await response.text();

    if (html.length < 500 || html.includes("authwall") || html.includes("sign in to")) {
      return { success: false, reason: "blocked_by_auth" };
    }

    const extractPrompt = `Extract job posting details from this HTML content. Respond with ONLY valid JSON (no markdown):
{
  "success": true,
  "title": "job title",
  "company": "company name",
  "description": "full job description text",
  "requirements": ["requirement 1", "requirement 2"]
}

If the HTML does not contain a valid job posting, respond with:
{ "success": false, "reason": "no_job_content" }

HTML CONTENT (truncated):
${html.substring(0, 15000)}`;

    const geminiResult = await callGemini(extractPrompt, "fetch_job_url");
    const text = geminiResult.candidates?.[0]?.content?.parts?.[0]?.text ?? "";

    try {
      return JSON.parse(cleanJsonResponse(text));
    } catch {
      return { success: false, reason: "parse_error" };
    }
  } catch (error) {
    return { success: false, reason: (error as Error).message };
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing authorization" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: { user }, error: authError } = await userClient.auth.getUser();
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const body: ActionRequest = await req.json();
    const { action, payload } = body;

    if (action === "fetch_job_url") {
      const result = await handleFetchJobUrl(payload);
      return new Response(JSON.stringify(result), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const prompt = buildPrompt(action, payload);
    const geminiResult = await callGemini(prompt, action);

    const serviceClient = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);
    await serviceClient.from("ai_usage_log").insert({
      user_id: user.id,
      function_name: action,
      model: "gemini-2.0-flash",
      input_tokens: geminiResult.usageMetadata?.promptTokenCount ?? 0,
      output_tokens: geminiResult.usageMetadata?.candidatesTokenCount ?? 0,
      cost_estimate_usd: calculateCost(
        geminiResult.usageMetadata?.promptTokenCount ?? 0,
        geminiResult.usageMetadata?.candidatesTokenCount ?? 0
      ),
    });

    let responseData: ActionResponse;
    const text = geminiResult.candidates?.[0]?.content?.parts?.[0]?.text ?? "";

    switch (action) {
      case "analyze_jd":
      case "detect_new_data":
      case "parse_resume":
      case "generate_suggestions": {
        try {
          responseData = JSON.parse(cleanJsonResponse(text));
        } catch (parseError) {
          console.error(`JSON parse failed for action=${action}. Text length=${text.length}. First 500 chars: ${text.slice(0, 500)}`);
          console.error(`Parse error: ${(parseError as Error).message}`);
          return new Response(
            JSON.stringify({ error: "Failed to parse AI response", raw_text: text.slice(0, 1000) }),
            {
              status: 502,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            }
          );
        }
        break;
      }
      case "generate_cover_letter":
      case "generate_cover_email":
      case "answer_question": {
        responseData = { content: text };
        break;
      }
      default:
        return new Response(JSON.stringify({ error: `Unknown action: ${action}` }), {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
    }

    return new Response(JSON.stringify(responseData), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
