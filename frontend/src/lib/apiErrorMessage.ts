import { AUTH_API_ERROR_MESSAGES, AUTH_API_ERROR_PATTERNS } from '@/lib/authErrorMessages'
import { RESTAURANT_API_ERROR_MESSAGES } from '@/lib/restaurantErrorMessages'

const API_ERROR_MESSAGES: Record<string, string> = {
  ...AUTH_API_ERROR_MESSAGES,
  ...RESTAURANT_API_ERROR_MESSAGES,
}

const API_ERROR_PATTERNS: Array<{ pattern: RegExp; message: string }> = [
  ...AUTH_API_ERROR_PATTERNS,
]

function translateErrorPart(part: string): string {
  const trimmed = part.trim()
  if (!trimmed) {
    return trimmed
  }

  const exact = API_ERROR_MESSAGES[trimmed]
  if (exact) {
    return exact
  }

  for (const { pattern, message } of API_ERROR_PATTERNS) {
    if (pattern.test(trimmed)) {
      return message
    }
  }

  return trimmed
}

export function translateApiErrorMessage(message: string): string {
  return message
    .split(';')
    .map((part) => translateErrorPart(part))
    .join('；')
}

export function extractApiErrorMessage(error: unknown): string | undefined {
  if (typeof error === 'string') {
    return error
  }

  if (error && typeof error === 'object') {
    const maybeError = error as {
      message?: string
      error?: { message?: string }
    }
    return maybeError.error?.message ?? maybeError.message
  }

  return undefined
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  const rawMessage = extractApiErrorMessage(error)
  if (!rawMessage) {
    return fallback
  }

  return translateApiErrorMessage(rawMessage)
}
