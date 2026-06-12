<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { components, operations } from '@/api/schema'
import client from '@/api/client'
import WarmButton from '@/components/warm/WarmButton.vue'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { formatOptionalDate } from '@/constants/restaurant'
import { getApiErrorMessage, RESTAURANT_FEEDBACK_MESSAGES } from '@/lib/apiErrorMessage'

type SelectionHistory = components['schemas']['SelectionHistoryResponse']
type SelectionHistoryQuery = operations['getMyGroupSelectionHistory']['parameters']['query']
type SortOrder = Exclude<NonNullable<SelectionHistoryQuery>['sort'], undefined>

const DEFAULT_LIMIT = 10

const sortOptions: Array<{ label: string; value: SortOrder }> = [
  { label: '新到舊', value: 'DESC' },
  { label: '舊到新', value: 'ASC' },
]

const histories = ref<SelectionHistory[]>([])
const sort = ref<SortOrder>('DESC')
const page = ref(1)
const limit = ref(DEFAULT_LIMIT)
const total = ref(0)
const isLoading = ref(false)
const { showFeedback } = useFeedbackDialog()
const router = useRouter()

const totalPages = computed(() => {
  if (!total.value || !limit.value) {
    return 1
  }
  return Math.max(1, Math.ceil(total.value / limit.value))
})

const hasPrevPage = computed(() => page.value > 1)
const hasNextPage = computed(() => page.value < totalPages.value)

const pagingText = computed(() => {
  if (!total.value) {
    return '目前沒有抽選歷史紀錄'
  }
  const start = (page.value - 1) * limit.value + 1
  const end = Math.min(page.value * limit.value, total.value)
  return `顯示第 ${start}-${end} 筆，共 ${total.value} 筆`
})

async function fetchSelectionHistory() {
  isLoading.value = true

  const { data, error } = await client.GET('/restaurants/selection-history', {
    params: {
      query: {
        sort: sort.value,
        page: page.value,
        limit: limit.value,
      },
    },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.history.fallback))
    isLoading.value = false
    return
  }

  histories.value = data?.data ?? []
  total.value = Number(data?.total ?? 0)
  isLoading.value = false
}

function goPrevPage() {
  if (!hasPrevPage.value || isLoading.value) {
    return
  }
  page.value -= 1
}

function goNextPage() {
  if (!hasNextPage.value || isLoading.value) {
    return
  }
  page.value += 1
}

function goRestaurantDetail(restaurantId?: number) {
  if (restaurantId == null) {
    return
  }

  void router.push({
    name: 'restaurant-detail',
    params: {
      id: String(restaurantId),
    },
  })
}

watch(page, () => {
  void fetchSelectionHistory()
})

watch(sort, () => {
  page.value = 1
  void fetchSelectionHistory()
})

onMounted(() => {
  void fetchSelectionHistory()
})
</script>

<template>
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-fixed bg-cover bg-center bg-no-repeat px-4 py-6 md:px-6"
  >
    <div
      class="relative z-10 mx-auto mt-6 w-full rounded-[10px] border border-[rgba(226,164,136,0.52)] bg-linear-to-br from-[rgba(255,248,241,0.9)] to-[rgba(255,233,219,0.84)] px-[30px] pt-[30px] pb-8 shadow-[0_14px_32px_rgba(95,57,41,0.24),inset_0_1px_0_rgba(255,255,255,0.55)] backdrop-blur-sm max-lg:mt-5 max-lg:px-6 max-lg:pt-6 max-lg:pb-7 max-md:mt-4 max-md:rounded-lg max-md:px-4 max-md:pt-4 max-md:pb-6"
    >
      <div class="space-y-5">
        <div class="flex flex-wrap items-end gap-3">
          <div class="w-[180px] space-y-2">
            <Label class="font-bold text-muted-foreground">抽選時間</Label>
            <Select v-model="sort">
              <SelectTrigger
                class="h-10 w-full rounded-md border border-border bg-muted/90 px-3 text-left text-sm text-popover-foreground"
              >
                <SelectValue placeholder="選擇排序方向" />
              </SelectTrigger>
              <SelectContent class="border-border bg-card text-popover-foreground">
                <SelectItem v-for="option in sortOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <div class="rounded-lg border border-border bg-card/70">
          <Table class="table-fixed">
            <TableHeader>
              <TableRow>
                <TableHead class="w-[80px] text-center">排序</TableHead>
                <TableHead class="text-center">餐廳名稱</TableHead>
                <TableHead class="w-[120px] text-center">類別</TableHead>
                <TableHead class="w-[180px] text-center">選擇時間</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="isLoading">
                <TableCell colspan="4" class="py-8 text-center text-muted-foreground">
                  載入資料中...
                </TableCell>
              </TableRow>
              <TableRow v-else-if="histories.length === 0">
                <TableCell colspan="4" class="py-8 text-center text-muted-foreground">
                  目前沒有抽選歷史紀錄
                </TableCell>
              </TableRow>
              <TableRow v-for="history in histories" :key="history.historyId">
                <TableCell class="text-center">{{ history.historyId ?? '-' }}</TableCell>
                <TableCell class="text-center">
                  <button
                    v-if="history.restaurantId != null"
                    type="button"
                    class="truncate font-medium"
                    :title="history.restaurantName ?? undefined"
                    @click="goRestaurantDetail(history.restaurantId)"
                  >
                    {{ history.restaurantName ?? '-' }}
                  </button>
                  <span v-else>{{ history.restaurantName ?? '-' }}</span>
                </TableCell>
                <TableCell class="text-center">{{ history.category ?? '-' }}</TableCell>
                <TableCell
                  class="truncate text-center"
                  :title="formatOptionalDate(history.selectedAt)"
                >
                  {{ formatOptionalDate(history.selectedAt) }}
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>

        <div class="flex flex-col items-center gap-3">
          <p class="text-sm text-muted-foreground">{{ pagingText }}</p>
          <div class="flex items-center justify-center gap-2">
            <WarmButton variant="outline-standard" :disabled="!hasPrevPage || isLoading" @click="goPrevPage">
              上一頁
            </WarmButton>
            <span class="text-sm font-medium text-card-foreground">
              第 {{ page }} / {{ totalPages }} 頁
            </span>
            <WarmButton variant="outline-standard" :disabled="!hasNextPage || isLoading" @click="goNextPage">
              下一頁
            </WarmButton>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
