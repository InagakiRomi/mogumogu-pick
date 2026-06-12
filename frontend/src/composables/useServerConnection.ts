import { onMounted, onUnmounted, ref } from 'vue'

export type ServerConnectionStatus = 'checking' | 'connected' | 'disconnected'

const SERVER_CHECK_PATH = '/v3/api-docs'
const SERVER_CHECK_TIMEOUT_MS = 5000
const DISCONNECTED_RECHECK_INTERVAL_MS = 60_000

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

export function useServerConnection() {
  const status = ref<ServerConnectionStatus>('checking')
  let recheckTimer: ReturnType<typeof setInterval> | null = null

  function clearRecheckTimer() {
    if (recheckTimer != null) {
      clearInterval(recheckTimer)
      recheckTimer = null
    }
  }

  function scheduleRecheck() {
    clearRecheckTimer()
    if (status.value !== 'disconnected') {
      return
    }

    recheckTimer = setInterval(() => {
      void checkConnection()
    }, DISCONNECTED_RECHECK_INTERVAL_MS)
  }

  async function checkConnection() {
    status.value = 'checking'

    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), SERVER_CHECK_TIMEOUT_MS)

      const response = await fetch(`${apiBaseUrl}${SERVER_CHECK_PATH}`, {
        method: 'GET',
        signal: controller.signal,
      })

      clearTimeout(timeoutId)
      status.value = response.ok ? 'connected' : 'disconnected'
    } catch {
      status.value = 'disconnected'
    }

    if (status.value === 'disconnected') {
      scheduleRecheck()
    } else {
      clearRecheckTimer()
    }
  }

  onMounted(() => {
    void checkConnection()
  })

  onUnmounted(() => {
    clearRecheckTimer()
  })

  return {
    status,
    checkConnection,
  }
}
