export function describeApiError(error: unknown, fallback = "Resposta inesperada do servidor."): string {
  if (typeof error === "object" && error != null && "message" in error && typeof error.message === "string") {
    return error.message;
  }
  return fallback;
}

export function readFieldErrors(error: unknown): Record<string, string> {
  if (
    typeof error === "object" &&
    error != null &&
    "fieldErrors" in error &&
    typeof error.fieldErrors === "object" &&
    error.fieldErrors != null
  ) {
    return error.fieldErrors as Record<string, string>;
  }
  return {};
}
