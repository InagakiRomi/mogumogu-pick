<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { components, operations } from '@/api/schema'
import client from '@/api/client'
import WarmButton from '@/components/warm/WarmButton.vue'
import { authSession } from '@/lib/authSession'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
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
import { getApiErrorMessage, RESTAURANT_FEEDBACK_MESSAGES } from '@/lib/apiErrorMessage'

type Restaurant = components['schemas']['RestaurantResponse']
type RestaurantListQuery = operations['getMyGroupRestaurants']['parameters']['query']
type OrderBy = Exclude<NonNullable<RestaurantListQuery>['orderBy'], undefined>
type SortOrder = Exclude<NonNullable<RestaurantListQuery>['sort'], undefined>

type CategoryOption = {
  label: string
  value: string
}

const ALL_CATEGORIES_VALUE = 'all'
const DEFAULT_LIMIT = 10
const DEFAULT_RESTAURANT_IMAGE = '/images/defaultRestaurant.jpg'

const failedImageIds = ref(new Set<number>())

const categoryOptions: CategoryOption[] = [
  { label: '全部', value: ALL_CATEGORIES_VALUE },
  { label: '主食', value: '1' },
  { label: '輕食', value: '2' },
  { label: '飲料', value: '3' },
]

const orderByOptions: Array<{ label: string; value: OrderBy }> = [
  { label: 'ID', value: 'DISPLAY_ORDER_ID' },
  { label: '建立時間', value: 'CREATED_AT' },
  { label: '餐廳名稱', value: 'RESTAURANT_NAME' },
  { label: '被選取次數', value: 'SELECTED_COUNT' },
  { label: '最後被選時間', value: 'LAST_SELECTED_AT' },
]

const sortOptions: Array<{ label: string; value: SortOrder }> = [
  { label: '新到舊 / 大到小', value: 'DESC' },
  { label: '舊到新 / 小到大', value: 'ASC' },
]

const restaurants = ref<Restaurant[]>([])
const searchInput = ref('')
const selectedCategory = ref(ALL_CATEGORIES_VALUE)
const orderBy = ref<OrderBy>('DISPLAY_ORDER_ID')
const sort = ref<SortOrder>('DESC')
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

const pagingText = computed(() => {
  if (!total.value) {
    return '目前沒有符合條件的餐廳'
  }
  const start = (page.value - 1) * limit.value + 1
  const end = Math.min(page.value * limit.value, total.value)
  return `顯示第 ${start}-${end} 筆，共 ${total.value} 筆`
})

function formatDate(value?: string): string {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString('zh-TW', { hour12: false })
}

function formatCategoryLabel(categoryId?: number): string {
  if (categoryId == null) {
    return '-'
  }

  return (
    categoryOptions.find((option) => option.value === String(categoryId))?.label ?? '-'
  )
}

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
    categoryId: '1',
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
  clearFeedback()
  isLoading.value = true

  try {
    const categoryId =
      selectedCategory.value !== ALL_CATEGORIES_VALUE ? Number(selectedCategory.value) : undefined

    const query: RestaurantListQuery = {
      search: searchInput.value.trim() || undefined,
      categoryId,
      orderBy: orderBy.value,
      sort: sort.value,
      page: page.value,
      limit: limit.value,
    }

    const { data, error } = await client.GET('/restaurants/my', {
      params: {
        query,
      },
    })

    if (error) {
      throw error
    }

    failedImageIds.value.clear()
    restaurants.value = data?.data ?? []
    total.value = Number(data?.total ?? 0)
  } catch (error) {
    showFeedback(getApiErrorMessage(error, RESTAURANT_FEEDBACK_MESSAGES.list.fallback))
  } finally {
    isLoading.value = false
  }
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
      throw error
    }

    isCreateDialogOpen.value = false
    resetCreateForm()
    showFeedback('新增餐廳成功', 'success')

    if (page.value !== 1) {
      page.value = 1
    } else {
      await fetchRestaurants()
    }
  } catch (error) {
    showFeedback(getApiErrorMessage(error, '新增餐廳失敗'))
  } finally {
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
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-fixed bg-cover bg-center bg-no-repeat px-4 py-6 md:px-6"
  >
    <div
      class="relative z-10 mx-auto mt-6 w-full rounded-[10px] border border-[rgba(226,164,136,0.52)] bg-linear-to-br from-[rgba(255,248,241,0.9)] to-[rgba(255,233,219,0.84)] px-[30px] pt-[30px] pb-8 shadow-[0_14px_32px_rgba(95,57,41,0.24),inset_0_1px_0_rgba(255,255,255,0.55)] backdrop-blur-sm max-lg:mt-5 max-lg:px-6 max-lg:pt-6 max-lg:pb-7 max-md:mt-4 max-md:rounded-lg max-md:px-4 max-md:pt-4 max-md:pb-6"
    >
      <div class="space-y-5">
        <div class="flex flex-wrap items-end gap-3">
          <div class="min-w-[220px] grow space-y-2">
            <Label for="restaurant-search" class="font-bold text-muted-foreground">餐廳名稱搜尋</Label>
            <Input
              id="restaurant-search"
              v-model="searchInput"
              class="h-10"
              placeholder="輸入關鍵字，例如：拉麵"
              @keyup.enter="handleSearch"
            />
          </div>

          <div class="w-[180px] space-y-2">
            <Label for="restaurant-category" class="font-bold text-muted-foreground">分類</Label>
            <Select v-model="selectedCategory">
              <SelectTrigger
                id="restaurant-category"
                class="h-10 w-full rounded-md border border-border bg-muted/90 px-3 text-left text-sm text-popover-foreground"
              >
                <SelectValue placeholder="選擇分類" />
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

          <div class="w-[180px] space-y-2">
            <Label class="font-bold text-muted-foreground">排序欄位</Label>
            <Select v-model="orderBy">
              <SelectTrigger
                class="h-10 w-full rounded-md border border-border bg-muted/90 px-3 text-left text-sm text-popover-foreground"
              >
                <SelectValue placeholder="選擇排序欄位" />
              </SelectTrigger>
              <SelectContent class="border-border bg-card text-popover-foreground">
                <SelectItem v-for="option in orderByOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="w-[180px] space-y-2">
            <Label class="font-bold text-muted-foreground">排序方向</Label>
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

          <WarmButton :disabled="isLoading" @click="handleSearch">
            {{ isLoading ? '查詢中...' : '查詢' }}
          </WarmButton>
          <WarmButton :disabled="isLoading || isCreating" @click="openCreateDialog">
            新增餐廳
          </WarmButton>
        </div>

        <div class="rounded-lg border border-border bg-card/70">
          <Table class="table-fixed">
            <TableHeader>
              <TableRow>
                <TableHead class="w-[72px] text-center">ID</TableHead>
                <TableHead class="w-[96px] text-center">餐廳圖片</TableHead>
                <TableHead class="w-[20%] text-center">餐廳名稱</TableHead>
                <TableHead class="w-[72px] text-center">分類</TableHead>
                <TableHead class="w-[120px] text-center">被選中的次數</TableHead>
                <TableHead class="w-[24%] text-center">備註</TableHead>
                <TableHead class="w-[180px] text-center">最後被選時間</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="isLoading">
                <TableCell colspan="7" class="py-8 text-center text-muted-foreground">
                  載入資料中...
                </TableCell>
              </TableRow>
              <TableRow v-else-if="restaurants.length === 0">
                <TableCell colspan="7" class="py-8 text-center text-muted-foreground">
                  找不到符合條件的餐廳
                </TableCell>
              </TableRow>
              <TableRow v-for="restaurant in restaurants" :key="restaurant.restaurantId">
                <TableCell class="text-center">{{ restaurant.displayOrderId ?? '-' }}</TableCell>
                <TableCell class="text-center">
                  <img
                    :src="getRestaurantImageUrl(restaurant)"
                    :alt="restaurant.restaurantName ?? '餐廳圖片'"
                    :title="restaurant.imageUrl?.trim() || undefined"
                    class="mx-auto h-12 w-16 rounded border border-border object-cover"
                    loading="lazy"
                    @error="handleImageError($event, restaurant.restaurantId)"
                  />
                </TableCell>
                <TableCell class="truncate text-center font-medium" :title="restaurant.restaurantName ?? undefined">
                  {{ restaurant.restaurantName ?? '-' }}
                </TableCell>
                <TableCell class="text-center">{{ formatCategoryLabel(restaurant.categoryId) }}</TableCell>
                <TableCell class="text-center">{{ restaurant.selectedCount ?? 0 }}</TableCell>
                <TableCell class="truncate" :title="restaurant.note || undefined">
                  {{ restaurant.note || '-' }}
                </TableCell>
                <TableCell class="truncate text-center" :title="formatDate(restaurant.lastSelectedAt)">
                  {{ formatDate(restaurant.lastSelectedAt) }}
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

    <AlertDialog :open="isCreateDialogOpen" @update:open="handleCreateDialogOpenChange">
      <AlertDialogContent
        class="max-w-[min(92vw,768px)]! data-[size=default]:max-w-[min(92vw,768px)]! data-[size=default]:sm:max-w-[min(92vw,768px)]! border-border bg-card text-card-foreground"
      >
        <AlertDialogHeader>
          <AlertDialogTitle>新增餐廳</AlertDialogTitle>
        </AlertDialogHeader>

        <form class="space-y-4" @submit.prevent="handleCreateRestaurant">
          <div class="space-y-2">
            <Label for="create-restaurant-name" class="font-bold text-muted-foreground">
              餐廳名稱
            </Label>
            <Input
              id="create-restaurant-name"
              v-model="createForm.restaurantName"
              maxlength="100"
              placeholder="例如：和食天國"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="create-restaurant-category" class="font-bold text-muted-foreground">分類</Label>
            <Select v-model="createForm.categoryId">
              <SelectTrigger
                id="create-restaurant-category"
                class="h-10 w-full rounded-md border border-border bg-muted/90 px-3 text-left text-sm text-popover-foreground"
              >
                <SelectValue placeholder="選擇分類" />
              </SelectTrigger>
              <SelectContent
                position="popper"
                class="z-10000 border-border bg-card text-popover-foreground"
              >
                <SelectItem
                  v-for="option in categoryOptions.filter((option) => option.value !== ALL_CATEGORIES_VALUE)"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label for="create-restaurant-note" class="font-bold text-muted-foreground">備註（選填）</Label>
            <Input
              id="create-restaurant-note"
              v-model="createForm.note"
              maxlength="255"
              placeholder="例如：可電話訂位"
            />
          </div>

          <div class="space-y-2">
            <Label for="create-restaurant-image-url" class="font-bold text-muted-foreground">
              圖片網址（選填）
            </Label>
            <Input
              id="create-restaurant-image-url"
              v-model="createForm.imageUrl"
              maxlength="255"
              placeholder="https://example.com/restaurant.jpg"
            />
          </div>

          <AlertDialogFooter class="mt-1 sm:gap-3">
            <WarmButton
              type="button"
              variant="outline-standard"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="isCreating"
              @click="handleCreateDialogOpenChange(false)"
            >
              取消
            </WarmButton>
            <WarmButton
              type="submit"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="!canSubmitCreateRestaurant"
            >
              {{ isCreating ? '新增中...' : '確認新增' }}
            </WarmButton>
          </AlertDialogFooter>
        </form>
      </AlertDialogContent>
    </AlertDialog>
  </main>
</template>
