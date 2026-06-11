import { useLocalStorage } from '@vueuse/core'

import type { components } from '@/api/schema'

type LoginResponse = components['schemas']['LoginResponse']

export type AuthSession = {
  userId?: number
  groupId?: number | null
  role?: number
  username?: string
  email?: string
}

const AUTH_SESSION_KEY = 'authSession'

function readAuthSession(raw: string): AuthSession | null {
  if (!raw || raw === '[object Object]') {
    return null
  }

  try {
    const parsed: unknown = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? (parsed as AuthSession) : null
  } catch {
    return null
  }
}

export const authSession = useLocalStorage<AuthSession | null>(AUTH_SESSION_KEY, null, {
  serializer: {
    read: readAuthSession,
    write: (value) => (value == null ? '' : JSON.stringify(value)),
  },
})

export function setAuthSession(data: LoginResponse) {
  authSession.value = {
    userId: data.userId,
    groupId: data.groupId,
    role: data.role,
    username: data.username,
    email: data.email,
  }
}

export function clearAuthSession() {
  authSession.value = null
}

export function hasGroup(session: AuthSession | null = authSession.value) {
  return session?.groupId != null && session.groupId > 0
}
