<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { components, operations } from '@/api/schema'
import client from '@/api/client'
import FormSelectField from '@/components/form/FormSelectField.vue'
import ListPagePanel from '@/components/form/ListPagePanel.vue'
import ListPagination from '@/components/form/ListPagination.vue'
import ListSection from '@/components/form/ListSection.vue'
import ListTable, {
  ListTableActions,
  ListTableCell,
  ListTableHead,
  ListTableRow,
} from '@/components/form/ListTable.vue'
import WarmButton from '@/components/warm/WarmButton.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { getApiErrorMessage, RESTAURANT_FEEDBACK_MESSAGES } from '@/lib/apiErrorMessage'

const DEFAULT_SORT_OPTIONS = [
  { label: '小到大', value: 'ASC' as const },
  { label: '大到小', value: 'DESC' as const },
]

type SelectionHistory = components['schemas']['SelectionHistoryResponse']
type SelectionHistoryQuery = operations['getMyGroupSelectionHistory']['parameters']['query']
type SortOrder = Exclude<NonNullable<SelectionHistoryQuery>['sort'], undefined>

const DEFAULT_LIMIT = 10

const historyListForm = {
  defaultSort: 'DESC' as SortOrder,
  sortOptions: DEFAULT_SORT_OPTIONS,
  loadingText: '載入資料中...',
  emptyText: '目前沒有抽選歷史紀錄',
}

const histories = ref<SelectionHistory[]>([])
const sort = ref<SortOrder>(historyListForm.defaultSort)
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
    return historyListForm.emptyText
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
  <ListPagePanel>
    <ListSection title="抽選歷史" :summary="`共 ${total} 筆紀錄`">
      <div class="flex flex-wrap items-end gap-3">
        <FormSelectField
          v-model="sort"
          label="排序方向"
          :options="historyListForm.sortOptions"
          placeholder="選擇排序方向"
        />
      </div>

      <ListTable
        :is-loading="isLoading"
        :is-empty="histories.length === 0"
        :column-count="5"
        :loading-text="historyListForm.loadingText"
        :empty-text="historyListForm.emptyText"
      >
        <template #header>
          <ListTableHead class="w-[80px]">排序</ListTableHead>
          <ListTableHead>餐廳名稱</ListTableHead>
          <ListTableHead class="w-[120px]">類別</ListTableHead>
          <ListTableHead class="w-[180px]">選擇時間</ListTableHead>
          <ListTableHead class="w-[140px]">操作</ListTableHead>
        </template>

        <ListTableRow
          v-for="history in histories"
          :key="history.historyId"
        >
          <ListTableCell>{{ history.historyId ?? '-' }}</ListTableCell>
          <ListTableCell truncate :title="history.restaurantName ?? undefined">
            {{ history.restaurantName ?? '-' }}
          </ListTableCell>
          <ListTableCell>{{ history.category ?? '-' }}</ListTableCell>
          <ListTableCell truncate :title="history.selectedAt?.trim() || '-'">
            {{ history.selectedAt?.trim() || '-' }}
          </ListTableCell>
          <ListTableCell>
            <ListTableActions>
              <WarmButton
                v-if="history.restaurantId != null"
                variant="outline-standard"
                class="h-9 px-3 text-sm"
                @click="goRestaurantDetail(history.restaurantId)"
              >
                查看詳細
              </WarmButton>
              <span v-else class="text-muted-foreground">-</span>
            </ListTableActions>
          </ListTableCell>
        </ListTableRow>
      </ListTable>

      <ListPagination
        :paging-text="pagingText"
        :page="page"
        :total-pages="totalPages"
        :has-prev-page="hasPrevPage"
        :has-next-page="hasNextPage"
        :is-loading="isLoading"
        @prev="goPrevPage"
        @next="goNextPage"
      />
    </ListSection>
  </ListPagePanel>
</template>
