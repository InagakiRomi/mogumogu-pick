<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { components } from '@/api/schema'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'

type RestaurantResult = components['schemas']['RestaurantResponse']

const START_IMAGE = '/images/start.jpg'
const DEFAULT_RESTAURANT_IMAGE = '/images/defaultRestaurant.jpg'

const props = withDefaults(
  defineProps<{
    restaurant?: RestaurantResult | null
    categoryLabel?: string
  }>(),
  {
    restaurant: null,
    categoryLabel: '未分類',
  },
)

const imageLoadFailed = ref(false)

const displayImage = computed(() => {
  if (!props.restaurant) {
    return START_IMAGE
  }

  const imageUrl = props.restaurant.imageUrl?.trim()
  if (!imageUrl || imageLoadFailed.value) {
    return DEFAULT_RESTAURANT_IMAGE
  }

  return imageUrl
})

watch(
  () => [props.restaurant?.restaurantId, props.restaurant?.imageUrl] as const,
  () => {
    imageLoadFailed.value = false
  },
)

function handleImageError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img.src.endsWith(DEFAULT_RESTAURANT_IMAGE)) {
    return
  }

  imageLoadFailed.value = true
  img.src = DEFAULT_RESTAURANT_IMAGE
}
const displayName = computed(() => props.restaurant?.restaurantName || '尚未抽選')
const displaySelectedCount = computed(() => props.restaurant?.selectedCount ?? 0)
const displayNote = computed(() => props.restaurant?.note || '無')
const displayLastSelectedAt = computed(() => props.restaurant?.lastSelectedAt || '尚無紀錄')
const displayUpdatedAt = computed(() => props.restaurant?.updatedAt || '尚無紀錄')
</script>

<template>
  <Card
    class="border-[rgba(198,134,105,0.45)] bg-linear-to-br from-[rgba(255,248,241,0.92)] to-[rgba(255,233,219,0.86)] shadow-[0_12px_28px_rgba(95,57,41,0.2)]"
  >
    <CardHeader class="pb-3">
      <CardTitle
        class="flex items-center justify-between gap-3 text-[1.08rem] text-[rgba(84,44,30,0.95)]"
      >
        <span class="truncate">{{ displayName }}</span>
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
        :src="displayImage"
        :alt="displayName"
        class="aspect-480/320 w-full rounded-lg border border-[rgba(198,134,105,0.45)] object-cover shadow-[inset_0_0_0_1px_rgba(255,255,255,0.35)]"
        @error="handleImageError"
      />

      <Separator class="bg-[rgba(198,134,105,0.45)]" />

      <div class="grid grid-cols-1 gap-2 text-sm text-[rgba(95,57,41,0.92)]">
        <p><span class="font-semibold">選擇次數：</span>{{ displaySelectedCount }}</p>
        <p><span class="font-semibold">最後選擇時間：</span>{{ displayLastSelectedAt }}</p>
        <p><span class="font-semibold">最後更新時間：</span>{{ displayUpdatedAt }}</p>
        <p><span class="font-semibold">備註：</span>{{ displayNote }}</p>
      </div>
    </CardContent>
  </Card>
</template>
