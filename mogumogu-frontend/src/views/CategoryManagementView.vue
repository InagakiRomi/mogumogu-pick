<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { components } from '@/api/schema'
import client from '@/api/client'
import AlertConfirm from '@/components/alert/AlertConfirm.vue'
import FormDialog from '@/components/form/FormDialog.vue'
import FormSelectField from '@/components/form/FormSelectField.vue'
import ListPagePanel from '@/components/list/ListPagePanel.vue'
import ListSection from '@/components/list/ListSection.vue'
import ListTable, {
  ListTableActions,
  ListTableCell,
  ListTableHead,
  ListTableRow,
} from '@/components/list/ListTable.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { getApiErrorMessage } from '@/lib/apiErrorMessage'
import { authSession } from '@/lib/authSession'
import { isGroupAdmin } from '@/lib/userRole'

const DEFAULT_SORT_OPTIONS = [
  { label: '小到大', value: 'ASC' as const },
  { label: '大到小', value: 'DESC' as const },
]

type RestaurantCategory = components['schemas']['RestaurantCategoryResponse']
type CategoryOrderBy = 'DISPLAY_ORDER_ID' | 'RESTAURANT_COUNT'
type SortOrder = 'ASC' | 'DESC'

const categoryListForm = {
  defaultOrderBy: 'DISPLAY_ORDER_ID' as CategoryOrderBy,
  defaultSort: 'ASC' as SortOrder,
  orderByOptions: [
    { label: '排序 ID', value: 'DISPLAY_ORDER_ID' as const },
    { label: '使用次數', value: 'RESTAURANT_COUNT' as const },
  ],
  sortOptions: DEFAULT_SORT_OPTIONS,
  loadingText: '載入資料中...',
  emptyText: '目前沒有分類資料',
}

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
const orderBy = ref<CategoryOrderBy>(categoryListForm.defaultOrderBy)
const sort = ref<SortOrder>(categoryListForm.defaultSort)

const isLastCategoryRemaining = computed(() => categories.value.length <= 1)
const canDeleteCategory = computed(() => isGroupAdmin(authSession.value?.role))

const sortedCategories = computed(() => {
  const direction = sort.value === 'ASC' ? 1 : -1

  return [...categories.value].sort((a, b) => {
    const primaryDiff =
      orderBy.value === 'DISPLAY_ORDER_ID'
        ? (a.displayOrderId ?? 0) - (b.displayOrderId ?? 0)
        : (a.restaurantCount ?? 0) - (b.restaurantCount ?? 0)

    if (primaryDiff !== 0) {
      return primaryDiff * direction
    }

    const secondaryDiff =
      orderBy.value === 'DISPLAY_ORDER_ID'
        ? (a.categoryId ?? 0) - (b.categoryId ?? 0)
        : (a.displayOrderId ?? 0) - (b.displayOrderId ?? 0)

    return secondaryDiff * direction
  })
})

async function fetchCategories() {
  isLoading.value = true
  const { data, error } = await client.GET('/restaurant-categories')
  if (error) {
    categories.value = []
    showFeedback(getApiErrorMessage(error, '取得分類清單失敗'))
    isLoading.value = false
    return
  }

  categories.value = Array.isArray(data) ? data : []
  isLoading.value = false
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
      showFeedback(getApiErrorMessage(error, '新增分類失敗'))
      return
    }

    showFeedback('新增分類成功', 'success')
    await fetchCategories()
  } finally {
    closeCreateDialog()
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
      showFeedback(getApiErrorMessage(error, '更新分類失敗'))
      return
    }

    showFeedback('更新分類成功', 'success')
    await fetchCategories()
  } finally {
    closeEditDialog()
    isUpdating.value = false
  }
}

function openDeleteDialog(category: RestaurantCategory) {
  if (!canDeleteCategory.value) {
    showFeedback('只有群組管理員可以刪除分類')
    return
  }

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
      showFeedback(getApiErrorMessage(error, '刪除分類失敗'))
      return
    }

    showFeedback('刪除分類成功', 'success')
    await fetchCategories()
  } finally {
    isDeleteDialogOpen.value = false
    deletingCategory.value = null
    isDeleting.value = false
  }
}

onMounted(() => {
  void fetchCategories()
})
</script>

<template>
  <ListPagePanel>
    <ListSection title="餐廳分類" :summary="`共 ${categories.length} 個分類`">
      <div class="flex flex-wrap items-end gap-3">
        <FormSelectField
          v-model="orderBy"
          label="排序欄位"
          :options="categoryListForm.orderByOptions"
        />
        <FormSelectField
          v-model="sort"
          label="排序方向"
          :options="categoryListForm.sortOptions"
        />
        <PrimaryButton :disabled="isLoading" @click="openCreateDialog"> 新增分類 </PrimaryButton>
      </div>

      <ListTable
        :is-loading="isLoading"
        :is-empty="sortedCategories.length === 0"
        :column-count="4"
        :loading-text="categoryListForm.loadingText"
        :empty-text="categoryListForm.emptyText"
      >
        <template #header>
          <ListTableHead class="w-30">排序 ID</ListTableHead>
          <ListTableHead>分類名稱</ListTableHead>
          <ListTableHead class="w-30">使用中餐廳</ListTableHead>
          <ListTableHead class="w-55">操作</ListTableHead>
        </template>

        <ListTableRow
          v-for="category in sortedCategories"
          :key="category.categoryId ?? category.categoryName"
        >
          <ListTableCell>{{ category.displayOrderId ?? '-' }}</ListTableCell>
          <ListTableCell truncate :title="category.categoryName ?? undefined">
            {{ category.categoryName ?? '-' }}
          </ListTableCell>
          <ListTableCell>{{ category.restaurantCount ?? 0 }} 間</ListTableCell>
          <ListTableCell>
            <ListTableActions>
              <PrimaryButton
                variant="outline"
                class="h-9 px-3"
                @click="openEditDialog(category)"
              >
                編輯
              </PrimaryButton>
              <PrimaryButton
                v-if="canDeleteCategory"
                variant="outline"
                class="h-9 px-3"
                @click="openDeleteDialog(category)"
              >
                刪除
              </PrimaryButton>
            </ListTableActions>
          </ListTableCell>
        </ListTableRow>
      </ListTable>
    </ListSection>

    <template #overlay>
      <FormDialog
        :open="isCreateDialogOpen"
        title="新增分類"
        submit-label="新增"
        loading-label="新增中..."
        :loading="isCreating"
        :can-submit="createNameInput.trim().length > 0"
        @update:open="isCreateDialogOpen = $event"
        @submit="createCategory"
        @cancel="closeCreateDialog"
      >
        <div>
          <Label for="create-category-name">分類名稱</Label>
          <Input
            id="create-category-name"
            v-model="createNameInput"
            maxlength="32"
          />
        </div>
      </FormDialog>

      <FormDialog
        :open="isEditDialogOpen"
        title="編輯分類"
        submit-label="更新"
        loading-label="更新中..."
        :loading="isUpdating"
        :can-submit="editNameInput.trim().length > 0 && editDisplayOrderIdInput.trim().length > 0"
        @update:open="isEditDialogOpen = $event"
        @submit="updateCategory"
        @cancel="closeEditDialog"
      >
        <div>
          <Label for="edit-category-name">分類名稱</Label>
          <Input
            id="edit-category-name"
            v-model="editNameInput"
            maxlength="32"
          />
        </div>

        <div>
          <Label for="edit-category-display-order-id">顯示排序 ID</Label>
          <Input
            id="edit-category-display-order-id"
            v-model="editDisplayOrderIdInput"
            type="number"
            min="1"
            step="1"
          />
        </div>
      </FormDialog>

      <AlertConfirm
        :open="isDeleteDialogOpen"
        title="確認刪除分類？"
        :description="`確定要刪除分類「${deletingCategory?.categoryName?.trim() || `ID ${deletingCategory?.categoryId ?? ''}`}」嗎？`"
        confirm-label="確認刪除"
        loading-label="刪除中..."
        :loading="isDeleting"
        @update:open="isDeleteDialogOpen = $event"
        @confirm="handleDeleteCategory"
      />
    </template>
  </ListPagePanel>
</template>
