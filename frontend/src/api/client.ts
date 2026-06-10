import createClient, { type Middleware } from 'openapi-fetch'

import { authToken } from '@/lib/authToken'
import type { paths } from './schema'

const authMiddleware: Middleware = {
  onRequest({ request, schemaPath }) {
    if (!schemaPath.startsWith('/auth/') && authToken.value) {
      request.headers.set('Authorization', `Bearer ${authToken.value}`)
    }
    return request
  },
  onResponse({ response, schemaPath }) {
    if (response.status === 401 && !schemaPath.startsWith('/auth/')) {
      authToken.value = null
      window.location.assign(import.meta.env.BASE_URL || '/')
    }
    return undefined
  },
}

const client = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL || '',
})

client.use(authMiddleware)

export default client
