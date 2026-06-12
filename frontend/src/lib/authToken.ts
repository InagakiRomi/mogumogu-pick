import { useLocalStorage } from '@vueuse/core'

const AUTH_TOKEN_KEY = 'authToken'
const JSON_STRING_PATTERN = /^"(?:[^"\\\u0000-\u001F]|\\["\\/bfnrt]|\\u[0-9a-fA-F]{4})*"$/

function readAuthToken(raw: string): string | null {
  if (!raw) {
    return null
  }

  // 相容舊版 JSON 序列化格式
  if (raw.startsWith('"')) {
    if (!JSON_STRING_PATTERN.test(raw)) {
      return raw
    }

    const parsed = JSON.parse(raw) as unknown
    if (typeof parsed === 'string') {
      return parsed.length > 0 ? parsed : null
    }
    return raw
  }

  return raw
}

export const authToken = useLocalStorage<string | null>(AUTH_TOKEN_KEY, null, {
  serializer: {
    read: readAuthToken,
    write: (value) => value ?? '',
  },
})
