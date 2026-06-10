import { useLocalStorage } from '@vueuse/core'

export const authToken = useLocalStorage<string | null>('authToken', null)
