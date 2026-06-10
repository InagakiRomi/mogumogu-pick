const API_ERROR_MESSAGES: Record<string, string> = {
  'Full authentication is required to access this resource': '請先登入後再操作',
  Unauthorized: '尚未登入或登入已過期',
  'Invalid email or password': '電子郵件或密碼錯誤',
  'This email is already registered': '此電子郵件已被註冊',
  'Validation failed': '資料驗證失敗',
  'email is required': '請輸入電子郵件',
  'password is required': '請輸入密碼',
  'username is required': '請輸入使用者名稱',
  'email must be a valid email address': '電子郵件格式不正確',
  'No available restaurants found for this filter': '此篩選條件下沒有可抽選的餐廳',
  'Restaurant not found': '找不到此餐廳',
  'Restaurant belongs to another group': '此餐廳不屬於您的群組',
  'Restaurant is archived': '此餐廳已封存',
  'User not found': '找不到使用者',
  '該帳號未加入群組': '該帳號未加入群組',
}

const API_ERROR_PATTERNS: Array<{ pattern: RegExp; message: string }> = [
  { pattern: /^User with email ".+" not found$/, message: '電子郵件或密碼錯誤' },
]

export const AUTH_FEEDBACK_MESSAGES = {
  login: {
    success: '登入成功',
    fallback: '登入失敗，請確認帳號密碼',
    missingToken: '登入失敗，伺服器未回傳有效的登入憑證',
  },
  register: {
    success: '註冊成功，請使用新帳號登入',
    fallback: '註冊失敗，請稍後再試',
  },
} as const

export const RESTAURANT_FEEDBACK_MESSAGES = {
  random: {
    fallback: '取得抽選餐廳失敗',
  },
  choose: {
    success: '選擇成功！今天就吃這間。',
    fallback: '選擇餐廳失敗',
  },
  clearPool: {
    success: '抽籤紀錄已清除，開始新的抽選！',
    fallback: '重置抽籤池失敗',
  },
  chooseRequiresRandom: '請先抽選餐廳。',
} as const

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

function translateApiErrorMessage(message: string): string {
  return message
    .split(';')
    .map((part) => translateErrorPart(part))
    .join('；')
}

function extractApiErrorMessage(error: unknown): string | undefined {
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
