<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { components } from '@/api/schema'
import client from '@/api/client'
import ConfirmAlertDialog from '@/components/feedback/ConfirmAlertDialog.vue'
import FormAlertDialog from '@/components/feedback/FormAlertDialog.vue'
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { getApiErrorMessage } from '@/lib/apiErrorMessage'

type RestaurantCategory = components['schemas']['RestaurantCategoryResponse']
type CategoryOrderBy = 'CATEGORY_ID' | 'DISPLAY_ORDER_ID' | 'RESTAURANT_COUNT'
type SortOrder = 'ASC' | 'DESC'

const orderByOptions: Array<{ label: string; value: CategoryOrderBy }> = [
  { label: '排序 ID', value: 'DISPLAY_ORDER_ID' },
  { label: '使用次數', value: 'RESTAURANT_COUNT' },
]

const sortOptions: Array<{ label: string; value: SortOrder }> = [
  { label: '小到大', value: 'ASC' },
  { label: '大到小', value: 'DESC' },
]

const { showFeedback, clearFeedback } = useFeedbackDialog()

const isLoading = ref(false)
const isCreating = ref(false)
const isUpdating = ref(false)
const isDeleting = ref(false)
const isDeleteDialogOpen = ref(false)
const deletingCategory = ref<RestaurantCategory | null>(null)
const categories = ref<RestaurantCategory[]>([])
const isCreateDialogOpen = ref(false)
const isEditDialogOpen = ref(false)
const createNameInput = ref('')
const editCategoryId = ref<number | null>(null)
const editNameInput = ref('')
const editDisplayOrderIdInput = ref('')
const orderBy = ref<CategoryOrderBy>('DISPLAY_ORDER_ID')
const sort = ref<SortOrder>('ASC')

const isLastCategoryRemaining = computed(() => categories.value.length <= 1)

const sortedCategories = computed(() => {
  const direction = sort.value === 'ASC' ? 1 : -1

  return [...categories.value].sort((a, b) => {
    const primaryDiff =
      orderBy.value === 'CATEGORY_ID'
        ? (a.categoryId ?? 0) - (b.categoryId ?? 0)
        : orderBy.value === 'DISPLAY_ORDER_ID'
          ? (a.displayOrderId ?? 0) - (b.displayOrderId ?? 0)
          : (a.restaurantCount ?? 0) - (b.restaurantCount ?? 0)

    if (primaryDiff !== 0) {
      return primaryDiff * direction
    }

    const secondaryDiff =
      orderBy.value === 'CATEGORY_ID'
        ? (a.displayOrderId ?? 0) - (b.displayOrderId ?? 0)
        : orderBy.value === 'DISPLAY_ORDER_ID'
          ? (a.categoryId ?? 0) - (b.categoryId ?? 0)
          : (a.displayOrderId ?? 0) - (b.displayOrderId ?? 0)

    return secondaryDiff * direction
  })
})

async function fetchCategories() {
  isLoading.value = true
  try {
    const { data, error } = await client.GET('/restaurant-categories')
    if (error) {
      throw error
    }
    categories.value = Array.isArray(data) ? data : []
  } catch (error) {
    categories.value = []
    showFeedback(getApiErrorMessage(error, '取得分類清單失敗'))
  } finally {
    isLoading.value = false
  }
}

function openCreateDialog() {
  createNameInput.value = ''
  isCreateDialogOpen.value = true
}

function closeCreateDialog() {
  isCreateDialogOpen.value = false
}

function openEditDialog(category: RestaurantCategory) {
  editCategoryId.value = category.categoryId ?? null
  editNameInput.value = category.categoryName?.trim() ?? ''
  editDisplayOrderIdInput.value = String(category.displayOrderId ?? 1)
  isEditDialogOpen.value = true
}

function closeEditDialog() {
  isEditDialogOpen.value = false
  editCategoryId.value = null
}

async function createCategory() {
  const categoryName = createNameInput.value.trim()
  if (!categoryName) {
    showFeedback('請輸入分類名稱')
    return
  }

  clearFeedback()
  isCreating.value = true

  try {
    const { error } = await client.POST('/restaurant-categories', {
      body: { categoryName },
    })

    if (error) {
      throw error
    }

    closeCreateDialog()
    showFeedback('新增分類成功', 'success')
    await fetchCategories()
  } catch (error) {
    closeCreateDialog()
    showFeedback(getApiErrorMessage(error, '新增分類失敗'))
  } finally {
    isCreating.value = false
  }
}

async function updateCategory() {
  const categoryId = editCategoryId.value
  if (categoryId == null) {
    showFeedback('找不到分類 ID')
    return
  }

  const categoryName = editNameInput.value.trim()
  const displayOrderId = Number(editDisplayOrderIdInput.value)
  if (!categoryName) {
    showFeedback('請輸入分類名稱')
    return
  }
  if (!Number.isInteger(displayOrderId) || displayOrderId < 1) {
    showFeedback('顯示排序必須為大於 0 的整數')
    return
  }

  clearFeedback()
  isUpdating.value = true

  try {
    const { error } = await client.PATCH('/restaurant-categories/{id}', {
      params: {
        path: { id: categoryId },
      },
      body: {
        categoryName,
        displayOrderId,
      },
    })

    if (error) {
      throw error
    }

    closeEditDialog()
    showFeedback('更新分類成功', 'success')
    await fetchCategories()
  } catch (error) {
    showFeedback(getApiErrorMessage(error, '更新分類失敗'))
  } finally {
    isUpdating.value = false
  }
}

function openDeleteDialog(category: RestaurantCategory) {
  if (category.categoryId == null) {
    showFeedback('找不到分類 ID')
    return
  }

  if (isLastCategoryRemaining.value) {
    showFeedback('每個群組至少需保留一個分類，無法刪除最後一個分類')
    return
  }

  deletingCategory.value = category
  isDeleteDialogOpen.value = true
}

async function handleDeleteCategory() {
  const categoryId = deletingCategory.value?.categoryId
  if (categoryId == null) {
    showFeedback('找不到分類 ID')
    return
  }

  if (isLastCategoryRemaining.value) {
    showFeedback('每個群組至少需保留一個分類，無法刪除最後一個分類')
    return
  }

  clearFeedback()
  isDeleting.value = true

  try {
    const { error } = await client.DELETE('/restaurant-categories/{id}', {
      params: {
        path: { id: categoryId },
      },
    })

    if (error) {
      throw error
    }

    isDeleteDialogOpen.value = false
    deletingCategory.value = null
    showFeedback('刪除分類成功', 'success')
    await fetchCategories()
  } catch (error) {
    isDeleteDialogOpen.value = false
    deletingCategory.value = null
    showFeedback(getApiErrorMessage(error, '刪除分類失敗'))
  } finally {
    isDeleting.value = false
  }
}

onMounted(() => {
  void fetchCategories()
})
</script>

<template>
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-fixed bg-cover bg-center bg-no-repeat px-4 py-6 md:px-6"
  >
    <div
      class="relative z-10 mx-auto mt-6 w-full rounded-[10px] border border-[rgba(226,164,136,0.52)] bg-linear-to-br from-[rgba(255,248,241,0.9)] to-[rgba(255,233,219,0.84)] px-[30px] pt-[30px] pb-8 shadow-[0_14px_32px_rgba(95,57,41,0.24),inset_0_1px_0_rgba(255,255,255,0.55)] backdrop-blur-sm max-lg:mt-5 max-lg:px-6 max-lg:pt-6 max-lg:pb-7 max-md:mt-4 max-md:rounded-lg max-md:px-4 max-md:pt-4 max-md:pb-6"
    >
      <section class="space-y-4 rounded-lg border border-border bg-card/70 p-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 class="text-lg font-bold text-card-foreground">餐廳分類</h2>
          <p class="text-sm text-muted-foreground">共 {{ categories.length }} 個分類</p>
        </div>

        <div class="flex flex-wrap items-end gap-3">
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

          <WarmButton :disabled="isLoading" @click="openCreateDialog">
            新增分類
          </WarmButton>
        </div>

        <Table class="table-fixed rounded-md border border-border bg-card">
          <TableHeader>
            <TableRow>
              <TableHead class="w-[120px] text-center">排序 ID</TableHead>
              <TableHead class="text-center">分類名稱</TableHead>
              <TableHead class="w-[120px] text-center">使用中餐廳</TableHead>
              <TableHead class="w-[220px] text-center">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="isLoading">
              <TableCell colspan="4" class="py-8 text-center text-muted-foreground">
                載入資料中...
              </TableCell>
            </TableRow>
            <TableRow v-else-if="sortedCategories.length === 0">
              <TableCell colspan="4" class="py-8 text-center text-muted-foreground">
                目前沒有分類資料
              </TableCell>
            </TableRow>
            <TableRow
              v-for="category in sortedCategories"
              :key="category.categoryId ?? category.categoryName"
              class="h-[52px]"
            >
              <TableCell class="h-[52px] py-2 text-center">{{ category.displayOrderId ?? '-' }}</TableCell>
              <TableCell
                class="h-[52px] truncate py-2 text-center"
                :title="category.categoryName ?? undefined"
              >
                {{ category.categoryName ?? '-' }}
              </TableCell>
              <TableCell class="h-[52px] py-2 text-center">
                {{ category.restaurantCount ?? 0 }} 間
              </TableCell>
              <TableCell class="h-[52px] py-2 text-center">
                <div class="flex h-9 items-center justify-center gap-2">
                  <WarmButton
                    variant="outline-standard"
                    class="h-9 px-3 text-sm"
                    @click="openEditDialog(category)"
                  >
                    編輯
                  </WarmButton>
                  <WarmButton
                    variant="outline-standard"
                    class="h-9 px-3 text-sm"
                    @click="openDeleteDialog(category)"
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

    <FormAlertDialog
      :open="isCreateDialogOpen"
      title="新增分類"
      submit-label="新增"
      :loading="isCreating"
      loading-label="新增中..."
      :can-submit="createNameInput.trim().length > 0"
      @update:open="isCreateDialogOpen = $event"
      @submit="createCategory"
      @cancel="closeCreateDialog"
    >
      <div class="space-y-2">
        <Label for="create-category-name" class="font-bold text-muted-foreground">分類名稱</Label>
        <Input
          id="create-category-name"
          v-model="createNameInput"
          maxlength="32"
          class="h-10"
          placeholder="例如：甜點"
        />
      </div>
    </FormAlertDialog>

    <FormAlertDialog
      :open="isEditDialogOpen"
      title="編輯分類"
      submit-label="更新"
      :loading="isUpdating"
      loading-label="更新中..."
      :can-submit="editNameInput.trim().length > 0 && editDisplayOrderIdInput.trim().length > 0"
      @update:open="isEditDialogOpen = $event"
      @submit="updateCategory"
      @cancel="closeEditDialog"
    >
      <div class="space-y-2">
        <Label for="edit-category-name" class="font-bold text-muted-foreground">分類名稱</Label>
        <Input
          id="edit-category-name"
          v-model="editNameInput"
          maxlength="32"
          class="h-10"
          placeholder="例如：甜點"
        />
      </div>
      <div class="space-y-2">
        <Label for="edit-category-display-order-id" class="font-bold text-muted-foreground">
          顯示排序 ID
        </Label>
        <Input
          id="edit-category-display-order-id"
          v-model="editDisplayOrderIdInput"
          type="number"
          min="1"
          step="1"
          class="h-10"
          placeholder="例如：1"
        />
      </div>
    </FormAlertDialog>

    <ConfirmAlertDialog
      v-model:open="isDeleteDialogOpen"
      title="確認刪除分類？"
      confirm-label="確認刪除"
      loading-label="刪除中..."
      :loading="isDeleting"
      @confirm="handleDeleteCategory"
    >
      確定要刪除分類「{{
        deletingCategory?.categoryName?.trim() || `ID ${deletingCategory?.categoryId ?? ''}`
      }}」嗎？
    </ConfirmAlertDialog>
  </main>
</template>
