import { computed, onMounted, ref } from 'vue'
import type { components } from '@/api/schema'
import client from '@/api/client'

export const ALL_CATEGORIES_VALUE = 'all'

type RestaurantCategoryOption = {
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

  const categoryOptionsWithAll = computed<RestaurantCategoryOption[]>(() => [
    { label: '全部', value: ALL_CATEGORIES_VALUE },
    ...categories.value,
  ])

  const defaultCategoryId = computed(() => categories.value[0]?.value ?? '')

  async function fetchCategories() {
    const { data, error } = await client.GET('/restaurant-categories')
    if (error) {
      categories.value = []
      return
    }

    categories.value = (data ?? []).map(toCategoryOption)
  }

  onMounted(() => {
    void fetchCategories()
  })

  return {
    categories,
    categoryOptionsWithAll,
    defaultCategoryId,
  }
}
