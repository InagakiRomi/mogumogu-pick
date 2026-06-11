<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { components } from '@/api/schema'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'

type RestaurantResult = components['schemas']['RestaurantResponse']

const START_IMAGE = '/images/start.jpg'
const DEFAULT_RESTAURANT_IMAGE = '/images/defaultRestaurant.jpg'
const FLIP_DURATION_MS = 700

const cardClass =
  'border-[rgba(198,134,105,0.45)] bg-linear-to-br from-[rgba(255,248,241,0.92)] to-[rgba(255,233,219,0.86)] shadow-[0_12px_28px_rgba(95,57,41,0.2)]'

const props = withDefaults(
  defineProps<{
    restaurant?: RestaurantResult | null
    categoryLabel?: string
    isDrawing?: boolean
  }>(),
  {
    restaurant: null,
    categoryLabel: '未分類',
    isDrawing: false,
  },
)

const isFlipping = ref(false)
const flipKey = ref(0)
const isPastMidpoint = ref(false)
const frontSnapshot = ref<RestaurantResult | null>(null)
const backSnapshot = ref<RestaurantResult | null | 'drawing'>('drawing')
const pendingRestaurant = ref<RestaurantResult | null>(null)

let midpointTimer: ReturnType<typeof setTimeout> | undefined

const imageLoadFailed = ref(false)

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

  return resolveContent(props.restaurant ?? null)
})

const backContent = computed(() => {
  if (isFlipping.value || backSnapshot.value !== 'drawing') {
    return resolveContent(backSnapshot.value)
  }

  return resolveContent(props.restaurant ?? null)
})

watch(
  () => [props.restaurant?.restaurantId, props.restaurant?.imageUrl] as const,
  () => {
    imageLoadFailed.value = false
  },
)

function applyBackSnapshot() {
  backSnapshot.value = pendingRestaurant.value ?? props.restaurant ?? 'drawing'
}

watch(
  () => props.isDrawing,
  (drawing) => {
    if (!drawing) {
      return
    }

    flipKey.value += 1
    isFlipping.value = true
    isPastMidpoint.value = false
    frontSnapshot.value = props.restaurant ?? null
    backSnapshot.value = 'drawing'
    pendingRestaurant.value = props.restaurant ?? null

    clearTimeout(midpointTimer)
    midpointTimer = setTimeout(() => {
      isPastMidpoint.value = true
      applyBackSnapshot()
    }, FLIP_DURATION_MS / 2)
  },
)

watch(
  () => props.restaurant,
  (restaurant) => {
    if (!isFlipping.value) {
      return
    }

    pendingRestaurant.value = restaurant ?? null

    if (isPastMidpoint.value) {
      applyBackSnapshot()
    }
  },
)

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
</script>

<template>
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
                {{ categoryLabel }}
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
                {{ categoryLabel }}
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
