import { onMounted, onUnmounted, ref } from 'vue'

import { wakeServer } from '@/api/wakeServer'

export type ServerConnectionStatus = 'checking' | 'connected' | 'disconnected'

const DISCONNECTED_RECHECK_INTERVAL_MS = 60_000

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

    const result = await wakeServer()
    status.value = result.ok ? 'connected' : 'disconnected'

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
