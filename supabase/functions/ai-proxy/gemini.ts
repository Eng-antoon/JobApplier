const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

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

const MAX_TOKENS_MAP: Record<string, number> = {
  analyze_jd: 1000,
  generate_cover_letter: 600,
  generate_cover_email: 400,
  answer_question: 400,
  detect_new_data: 500,
  parse_resume: 4000,
  generate_suggestions: 3000,
  fetch_job_url: 2000,
};

export async function callGemini(prompt: string, action: string): Promise<GeminiResponse> {
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
    throw new Error(`Gemini API error (${response.status}): ${errorText}`);
  }

  return await response.json();
}
