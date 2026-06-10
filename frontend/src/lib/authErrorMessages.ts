export const AUTH_API_ERROR_MESSAGES: Record<string, string> = {
  'Invalid email or password': '電子郵件或密碼錯誤',
  'This email is already registered': '此電子郵件已被註冊',
  'Invalid role': '帳號類型無效',
  'SYSTEM_ADMIN already exists': '系統管理員已存在',
  'Validation failed': '資料驗證失敗',
  'email is required': '請輸入電子郵件',
  'password is required': '請輸入密碼',
  'username is required': '請輸入使用者名稱',
  'email must be a valid email address': '電子郵件格式不正確',
  'username size is out of allowed range': '使用者名稱不可超過 64 個字',
  'password size is out of allowed range': '密碼不可超過 128 個字',
}

export const AUTH_API_ERROR_PATTERNS: Array<{ pattern: RegExp; message: string }> = [
  { pattern: /^User with email ".+" not found$/, message: '電子郵件或密碼錯誤' },
]

export const AUTH_FEEDBACK_MESSAGES = {
  login: {
    success: '登入成功',
    fallback: '登入失敗，請確認帳號密碼',
  },
  register: {
    success: '註冊成功，請使用新帳號登入',
    fallback: '註冊失敗，請稍後再試',
  },
} as const
