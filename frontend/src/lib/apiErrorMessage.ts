import {
  AUTH_API_ERROR_MESSAGES,
  AUTH_API_ERROR_PATTERNS,
} from '@/lib/authErrorMessages'

function translateErrorPart(part: string): string {
  const trimmed = part.trim()
  if (!trimmed) {
    return trimmed
  }

  const exact = AUTH_API_ERROR_MESSAGES[trimmed]
  if (exact) {
    return exact
  }

  for (const { pattern, message } of AUTH_API_ERROR_PATTERNS) {
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
