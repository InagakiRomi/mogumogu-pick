<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { components, operations } from '@/api/schema'
import client from '@/api/client'
import AlertConfirm from '@/components/alert/AlertConfirm.vue'
import FormSelectField from '@/components/form/FormSelectField.vue'
import ListPagePanel from '@/components/list/ListPagePanel.vue'
import ListPagination from '@/components/list/ListPagination.vue'
import ListSection from '@/components/list/ListSection.vue'
import ListTable, {
  ListTableActions,
  ListTableCell,
  ListTableHead,
  ListTableRow,
} from '@/components/list/ListTable.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { getApiErrorMessage, RESTAURANT_FEEDBACK_MESSAGES } from '@/lib/apiErrorMessage'
import { authSession } from '@/lib/authSession'
import { isGroupAdmin } from '@/lib/userRole'

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
const isClearing = ref(false)
const isClearDialogOpen = ref(false)
const { showFeedback, clearFeedback } = useFeedbackDialog()
const router = useRouter()

const canClearHistory = computed(() => isGroupAdmin(authSession.value?.role))

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

function openClearDialog() {
  if (!canClearHistory.value) {
    showFeedback('只有群組管理員可以清除歷史紀錄')
    return
  }
  isClearDialogOpen.value = true
}

async function clearSelectionHistory() {
  clearFeedback()
  isClearing.value = true

  try {
    const { error } = await client.DELETE('/restaurants/selection-history')

    if (error) {
      showFeedback(getApiErrorMessage(error, '清除歷史紀錄失敗'))
      return
    }

    histories.value = []
    total.value = 0
    page.value = 1
    showFeedback('已清除群組所有抽選歷史紀錄', 'success')
  } finally {
    isClearDialogOpen.value = false
    isClearing.value = false
  }
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
        />
        <PrimaryButton
          v-if="canClearHistory"
          variant="standard"
          :disabled="isLoading || isClearing || total === 0"
          @click="openClearDialog"
        >
          清除所有紀錄
        </PrimaryButton>
      </div>

      <ListTable
        :is-loading="isLoading"
        :is-empty="histories.length === 0"
        :column-count="5"
        :loading-text="historyListForm.loadingText"
        :empty-text="historyListForm.emptyText"
      >
        <template #header>
          <ListTableHead class="w-20">排序</ListTableHead>
          <ListTableHead>餐廳名稱</ListTableHead>
          <ListTableHead class="w-30">類別</ListTableHead>
          <ListTableHead class="w-45">選擇時間</ListTableHead>
          <ListTableHead class="w-35">操作</ListTableHead>
        </template>

        <ListTableRow v-for="history in histories" :key="history.historyId">
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
              <PrimaryButton
                v-if="history.restaurantId != null"
                variant="outline"
                class="h-9 px-3"
                @click="goRestaurantDetail(history.restaurantId)"
              >
                查看詳細
              </PrimaryButton>
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

    <template #overlay>
      <AlertConfirm
        :open="isClearDialogOpen"
        title="確認清除所有歷史紀錄？"
        description="此操作會永久刪除該群組的所有抽選歷史紀錄，且無法復原。"
        confirm-label="確認清除"
        loading-label="清除中..."
        :loading="isClearing"
        @update:open="isClearDialogOpen = $event"
        @confirm="clearSelectionHistory"
      />
    </template>
  </ListPagePanel>
</template>
