export type RestaurantCategoryOption = {
  label: string
  value: string
}

export const ALL_CATEGORIES_VALUE = 'all'
export const DEFAULT_RESTAURANT_IMAGE = '/images/defaultRestaurant.jpg'

export const RESTAURANT_CATEGORY_OPTIONS: RestaurantCategoryOption[] = [
  { label: '主食', value: '1' },
  { label: '輕食', value: '2' },
  { label: '飲料', value: '3' },
]

export const RESTAURANT_CATEGORY_OPTIONS_WITH_ALL: RestaurantCategoryOption[] = [
  { label: '全部', value: ALL_CATEGORIES_VALUE },
  ...RESTAURANT_CATEGORY_OPTIONS,
]

export function formatRestaurantDate(value?: string): string {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString('zh-TW', { hour12: false })
}

export function formatRestaurantCategoryLabel(categoryId?: number): string {
  if (categoryId == null) {
    return '-'
  }

  return (
    RESTAURANT_CATEGORY_OPTIONS.find((option) => option.value === String(categoryId))?.label ?? '-'
  )
}
