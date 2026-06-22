import {
  generateStructuredJson,
  StructuredResponseError,
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

Deno.test("returns valid structured JSON without retrying", async () => {
  let attempts = 0;

  const result = await generateStructuredJson<{ match_score: number }>(async () => {
    attempts += 1;
    return response('{"match_score":82}');
  });

  if (result.data.match_score !== 82) throw new Error("JSON was not parsed");
  if (attempts !== 1) throw new Error(`expected 1 attempt, got ${attempts}`);
  if (result.usageMetadata.promptTokenCount !== 10) throw new Error("prompt usage was not preserved");
  if (result.usageMetadata.candidatesTokenCount !== 20) throw new Error("output usage was not preserved");
});

Deno.test("retries once after a truncated response and combines token usage", async () => {
  const responses = [
    response('{"requirements":["five years', "MAX_TOKENS", 100, 200),
    response('{"requirements":["five years"]}', "STOP", 110, 30),
  ];
  const seenAttempts: number[] = [];

  const result = await generateStructuredJson<{ requirements: string[] }>(async (attempt) => {
    seenAttempts.push(attempt);
    return responses[attempt];
  });

  if (JSON.stringify(seenAttempts) !== JSON.stringify([0, 1])) {
    throw new Error(`unexpected attempts: ${JSON.stringify(seenAttempts)}`);
  }
  if (result.data.requirements[0] !== "five years") throw new Error("retry result was not returned");
  if (result.usageMetadata.promptTokenCount !== 210) throw new Error("prompt usage was not combined");
  if (result.usageMetadata.candidatesTokenCount !== 230) throw new Error("output usage was not combined");
});

Deno.test("throws a sanitized error after two malformed responses", async () => {
  const sensitive = "Bearer secret-session-token";
  let attempts = 0;

  try {
    await generateStructuredJson(async () => {
      attempts += 1;
      return response(`{\"broken\":\"${sensitive}`);
    });
    throw new Error("expected structured response failure");
  } catch (error) {
    if (!(error instanceof StructuredResponseError)) throw error;
    if (error.code !== "ai_response_invalid") throw new Error(`unexpected code: ${error.code}`);
    if (error.message.includes(sensitive)) throw new Error("error exposed raw model output");
  }

  if (attempts !== 2) throw new Error(`expected 2 attempts, got ${attempts}`);
});
