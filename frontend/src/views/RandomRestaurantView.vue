<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { components } from '@/api/schema'
import client from '@/api/client'
import WarmAlertDialogShell from '@/components/feedback/WarmAlertDialogShell.vue'
import WarmButton from '@/components/warm/WarmButton.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import WarmPanel from '@/components/warm/WarmPanel.vue'
import WarmSelectTrigger from '@/components/warm/WarmSelectTrigger.vue'
import {
  ALL_CATEGORIES_VALUE,
  DEFAULT_RESTAURANT_IMAGE,
  RESTAURANT_CATEGORY_OPTIONS_WITH_ALL,
} from '@/constants/restaurant'
import {
  AlertDialog,
  AlertDialogDescription,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectValue } from '@/components/ui/select'
import { Separator } from '@/components/ui/separator'
import { getApiErrorMessage, RESTAURANT_FEEDBACK_MESSAGES } from '@/lib/apiErrorMessage'

type RestaurantResult = components['schemas']['RestaurantResponse']

const START_IMAGE = '/images/start.jpg'
const FLIP_DURATION_MS = 700
const cardClass =
  'border-[rgba(198,134,105,0.45)] bg-linear-to-br from-[rgba(255,248,241,0.92)] to-[rgba(255,233,219,0.86)] shadow-[0_12px_28px_rgba(95,57,41,0.2)]'

const isFlipping = ref(false)
const flipKey = ref(0)
const isPastMidpoint = ref(false)
const frontSnapshot = ref<RestaurantResult | null>(null)
const backSnapshot = ref<RestaurantResult | null | 'drawing'>('drawing')
const pendingRestaurant = ref<RestaurantResult | null>(null)
const imageLoadFailed = ref(false)

let midpointTimer: ReturnType<typeof setTimeout> | undefined

const categoryOptions = RESTAURANT_CATEGORY_OPTIONS_WITH_ALL

const router = useRouter()

const selectedCategory = ref(ALL_CATEGORIES_VALUE)
const currentRestaurant = ref<RestaurantResult | null>(null)
const isRandomLoading = ref(false)
const isChooseLoading = ref(false)
const isPostChooseDialogOpen = ref(false)
const chosenRestaurantId = ref<number | null>(null)
const chosenRestaurantName = ref('')
const { showFeedback, clearFeedback } = useFeedbackDialog()

const selectedCategoryLabel = computed(() => {
  const matched = categoryOptions.find((option) => option.value === selectedCategory.value)
  return matched?.label ?? '未分類'
})

const canChooseRestaurant = computed(() => !!currentRestaurant.value?.restaurantId && !isChooseLoading.value)

function resolveImage(restaurant: RestaurantResult | null): string {
  if (!restaurant) {
    return START_IMAGE
  }

  const imageUrl = restaurant.imageUrl?.trim()
  if (!imageUrl || imageLoadFailed.value) {
    return DEFAULT_RESTAURANT_IMAGE
  }

  return imageUrl
}

function resolveContent(restaurant: RestaurantResult | null | 'drawing') {
  if (restaurant === 'drawing') {
    return {
      name: '抽選中...',
      image: START_IMAGE,
      selectedCount: '—',
      lastSelectedAt: '—',
      updatedAt: '—',
      note: '—',
      isPlaceholder: true,
    }
  }

  return {
    name: restaurant?.restaurantName || '尚未抽選',
    image: resolveImage(restaurant),
    selectedCount: restaurant?.selectedCount ?? 0,
    lastSelectedAt: restaurant?.lastSelectedAt || '尚無紀錄',
    updatedAt: restaurant?.updatedAt || '尚無紀錄',
    note: restaurant?.note || '無',
    isPlaceholder: false,
  }
}

const frontContent = computed(() => {
  if (isFlipping.value) {
    return resolveContent(frontSnapshot.value)
  }

  return resolveContent(currentRestaurant.value ?? null)
})

const backContent = computed(() => {
  if (isFlipping.value || backSnapshot.value !== 'drawing') {
    return resolveContent(backSnapshot.value)
  }

  return resolveContent(currentRestaurant.value ?? null)
})

watch(
  () => [currentRestaurant.value?.restaurantId, currentRestaurant.value?.imageUrl] as const,
  () => {
    imageLoadFailed.value = false
  },
)

function applyBackSnapshot() {
  backSnapshot.value = pendingRestaurant.value ?? currentRestaurant.value ?? 'drawing'
}

watch(
  () => isRandomLoading.value,
  (drawing) => {
    if (!drawing) {
      return
    }

    flipKey.value += 1
    isFlipping.value = true
    isPastMidpoint.value = false
    frontSnapshot.value = currentRestaurant.value ?? null
    backSnapshot.value = 'drawing'
    pendingRestaurant.value = currentRestaurant.value ?? null

    clearTimeout(midpointTimer)
    midpointTimer = setTimeout(() => {
      isPastMidpoint.value = true
      applyBackSnapshot()
    }, FLIP_DURATION_MS / 2)
  },
)

watch(currentRestaurant, (restaurant) => {
  if (!isFlipping.value) {
    return
  }

  pendingRestaurant.value = restaurant ?? null

  if (isPastMidpoint.value) {
    applyBackSnapshot()
  }
})

function handleFlipEnd() {
  isFlipping.value = false
}

function handleImageError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img.src.endsWith(DEFAULT_RESTAURANT_IMAGE)) {
    return
  }

  imageLoadFailed.value = true
  img.src = DEFAULT_RESTAURANT_IMAGE
}

function handleClosePostChooseDialog() {
  isPostChooseDialogOpen.value = false
}

function handleViewChosenRestaurantDetail() {
  const restaurantId = chosenRestaurantId.value
  if (restaurantId == null) {
    return
  }

  isPostChooseDialogOpen.value = false
  void router.push({
    name: 'restaurant-detail',
    params: { id: restaurantId },
  })
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
  isPostChooseDialogOpen.value = false
  chosenRestaurantId.value = null
  chosenRestaurantName.value = ''
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
    chosenRestaurantId.value = currentRestaurant.value.restaurantId
    chosenRestaurantName.value = chosenName
    currentRestaurant.value = null
    isPostChooseDialogOpen.value = true
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

        <div class="flip-scene">
          <div
            :key="flipKey"
            class="flip-card"
            :class="{ 'flip-card--drawing': isFlipping }"
            @animationend="handleFlipEnd"
          >
            <div class="flip-face flip-face--front">
              <Card :class="cardClass">
                <CardHeader class="pb-3">
                  <CardTitle
                    class="flex items-center justify-between gap-3 text-[1.08rem] text-[rgba(84,44,30,0.95)]"
                  >
                    <span class="truncate">{{ frontContent.name }}</span>
                    <Badge
                      variant="outline"
                      class="border-[rgba(186,118,88,0.55)] bg-white/65 text-[rgba(95,57,41,0.9)]"
                    >
                      {{ selectedCategoryLabel }}
                    </Badge>
                  </CardTitle>
                </CardHeader>

                <CardContent class="space-y-4">
                  <img
                    :src="frontContent.image"
                    :alt="frontContent.name"
                    class="aspect-480/320 w-full rounded-lg border border-[rgba(198,134,105,0.45)] object-cover shadow-[inset_0_0_0_1px_rgba(255,255,255,0.35)]"
                    @error="handleImageError"
                  />

                  <Separator class="bg-[rgba(198,134,105,0.45)]" />

                  <div class="grid grid-cols-1 gap-2 text-sm text-[rgba(95,57,41,0.92)]">
                    <p><span class="font-semibold">選擇次數：</span>{{ frontContent.selectedCount }}</p>
                    <p><span class="font-semibold">最後選擇時間：</span>{{ frontContent.lastSelectedAt }}</p>
                    <p><span class="font-semibold">最後更新時間：</span>{{ frontContent.updatedAt }}</p>
                    <p><span class="font-semibold">備註：</span>{{ frontContent.note }}</p>
                  </div>
                </CardContent>
              </Card>
            </div>

            <div class="flip-face flip-face--back">
              <Card :class="cardClass">
                <CardHeader class="pb-3">
                  <CardTitle
                    class="flex items-center justify-between gap-3 text-[1.08rem] text-[rgba(84,44,30,0.95)]"
                  >
                    <span class="truncate">{{ backContent.name }}</span>
                    <Badge
                      variant="outline"
                      class="border-[rgba(186,118,88,0.55)] bg-white/65 text-[rgba(95,57,41,0.9)]"
                    >
                      {{ selectedCategoryLabel }}
                    </Badge>
                  </CardTitle>
                </CardHeader>

                <CardContent class="space-y-4">
                  <img
                    :src="backContent.image"
                    :alt="backContent.name"
                    class="aspect-480/320 w-full rounded-lg border border-[rgba(198,134,105,0.45)] object-cover shadow-[inset_0_0_0_1px_rgba(255,255,255,0.35)]"
                    :class="{ 'opacity-75': backContent.isPlaceholder }"
                    @error="handleImageError"
                  />

                  <Separator class="bg-[rgba(198,134,105,0.45)]" />

                  <div
                    class="grid grid-cols-1 gap-2 text-sm"
                    :class="
                      backContent.isPlaceholder
                        ? 'text-[rgba(95,57,41,0.55)]'
                        : 'text-[rgba(95,57,41,0.92)]'
                    "
                  >
                    <p><span class="font-semibold">選擇次數：</span>{{ backContent.selectedCount }}</p>
                    <p><span class="font-semibold">最後選擇時間：</span>{{ backContent.lastSelectedAt }}</p>
                    <p><span class="font-semibold">最後更新時間：</span>{{ backContent.updatedAt }}</p>
                    <p><span class="font-semibold">備註：</span>{{ backContent.note }}</p>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>

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

    <AlertDialog :open="isPostChooseDialogOpen" @update:open="isPostChooseDialogOpen = $event">
      <WarmAlertDialogShell>
        <AlertDialogTitle class="w-full text-center text-2xl font-bold text-[#5e3a28]">
          選擇成功！
        </AlertDialogTitle>
        <AlertDialogDescription
          class="w-full text-center text-xl font-semibold tracking-wide text-[#5e3a28]/90"
        >
          {{ RESTAURANT_FEEDBACK_MESSAGES.choose.success(chosenRestaurantName) }}
        </AlertDialogDescription>
        <div class="flex flex-wrap items-center justify-center gap-3">
          <WarmButton
            class="min-w-[120px]"
            variant="outline-standard"
            @click="handleClosePostChooseDialog"
          >
            關閉
          </WarmButton>
          <WarmButton class="min-w-[120px]" @click="handleViewChosenRestaurantDetail">
            查看詳細
          </WarmButton>
        </div>
      </WarmAlertDialogShell>
    </AlertDialog>
  </main>
</template>

<style scoped>
.flip-scene {
  perspective: 1400px;
}

.flip-card {
  position: relative;
  transform-style: preserve-3d;
  transform-origin: center center;
}

.flip-face {
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
}

.flip-face--front {
  position: relative;
}

.flip-face--back {
  position: absolute;
  inset: 0;
  transform: rotateY(180deg);
}

.flip-card--drawing {
  animation: page-flip-once 0.7s ease-in-out forwards;
}

@keyframes page-flip-once {
  from {
    transform: rotateY(0deg);
  }

  to {
    transform: rotateY(180deg);
  }
}
</style>
