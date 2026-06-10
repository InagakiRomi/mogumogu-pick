export const RESTAURANT_API_ERROR_MESSAGES: Record<string, string> = {
  'No available restaurants found for this filter': '此篩選條件下沒有可抽選的餐廳',
  'Restaurant not found': '找不到此餐廳',
  'Restaurant belongs to another group': '此餐廳不屬於您的群組',
  'Restaurant is archived': '此餐廳已封存',
  'User not found': '找不到使用者',
  '該帳號未加入群組': '該帳號未加入群組',
}

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
