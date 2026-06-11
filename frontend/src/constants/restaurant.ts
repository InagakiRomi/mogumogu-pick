export const ALL_CATEGORIES_VALUE = 'all'
export const DEFAULT_RESTAURANT_IMAGE = '/images/defaultRestaurant.jpg'

export function formatOptionalDate(value?: string): string {
  return value?.trim() || '-'
}
