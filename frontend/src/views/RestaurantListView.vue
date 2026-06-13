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
import { authSession } from '@/lib/authSession'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import RestaurantFormDialog from '@/components/restaurant/RestaurantFormDialog.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { ALL_CATEGORIES_VALUE, useRestaurantCategories } from '@/composables/useRestaurantCategories'
import { getApiErrorMessage, RESTAURANT_FEEDBACK_MESSAGES } from '@/lib/apiErrorMessage'
import { publicAsset } from '@/lib/utils'

const DEFAULT_RESTAURANT_IMAGE = publicAsset('images/defaultRestaurant.jpg')
const DEFAULT_SORT_OPTIONS = [
  { label: '小到大', value: 'ASC' as const },
  { label: '大到小', value: 'DESC' as const },
]

type Restaurant = components['schemas']['RestaurantResponse']
type RestaurantListQuery = operations['getRestaurants']['parameters']['query']
type RestaurantOrderBy = Exclude<NonNullable<RestaurantListQuery>['orderBy'], undefined>
type SortOrder = Exclude<NonNullable<RestaurantListQuery>['sort'], undefined>

const DEFAULT_LIMIT = 10

const restaurantListForm = {
  defaultOrderBy: 'DISPLAY_ORDER_ID' as RestaurantOrderBy,
  defaultSort: 'DESC' as SortOrder,
  orderByOptions: [
    { label: 'ID', value: 'DISPLAY_ORDER_ID' as const },
    { label: '建立時間', value: 'CREATED_AT' as const },
    { label: '被選取次數', value: 'SELECTED_COUNT' as const },
    { label: '最後被選時間', value: 'LAST_SELECTED_AT' as const },
  ],
  sortOptions: DEFAULT_SORT_OPTIONS,
  searchLabel: '餐廳名稱搜尋',
  searchPlaceholder: '輸入關鍵字，例如：拉麵',
  categoryLabel: '分類',
  categoryPlaceholder: '選擇分類',
  loadingText: '載入資料中...',
  emptyText: '找不到符合條件的餐廳',
}

const failedImageIds = ref(new Set<number>())
const { categoryOptionsWithAll, categories, defaultCategoryId } = useRestaurantCategories()
const categoryOptions = categoryOptionsWithAll

const restaurants = ref<Restaurant[]>([])
const searchInput = ref('')
const selectedCategory = ref(ALL_CATEGORIES_VALUE)
const orderBy = ref<RestaurantOrderBy>(restaurantListForm.defaultOrderBy)
const sort = ref<SortOrder>(restaurantListForm.defaultSort)
const page = ref(1)
const limit = ref(DEFAULT_LIMIT)
const total = ref(0)
const isLoading = ref(false)
const isCreateDialogOpen = ref(false)
const isCreating = ref(false)
const createForm = ref({
  restaurantName: '',
  categoryId: '1',
  note: '',
  imageUrl: '',
})
const { showFeedback, clearFeedback } = useFeedbackDialog()
const router = useRouter()

const totalPages = computed(() => {
  if (!total.value || !limit.value) {
    return 1
  }
  return Math.max(1, Math.ceil(total.value / limit.value))
})

const hasPrevPage = computed(() => page.value > 1)
const hasNextPage = computed(() => page.value < totalPages.value)
const canSubmitCreateRestaurant = computed(
  () => createForm.value.restaurantName.trim().length > 0 && !isCreating.value,
)
function resolveCategoryId(category: string): number | undefined {
  return category !== ALL_CATEGORIES_VALUE ? Number(category) : undefined
}

const pagingText = computed(() => {
  if (!total.value) {
    return '目前沒有符合條件的餐廳'
  }
  const start = (page.value - 1) * limit.value + 1
  const end = Math.min(page.value * limit.value, total.value)
  return `顯示第 ${start}-${end} 筆，共 ${total.value} 筆`
})

function getRestaurantImageUrl(restaurant: Restaurant): string {
  if (
    restaurant.restaurantId != null &&
    failedImageIds.value.has(restaurant.restaurantId)
  ) {
    return DEFAULT_RESTAURANT_IMAGE
  }

  const imageUrl = restaurant.imageUrl?.trim()
  return imageUrl || DEFAULT_RESTAURANT_IMAGE
}

function handleImageError(event: Event, restaurantId?: number) {
  const img = event.target as HTMLImageElement
  if (img.src.endsWith(DEFAULT_RESTAURANT_IMAGE)) {
    return
  }

  if (restaurantId != null) {
    failedImageIds.value.add(restaurantId)
  }

  img.src = DEFAULT_RESTAURANT_IMAGE
}

function resetCreateForm() {
  createForm.value = {
    restaurantName: '',
    categoryId: defaultCategoryId.value || '1',
    note: '',
    imageUrl: '',
  }
}

function openCreateDialog() {
  clearFeedback()
  resetCreateForm()
  isCreateDialogOpen.value = true
}

function handleCreateDialogOpenChange(open: boolean) {
  isCreateDialogOpen.value = open
  if (!open) {
    resetCreateForm()
  }
}

async function fetchRestaurants() {
  isLoading.value = true

  const query: RestaurantListQuery = {
    search: searchInput.value.trim() || undefined,
    categoryId: resolveCategoryId(selectedCategory.value),
    isArchived: false,
    orderBy: orderBy.value,
    sort: sort.value,
    page: page.value,
    limit: limit.value,
  }

  const { data, error } = await client.GET('/restaurants', {
    params: {
      query,
    },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.list.fallback))
    isLoading.value = false
    return
  }

  failedImageIds.value.clear()
  restaurants.value = data?.data ?? []
  total.value = Number(data?.total ?? 0)
  isLoading.value = false
}

async function handleCreateRestaurant() {
  const groupId = authSession.value?.groupId
  if (groupId == null || groupId <= 0) {
    showFeedback('請先加入群組後才能新增餐廳')
    return
  }

  const restaurantName = createForm.value.restaurantName.trim()
  if (!restaurantName) {
    showFeedback('請輸入餐廳名稱')
    return
  }

  clearFeedback()
  isCreating.value = true

  try {
    const { error } = await client.POST('/restaurants', {
      body: {
        groupId,
        categoryId: Number(createForm.value.categoryId),
        restaurantName,
        note: createForm.value.note.trim() || undefined,
        imageUrl: createForm.value.imageUrl.trim() || undefined,
      },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '新增餐廳失敗'))
      return
    }

    showFeedback('新增餐廳成功', 'success')

    if (page.value !== 1) {
      page.value = 1
    } else {
      await fetchRestaurants()
    }
  } finally {
    handleCreateDialogOpenChange(false)
    isCreating.value = false
  }
}

function handleSearch() {
  page.value = 1
  void fetchRestaurants()
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
  void fetchRestaurants()
})

watch([selectedCategory, orderBy, sort], () => {
  page.value = 1
  void fetchRestaurants()
})

onMounted(() => {
  void fetchRestaurants()
})
</script>

<template>
  <ListPagePanel>
    <ListSection title="餐廳列表" :summary="`共 ${total} 間餐廳`">
      <div class="flex flex-wrap items-end gap-3">
        <div class="min-w-[220px] grow space-y-2">
          <Label for="restaurant-search" class="font-bold text-muted-foreground">
            {{ restaurantListForm.searchLabel }}
          </Label>
          <Input
            id="restaurant-search"
            v-model="searchInput"
            class="h-10 px-2.5 text-sm rounded-md border border-border bg-muted/90 text-popover-foreground"
            :placeholder="restaurantListForm.searchPlaceholder"
            @keyup.enter="handleSearch"
          />
        </div>

        <FormSelectField
          id="restaurant-category"
          v-model="selectedCategory"
          :label="restaurantListForm.categoryLabel"
          :options="categoryOptions"
          :placeholder="restaurantListForm.categoryPlaceholder"
        />
        <FormSelectField
          v-model="orderBy"
          label="排序欄位"
          :options="restaurantListForm.orderByOptions"
          placeholder="選擇排序欄位"
        />
        <FormSelectField
          v-model="sort"
          label="排序方向"
          :options="restaurantListForm.sortOptions"
          placeholder="選擇排序方向"
        />

        <WarmButton :disabled="isLoading" @click="handleSearch">
          {{ isLoading ? '查詢中...' : '查詢' }}
        </WarmButton>
        <WarmButton :disabled="isLoading || isCreating" @click="openCreateDialog">
          新增餐廳
        </WarmButton>
      </div>

      <ListTable
        :is-loading="isLoading"
        :is-empty="restaurants.length === 0"
        :column-count="8"
        :loading-text="restaurantListForm.loadingText"
        :empty-text="restaurantListForm.emptyText"
      >
        <template #header>
          <ListTableHead class="w-[72px]">ID</ListTableHead>
          <ListTableHead class="w-[96px]">餐廳圖片</ListTableHead>
          <ListTableHead class="w-[20%]">餐廳名稱</ListTableHead>
          <ListTableHead class="w-[72px]">分類</ListTableHead>
          <ListTableHead class="w-[120px]">被選中的次數</ListTableHead>
          <ListTableHead class="w-[24%]">備註</ListTableHead>
          <ListTableHead class="w-[180px]">最後被選時間</ListTableHead>
          <ListTableHead class="w-[140px]">操作</ListTableHead>
        </template>

        <ListTableRow
          v-for="restaurant in restaurants"
          :key="restaurant.restaurantId"
        >
          <ListTableCell>{{ restaurant.displayOrderId ?? '-' }}</ListTableCell>
          <ListTableCell>
            <img
              :src="getRestaurantImageUrl(restaurant)"
              :alt="restaurant.restaurantName ?? '餐廳圖片'"
              :title="restaurant.imageUrl?.trim() || undefined"
              class="mx-auto h-12 w-16 rounded border border-border object-cover"
              loading="lazy"
              @error="handleImageError($event, restaurant.restaurantId)"
            />
          </ListTableCell>
          <ListTableCell
            truncate
            class="font-medium"
            :title="restaurant.restaurantName ?? undefined"
          >
            {{ restaurant.restaurantName ?? '-' }}
          </ListTableCell>
          <ListTableCell>{{ restaurant.categoryName ?? '-' }}</ListTableCell>
          <ListTableCell>{{ restaurant.selectedCount ?? 0 }}</ListTableCell>
          <ListTableCell truncate :title="restaurant.note || undefined">
            {{ restaurant.note || '-' }}
          </ListTableCell>
          <ListTableCell truncate :title="restaurant.lastSelectedAt?.trim() || '-'">
            {{ restaurant.lastSelectedAt?.trim() || '-' }}
          </ListTableCell>
          <ListTableCell>
            <ListTableActions>
              <WarmButton
                variant="outline-standard"
                class="h-9 px-3 text-sm"
                @click="goRestaurantDetail(restaurant.restaurantId)"
              >
                查看詳細
              </WarmButton>
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
      <RestaurantFormDialog
        :open="isCreateDialogOpen"
        v-model="createForm"
        :category-options="categories"
        mode="create"
        title="新增餐廳"
        id-prefix="create-restaurant"
        submit-label="確認新增"
        loading-label="新增中..."
        :loading="isCreating"
        :can-submit="canSubmitCreateRestaurant"
        @update:open="handleCreateDialogOpenChange"
        @submit="handleCreateRestaurant"
        @cancel="handleCreateDialogOpenChange(false)"
      />
    </template>
  </ListPagePanel>
</template>
