import { computed, onMounted, ref } from 'vue'
import type { components } from '@/api/schema'
import client from '@/api/client'
import { ALL_CATEGORIES_VALUE } from '@/constants/restaurant'

export type RestaurantCategoryOption = {
  label: string
  value: string
}

type RestaurantCategory = components['schemas']['RestaurantCategoryResponse']

function toCategoryOption(category: RestaurantCategory): RestaurantCategoryOption {
  return {
    label: category.categoryName ?? '-',
    value: String(category.categoryId ?? ''),
  }
}

export function useRestaurantCategories() {
  const categories = ref<RestaurantCategoryOption[]>([])
  const isLoading = ref(false)
  const errorMessage = ref<string | null>(null)

  const categoryOptionsWithAll = computed<RestaurantCategoryOption[]>(() => [
    { label: '全部', value: ALL_CATEGORIES_VALUE },
    ...categories.value,
  ])

  const defaultCategoryId = computed(() => categories.value[0]?.value ?? '')

  async function fetchCategories() {
    isLoading.value = true
    errorMessage.value = null

  const { data, error } = await client.GET('/restaurant-categories')
  if (error) {
      categories.value = []
      errorMessage.value = '取得餐廳分類失敗'
      console.error(error)
      isLoading.value = false
    return
    }

  categories.value = (data ?? []).map(toCategoryOption)
  isLoading.value = false
  }

  onMounted(() => {
    void fetchCategories()
  })

  return {
    categories,
    categoryOptions: categories,
    categoryOptionsWithAll,
    defaultCategoryId,
    isLoading,
    errorMessage,
    fetchCategories,
  }
}
