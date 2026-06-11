import { useLocalStorage } from '@vueuse/core'

const AUTH_TOKEN_KEY = 'authToken'

function readAuthToken(raw: string): string | null {
  if (!raw) {
    return null
  }

  // 相容舊版 JSON 序列化格式
  if (raw.startsWith('"')) {
    try {
      const parsed: unknown = JSON.parse(raw)
      return typeof parsed === 'string' && parsed.length > 0 ? parsed : null
    } catch {
      return raw
    }
  }

  return raw
}

export const authToken = useLocalStorage<string | null>(AUTH_TOKEN_KEY, null, {
  serializer: {
    read: readAuthToken,
    write: (value) => value ?? '',
  },
})
