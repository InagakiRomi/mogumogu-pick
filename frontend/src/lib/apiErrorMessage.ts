const API_ERROR_MESSAGES: Record<string, string> = {
  'Full authentication is required to access this resource': '請先登入後再操作',
  Unauthorized: '尚未登入或登入已過期',
  Forbidden: '無權限執行此操作',
  'Internal Server Error': '伺服器發生錯誤，請稍後再試',
  'Invalid email or password': '電子郵件或密碼錯誤',
  'Invalid role': '角色無效',
  'This email is already registered': '此電子郵件已被註冊',
  'SYSTEM_ADMIN already exists': '系統管理員已存在',
  'Validation failed': '資料驗證失敗',
  'email is required': '請輸入電子郵件',
  'password is required': '請輸入密碼',
  'username is required': '請輸入使用者名稱',
  'email must be a valid email address': '電子郵件格式不正確',
  'password size is out of allowed range': '密碼不可超過 128 字',
  'username size is out of allowed range': '使用者名稱不可超過 64 字',
  'No available restaurants found for this filter': '此篩選條件下沒有可抽選的餐廳',
  'Restaurant not found': '找不到此餐廳',
  'Restaurant belongs to another group': '此餐廳不屬於您的群組',
  'Restaurant is archived': '該餐廳已被刪除',
  'restaurantId must not be null': '餐廳 ID 不可為空',
  'User not found': '找不到使用者',
  'User is not in a group': '使用者未加入群組',
  'Group not found': '找不到此群組',
  'displayOrderId already exists in this group': '此群組內已存在相同的顯示排序 ID',
  'displayOrderId already exists in this restaurant': '此餐廳內已存在相同的顯示排序 ID',
  'Category not found': '找不到此分類，或分類不屬於此群組',
  'Dish not found': '找不到此餐點',
  'dishId must not be null': '餐點 ID 不可為空',
  'groupId is required': '請指定群組',
  'categoryId is required': '請選擇分類',
  'restaurantName is required': '請輸入餐廳名稱',
  'restaurantName size is out of allowed range': '餐廳名稱不可超過 64 字',
  'note size is out of allowed range': '備註不可超過 512 字',
  'imageUrl size is out of allowed range': '圖片網址不可超過 512 字',
  'groupId must be greater than or equal to 0': '群組 ID 格式不正確',
  'categoryId must be greater than or equal to 0': '分類 ID 格式不正確',
  'displayOrderId must be greater than or equal to 0': '顯示排序不可小於 0',
  'selectedCount must be greater than or equal to 0': '選中次數不可小於 0',
  'restaurantId is required': '請指定餐廳',
  'dishName is required': '請輸入餐點名稱',
  'dishName size is out of allowed range': '餐點名稱不可超過 64 字',
  'price is required': '請輸入價格',
  'price must be greater than or equal to 0': '價格不可小於 0',
  'displayOrderId is required': '請輸入顯示排序',
  'displayOrderId must be greater than or equal to the minimum value': '顯示排序必須大於等於 1',
  'restaurantId must be greater than or equal to 0': '餐廳 ID 格式不正確',
  'page must be greater than or equal to the minimum value': '頁碼必須大於等於 1',
  'limit must be greater than or equal to the minimum value': '每頁筆數必須大於等於 1',
  'userId is required': '請指定使用者',
  'groupName is required': '請輸入群組名稱',
  'groupName size is out of allowed range': '群組名稱不可超過 64 字',
  'Target user not found': '找不到目標使用者',
  'You are already in this group': '您已在這個群組中',
  'Target user already belongs to a group': '目標使用者已加入其他群組',
  'Cannot remove yourself via this endpoint': '無法透過此操作移除自己',
  'Target user is not in your group': '目標使用者不在您的群組中',
  'Cannot remove current group admin': '無法移除目前的群組管理員',
  'Target user is already group admin': '目標使用者已是群組管理員',
  'Target user is a system admin': '目標使用者是系統管理員',
  'Please transfer group admin before leaving': '請先移轉群組管理權再退出',
  'Only group admin can perform this action': '只有群組管理員可以執行此操作',
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
  list: {
    fallback: '取得餐廳清單失敗',
  },
  history: {
    fallback: '查詢歷史紀錄失敗',
  },
  choose: {
    success: (restaurantName: string) => `選擇成功！今天就吃${restaurantName}。`,
    fallback: '選擇餐廳失敗',
  },
  clearPool: {
    success: '抽籤紀錄已清除，開始新的抽選！',
    fallback: '重置抽籤池失敗',
  },
  chooseRequiresRandom: '請先抽選餐廳。',
} as const

export const RESTAURANT_UPDATE_FEEDBACK_MESSAGES = {
  success: '修改餐廳成功',
  fallback: '修改餐廳失敗',
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

export function isArchivedRestaurantError(error: unknown): boolean {
  if (error && typeof error === 'object') {
    const maybeError = error as {
      message?: string
      error?: { message?: string; statusCode?: number }
    }
    const message = maybeError.error?.message ?? maybeError.message
    const statusCode = maybeError.error?.statusCode

    return statusCode === 410 || message === 'Restaurant is archived'
  }

  return false
}

export const ARCHIVED_RESTAURANT_MESSAGE = '該餐廳已被刪除'
