<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { components } from '@/api/schema'
import client from '@/api/client'
import ConfirmAlertDialog from '@/components/feedback/ConfirmAlertDialog.vue'
import FormAlertDialog from '@/components/feedback/FormAlertDialog.vue'
import RestaurantFormDialog from '@/components/restaurant/RestaurantFormDialog.vue'
import WarmButton from '@/components/warm/WarmButton.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { useRestaurantCategories } from '@/composables/useRestaurantCategories'
import { authSession } from '@/lib/authSession'
import {
  ARCHIVED_RESTAURANT_MESSAGE,
  getApiErrorMessage,
  isArchivedRestaurantError,
  RESTAURANT_UPDATE_FEEDBACK_MESSAGES,
} from '@/lib/apiErrorMessage'
import { isGroupAdmin } from '@/lib/userRole'
import { homeBgBackgroundStyle, publicAsset } from '@/lib/utils'

const FORM_LABEL_CLASS = 'font-bold text-muted-foreground'
const FORM_INPUT_CLASS =
  'h-10 px-2.5 text-sm rounded-md border border-border bg-muted/90 text-popover-foreground'
const DEFAULT_RESTAURANT_IMAGE = publicAsset('images/defaultRestaurant.jpg')

type Restaurant = components['schemas']['RestaurantResponse']
type Dish = components['schemas']['DishResponse']

/** 預設圖原始尺寸 480×320；詳細頁固定以 120×80 顯示 */

const route = useRoute()
const router = useRouter()
const { showFeedback, clearFeedback } = useFeedbackDialog()
const { categories: categoryOptions } = useRestaurantCategories()

const restaurantId = computed(() => {
  const parsed = Number(route.params.id)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
})

const restaurant = ref<Restaurant | null>(null)
const dishes = ref<Dish[]>([])
const dishTotal = ref(0)
const isLoading = ref(false)
const isDishesLoading = ref(false)
const isSaving = ref(false)
const isDeleting = ref(false)
const isCreatingDish = ref(false)
const isSavingDish = ref(false)
const isDeletingDish = ref(false)
const isEditDialogOpen = ref(false)
const allowEditRestaurantDialogClose = ref(false)
const isDeleteDialogOpen = ref(false)
const isCreateDishDialogOpen = ref(false)
const isEditDishDialogOpen = ref(false)
const allowEditDishDialogClose = ref(false)
const isDeleteDishDialogOpen = ref(false)
const deletingDish = ref<Dish | null>(null)
const editingDish = ref<Dish | null>(null)
const failedImage = ref(false)

const editForm = ref({
  restaurantName: '',
  categoryId: '1',
  displayOrderId: '',
  selectedCount: '',
  note: '',
  imageUrl: '',
  lastSelectedAt: '',
})

const createDishForm = ref({
  dishName: '',
  price: '',
})

const editDishForm = ref({
  displayOrderId: '',
  dishName: '',
  price: '',
})

const canSave = computed(() => {
  const displayOrderId = Number(editForm.value.displayOrderId)
  const selectedCount = Number(editForm.value.selectedCount)
  const name = editForm.value.restaurantName.trim()

  return (
    name.length > 0 &&
    name.length <= 64 &&
    Number.isInteger(displayOrderId) &&
    displayOrderId >= 0 &&
    Number.isInteger(selectedCount) &&
    selectedCount >= 0 &&
    editForm.value.note.length <= 512 &&
    editForm.value.imageUrl.length <= 512
  )
})

const canCreateDish = computed(() => validateCreateDish() === null && !isCreatingDish.value)
const canSaveDish = computed(() => validateEditDish() === null)

const canDeleteAsGroupAdmin = computed(() => isGroupAdmin(authSession.value?.role))
const EMPTY_DISH_FORM = { displayOrderId: '', dishName: '', price: '' }
const EMPTY_CREATE_DISH_FORM = { dishName: '', price: '' }
const DELETE_PERMISSION_MESSAGE = '只有群組管理員可以刪除'
const INVALID_RESTAURANT_ID_MESSAGE = '餐廳 ID 不正確'
const INVALID_DISH_ID_MESSAGE = '餐點 ID 不正確'

const toPadded = (part: number) => String(part).padStart(2, '0')
const toNonNegativeInteger = (value: string) => Number(value)
const isValidNonNegativeInteger = (value: number) => Number.isInteger(value) && value >= 0
const isValidPositiveInteger = (value: number) => Number.isInteger(value) && value >= 1

function showValidationError(error: string | null): boolean {
  if (error) {
    showFeedback(error)
    return false
  }

  return true
}

function validateEditRestaurant(): string | null {
  const restaurantName = editForm.value.restaurantName.trim()
  const displayOrderId = toNonNegativeInteger(editForm.value.displayOrderId)
  const selectedCount = toNonNegativeInteger(editForm.value.selectedCount)

  if (!restaurantName) {
    return '請輸入餐廳名稱'
  }
  if (restaurantName.length > 64) {
    return '餐廳名稱不可超過 64 字'
  }
  if (!isValidNonNegativeInteger(displayOrderId)) {
    return '請輸入有效的顯示排序 ID（須為 0 以上的整數）'
  }
  if (!isValidNonNegativeInteger(selectedCount)) {
    return '請輸入有效的被選取次數（須為 0 以上的整數）'
  }
  if (editForm.value.note.length > 512) {
    return '備註不可超過 512 字'
  }
  if (editForm.value.imageUrl.length > 512) {
    return '圖片網址不可超過 512 字'
  }
  if (editForm.value.lastSelectedAt.trim() && !toApiDatetime(editForm.value.lastSelectedAt)) {
    return '請輸入有效的最後被選取時間'
  }

  return null
}

function validateCreateDish(): string | null {
  const dishName = createDishForm.value.dishName.trim()
  const price = toNonNegativeInteger(createDishForm.value.price)

  if (!dishName) {
    return '請輸入餐點名稱'
  }
  if (!isValidNonNegativeInteger(price)) {
    return '請輸入有效的餐點價格'
  }

  return null
}

function validateEditDish(): string | null {
  const displayOrderId = toNonNegativeInteger(editDishForm.value.displayOrderId)
  const dishName = editDishForm.value.dishName.trim()
  const price = toNonNegativeInteger(editDishForm.value.price)

  if (!isValidPositiveInteger(displayOrderId)) {
    return '請輸入有效的顯示排序 ID（須為 1 以上的整數）'
  }
  if (!dishName) {
    return '請輸入餐點名稱'
  }
  if (!isValidNonNegativeInteger(price)) {
    return '請輸入有效的餐點價格'
  }

  return null
}

function formatPrice(value?: number): string {
  if (value == null) {
    return '-'
  }

  return `$${value.toLocaleString('zh-TW')}`
}

function toDatetimeLocalValue(value?: string): string {
  if (!value) {
    return ''
  }

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const parsed = new Date(normalized)
  if (Number.isNaN(parsed.getTime())) {
    return ''
  }

  return `${parsed.getFullYear()}-${toPadded(parsed.getMonth() + 1)}-${toPadded(parsed.getDate())}T${toPadded(parsed.getHours())}:${toPadded(parsed.getMinutes())}`
}

function toApiDatetime(value: string): string | undefined {
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }

  const parsed = new Date(trimmed)
  if (Number.isNaN(parsed.getTime())) {
    return undefined
  }

  return `${parsed.getFullYear()}-${toPadded(parsed.getMonth() + 1)}-${toPadded(parsed.getDate())} ${toPadded(parsed.getHours())}:${toPadded(parsed.getMinutes())}:${toPadded(parsed.getSeconds())}`
}

const displayImageUrl = computed(() => {
  if (failedImage.value) {
    return DEFAULT_RESTAURANT_IMAGE
  }

  const imageUrl = restaurant.value?.imageUrl?.trim()
  return imageUrl || DEFAULT_RESTAURANT_IMAGE
})

function handleImageError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img.src.endsWith(DEFAULT_RESTAURANT_IMAGE)) {
    return
  }

  failedImage.value = true
  img.src = DEFAULT_RESTAURANT_IMAGE
}

function redirectToRestaurantList() {
  void router.replace({ name: 'list-restaurant' })
}

function showArchivedRestaurantFeedback() {
  restaurant.value = null
  showFeedback(ARCHIVED_RESTAURANT_MESSAGE, 'error', redirectToRestaurantList)
}

function syncEditForm(data: Restaurant) {
  editForm.value = {
    restaurantName: data.restaurantName ?? '',
    categoryId: String(data.categoryId ?? 1),
    displayOrderId: data.displayOrderId != null ? String(data.displayOrderId) : '',
    selectedCount: data.selectedCount != null ? String(data.selectedCount) : '0',
    note: data.note ?? '',
    imageUrl: data.imageUrl ?? '',
    lastSelectedAt: toDatetimeLocalValue(data.lastSelectedAt),
  }
}

async function fetchDishes(id: number) {
  isDishesLoading.value = true

  const { data, error } = await client.GET('/restaurants/{id}', {
    params: {
      path: { id },
      query: { includeDishes: true },
    },
  })

  if (error) {
    dishes.value = []
    dishTotal.value = 0
    showFeedback(getApiErrorMessage(error, '取得餐點清單失敗'))
    isDishesLoading.value = false
    return
  }

  dishes.value = data?.dishes ?? []
  dishTotal.value = Number(data?.dishTotal ?? dishes.value.length)
  isDishesLoading.value = false
}

async function fetchRestaurantDetail() {
  const id = restaurantId.value
  if (id == null) {
    showFeedback(INVALID_RESTAURANT_ID_MESSAGE, 'error', redirectToRestaurantList)
    return
  }

  clearFeedback()
  isLoading.value = true
  failedImage.value = false
  dishes.value = []
  dishTotal.value = 0

  const { data: restaurantData, error: restaurantError } = await client.GET('/restaurants/{id}', {
    params: {
      path: { id },
      query: { includeDishes: true },
    },
  })

  if (restaurantError) {
    const error = restaurantError
    if (isArchivedRestaurantError(error)) {
      showArchivedRestaurantFeedback()
      isLoading.value = false
      return
    }

    restaurant.value = null
    showFeedback(getApiErrorMessage(error, '取得餐廳詳細資料失敗'), 'error', redirectToRestaurantList)
    isLoading.value = false
    return
  }

  if (restaurantData?.isArchived) {
    showArchivedRestaurantFeedback()
    isLoading.value = false
    return
  }

  restaurant.value = restaurantData ?? null
  dishes.value = restaurantData?.dishes ?? []
  dishTotal.value = Number(restaurantData?.dishTotal ?? dishes.value.length)
  isLoading.value = false
}

async function handleSaveRestaurant() {
  if (isSaving.value) {
    return
  }

  if (restaurantId.value == null) {
    showFeedback(INVALID_RESTAURANT_ID_MESSAGE)
    return
  }

  if (!showValidationError(validateEditRestaurant())) {
    return
  }

  const restaurantName = editForm.value.restaurantName.trim()
  const displayOrderId = toNonNegativeInteger(editForm.value.displayOrderId)
  const selectedCount = toNonNegativeInteger(editForm.value.selectedCount)
  const lastSelectedAt = toApiDatetime(editForm.value.lastSelectedAt)

  clearFeedback()
  isSaving.value = true

  const payload: components['schemas']['UpdateRestaurantDto'] = {
    restaurantName,
    categoryId: Number(editForm.value.categoryId),
    displayOrderId,
    selectedCount,
    note: editForm.value.note.trim(),
    imageUrl: editForm.value.imageUrl.trim(),
    lastSelectedAt,
  }

  try {
    const { data, error } = await client.PATCH('/restaurants/{id}', {
      params: {
        path: {
          id: restaurantId.value,
        },
      },
      body: payload,
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, RESTAURANT_UPDATE_FEEDBACK_MESSAGES.fallback))
      return
    }

    restaurant.value = data ?? restaurant.value
    failedImage.value = false
    showFeedback(RESTAURANT_UPDATE_FEEDBACK_MESSAGES.success, 'success')
  } finally {
    closeEditRestaurantDialog()
    isSaving.value = false
  }
}

function openEditDialog() {
  if (!restaurant.value) {
    return
  }

  clearFeedback()
  allowEditRestaurantDialogClose.value = false
  syncEditForm(restaurant.value)
  isEditDialogOpen.value = true
}

function closeEditRestaurantDialog() {
  allowEditRestaurantDialogClose.value = true
  isEditDialogOpen.value = false
}

function handleEditDialogOpenChange(open: boolean) {
  if (!open && !allowEditRestaurantDialogClose.value) {
    return
  }

  allowEditRestaurantDialogClose.value = false
  isEditDialogOpen.value = open
}

function openDeleteDialog() {
  if (!canDeleteAsGroupAdmin.value) {
    showFeedback('只有群組管理員可以刪除餐廳')
    return
  }

  isDeleteDialogOpen.value = true
}

async function handleDeleteRestaurant() {
  if (!canDeleteAsGroupAdmin.value) {
    showFeedback('只有群組管理員可以刪除餐廳')
    return
  }

  if (restaurantId.value == null) {
    showFeedback(INVALID_RESTAURANT_ID_MESSAGE)
    return
  }

  clearFeedback()
  isDeleting.value = true

  try {
    const { error } = await client.DELETE('/restaurants/{id}', {
      params: {
        path: {
          id: restaurantId.value,
        },
      },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '刪除餐廳失敗'))
      return
    }

    showFeedback('刪除餐廳成功', 'success', () => {
      void router.push({ name: 'list-restaurant' })
    })
  } finally {
    isDeleteDialogOpen.value = false
    isDeleting.value = false
  }
}

function backToList() {
  void router.push({ name: 'list-restaurant' })
}

function resetCreateDishForm() {
  createDishForm.value = { ...EMPTY_CREATE_DISH_FORM }
}

function openCreateDishDialog() {
  clearFeedback()
  resetCreateDishForm()
  isCreateDishDialogOpen.value = true
}

function handleCreateDishDialogOpenChange(open: boolean) {
  isCreateDishDialogOpen.value = open
  if (!open) {
    resetCreateDishForm()
  }
}

async function handleCreateDish() {
  const id = restaurantId.value
  if (id == null) {
    showFeedback(INVALID_RESTAURANT_ID_MESSAGE)
    return
  }

  if (!showValidationError(validateCreateDish())) {
    return
  }

  const dishName = createDishForm.value.dishName.trim()
  const price = toNonNegativeInteger(createDishForm.value.price)

  clearFeedback()
  isCreatingDish.value = true

  try {
    const { error } = await client.POST('/dishes', {
      body: {
        restaurantId: id,
        dishName,
        price,
      },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '新增餐點失敗'))
      return
    }

    showFeedback('新增餐點成功', 'success')
    await fetchDishes(id)
  } finally {
    handleCreateDishDialogOpenChange(false)
    isCreatingDish.value = false
  }
}

function openEditDishDialog(dish: Dish) {
  if (dish.dishId == null) {
    return
  }

  clearFeedback()
  allowEditDishDialogClose.value = false
  editingDish.value = dish
  editDishForm.value = {
    displayOrderId: dish.displayOrderId != null ? String(dish.displayOrderId) : '',
    dishName: dish.dishName ?? '',
    price: dish.price != null ? String(dish.price) : '',
  }
  isEditDishDialogOpen.value = true
}

function closeEditDishDialog() {
  allowEditDishDialogClose.value = true
  isEditDishDialogOpen.value = false
  editingDish.value = null
  editDishForm.value = { ...EMPTY_DISH_FORM }
}

function handleEditDishDialogOpenChange(open: boolean) {
  if (!open && !allowEditDishDialogClose.value) {
    return
  }

  allowEditDishDialogClose.value = false
  isEditDishDialogOpen.value = open
  if (!open) {
    editingDish.value = null
    editDishForm.value = { ...EMPTY_DISH_FORM }
  }
}

async function handleSaveDish() {
  if (isSavingDish.value) {
    return
  }

  const id = restaurantId.value
  const dishId = editingDish.value?.dishId
  if (id == null || dishId == null) {
    showFeedback(INVALID_DISH_ID_MESSAGE)
    return
  }

  if (!showValidationError(validateEditDish())) {
    return
  }

  const displayOrderId = toNonNegativeInteger(editDishForm.value.displayOrderId)
  const dishName = editDishForm.value.dishName.trim()
  const price = toNonNegativeInteger(editDishForm.value.price)

  clearFeedback()
  isSavingDish.value = true

  const payload: components['schemas']['UpdateDishDto'] = {
    displayOrderId,
    dishName,
    price,
  }

  try {
    const { error } = await client.PATCH('/dishes/{id}', {
      params: {
        path: { id: dishId },
      },
      body: payload,
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '修改餐點失敗'))
      return
    }

    showFeedback('修改餐點成功', 'success')
    await fetchDishes(id)
  } finally {
    closeEditDishDialog()
    isSavingDish.value = false
  }
}

function openDeleteDishDialog(dish: Dish) {
  if (!canDeleteAsGroupAdmin.value) {
    showFeedback(DELETE_PERMISSION_MESSAGE)
    return
  }

  deletingDish.value = dish
  isDeleteDishDialogOpen.value = true
}

async function handleDeleteDish() {
  if (!canDeleteAsGroupAdmin.value) {
    showFeedback(DELETE_PERMISSION_MESSAGE)
    return
  }

  const id = restaurantId.value
  const dishId = deletingDish.value?.dishId
  if (id == null || dishId == null) {
    showFeedback(INVALID_DISH_ID_MESSAGE)
    return
  }

  clearFeedback()
  isDeletingDish.value = true

  try {
    const { error } = await client.DELETE('/dishes/{id}', {
      params: {
        path: { id: dishId },
      },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '刪除餐點失敗'))
      return
    }

    showFeedback('刪除餐點成功', 'success')
    await fetchDishes(id)
  } finally {
    isDeleteDishDialogOpen.value = false
    deletingDish.value = null
    isDeletingDish.value = false
  }
}

watch(
  restaurantId,
  () => {
    void fetchRestaurantDetail()
  },
  { immediate: true },
)

watch(isDeleteDishDialogOpen, (open) => {
  if (!open) {
    deletingDish.value = null
  }
})
</script>

<template>
  <main
    class="min-h-screen bg-fixed bg-cover bg-center bg-no-repeat px-4 py-6 md:px-6"
    :style="homeBgBackgroundStyle"
  >
    <div
      class="relative z-10 mx-auto mt-6 w-full max-w-4xl rounded-[10px] border border-[rgba(226,164,136,0.52)] bg-linear-to-br from-[rgba(255,248,241,0.9)] to-[rgba(255,233,219,0.84)] px-[30px] pt-[30px] pb-8 shadow-[0_14px_32px_rgba(95,57,41,0.24),inset_0_1px_0_rgba(255,255,255,0.55)] backdrop-blur-sm max-lg:mt-5 max-lg:px-6 max-lg:pt-6 max-lg:pb-7 max-md:mt-4 max-md:rounded-lg max-md:px-4 max-md:pt-4 max-md:pb-6"
    >
      <div class="space-y-6">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h1 class="text-2xl font-bold text-card-foreground">餐廳詳細資料</h1>
          <div class="flex flex-wrap items-center justify-end gap-3">
            <template v-if="restaurant">
              <WarmButton
                variant="outline-standard"
                class="h-10 px-4"
                :disabled="isDeleting || isSaving"
                @click="openEditDialog"
              >
                修改餐廳
              </WarmButton>
              <WarmButton
                variant="outline-standard"
                class="h-10 px-4"
                :disabled="isDeleting || isSaving"
                @click="openDeleteDialog"
              >
                {{ isDeleting ? '刪除中...' : '刪除餐廳' }}
              </WarmButton>
            </template>
            <WarmButton class="h-10 px-4" @click="backToList">
              返回列表
            </WarmButton>
          </div>
        </div>

        <div v-if="isLoading" class="rounded-lg border border-border bg-card/70 px-4 py-8 text-center">
          <p class="text-muted-foreground">載入資料中...</p>
        </div>

        <div v-else-if="restaurant" class="space-y-6">
          <div
            class="flex flex-col gap-4 rounded-lg border border-border bg-card/70 p-4 sm:flex-row sm:items-start"
          >
            <img
              :src="displayImageUrl"
              :alt="restaurant.restaurantName ?? '餐廳圖片'"
              class="mx-auto shrink-0 rounded border border-border object-cover sm:mx-0"
              :style="{
                width: '120px',
                height: '80px',
              }"
              loading="lazy"
              @error="handleImageError"
            />

            <div class="grid min-w-0 flex-1 gap-2 text-sm text-card-foreground sm:grid-cols-2">
              <p class="sm:col-span-2 text-lg font-semibold">
                {{ restaurant.restaurantName ?? '-' }}
              </p>
              <p>分類：{{ restaurant.categoryName ?? '-' }}</p>
              <p>顯示排序 ID：{{ restaurant.displayOrderId ?? '-' }}</p>
              <p>被選取次數：{{ restaurant.selectedCount ?? 0 }}</p>
              <p>最後被選時間：{{ restaurant.lastSelectedAt?.trim() || '-' }}</p>
              <p>建立時間：{{ restaurant.createdAt?.trim() || '-' }}</p>
              <p>更新時間：{{ restaurant.updatedAt?.trim() || '-' }}</p>
              <p class="sm:col-span-2">備註：{{ restaurant.note || '-' }}</p>
            </div>
          </div>

          <section class="space-y-3 rounded-lg border border-border bg-card/70 p-4">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <h2 class="text-lg font-bold text-card-foreground">
                餐點資訊
                <span v-if="!isDishesLoading" class="ml-2 text-sm font-normal text-muted-foreground">
                  （共 {{ dishTotal }} 筆）
                </span>
              </h2>
              <WarmButton
                class="h-9 px-3 text-sm"
                :disabled="isDishesLoading || isCreatingDish"
                @click="openCreateDishDialog"
              >
                新增餐點
              </WarmButton>
            </div>

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead class="w-[120px] text-center">顯示排序 ID</TableHead>
                  <TableHead class="text-center">餐點名稱</TableHead>
                  <TableHead class="w-[120px] text-center">價格</TableHead>
                  <TableHead class="w-[180px] text-center">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-if="isDishesLoading">
                  <TableCell colspan="4" class="py-8 text-center text-muted-foreground">
                    載入餐點中...
                  </TableCell>
                </TableRow>
                <TableRow v-else-if="dishes.length === 0">
                  <TableCell colspan="4" class="py-8 text-center text-muted-foreground">
                    此餐廳目前沒有餐點資料
                  </TableCell>
                </TableRow>
                <TableRow v-for="dish in dishes" :key="dish.dishId">
                  <TableCell class="text-center">{{ dish.displayOrderId ?? '-' }}</TableCell>
                  <TableCell class="text-center font-medium">{{ dish.dishName ?? '-' }}</TableCell>
                  <TableCell class="text-center">{{ formatPrice(dish.price) }}</TableCell>
                  <TableCell class="text-center">
                    <div class="flex flex-wrap items-center justify-center gap-2">
                      <WarmButton
                        variant="outline-standard"
                        class="h-8 px-2 text-xs"
                        :disabled="isSavingDish || isDeletingDish"
                        @click="openEditDishDialog(dish)"
                      >
                        修改
                      </WarmButton>
                      <WarmButton
                        variant="outline-standard"
                        class="h-8 px-2 text-xs"
                        :disabled="isSavingDish || isDeletingDish"
                        @click="openDeleteDishDialog(dish)"
                      >
                        刪除
                      </WarmButton>
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </section>
        </div>
      </div>
    </div>

    <RestaurantFormDialog
      :open="isEditDialogOpen"
      v-model="editForm"
      :category-options="categoryOptions"
      mode="edit"
      title="修改餐廳"
      id-prefix="edit-restaurant"
      submit-label="確認修改"
      loading-label="儲存中..."
      :loading="isSaving"
      :can-submit="canSave"
      @update:open="handleEditDialogOpenChange"
      @submit="handleSaveRestaurant"
      @cancel="closeEditRestaurantDialog"
    />

    <ConfirmAlertDialog
      v-model:open="isDeleteDialogOpen"
      title="確認刪除餐廳？"
      confirm-label="確認刪除"
      loading-label="刪除中..."
      :loading="isDeleting"
      @confirm="handleDeleteRestaurant"
    >
      刪除後會封存此餐廳，確定要刪除「{{ restaurant?.restaurantName ?? '此餐廳' }}」嗎？
    </ConfirmAlertDialog>

    <FormAlertDialog
      :open="isCreateDishDialogOpen"
      title="新增餐點"
      submit-label="確認新增"
      loading-label="新增中..."
      :loading="isCreatingDish"
      :can-submit="canCreateDish"
      @update:open="handleCreateDishDialogOpenChange"
      @submit="handleCreateDish"
      @cancel="handleCreateDishDialogOpenChange(false)"
    >
      <div class="space-y-2">
        <Label for="create-dish-name" :class="FORM_LABEL_CLASS">餐點名稱</Label>
        <Input
          id="create-dish-name"
          v-model="createDishForm.dishName"
          maxlength="64"
          :class="FORM_INPUT_CLASS"
          placeholder="例如：牛肉拉麵"
          required
        />
      </div>

      <div class="space-y-2">
        <Label for="create-dish-price" :class="FORM_LABEL_CLASS">價格</Label>
        <Input
          id="create-dish-price"
          v-model="createDishForm.price"
          type="number"
          min="0"
          step="1"
          :class="FORM_INPUT_CLASS"
          placeholder="例如：130"
          required
        />
      </div>
    </FormAlertDialog>

    <FormAlertDialog
      :open="isEditDishDialogOpen"
      title="修改餐點"
      submit-label="確認修改"
      loading-label="儲存中..."
      :loading="isSavingDish"
      :can-submit="canSaveDish"
      @update:open="handleEditDishDialogOpenChange"
      @submit="handleSaveDish"
      @cancel="closeEditDishDialog"
    >
      <div class="space-y-2">
        <Label for="edit-dish-display-order-id" :class="FORM_LABEL_CLASS">顯示排序 ID</Label>
        <Input
          id="edit-dish-display-order-id"
          v-model="editDishForm.displayOrderId"
          type="number"
          min="1"
          step="1"
          :class="FORM_INPUT_CLASS"
          placeholder="例如：1"
          required
        />
      </div>

      <div class="space-y-2">
        <Label for="edit-dish-name" :class="FORM_LABEL_CLASS">餐點名稱</Label>
        <Input
          id="edit-dish-name"
          v-model="editDishForm.dishName"
          maxlength="64"
          :class="FORM_INPUT_CLASS"
          placeholder="例如：雙倍叉燒拉麵"
          required
        />
      </div>

      <div class="space-y-2">
        <Label for="edit-dish-price" :class="FORM_LABEL_CLASS">價格</Label>
        <Input
          id="edit-dish-price"
          v-model="editDishForm.price"
          type="number"
          min="0"
          step="1"
          :class="FORM_INPUT_CLASS"
          placeholder="例如：180"
          required
        />
      </div>
    </FormAlertDialog>

    <ConfirmAlertDialog
      v-model:open="isDeleteDishDialogOpen"
      title="確認刪除餐點？"
      confirm-label="確認刪除"
      loading-label="刪除中..."
      :loading="isDeletingDish"
      @confirm="handleDeleteDish"
    >
      確定要刪除「{{ deletingDish?.dishName ?? '此餐點' }}」嗎？此操作無法復原。
    </ConfirmAlertDialog>
  </main>
</template>
