<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { components } from '@/api/schema'
import client from '@/api/client'
import WarmButton from '@/components/warm/WarmButton.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import ConfirmAlertDialog from '@/components/feedback/ConfirmAlertDialog.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { authSession } from '@/lib/authSession'
import {
  ARCHIVED_RESTAURANT_MESSAGE,
  getApiErrorMessage,
  isArchivedRestaurantError,
  RESTAURANT_UPDATE_FEEDBACK_MESSAGES,
} from '@/lib/apiErrorMessage'
import { isGroupAdmin } from '@/lib/userRole'

type Restaurant = components['schemas']['RestaurantResponse']
type Dish = components['schemas']['DishResponse']

type CategoryOption = {
  label: string
  value: string
}

const DEFAULT_RESTAURANT_IMAGE = '/images/defaultRestaurant.jpg'
/** 預設圖原始尺寸 480×320；詳細頁固定以 120×80 顯示 */
const RESTAURANT_IMAGE_WIDTH = 120
const RESTAURANT_IMAGE_HEIGHT = 80

const categoryOptions: CategoryOption[] = [
  { label: '主食', value: '1' },
  { label: '輕食', value: '2' },
  { label: '飲料', value: '3' },
]

const route = useRoute()
const router = useRouter()
const { showFeedback, clearFeedback } = useFeedbackDialog()

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

const canCreateDish = computed(() => {
  const price = Number(createDishForm.value.price)
  return createDishForm.value.dishName.trim().length > 0 && Number.isInteger(price) && price >= 0 && !isCreatingDish.value
})

const canSaveDish = computed(() => {
  const displayOrderId = Number(editDishForm.value.displayOrderId)
  const price = Number(editDishForm.value.price)
  return (
    Number.isInteger(displayOrderId) &&
    displayOrderId >= 1 &&
    editDishForm.value.dishName.trim().length > 0 &&
    Number.isInteger(price) &&
    price >= 0
  )
})

const canDeleteAsGroupAdmin = computed(() => isGroupAdmin(authSession.value?.role))

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

  return categoryOptions.find((option) => option.value === String(categoryId))?.label ?? '-'
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

  const pad = (part: number) => String(part).padStart(2, '0')
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())}T${pad(parsed.getHours())}:${pad(parsed.getMinutes())}`
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

  const pad = (part: number) => String(part).padStart(2, '0')
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())} ${pad(parsed.getHours())}:${pad(parsed.getMinutes())}:${pad(parsed.getSeconds())}`
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

  try {
    const { data, error } = await client.GET('/restaurants/{restaurantId}/dishes', {
      params: {
        path: { restaurantId: id },
      },
    })

    if (error) {
      throw error
    }

    dishes.value = data?.data ?? []
    dishTotal.value = Number(data?.total ?? dishes.value.length)
  } catch (error) {
    dishes.value = []
    dishTotal.value = 0
    showFeedback(getApiErrorMessage(error, '取得餐點清單失敗'))
  } finally {
    isDishesLoading.value = false
  }
}

async function fetchRestaurantDetail() {
  const id = restaurantId.value
  if (id == null) {
    showFeedback('餐廳 ID 不正確')
    void router.replace({ name: 'list-restaurant' })
    return
  }

  clearFeedback()
  isLoading.value = true
  failedImage.value = false
  dishes.value = []
  dishTotal.value = 0

  try {
    const { data: restaurantData, error: restaurantError } = await client.GET('/restaurants/{id}', {
      params: {
        path: { id },
      },
    })

    if (restaurantError) {
      throw restaurantError
    }

    if (restaurantData?.isArchived) {
      showArchivedRestaurantFeedback()
      return
    }

    restaurant.value = restaurantData ?? null
    if (restaurant.value) {
      await fetchDishes(id)
    }
  } catch (error) {
    if (isArchivedRestaurantError(error)) {
      showArchivedRestaurantFeedback()
      return
    }

    restaurant.value = null
    showFeedback(getApiErrorMessage(error, '取得餐廳詳細資料失敗'))
  } finally {
    isLoading.value = false
  }
}

async function handleSaveRestaurant() {
  if (isSaving.value) {
    return
  }

  if (restaurantId.value == null) {
    showFeedback('餐廳 ID 不正確')
    return
  }

  const restaurantName = editForm.value.restaurantName.trim()
  const displayOrderId = Number(editForm.value.displayOrderId)
  const selectedCount = Number(editForm.value.selectedCount)
  const lastSelectedAt = toApiDatetime(editForm.value.lastSelectedAt)

  if (!restaurantName) {
    showFeedback('請輸入餐廳名稱')
    return
  }
  if (restaurantName.length > 64) {
    showFeedback('餐廳名稱不可超過 64 字')
    return
  }
  if (!Number.isInteger(displayOrderId) || displayOrderId < 0) {
    showFeedback('請輸入有效的顯示排序 ID（須為 0 以上的整數）')
    return
  }
  if (!Number.isInteger(selectedCount) || selectedCount < 0) {
    showFeedback('請輸入有效的被選取次數（須為 0 以上的整數）')
    return
  }
  if (editForm.value.note.length > 512) {
    showFeedback('備註不可超過 512 字')
    return
  }
  if (editForm.value.imageUrl.length > 512) {
    showFeedback('圖片網址不可超過 512 字')
    return
  }
  if (editForm.value.lastSelectedAt.trim() && !lastSelectedAt) {
    showFeedback('請輸入有效的最後被選取時間')
    return
  }

  clearFeedback()
  isSaving.value = true

  try {
    const payload: components['schemas']['UpdateRestaurantDto'] = {
      restaurantName,
      categoryId: Number(editForm.value.categoryId),
      displayOrderId,
      selectedCount,
      note: editForm.value.note.trim(),
      imageUrl: editForm.value.imageUrl.trim(),
      lastSelectedAt,
    }

    const { data, error } = await client.PATCH('/restaurants/{id}', {
      params: {
        path: {
          id: restaurantId.value,
        },
      },
      body: payload,
    })

    if (error) {
      throw error
    }

    restaurant.value = data ?? restaurant.value
    failedImage.value = false
    showFeedback(RESTAURANT_UPDATE_FEEDBACK_MESSAGES.success, 'success')
  } catch (error) {
    showFeedback(getApiErrorMessage(error, RESTAURANT_UPDATE_FEEDBACK_MESSAGES.fallback))
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
    showFeedback('餐廳 ID 不正確')
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
      throw error
    }

    isDeleteDialogOpen.value = false
    showFeedback('刪除餐廳成功', 'success', () => {
      void router.push({ name: 'list-restaurant' })
    })
  } catch (error) {
    showFeedback(getApiErrorMessage(error, '刪除餐廳失敗'))
  } finally {
    isDeleting.value = false
  }
}

function backToList() {
  void router.push({ name: 'list-restaurant' })
}

function resetCreateDishForm() {
  createDishForm.value = {
    dishName: '',
    price: '',
  }
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
    showFeedback('餐廳 ID 不正確')
    return
  }

  const dishName = createDishForm.value.dishName.trim()
  const price = Number(createDishForm.value.price)
  if (!dishName) {
    showFeedback('請輸入餐點名稱')
    return
  }
  if (!Number.isInteger(price) || price < 0) {
    showFeedback('請輸入有效的餐點價格')
    return
  }

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
      throw error
    }

    isCreateDishDialogOpen.value = false
    resetCreateDishForm()
    showFeedback('新增餐點成功', 'success')
    await fetchDishes(id)
  } catch (error) {
    showFeedback(getApiErrorMessage(error, '新增餐點失敗'))
  } finally {
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
  editDishForm.value = { displayOrderId: '', dishName: '', price: '' }
}

function handleEditDishDialogOpenChange(open: boolean) {
  if (!open && !allowEditDishDialogClose.value) {
    return
  }

  allowEditDishDialogClose.value = false
  isEditDishDialogOpen.value = open
  if (!open) {
    editingDish.value = null
    editDishForm.value = { displayOrderId: '', dishName: '', price: '' }
  }
}

async function handleSaveDish() {
  if (isSavingDish.value) {
    return
  }

  const id = restaurantId.value
  const dishId = editingDish.value?.dishId
  if (id == null || dishId == null) {
    showFeedback('餐點 ID 不正確')
    return
  }

  const displayOrderId = Number(editDishForm.value.displayOrderId)
  const dishName = editDishForm.value.dishName.trim()
  const price = Number(editDishForm.value.price)
  if (!Number.isInteger(displayOrderId) || displayOrderId < 1) {
    showFeedback('請輸入有效的顯示排序 ID（須為 1 以上的整數）')
    return
  }
  if (!dishName) {
    showFeedback('請輸入餐點名稱')
    return
  }
  if (!Number.isInteger(price) || price < 0) {
    showFeedback('請輸入有效的餐點價格')
    return
  }

  clearFeedback()
  isSavingDish.value = true

  try {
    const payload: components['schemas']['UpdateDishDto'] = {
      displayOrderId,
      dishName,
      price,
    }

    const { error } = await client.PATCH('/dishes/{id}', {
      params: {
        path: { id: dishId },
      },
      body: payload,
    })

    if (error) {
      throw error
    }

    showFeedback('修改餐點成功', 'success')
    await fetchDishes(id)
  } catch (error) {
    showFeedback(getApiErrorMessage(error, '修改餐點失敗'))
  } finally {
    closeEditDishDialog()
    isSavingDish.value = false
  }
}

function openDeleteDishDialog(dish: Dish) {
  if (!canDeleteAsGroupAdmin.value) {
    showFeedback('只有群組管理員可以刪除')
    return
  }

  deletingDish.value = dish
  isDeleteDishDialogOpen.value = true
}

async function handleDeleteDish() {
  if (!canDeleteAsGroupAdmin.value) {
    showFeedback('只有群組管理員可以刪除')
    return
  }

  const id = restaurantId.value
  const dishId = deletingDish.value?.dishId
  if (id == null || dishId == null) {
    showFeedback('餐點 ID 不正確')
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
      throw error
    }

    isDeleteDishDialogOpen.value = false
    deletingDish.value = null
    showFeedback('刪除餐點成功', 'success')
    await fetchDishes(id)
  } catch (error) {
    showFeedback(getApiErrorMessage(error, '刪除餐點失敗'))
  } finally {
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
</script>

<template>
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-fixed bg-cover bg-center bg-no-repeat px-4 py-6 md:px-6"
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
                width: `${RESTAURANT_IMAGE_WIDTH}px`,
                height: `${RESTAURANT_IMAGE_HEIGHT}px`,
              }"
              loading="lazy"
              @error="handleImageError"
            />

            <div class="grid min-w-0 flex-1 gap-2 text-sm text-card-foreground sm:grid-cols-2">
              <p class="sm:col-span-2 text-lg font-semibold">
                {{ restaurant.restaurantName ?? '-' }}
              </p>
              <p>分類：{{ formatCategoryLabel(restaurant.categoryId) }}</p>
              <p>顯示排序 ID：{{ restaurant.displayOrderId ?? '-' }}</p>
              <p>被選取次數：{{ restaurant.selectedCount ?? 0 }}</p>
              <p>最後被選時間：{{ formatDate(restaurant.lastSelectedAt) }}</p>
              <p>建立時間：{{ formatDate(restaurant.createdAt) }}</p>
              <p>更新時間：{{ formatDate(restaurant.updatedAt) }}</p>
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

    <AlertDialog :open="isEditDialogOpen" @update:open="handleEditDialogOpenChange">
      <AlertDialogContent
        class="max-w-[min(92vw,768px)]! data-[size=default]:max-w-[min(92vw,768px)]! data-[size=default]:sm:max-w-[min(92vw,768px)]! border-border bg-card text-card-foreground"
      >
        <AlertDialogHeader>
          <AlertDialogTitle>修改餐廳</AlertDialogTitle>
        </AlertDialogHeader>

        <form class="space-y-4" @submit.prevent="handleSaveRestaurant">
          <div class="space-y-2">
            <Label for="edit-restaurant-name" class="font-bold text-muted-foreground">餐廳名稱</Label>
            <Input
              id="edit-restaurant-name"
              v-model="editForm.restaurantName"
              maxlength="64"
              placeholder="例如：和食天國"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="edit-restaurant-category" class="font-bold text-muted-foreground">分類</Label>
            <Select v-model="editForm.categoryId">
              <SelectTrigger
                id="edit-restaurant-category"
                class="h-10 w-full rounded-md border border-border bg-muted/90 px-3 text-left text-sm text-popover-foreground"
              >
                <SelectValue placeholder="選擇分類" />
              </SelectTrigger>
              <SelectContent
                position="popper"
                class="z-10000 border-border bg-card text-popover-foreground"
              >
                <SelectItem v-for="option in categoryOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <Label for="edit-restaurant-display-order-id" class="font-bold text-muted-foreground">
                顯示排序 ID
              </Label>
              <Input
                id="edit-restaurant-display-order-id"
                v-model="editForm.displayOrderId"
                type="number"
                min="0"
                step="1"
                placeholder="例如：1"
                required
              />
            </div>

            <div class="space-y-2">
              <Label for="edit-restaurant-selected-count" class="font-bold text-muted-foreground">
                被選取次數
              </Label>
              <Input
                id="edit-restaurant-selected-count"
                v-model="editForm.selectedCount"
                type="number"
                min="0"
                step="1"
                placeholder="例如：0"
                required
              />
            </div>
          </div>

          <div class="space-y-2">
            <Label for="edit-restaurant-note" class="font-bold text-muted-foreground">備註</Label>
            <Input
              id="edit-restaurant-note"
              v-model="editForm.note"
              maxlength="512"
              placeholder="例如：可電話訂位"
            />
          </div>

          <div class="space-y-2">
            <Label for="edit-restaurant-image-url" class="font-bold text-muted-foreground">圖片網址</Label>
            <Input
              id="edit-restaurant-image-url"
              v-model="editForm.imageUrl"
              maxlength="512"
              placeholder="https://example.com/restaurant.jpg"
            />
          </div>

          <div class="space-y-2">
            <Label for="edit-restaurant-last-selected-at" class="font-bold text-muted-foreground">
              最後被選取時間
            </Label>
            <Input
              id="edit-restaurant-last-selected-at"
              v-model="editForm.lastSelectedAt"
              type="datetime-local"
              step="1"
            />
          </div>

          <AlertDialogFooter class="mt-1 sm:gap-3">
            <WarmButton
              type="button"
              variant="outline-standard"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="isSaving"
              @click="closeEditRestaurantDialog"
            >
              取消
            </WarmButton>
            <WarmButton
              type="submit"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="!canSave || isSaving"
            >
              {{ isSaving ? '儲存中...' : '確認修改' }}
            </WarmButton>
          </AlertDialogFooter>
        </form>
      </AlertDialogContent>
    </AlertDialog>

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

    <AlertDialog :open="isCreateDishDialogOpen" @update:open="handleCreateDishDialogOpenChange">
      <AlertDialogContent
        class="max-w-[min(92vw,768px)]! data-[size=default]:max-w-[min(92vw,768px)]! data-[size=default]:sm:max-w-[min(92vw,768px)]! border-border bg-card text-card-foreground"
      >
        <AlertDialogHeader>
          <AlertDialogTitle>新增餐點</AlertDialogTitle>
        </AlertDialogHeader>

        <form class="space-y-4" @submit.prevent="handleCreateDish">
          <div class="space-y-2">
            <Label for="create-dish-name" class="font-bold text-muted-foreground">餐點名稱</Label>
            <Input
              id="create-dish-name"
              v-model="createDishForm.dishName"
              maxlength="64"
              placeholder="例如：牛肉拉麵"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="create-dish-price" class="font-bold text-muted-foreground">價格</Label>
            <Input
              id="create-dish-price"
              v-model="createDishForm.price"
              type="number"
              min="0"
              step="1"
              placeholder="例如：130"
              required
            />
          </div>

          <AlertDialogFooter class="mt-1 sm:gap-3">
            <WarmButton
              type="button"
              variant="outline-standard"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="isCreatingDish"
              @click="handleCreateDishDialogOpenChange(false)"
            >
              取消
            </WarmButton>
            <WarmButton
              type="submit"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="!canCreateDish"
            >
              {{ isCreatingDish ? '新增中...' : '確認新增' }}
            </WarmButton>
          </AlertDialogFooter>
        </form>
      </AlertDialogContent>
    </AlertDialog>

    <AlertDialog :open="isEditDishDialogOpen" @update:open="handleEditDishDialogOpenChange">
      <AlertDialogContent
        class="max-w-[min(92vw,768px)]! data-[size=default]:max-w-[min(92vw,768px)]! data-[size=default]:sm:max-w-[min(92vw,768px)]! border-border bg-card text-card-foreground"
      >
        <AlertDialogHeader>
          <AlertDialogTitle>修改餐點</AlertDialogTitle>
        </AlertDialogHeader>

        <form class="space-y-4" @submit.prevent="handleSaveDish">
          <div class="space-y-2">
            <Label for="edit-dish-display-order-id" class="font-bold text-muted-foreground">
              顯示排序 ID
            </Label>
            <Input
              id="edit-dish-display-order-id"
              v-model="editDishForm.displayOrderId"
              type="number"
              min="1"
              step="1"
              placeholder="例如：1"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="edit-dish-name" class="font-bold text-muted-foreground">餐點名稱</Label>
            <Input
              id="edit-dish-name"
              v-model="editDishForm.dishName"
              maxlength="64"
              placeholder="例如：雙倍叉燒拉麵"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="edit-dish-price" class="font-bold text-muted-foreground">價格</Label>
            <Input
              id="edit-dish-price"
              v-model="editDishForm.price"
              type="number"
              min="0"
              step="1"
              placeholder="例如：180"
              required
            />
          </div>

          <AlertDialogFooter class="mt-1 sm:gap-3">
            <WarmButton
              type="button"
              variant="outline-standard"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="isSavingDish"
              @click="closeEditDishDialog"
            >
              取消
            </WarmButton>
            <WarmButton
              type="submit"
              class="h-11 min-w-[120px] flex-1 sm:flex-none"
              :disabled="!canSaveDish || isSavingDish"
            >
              {{ isSavingDish ? '儲存中...' : '確認修改' }}
            </WarmButton>
          </AlertDialogFooter>
        </form>
      </AlertDialogContent>
    </AlertDialog>

    <AlertDialog
      :open="isDeleteDishDialogOpen"
      @update:open="
        (value) => {
          isDeleteDishDialogOpen = value
          if (!value) deletingDish = null
        }
      "
    >
      <AlertDialogContent class="border-border bg-card text-card-foreground">
        <AlertDialogHeader>
          <AlertDialogTitle>確認刪除餐點？</AlertDialogTitle>
          <AlertDialogDescription>
            確定要刪除「{{ deletingDish?.dishName ?? '此餐點' }}」嗎？此操作無法復原。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="isDeletingDish">取消</AlertDialogCancel>
          <AlertDialogAction :disabled="isDeletingDish" @click="handleDeleteDish">
            {{ isDeletingDish ? '刪除中...' : '確認刪除' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </main>
</template>
