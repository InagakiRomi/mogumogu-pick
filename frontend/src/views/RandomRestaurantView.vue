<script setup lang="ts">
import { computed, ref } from 'vue'
import type { components } from '@/api/schema'
import client from '@/api/client'
import WarmButton from '@/components/warm/WarmButton.vue'
import WarmInfoCard from '@/components/warm/WarmInfoCard.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import WarmPanel from '@/components/warm/WarmPanel.vue'
import WarmSelectTrigger from '@/components/warm/WarmSelectTrigger.vue'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectValue } from '@/components/ui/select'
import { getApiErrorMessage, RESTAURANT_FEEDBACK_MESSAGES } from '@/lib/apiErrorMessage'
type CategoryOption = {
  label: string
  value: string
}

type RestaurantResult = components['schemas']['RestaurantResponse']

const ALL_CATEGORIES_VALUE = 'all'

const categoryOptions: CategoryOption[] = [
  { label: '全部', value: ALL_CATEGORIES_VALUE },
  { label: '主食', value: '1' },
  { label: '輕食', value: '2' },
  { label: '飲料', value: '3' },
]

const selectedCategory = ref(ALL_CATEGORIES_VALUE)
const currentRestaurant = ref<RestaurantResult | null>(null)
const isRandomLoading = ref(false)
const isChooseLoading = ref(false)
const { showFeedback, clearFeedback } = useFeedbackDialog()

const selectedCategoryLabel = computed(() => {
  const matched = categoryOptions.find((option) => option.value === selectedCategory.value)
  return matched?.label ?? '未分類'
})

const canChooseRestaurant = computed(() => !!currentRestaurant.value?.restaurantId && !isChooseLoading.value)

async function resetRandomPool() {
  const { error } = await client.POST('/restaurants/my/random/clear')
  if (error) {
    throw error
  }
}

async function handleCategoryChange(value: unknown) {
  selectedCategory.value = typeof value === 'string' ? value : ALL_CATEGORIES_VALUE
  currentRestaurant.value = null
  clearFeedback()

  try {
    await resetRandomPool()
    showFeedback(RESTAURANT_FEEDBACK_MESSAGES.clearPool.success, 'success')
  } catch (error) {
    showFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.clearPool.fallback))
  }
}

async function handleRandomRestaurant() {
  clearFeedback()
  isRandomLoading.value = true

  try {
    const categoryId =
      selectedCategory.value !== ALL_CATEGORIES_VALUE ? Number(selectedCategory.value) : undefined
    const { data, error } = await client.GET('/restaurants/my/random', {
      params: {
        query: {
          categoryId,
        },
      },
    })

    if (error) {
      throw error
    }

    currentRestaurant.value = data ?? null
  } catch (error) {
    showFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.random.fallback))
  } finally {
    isRandomLoading.value = false
  }
}

async function handleChooseRestaurant() {
  if (!currentRestaurant.value?.restaurantId) {
    showFeedback(RESTAURANT_FEEDBACK_MESSAGES.chooseRequiresRandom)
    return
  }

  clearFeedback()
  isChooseLoading.value = true

  try {
    const { error } = await client.PATCH('/restaurants/my/choose/{id}', {
      params: {
        path: {
          id: currentRestaurant.value.restaurantId,
        },
      },
    })

    if (error) {
      throw error
    }

    const chosenName = currentRestaurant.value.restaurantName ?? '這間'
    currentRestaurant.value = null
    showFeedback(RESTAURANT_FEEDBACK_MESSAGES.choose.success(chosenName), 'success')
  } catch (error) {
    showFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.choose.fallback))
  } finally {
    isChooseLoading.value = false
  }
}
</script>

<template>
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-cover bg-center bg-no-repeat px-4 py-6"
  >
    <WarmPanel>
      <div class="space-y-5">
        <div class="space-y-2">
          <Label for="restaurant-category" class="font-bold text-muted-foreground">篩選類別</Label>
          <Select :model-value="selectedCategory" @update:model-value="handleCategoryChange">
            <WarmSelectTrigger id="restaurant-category">
              <SelectValue placeholder="選擇類別" />
            </WarmSelectTrigger>
            <SelectContent class="border-border bg-card text-popover-foreground">
              <SelectItem
                v-for="option in categoryOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <WarmInfoCard
          :restaurant="currentRestaurant"
          :category-label="selectedCategoryLabel"
          :is-drawing="isRandomLoading"
        />

        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <WarmButton :disabled="isRandomLoading" @click="handleRandomRestaurant">
            {{ isRandomLoading ? '抽選中...' : '抽！' }}
          </WarmButton>
          <WarmButton
            :disabled="!canChooseRestaurant"
            variant="outline-standard"
            @click="handleChooseRestaurant"
          >
            {{ isChooseLoading ? '送出中...' : '就決定選這間！' }}
          </WarmButton>
        </div>
      </div>
    </WarmPanel>
  </main>
</template>
