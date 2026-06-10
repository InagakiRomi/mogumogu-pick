<script setup lang="ts">
import { computed, ref } from 'vue'
import type { components } from '@/api/schema'
import client from '@/api/client'
import AuthFeedback from '@/components/auth/AuthFeedback.vue'
import RandomRestaurantResultCard from '@/components/restaurant/RandomRestaurantResultCard.vue'
import { Button } from '@/components/ui/button'
import AuthPageCard from '@/components/auth/AuthPageCard.vue'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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
const feedback = ref('')
const feedbackType = ref<'error' | 'success'>('error')

const selectedCategoryLabel = computed(() => {
  const matched = categoryOptions.find((option) => option.value === selectedCategory.value)
  return matched?.label ?? '未分類'
})

const canChooseRestaurant = computed(() => !!currentRestaurant.value?.restaurantId && !isChooseLoading.value)

function setFeedback(message: string, type: 'error' | 'success' = 'error') {
  feedback.value = message
  feedbackType.value = type
}

function clearFeedback() {
  feedback.value = ''
}

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
    setFeedback(RESTAURANT_FEEDBACK_MESSAGES.clearPool.success, 'success')
  } catch (error) {
    setFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.clearPool.fallback))
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
    setFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.random.fallback))
  } finally {
    isRandomLoading.value = false
  }
}

async function handleChooseRestaurant() {
  if (!currentRestaurant.value?.restaurantId) {
    setFeedback(RESTAURANT_FEEDBACK_MESSAGES.chooseRequiresRandom)
    return
  }

  clearFeedback()
  isChooseLoading.value = true

  try {
    const { data, error } = await client.PATCH('/restaurants/my/choose/{id}', {
      params: {
        path: {
          id: currentRestaurant.value.restaurantId,
        },
      },
    })

    if (error) {
      throw error
    }

    currentRestaurant.value = data ?? currentRestaurant.value
    setFeedback(RESTAURANT_FEEDBACK_MESSAGES.choose.success, 'success')
  } catch (error) {
    setFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.choose.fallback))
  } finally {
    isChooseLoading.value = false
  }
}
</script>

<template>
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-cover bg-center bg-no-repeat px-4 py-6"
  >
    <AuthPageCard>
      <div class="space-y-5">
        <div class="space-y-2">
          <Label for="restaurant-category" class="font-bold text-muted-foreground">篩選類別</Label>
          <Select :model-value="selectedCategory" @update:model-value="handleCategoryChange">
            <SelectTrigger
              id="restaurant-category"
              class="h-[42px] w-full rounded-md border-border bg-muted/90 px-2.5 text-base text-popover-foreground shadow-[inset_0_1px_2px_rgba(121,73,52,0.12)]"
            >
              <SelectValue placeholder="選擇類別" />
            </SelectTrigger>
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

        <RandomRestaurantResultCard
          :restaurant="currentRestaurant"
          :category-label="selectedCategoryLabel"
        />

        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Button
            :disabled="isRandomLoading"
            class="h-11 rounded-lg bg-linear-to-br from-[#d78867] to-[#c96d57] text-primary-foreground shadow-[0_8px_18px_rgba(138,73,52,0.3)] hover:-translate-y-0.5"
            @click="handleRandomRestaurant"
          >
            {{ isRandomLoading ? '抽選中...' : '抽！' }}
          </Button>
          <Button
            variant="outline"
            :disabled="!canChooseRestaurant"
            class="h-11 rounded-lg border-[rgba(146,80,58,0.32)] bg-[rgba(255,248,241,0.9)] text-[rgba(84,44,30,0.95)] hover:bg-[rgba(255,236,223,0.95)]"
            @click="handleChooseRestaurant"
          >
            {{ isChooseLoading ? '送出中...' : '就決定選這間！' }}
          </Button>
        </div>
      </div>

      <AuthFeedback v-if="feedback" :type="feedbackType" :message="feedback" />
    </AuthPageCard>
  </main>
</template>
