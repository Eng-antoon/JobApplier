import {
  buildGenerationConfig,
  buildPrompt,
  generateStructuredJson,
  StructuredResponseError,
  validateStructuredData,
} from "./index.ts";

function response(
  text: string,
  finishReason = "STOP",
  promptTokens = 10,
  outputTokens = 20,
) {
  return {
    candidates: [{
      finishReason,
      content: { parts: [{ text }] },
    }],
    usageMetadata: {
      promptTokenCount: promptTokens,
      candidatesTokenCount: outputTokens,
    },
  };
}

const validAnalysis = {
  requirements: ["One", "Two", "Three", "Four", "Five"],
  match_score: 82,
  matched: ["Kotlin"],
  gaps: ["Terraform"],
  partial: [],
  suggestions: ["Show measurable delivery outcomes."],
};

Deno.test("structured job analysis disables thinking and uses bounded output", () => {
  const config = buildGenerationConfig("analyze_jd");

  if (config.responseMimeType !== "application/json") {
    throw new Error("JSON MIME type is missing");
  }
  if (config.maxOutputTokens !== 4096) {
    throw new Error(`unexpected output budget: ${config.maxOutputTokens}`);
  }
  if (
    JSON.stringify(config.thinkingConfig) !==
      JSON.stringify({ thinkingBudget: 0 })
  ) {
    throw new Error(
      `thinking was not disabled: ${JSON.stringify(config.thinkingConfig)}`,
    );
  }
});

Deno.test("long job descriptions keep the supported input boundary and bounded response instructions", () => {
  const description = `${"A".repeat(4000)}UNSUPPORTED_TAIL_SENTINEL${
    "Z".repeat(2475)
  }`;
  const profile = "Senior Android engineer ".repeat(120);

  const prompt = buildPrompt("analyze_jd", {
    job_description: description,
    user_profile: profile,
  });

  if (!prompt.includes("A".repeat(4000))) {
    throw new Error("supported job description content was dropped");
  }
  if (prompt.includes("UNSUPPORTED_TAIL_SENTINEL")) {
    throw new Error("job description exceeded its 4,000-character boundary");
  }
  if (!prompt.includes(profile)) {
    throw new Error("candidate profile was dropped");
  }
  if (
    !prompt.includes("5-12 distinct requirements") ||
    !prompt.includes("up to 8")
  ) {
    throw new Error("bounded output instructions are missing");
  }
});

Deno.test("job analysis validation enforces balanced list and score bounds", () => {
  validateStructuredData("analyze_jd", validAnalysis);

  for (
    const invalid of [
      {
        ...validAnalysis,
        requirements: validAnalysis.requirements.slice(0, 4),
      },
      {
        ...validAnalysis,
        requirements: [
          ...validAnalysis.requirements,
          "Six",
          "Seven",
          "Eight",
          "Nine",
          "Ten",
          "Eleven",
          "Twelve",
          "Thirteen",
        ],
      },
      { ...validAnalysis, match_score: 101 },
      {
        ...validAnalysis,
        gaps: Array.from({ length: 9 }, (_, index) => `Gap ${index}`),
      },
    ]
  ) {
    let rejected = false;
    try {
      validateStructuredData("analyze_jd", invalid);
    } catch {
      rejected = true;
    }
    if (!rejected) {
      throw new Error(
        `invalid analysis was accepted: ${JSON.stringify(invalid)}`,
      );
    }
  }
});

Deno.test("returns valid structured JSON without retrying", async () => {
  let attempts = 0;

  const result = await generateStructuredJson<{ match_score: number }>(
    "unknown",
    "request-1",
    async () => {
      attempts += 1;
      return response('{"match_score":82}');
    },
  );

  if (result.data.match_score !== 82) throw new Error("JSON was not parsed");
  if (attempts !== 1) throw new Error(`expected 1 attempt, got ${attempts}`);
  if (result.usageMetadata.promptTokenCount !== 10) {
    throw new Error("prompt usage was not preserved");
  }
  if (result.usageMetadata.candidatesTokenCount !== 20) {
    throw new Error("output usage was not preserved");
  }
});

Deno.test("joins all candidate text parts before parsing", async () => {
  const result = await generateStructuredJson<typeof validAnalysis>(
    "analyze_jd",
    "request-parts",
    async () => ({
      candidates: [{
        finishReason: "STOP",
        content: {
          parts: [
            {
              text:
                '{"requirements":["One","Two","Three","Four","Five"],"match_score":82,',
            },
            {
              text:
                '"matched":["Kotlin"],"gaps":[],"partial":[],"suggestions":[]}',
            },
          ],
        },
      }],
    }),
  );

  if (result.data.match_score !== 82) {
    throw new Error("multipart JSON was not parsed");
  }
});

Deno.test("retries once after a truncated response and combines token usage", async () => {
  const responses = [
    response('{"requirements":["five years', "MAX_TOKENS", 100, 200),
    response('{"requirements":["five years"]}', "STOP", 110, 30),
  ];
  const seenAttempts: number[] = [];

  const validRetry = JSON.stringify({
    ...validAnalysis,
    requirements: ["One", "Two", "Three", "Four", "Five"],
  });
  responses[1] = response(validRetry, "STOP", 110, 30);

  const result = await generateStructuredJson<typeof validAnalysis>(
    "analyze_jd",
    "request-2",
    async (attempt) => {
      seenAttempts.push(attempt);
      return responses[attempt];
    },
  );

  if (JSON.stringify(seenAttempts) !== JSON.stringify([0, 1])) {
    throw new Error(`unexpected attempts: ${JSON.stringify(seenAttempts)}`);
  }
  if (result.data.requirements[0] !== "One") {
    throw new Error("retry result was not returned");
  }
  if (result.usageMetadata.promptTokenCount !== 210) {
    throw new Error("prompt usage was not combined");
  }
  if (result.usageMetadata.candidatesTokenCount !== 230) {
    throw new Error("output usage was not combined");
  }
});

Deno.test("throws a sanitized error after two malformed responses", async () => {
  const sensitive = "Bearer secret-session-token";
  let attempts = 0;

  try {
    await generateStructuredJson("analyze_jd", "request-safe", async () => {
      attempts += 1;
      return response(`{\"broken\":\"${sensitive}`);
    });
    throw new Error("expected structured response failure");
  } catch (error) {
    if (!(error instanceof StructuredResponseError)) throw error;
    if (error.code !== "ai_response_invalid") {
      throw new Error(`unexpected code: ${error.code}`);
    }
    if (error.requestId !== "request-safe") {
      throw new Error(`unexpected request ID: ${error.requestId}`);
    }
    if (error.message.includes(sensitive)) {
      throw new Error("error exposed raw model output");
    }
  }

  if (attempts !== 2) throw new Error(`expected 2 attempts, got ${attempts}`);
});
