import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

interface ActionRequest {
  action: string;
  payload: Record<string, unknown>;
}

type ActionResponse = Record<string, unknown>;

export interface GeminiResponse {
  candidates?: Array<{
    finishReason?: string;
    content?: {
      parts?: Array<{ text?: string }>;
    };
  }>;
  usageMetadata?: {
    promptTokenCount?: number;
    candidatesTokenCount?: number;
    cachedContentTokenCount?: number;
    thoughtsTokenCount?: number;
    totalTokenCount?: number;
  };
}

interface CombinedUsageMetadata {
  promptTokenCount: number;
  candidatesTokenCount: number;
  cachedContentTokenCount: number;
  thoughtsTokenCount: number;
  totalTokenCount: number;
}

export class StructuredResponseError extends Error {
  readonly code = "ai_response_invalid";

  constructor(readonly requestId: string) {
    super("The AI returned an invalid structured response");
    this.name = "StructuredResponseError";
  }
}

class StructuredValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "StructuredValidationError";
  }
}

function cleanStructuredJson(text: string): string {
  return text
    .trim()
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```$/, "")
    .trim();
}

export async function generateStructuredJson<T extends Record<string, unknown>>(
  action: string,
  requestId: string,
  callAttempt: (attempt: number) => Promise<GeminiResponse>,
): Promise<{ data: T; usageMetadata: CombinedUsageMetadata }> {
  const usageMetadata: CombinedUsageMetadata = {
    promptTokenCount: 0,
    candidatesTokenCount: 0,
    cachedContentTokenCount: 0,
    thoughtsTokenCount: 0,
    totalTokenCount: 0,
  };

  for (let attempt = 0; attempt < 2; attempt += 1) {
    const startedAt = Date.now();
    const response = await callAttempt(attempt);
    usageMetadata.promptTokenCount +=
      response.usageMetadata?.promptTokenCount ?? 0;
    usageMetadata.candidatesTokenCount +=
      response.usageMetadata?.candidatesTokenCount ?? 0;
    usageMetadata.cachedContentTokenCount +=
      response.usageMetadata?.cachedContentTokenCount ?? 0;
    usageMetadata.thoughtsTokenCount +=
      response.usageMetadata?.thoughtsTokenCount ?? 0;
    usageMetadata.totalTokenCount += response.usageMetadata?.totalTokenCount ??
      0;

    const candidate = response.candidates?.[0];
    const text = candidate?.content?.parts?.map((part) =>
      part.text ?? ""
    ).join("") ?? "";
    try {
      if (!text || candidate?.finishReason !== "STOP") {
        throw new StructuredValidationError("Response did not finish normally");
      }
      const data = JSON.parse(cleanStructuredJson(text)) as T;
      validateStructuredData(action, data);
      return {
        data,
        usageMetadata,
      };
    } catch (error) {
      const category = error instanceof SyntaxError
        ? "parse"
        : error instanceof StructuredValidationError
        ? "validation"
        : "unknown";
      console.error(
        `Structured AI response invalid: requestId=${requestId}, action=${action}, attempt=${
          attempt + 1
        }, finishReason=${candidate?.finishReason ?? "unknown"}, durationMs=${
          Date.now() - startedAt
        }, textLength=${text.length}, promptTokens=${
          response.usageMetadata?.promptTokenCount ?? 0
        }, outputTokens=${
          response.usageMetadata?.candidatesTokenCount ?? 0
        }, thoughtTokens=${
          response.usageMetadata?.thoughtsTokenCount ?? 0
        }, category=${category}`,
      );
    }
  }

  throw new StructuredResponseError(requestId);
}

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const GEMINI_MODEL = "gemini-2.5-flash";
const GEMINI_URL =
  `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Expose-Headers": "x-request-id",
};

const MAX_TOKENS_MAP: Record<string, number> = {
  analyze_jd: 4096,
  generate_cover_letter: 1000,
  generate_cover_email: 600,
  answer_question: 800,
  detect_new_data: 1000,
  parse_resume: 8192,
  generate_suggestions: 4000,
  fetch_job_url: 4000,
};

const SUPPORTED_ACTIONS = new Set([
  "analyze_jd",
  "generate_cover_letter",
  "generate_cover_email",
  "answer_question",
  "detect_new_data",
  "parse_resume",
  "generate_suggestions",
  "fetch_job_url",
]);

interface JsonSchema {
  type?: string | string[];
  properties?: Record<string, JsonSchema>;
  required?: string[];
  items?: JsonSchema;
  minItems?: number;
  maxItems?: number;
  minimum?: number;
  maximum?: number;
  maxLength?: number;
  additionalProperties?: boolean;
  description?: string;
}

const STRING_ARRAY_SCHEMA: JsonSchema = {
  type: "array",
  items: { type: "string" },
};

const ANALYSIS_ITEM_SCHEMA: JsonSchema = {
  type: "string",
  maxLength: 240,
  description:
    "One concise sentence grounded in the supplied job description or candidate profile.",
};

const JSON_SCHEMAS: Record<string, JsonSchema> = {
  analyze_jd: {
    type: "object",
    additionalProperties: false,
    properties: {
      requirements: {
        type: "array",
        items: ANALYSIS_ITEM_SCHEMA,
        minItems: 5,
        maxItems: 12,
        description:
          "Five to twelve distinct qualifications or responsibilities from the job posting.",
      },
      match_score: {
        type: "integer",
        minimum: 0,
        maximum: 100,
        description: "Overall evidence-based candidate fit from 0 to 100.",
      },
      matched: { type: "array", items: ANALYSIS_ITEM_SCHEMA, maxItems: 8 },
      gaps: { type: "array", items: ANALYSIS_ITEM_SCHEMA, maxItems: 8 },
      partial: { type: "array", items: ANALYSIS_ITEM_SCHEMA, maxItems: 8 },
      suggestions: { type: "array", items: ANALYSIS_ITEM_SCHEMA, maxItems: 8 },
    },
    required: [
      "requirements",
      "match_score",
      "matched",
      "gaps",
      "partial",
      "suggestions",
    ],
  },
  detect_new_data: {
    type: "object",
    properties: {
      detected_skills: {
        type: "array",
        items: {
          type: "object",
          properties: {
            name: { type: "string" },
            category: { type: "string" },
            proficiency: { type: "string" },
          },
          required: ["name"],
        },
      },
      detected_experiences: {
        type: "array",
        items: {
          type: "object",
          properties: {
            company: { type: "string" },
            title: { type: "string" },
            description: { type: "string" },
          },
        },
      },
      detected_certifications: {
        type: "array",
        items: {
          type: "object",
          properties: {
            name: { type: "string" },
            issuing_org: { type: "string" },
          },
        },
      },
    },
    required: [
      "detected_skills",
      "detected_experiences",
      "detected_certifications",
    ],
  },
  parse_resume: {
    type: "object",
    properties: {
      full_name: { type: "string" },
      email: { type: "string" },
      phone: { type: "string" },
      location: { type: "string" },
      linkedin_url: { type: "string" },
      summary: { type: "string" },
      desired_role: { type: "string" },
      skills: {
        type: "array",
        items: {
          type: "object",
          properties: {
            name: { type: "string" },
            category: { type: "string" },
            proficiency: { type: "string" },
            years_experience: { type: "integer" },
          },
          required: ["name"],
        },
      },
      experiences: {
        type: "array",
        items: {
          type: "object",
          properties: {
            company: { type: "string" },
            title: { type: "string" },
            location: { type: "string" },
            start_date: { type: "string" },
            end_date: { type: "string" },
            is_current: { type: "boolean" },
            description: { type: "string" },
            achievements: STRING_ARRAY_SCHEMA,
            technologies_used: STRING_ARRAY_SCHEMA,
          },
          required: [
            "company",
            "title",
            "is_current",
            "achievements",
            "technologies_used",
          ],
        },
      },
      education: {
        type: "array",
        items: {
          type: "object",
          properties: {
            institution: { type: "string" },
            degree: { type: "string" },
            field_of_study: { type: "string" },
            start_date: { type: "string" },
            end_date: { type: "string" },
            gpa: { type: "string" },
            description: { type: "string" },
          },
          required: ["institution", "degree"],
        },
      },
      certifications: {
        type: "array",
        items: {
          type: "object",
          properties: {
            name: { type: "string" },
            issuing_org: { type: "string" },
            issue_date: { type: "string" },
            expiry_date: { type: "string" },
            credential_url: { type: "string" },
          },
          required: ["name"],
        },
      },
      languages: {
        type: "array",
        items: {
          type: "object",
          properties: {
            name: { type: "string" },
            proficiency: { type: "string" },
          },
          required: ["name"],
        },
      },
    },
    required: [
      "skills",
      "experiences",
      "education",
      "certifications",
      "languages",
    ],
  },
  generate_suggestions: {
    type: "object",
    properties: {
      headline_suggestions: STRING_ARRAY_SCHEMA,
      summary_rewrites: STRING_ARRAY_SCHEMA,
      skill_gaps: {
        type: "array",
        items: {
          type: "object",
          properties: {
            skill: { type: "string" },
            reason: { type: "string" },
            priority: { type: "string" },
          },
          required: ["skill", "reason", "priority"],
        },
      },
      cover_email_templates: {
        type: "array",
        items: {
          type: "object",
          properties: {
            job_id: { type: "string" },
            template: { type: "string" },
          },
          required: ["job_id", "template"],
        },
      },
      general_tips: STRING_ARRAY_SCHEMA,
    },
    required: [
      "headline_suggestions",
      "summary_rewrites",
      "skill_gaps",
      "cover_email_templates",
      "general_tips",
    ],
  },
  fetch_job_url: {
    type: "object",
    properties: {
      success: { type: "boolean" },
      title: { type: "string" },
      company: { type: "string" },
      description: { type: "string" },
      requirements: STRING_ARRAY_SCHEMA,
      reason: { type: "string" },
    },
    required: ["success", "requirements"],
  },
};

function matchesType(value: unknown, type: string): boolean {
  switch (type) {
    case "object":
      return typeof value === "object" && value !== null &&
        !Array.isArray(value);
    case "array":
      return Array.isArray(value);
    case "integer":
      return typeof value === "number" && Number.isInteger(value);
    case "number":
      return typeof value === "number" && Number.isFinite(value);
    case "string":
      return typeof value === "string";
    case "boolean":
      return typeof value === "boolean";
    case "null":
      return value === null;
    default:
      return false;
  }
}

function validateValue(schema: JsonSchema, value: unknown, path: string): void {
  const allowedTypes = Array.isArray(schema.type)
    ? schema.type
    : schema.type
    ? [schema.type]
    : [];
  if (
    allowedTypes.length > 0 &&
    !allowedTypes.some((type) => matchesType(value, type))
  ) {
    throw new StructuredValidationError(`${path} has an invalid type`);
  }

  if (typeof value === "number") {
    if (schema.minimum !== undefined && value < schema.minimum) {
      throw new StructuredValidationError(`${path} is below its minimum`);
    }
    if (schema.maximum !== undefined && value > schema.maximum) {
      throw new StructuredValidationError(`${path} is above its maximum`);
    }
  }

  if (
    typeof value === "string" && schema.maxLength !== undefined &&
    value.length > schema.maxLength
  ) {
    throw new StructuredValidationError(`${path} is too long`);
  }

  if (Array.isArray(value)) {
    if (schema.minItems !== undefined && value.length < schema.minItems) {
      throw new StructuredValidationError(`${path} has too few items`);
    }
    if (schema.maxItems !== undefined && value.length > schema.maxItems) {
      throw new StructuredValidationError(`${path} has too many items`);
    }
    if (schema.items) {
      value.forEach((item, index) =>
        validateValue(schema.items!, item, `${path}[${index}]`)
      );
    }
  }

  if (typeof value === "object" && value !== null && !Array.isArray(value)) {
    const record = value as Record<string, unknown>;
    for (const required of schema.required ?? []) {
      if (!Object.prototype.hasOwnProperty.call(record, required)) {
        throw new StructuredValidationError(`${path}.${required} is required`);
      }
    }
    for (const [key, item] of Object.entries(record)) {
      const propertySchema = schema.properties?.[key];
      if (propertySchema) validateValue(propertySchema, item, `${path}.${key}`);
      else if (schema.additionalProperties === false) {
        throw new StructuredValidationError(`${path}.${key} is not allowed`);
      }
    }
  }
}

export function validateStructuredData(
  action: string,
  data: Record<string, unknown>,
): void {
  const schema = JSON_SCHEMAS[action];
  if (schema) validateValue(schema, data, "$response");
}

function isJsonAction(action: string): boolean {
  return Object.prototype.hasOwnProperty.call(JSON_SCHEMAS, action);
}

async function callGemini(
  prompt: string,
  action: string,
  maxTokensOverride?: number,
): Promise<GeminiResponse> {
  const generationConfig = buildGenerationConfig(action, maxTokensOverride);

  const response = await fetch(`${GEMINI_URL}?key=${GEMINI_API_KEY}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig,
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error(
      `Gemini API error for action=${action}: status=${response.status}, body=${
        errorText.slice(0, 500)
      }`,
    );
    throw new Error(`Gemini API error (${response.status}): ${errorText}`);
  }

  return await response.json();
}

export function buildGenerationConfig(
  action: string,
  maxTokensOverride?: number,
): Record<string, unknown> {
  const generationConfig: Record<string, unknown> = {
    maxOutputTokens: maxTokensOverride ?? MAX_TOKENS_MAP[action] ?? 500,
    temperature: action === "analyze_jd" || action === "detect_new_data" ||
        action === "parse_resume"
      ? 0.2
      : 0.7,
  };

  if (isJsonAction(action)) {
    generationConfig.responseMimeType = "application/json";
    generationConfig.responseJsonSchema = JSON_SCHEMAS[action];
    generationConfig.thinkingConfig = { thinkingBudget: 0 };
  }

  return generationConfig;
}

export function buildPrompt(
  action: string,
  payload: Record<string, unknown>,
): string {
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
  "requirements": ["5-12 distinct requirements, each one concise sentence"],
  "match_score": 0-100,
  "matched": ["up to 8 concise evidence-based matches"],
  "gaps": ["up to 8 concise missing requirements"],
  "partial": ["up to 8 concise partial matches"],
  "suggestions": ["up to 8 concise actionable improvements"]
}

Use only evidence present in the supplied text. Do not invent requirements or candidate experience. Every array item must be a single sentence no longer than 240 characters.`;
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
  return `You are a career advisor AI. Based on the user's profile${
    hasJobs ? " and their saved job descriptions" : ""
  }, provide actionable suggestions to improve their job application success.

USER PROFILE:
${p.user_profile as string || ""}
${
    hasJobs
      ? `\nSAVED JOB DESCRIPTIONS:\n${
        (p.job_descriptions as string).slice(0, 4000)
      }`
      : ""
  }

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
  return (inputTokens * 0.30 + outputTokens * 2.50) / 1_000_000;
}

interface QuotaCheckResult {
  allowed: boolean;
  isLastResumeParse?: boolean;
  quotaError?: {
    error: string;
    quota_type: "weekly" | "resume";
    used: number;
    limit: number;
    extra_remaining: number;
    weekly_extra_remaining: number;
    resume_extra_remaining: number;
    resets_at: string | null;
    can_request_extra: boolean;
  };
}

async function checkAndIncrementQuota(
  serviceClient: ReturnType<typeof createClient>,
  userId: string,
  action: string,
): Promise<QuotaCheckResult> {
  // Get or create user_quotas row
  let { data: quota } = await serviceClient
    .from("user_quotas")
    .select("*")
    .eq("user_id", userId)
    .single();

  if (!quota) {
    const { data: newQuota } = await serviceClient
      .from("user_quotas")
      .insert({ user_id: userId })
      .select()
      .single();
    quota = newQuota;
  }

  if (!quota) {
    return {
      allowed: false,
      quotaError: {
        error: "quota_exceeded",
        quota_type: "weekly",
        used: 0,
        limit: 0,
        extra_remaining: 0,
        weekly_extra_remaining: 0,
        resume_extra_remaining: 0,
        resets_at: null,
        can_request_extra: true,
      },
    };
  }

  if (action === "parse_resume") {
    const resumeExtraRemaining = quota.extra_resume_parse_remaining ?? 0;
    if (
      quota.resume_parse_count >= quota.resume_parse_limit &&
      resumeExtraRemaining <= 0
    ) {
      // Check if can request extra (no pending request)
      const { data: pending } = await serviceClient
        .from("quota_requests")
        .select("id")
        .eq("user_id", userId)
        .eq("status", "pending")
        .eq("request_type", "resume");
      const canRequest = !pending || pending.length === 0;

      return {
        allowed: false,
        quotaError: {
          error: "quota_exceeded",
          quota_type: "resume",
          used: quota.resume_parse_count,
          limit: quota.resume_parse_limit,
          extra_remaining: resumeExtraRemaining,
          weekly_extra_remaining: quota.extra_quota_remaining ?? 0,
          resume_extra_remaining: resumeExtraRemaining,
          resets_at: null,
          can_request_extra: canRequest,
        },
      };
    }

    if (quota.resume_parse_count < quota.resume_parse_limit) {
      // Atomic increment of included lifetime resume parses
      const { data: updated } = await serviceClient
        .from("user_quotas")
        .update({
          resume_parse_count: quota.resume_parse_count + 1,
          updated_at: new Date().toISOString(),
        })
        .eq("user_id", userId)
        .eq("resume_parse_count", quota.resume_parse_count)
        .select()
        .single();

      if (!updated) {
        return {
          allowed: false,
          quotaError: {
            error: "quota_exceeded",
            quota_type: "resume",
            used: quota.resume_parse_count,
            limit: quota.resume_parse_limit,
            extra_remaining: resumeExtraRemaining,
            weekly_extra_remaining: quota.extra_quota_remaining ?? 0,
            resume_extra_remaining: resumeExtraRemaining,
            resets_at: null,
            can_request_extra: true,
          },
        };
      }

      const isLast = updated.resume_parse_count >= updated.resume_parse_limit;
      return { allowed: true, isLastResumeParse: isLast };
    }

    // Consume approved resume extras after included parses are exhausted.
    const { data: updatedExtra } = await serviceClient
      .from("user_quotas")
      .update({
        extra_resume_parse_remaining: resumeExtraRemaining - 1,
        updated_at: new Date().toISOString(),
      })
      .eq("user_id", userId)
      .eq("extra_resume_parse_remaining", resumeExtraRemaining)
      .select()
      .single();

    if (!updatedExtra) {
      return {
        allowed: false,
        quotaError: {
          error: "quota_exceeded",
          quota_type: "resume",
          used: quota.resume_parse_count,
          limit: quota.resume_parse_limit,
          extra_remaining: 0,
          weekly_extra_remaining: quota.extra_quota_remaining ?? 0,
          resume_extra_remaining: 0,
          resets_at: null,
          can_request_extra: true,
        },
      };
    }

    return { allowed: true };
  }

  const weeklyExtraRemaining = quota.extra_quota_remaining ?? 0;
  // Regular actions: consume included weekly quota first, then approved weekly extras.
  if (
    quota.weekly_usage_count >= quota.weekly_ai_limit &&
    weeklyExtraRemaining <= 0
  ) {
    const { data: pending } = await serviceClient
      .from("quota_requests")
      .select("id")
      .eq("user_id", userId)
      .eq("status", "pending")
      .eq("request_type", "weekly");
    const canRequest = !pending || pending.length === 0;

    // Calculate next Monday reset
    const now = new Date();
    const daysUntilMonday = (8 - now.getUTCDay()) % 7 || 7;
    const nextMonday = new Date(now);
    nextMonday.setUTCDate(now.getUTCDate() + daysUntilMonday);
    nextMonday.setUTCHours(0, 0, 0, 0);

    return {
      allowed: false,
      quotaError: {
        error: "quota_exceeded",
        quota_type: "weekly",
        used: quota.weekly_usage_count,
        limit: quota.weekly_ai_limit,
        extra_remaining: weeklyExtraRemaining,
        weekly_extra_remaining: weeklyExtraRemaining,
        resume_extra_remaining: quota.extra_resume_parse_remaining ?? 0,
        resets_at: nextMonday.toISOString(),
        can_request_extra: canRequest,
      },
    };
  }

  // Consume: first from weekly limit, then from extra
  if (quota.weekly_usage_count < quota.weekly_ai_limit) {
    const { data: updated } = await serviceClient
      .from("user_quotas")
      .update({
        weekly_usage_count: quota.weekly_usage_count + 1,
        updated_at: new Date().toISOString(),
      })
      .eq("user_id", userId)
      .eq("weekly_usage_count", quota.weekly_usage_count)
      .select()
      .single();

    if (!updated) {
      return {
        allowed: false,
        quotaError: {
          error: "quota_exceeded",
          quota_type: "weekly",
          used: quota.weekly_usage_count,
          limit: quota.weekly_ai_limit,
          extra_remaining: weeklyExtraRemaining,
          weekly_extra_remaining: weeklyExtraRemaining,
          resume_extra_remaining: quota.extra_resume_parse_remaining ?? 0,
          resets_at: null,
          can_request_extra: true,
        },
      };
    }
  } else {
    // Consuming from extra quota
    const { data: updated } = await serviceClient
      .from("user_quotas")
      .update({
        extra_quota_remaining: weeklyExtraRemaining - 1,
        updated_at: new Date().toISOString(),
      })
      .eq("user_id", userId)
      .eq("extra_quota_remaining", weeklyExtraRemaining)
      .select()
      .single();

    if (!updated) {
      return {
        allowed: false,
        quotaError: {
          error: "quota_exceeded",
          quota_type: "weekly",
          used: quota.weekly_usage_count,
          limit: quota.weekly_ai_limit,
          extra_remaining: 0,
          weekly_extra_remaining: 0,
          resume_extra_remaining: quota.extra_resume_parse_remaining ?? 0,
          resets_at: null,
          can_request_extra: true,
        },
      };
    }
  }

  return { allowed: true };
}

function extractDescriptionFromHtml(html: string): string | null {
  const patterns = [
    /class="show-more-less-html__markup[^"]*"[^>]*>([\s\S]*?)<\/div>/i,
    /class="description__text[^"]*"[^>]*>([\s\S]*?)<\/section>/i,
    /class="jobs-description-content__text[^"]*"[^>]*>([\s\S]*?)<\/div>/i,
    /class="jobs-box__html-content[^"]*"[^>]*>([\s\S]*?)<\/div>/i,
    /class="job-description[^"]*"[^>]*>([\s\S]*?)<\/div>/i,
    /class="jobsearch-JobComponent-description[^"]*"[^>]*>([\s\S]*?)<\/div>/i,
  ];
  for (const pattern of patterns) {
    const match = html.match(pattern);
    if (match?.[1]) {
      const text = match[1]
        .replace(/<br\s*\/?>/gi, "\n")
        .replace(/<\/?(p|li|ul|ol|div|h[1-6])[^>]*>/gi, "\n")
        .replace(/<[^>]+>/g, "")
        .replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&nbsp;/g, " ")
        .replace(/\n{3,}/g, "\n\n")
        .trim();
      if (text.length > 100) return text;
    }
  }
  return null;
}

async function handleFetchJobUrl(
  payload: Record<string, unknown>,
  requestId: string,
): Promise<Record<string, unknown>> {
  const url = payload.url as string;
  if (!url) return { success: false, reason: "url is required" };

  try {
    const response = await fetch(url, {
      headers: {
        "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept":
          "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "none",
      },
      redirect: "follow",
    });

    if (!response.ok) {
      return { success: false, reason: `HTTP ${response.status}` };
    }

    const html = await response.text();

    const lowerHtml = html.toLowerCase();
    const blockedPatterns = [
      "authwall",
      "sign in to",
      "login_required",
      "please log in",
      "join now to see",
      "sign up to view",
      "create an account",
      "verify you're not a robot",
    ];
    if (
      html.length < 500 || blockedPatterns.some((p) => lowerHtml.includes(p))
    ) {
      return {
        success: false,
        reason: "blocked_by_auth",
        hint:
          "This job post requires sign-in. Copy and paste the job description directly.",
      };
    }

    // Try JSON-LD extraction first (LinkedIn and many job boards embed structured data)
    const jsonLdMatches = html.matchAll(
      /<script type="application\/ld\+json">([\s\S]*?)<\/script>/g,
    );
    for (const match of jsonLdMatches) {
      try {
        const ld = JSON.parse(match[1]);
        const posting = ld["@type"] === "JobPosting"
          ? ld
          : Array.isArray(ld["@graph"])
          ? ld["@graph"].find((n: Record<string, unknown>) =>
            n["@type"] === "JobPosting"
          )
          : null;
        if (posting) {
          const orgName = typeof posting.hiringOrganization === "string"
            ? posting.hiringOrganization
            : posting.hiringOrganization?.name ?? "";
          const htmlDescription = extractDescriptionFromHtml(html);
          const jsonLdDesc = (posting.description || "") as string;
          const description =
            (htmlDescription && htmlDescription.length > jsonLdDesc.length)
              ? htmlDescription
              : jsonLdDesc;
          return {
            success: true,
            title: posting.title || posting.name || "",
            company: orgName,
            description,
            requirements: [],
          };
        }
      } catch { /* try next match or fall through to AI */ }
    }

    const extractPrompt =
      `Extract job posting details from this HTML content. Respond with ONLY valid JSON (no markdown):
{
  "success": true,
  "title": "job title",
  "company": "company name",
  "description": "full job description - include ALL sections: overview, responsibilities, qualifications, requirements, benefits, about the company. Do NOT truncate or summarize.",
  "requirements": ["requirement 1", "requirement 2"]
}

If the HTML does not contain a valid job posting, respond with:
{ "success": false, "reason": "no_job_content" }

IMPORTANT: Extract the COMPLETE and FULL description with every section of the job posting. For LinkedIn pages, look for content in elements with classes like "description__text", "show-more-less-html", "jobs-description-content__text", or "jobs-box__html-content". For Indeed, look for "jobsearch-JobComponent-description". Include ALL text, not just the first paragraph.

HTML CONTENT (truncated):
${html.substring(0, 80000)}`;

    try {
      const result = await generateStructuredJson<Record<string, unknown>>(
        "fetch_job_url",
        requestId,
        (attempt) =>
          callGemini(
            attempt === 0
              ? extractPrompt
              : `${extractPrompt}\n\nKeep the retry response concise while preserving every required JSON field.`,
            "fetch_job_url",
          ),
      );
      return result.data;
    } catch (error) {
      if (error instanceof StructuredResponseError) {
        return { success: false, reason: error.code };
      }
      throw error;
    }
  } catch (error) {
    return { success: false, reason: (error as Error).message };
  }
}

export async function handleRequest(req: Request): Promise<Response> {
  const requestId = req.headers.get("x-request-id") ?? crypto.randomUUID();
  const responseHeaders = {
    ...corsHeaders,
    "Content-Type": "application/json",
    "x-request-id": requestId,
  };

  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: { ...corsHeaders, "x-request-id": requestId },
    });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing authorization" }), {
        status: 401,
        headers: responseHeaders,
      });
    }

    const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: { user }, error: authError } = await userClient.auth
      .getUser();
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: responseHeaders,
      });
    }

    const body: ActionRequest = await req.json();
    const { action, payload } = body;

    if (!SUPPORTED_ACTIONS.has(action)) {
      return new Response(
        JSON.stringify({ error: `Unknown action: ${action}` }),
        {
          status: 400,
          headers: responseHeaders,
        },
      );
    }

    const serviceClient = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

    // Quota check for all AI actions
    const quotaResult = await checkAndIncrementQuota(
      serviceClient,
      user.id,
      action,
    );
    if (!quotaResult.allowed) {
      return new Response(JSON.stringify(quotaResult.quotaError), {
        status: 429,
        headers: responseHeaders,
      });
    }

    if (action === "fetch_job_url") {
      const result = await handleFetchJobUrl(payload, requestId);

      // Log usage for fetch_job_url too
      const fetchTokenEstimate = 500;
      await serviceClient.from("ai_usage_log").insert({
        user_id: user.id,
        function_name: action,
        model: GEMINI_MODEL,
        input_tokens: fetchTokenEstimate,
        output_tokens: fetchTokenEstimate,
        cost_estimate_usd: calculateCost(
          fetchTokenEstimate,
          fetchTokenEstimate,
        ),
      });

      return new Response(JSON.stringify(result), {
        headers: responseHeaders,
      });
    }

    const prompt = buildPrompt(action, payload);
    let responseData: ActionResponse;
    let usageMetadata = {
      promptTokenCount: 0,
      candidatesTokenCount: 0,
      cachedContentTokenCount: 0,
      thoughtsTokenCount: 0,
      totalTokenCount: 0,
    };

    if (isJsonAction(action)) {
      try {
        const result = await generateStructuredJson<ActionResponse>(
          action,
          requestId,
          (attempt) =>
            callGemini(
              attempt === 0
                ? prompt
                : `${prompt}\n\nKeep the retry response concise while preserving every required JSON field.`,
              action,
            ),
        );
        responseData = result.data;
        usageMetadata = result.usageMetadata;
      } catch (error) {
        if (error instanceof StructuredResponseError) {
          return new Response(
            JSON.stringify({
              error: error.code,
              retryable: true,
              request_id: error.requestId,
            }),
            {
              status: 502,
              headers: responseHeaders,
            },
          );
        }
        throw error;
      }
    } else {
      const geminiResult = await callGemini(prompt, action);
      usageMetadata = {
        promptTokenCount: geminiResult.usageMetadata?.promptTokenCount ?? 0,
        candidatesTokenCount:
          geminiResult.usageMetadata?.candidatesTokenCount ?? 0,
        cachedContentTokenCount:
          geminiResult.usageMetadata?.cachedContentTokenCount ?? 0,
        thoughtsTokenCount: geminiResult.usageMetadata?.thoughtsTokenCount ?? 0,
        totalTokenCount: geminiResult.usageMetadata?.totalTokenCount ?? 0,
      };
      const text = geminiResult.candidates?.[0]?.content?.parts?.map((part) =>
        part.text ?? ""
      ).join("") ?? "";
      responseData = { content: text };
    }

    await serviceClient.from("ai_usage_log").insert({
      user_id: user.id,
      function_name: action,
      model: GEMINI_MODEL,
      input_tokens: usageMetadata.promptTokenCount,
      output_tokens: usageMetadata.candidatesTokenCount +
        usageMetadata.thoughtsTokenCount,
      cost_estimate_usd: calculateCost(
        usageMetadata.promptTokenCount,
        usageMetadata.candidatesTokenCount + usageMetadata.thoughtsTokenCount,
      ),
    });

    // Add quota warning for last resume parse
    if (action === "parse_resume" && quotaResult.isLastResumeParse) {
      responseData["quota_warning"] = "last_resume_parse";
    }

    return new Response(JSON.stringify(responseData), {
      headers: responseHeaders,
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), {
      status: 500,
      headers: responseHeaders,
    });
  }
}

if (import.meta.main) {
  Deno.serve(handleRequest);
}
