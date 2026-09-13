<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { components, operations } from '@/api/schema'
import client from '@/api/client'
import FormDialog from '@/components/form/FormDialog.vue'
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
import PrimarySelectTrigger from '@/components/common/PrimarySelectTrigger.vue'
import { authSession } from '@/lib/authSession'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectValue } from '@/components/ui/select'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import {
  ALL_CATEGORIES_VALUE,
  useRestaurantCategories,
} from '@/composables/useRestaurantCategories'
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
  if (restaurant.restaurantId != null && failedImageIds.value.has(restaurant.restaurantId)) {
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
        <div class="min-w-55 grow space-y-2">
          <Label for="restaurant-search" class="font-bold text-muted-foreground">
            {{ restaurantListForm.searchLabel }}
          </Label>
          <Input
            id="restaurant-search"
            v-model="searchInput"
            class="h-10 px-2.5 rounded-md border border-border bg-muted/90 text-popover-foreground"
            :placeholder="restaurantListForm.searchPlaceholder"
            @keyup.enter="handleSearch"
          />
        </div>

        <FormSelectField
          id="restaurant-category"
          v-model="selectedCategory"
          :label="restaurantListForm.categoryLabel"
          :options="categoryOptionsWithAll"
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

        <PrimaryButton :disabled="isLoading" @click="handleSearch">
          {{ isLoading ? '查詢中...' : '查詢' }}
        </PrimaryButton>
        <PrimaryButton :disabled="isLoading || isCreating" @click="openCreateDialog">
          新增餐廳
        </PrimaryButton>
      </div>

      <ListTable
        :is-loading="isLoading"
        :is-empty="restaurants.length === 0"
        :column-count="8"
        :loading-text="restaurantListForm.loadingText"
        :empty-text="restaurantListForm.emptyText"
      >
        <template #header>
          <ListTableHead class="w-18">ID</ListTableHead>
          <ListTableHead class="w-24">餐廳圖片</ListTableHead>
          <ListTableHead class="w-[20%]">餐廳名稱</ListTableHead>
          <ListTableHead class="w-18">分類</ListTableHead>
          <ListTableHead class="w-30">被選中的次數</ListTableHead>
          <ListTableHead class="w-[24%]">備註</ListTableHead>
          <ListTableHead class="w-45">最後被選時間</ListTableHead>
          <ListTableHead class="w-35">操作</ListTableHead>
        </template>

        <ListTableRow v-for="restaurant in restaurants" :key="restaurant.restaurantId">
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
              <PrimaryButton
                variant="outline"
                class="h-9 px-3"
                @click="goRestaurantDetail(restaurant.restaurantId)"
              >
                查看詳細
              </PrimaryButton>
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
      <FormDialog
        :open="isCreateDialogOpen"
        title="新增餐廳"
        submit-label="確認新增"
        loading-label="新增中..."
        :loading="isCreating"
        :can-submit="canSubmitCreateRestaurant"
        @update:open="handleCreateDialogOpenChange"
        @submit="handleCreateRestaurant"
        @cancel="handleCreateDialogOpenChange(false)"
      >
        <div>
          <Label for="create-restaurant-name">餐廳名稱</Label>
          <Input
            id="create-restaurant-name"
            v-model="createForm.restaurantName"
            maxlength="100"
            placeholder="例如：和食天國"
            required
          />
        </div>

        <div>
          <Label for="create-restaurant-category">分類</Label>
          <Select v-model="createForm.categoryId">
            <PrimarySelectTrigger id="create-restaurant-category">
              <SelectValue placeholder="選擇分類" />
            </PrimarySelectTrigger>
            <SelectContent position="popper">
              <SelectItem v-for="option in categories" :key="option.value" :value="option.value">
                {{ option.label }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div>
          <Label for="create-restaurant-note">備註（選填）</Label>
          <Input
            id="create-restaurant-note"
            v-model="createForm.note"
            maxlength="255"
            placeholder="例如：可電話訂位"
          />
        </div>

        <div>
          <Label for="create-restaurant-image-url">圖片網址（選填）</Label>
          <Input
            id="create-restaurant-image-url"
            v-model="createForm.imageUrl"
            maxlength="255"
            placeholder="https://example.com/restaurant.jpg"
          />
        </div>
      </FormDialog>
    </template>
  </ListPagePanel>
</template>
