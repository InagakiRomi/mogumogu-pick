<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import client from '@/api/client'
import FormDialog from '@/components/form/FormDialog.vue'
import PrimarySelectContent from '@/components/common/PrimarySelectContent.vue'
import PrimarySelectTrigger from '@/components/common/PrimarySelectTrigger.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectItem, SelectValue } from '@/components/ui/select'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { useRestaurantCategories } from '@/composables/useRestaurantCategories'
import { getApiErrorMessage } from '@/lib/apiErrorMessage'
import { authSession } from '@/lib/authSession'

export type CreateRestaurantPrefill = {
  restaurantName?: string | null
  address?: string | null
}

const props = defineProps<{
  open: boolean
  prefill?: CreateRestaurantPrefill | null
}>()

const emit = defineEmits<{
  'update:open': [boolean]
  created: []
}>()

const { categories, defaultCategoryId } = useRestaurantCategories()
const { showFeedback, clearFeedback } = useFeedbackDialog()

const isCreating = ref(false)
const createForm = ref({
  restaurantName: '',
  categoryId: '1',
  address: '',
  note: '',
  imageUrl: '',
})

const canSubmit = computed(
  () => createForm.value.restaurantName.trim().length > 0 && !isCreating.value,
)

function resetCreateForm() {
  createForm.value = {
    restaurantName: props.prefill?.restaurantName?.trim() ?? '',
    categoryId: defaultCategoryId.value || '1',
    address: props.prefill?.address?.trim() ?? '',
    note: '',
    imageUrl: '',
  }
}

function handleOpenChange(open: boolean) {
  emit('update:open', open)

  if (!open) {
    resetCreateForm()
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
        address: createForm.value.address.trim() || undefined,
        note: createForm.value.note.trim() || undefined,
        imageUrl: createForm.value.imageUrl.trim() || undefined,
      },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '新增餐廳失敗'))
      return
    }

    showFeedback('新增餐廳成功', 'success')
    emit('created')
  } finally {
    handleOpenChange(false)
    isCreating.value = false
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      clearFeedback()
      resetCreateForm()
    }
  },
)
</script>

<template>
  <FormDialog
    :open="open"
    title="新增餐廳"
    submit-label="確認新增"
    loading-label="新增中..."
    :loading="isCreating"
    :can-submit="canSubmit"
    @update:open="handleOpenChange"
    @submit="handleCreateRestaurant"
    @cancel="handleOpenChange(false)"
  >
    <div>
      <Label for="create-restaurant-name">餐廳名稱</Label>
      <Input
        id="create-restaurant-name"
        v-model="createForm.restaurantName"
        maxlength="100"
        required
      />
    </div>

    <div>
      <Label for="create-restaurant-address">地址（選填）</Label>
      <Input id="create-restaurant-address" v-model="createForm.address" maxlength="255" />
    </div>

    <div>
      <Label for="create-restaurant-category">分類</Label>
      <Select v-model="createForm.categoryId">
        <PrimarySelectTrigger id="create-restaurant-category">
          <SelectValue />
        </PrimarySelectTrigger>
        <PrimarySelectContent
          position="popper"
          align="start"
          class="w-(--reka-select-trigger-width) max-w-(--reka-select-trigger-width) border-border bg-card text-popover-foreground"
        >
          <SelectItem v-for="option in categories" :key="option.value" :value="option.value">
            {{ option.label }}
          </SelectItem>
        </PrimarySelectContent>
      </Select>
    </div>

    <div>
      <Label for="create-restaurant-note">備註（選填）</Label>
      <Input id="create-restaurant-note" v-model="createForm.note" maxlength="255" />
    </div>

    <div>
      <Label for="create-restaurant-image-url">圖片網址（選填）</Label>
      <Input id="create-restaurant-image-url" v-model="createForm.imageUrl" maxlength="255" />
    </div>
  </FormDialog>
</template>
