import client from '@/api/client'

const WAKE_TIMEOUT_MS = 30_000

export type WakeServerResult =
  | { ok: true }
  | { ok: false; reason: 'timeout' | 'unavailable' }

export async function wakeServer(): Promise<WakeServerResult> {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), WAKE_TIMEOUT_MS)

  try {
    const { data, error, response } = await client.GET('/health', {
      signal: controller.signal,
    })

    if (error || !response.ok || data?.status !== 'ok') {
      return { ok: false, reason: 'unavailable' }
    }

    return { ok: true }
  } catch {
    return { ok: false, reason: 'timeout' }
  } finally {
    clearTimeout(timeoutId)
  }
}
