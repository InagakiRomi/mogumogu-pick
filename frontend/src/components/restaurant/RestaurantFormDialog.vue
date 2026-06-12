<script setup lang="ts">
import FormAlertDialog from '@/components/feedback/FormAlertDialog.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { RestaurantCategoryOption } from '@/composables/useRestaurantCategories'

const FORM_LABEL_CLASS = 'font-bold text-muted-foreground'
const FORM_INPUT_CLASS =
  'h-10 px-2.5 text-sm rounded-md border border-border bg-muted/90 text-popover-foreground'

export type RestaurantFormData = {
  restaurantName: string
  categoryId: string
  note: string
  imageUrl: string
  displayOrderId?: string
  selectedCount?: string
  lastSelectedAt?: string
}

const form = defineModel<RestaurantFormData>({ required: true })

withDefaults(
  defineProps<{
    open: boolean
    mode: 'create' | 'edit'
    title: string
    idPrefix: string
    categoryOptions: RestaurantCategoryOption[]
    loading?: boolean
    canSubmit?: boolean
    submitLabel?: string
    loadingLabel?: string
  }>(),
  {
    loading: false,
    canSubmit: true,
    submitLabel: '確認',
    loadingLabel: '處理中...',
  },
)

const emit = defineEmits<{
  'update:open': [boolean]
  submit: []
  cancel: []
}>()

const nameMaxLength = (mode: 'create' | 'edit') => (mode === 'create' ? 100 : 64)
const noteMaxLength = (mode: 'create' | 'edit') => (mode === 'create' ? 255 : 512)
const imageUrlMaxLength = (mode: 'create' | 'edit') => (mode === 'create' ? 255 : 512)
</script>

<template>
  <FormAlertDialog
    :open="open"
    :title="title"
    :loading="loading"
    :can-submit="canSubmit"
    :submit-label="submitLabel"
    :loading-label="loadingLabel"
    @update:open="emit('update:open', $event)"
    @submit="emit('submit')"
    @cancel="emit('cancel')"
  >
    <div class="space-y-2">
      <Label :for="`${idPrefix}-name`" :class="FORM_LABEL_CLASS">餐廳名稱</Label>
      <Input
        :id="`${idPrefix}-name`"
        v-model="form.restaurantName"
        :maxlength="nameMaxLength(mode)"
        :class="FORM_INPUT_CLASS"
        placeholder="例如：和食天國"
        required
      />
    </div>

    <div class="space-y-2">
      <Label :for="`${idPrefix}-category`" :class="FORM_LABEL_CLASS">分類</Label>
      <Select v-model="form.categoryId">
        <SelectTrigger
          :id="`${idPrefix}-category`"
          class="h-10 w-full px-3 text-left text-sm rounded-md border border-border bg-muted/90 text-popover-foreground"
        >
          <SelectValue placeholder="選擇分類" />
        </SelectTrigger>
        <SelectContent position="popper" class="z-10000 border-border bg-card text-popover-foreground">
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

    <template v-if="mode === 'edit'">
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="space-y-2">
          <Label :for="`${idPrefix}-display-order-id`" :class="FORM_LABEL_CLASS">
            顯示排序 ID
          </Label>
          <Input
            :id="`${idPrefix}-display-order-id`"
            v-model="form.displayOrderId"
            type="number"
            min="0"
            step="1"
            :class="FORM_INPUT_CLASS"
            placeholder="例如：1"
            required
          />
        </div>

        <div class="space-y-2">
          <Label :for="`${idPrefix}-selected-count`" :class="FORM_LABEL_CLASS">
            被選取次數
          </Label>
          <Input
            :id="`${idPrefix}-selected-count`"
            v-model="form.selectedCount"
            type="number"
            min="0"
            step="1"
            :class="FORM_INPUT_CLASS"
            placeholder="例如：0"
            required
          />
        </div>
      </div>
    </template>

    <div class="space-y-2">
      <Label :for="`${idPrefix}-note`" :class="FORM_LABEL_CLASS">
        {{ mode === 'create' ? '備註（選填）' : '備註' }}
      </Label>
      <Input
        :id="`${idPrefix}-note`"
        v-model="form.note"
        :maxlength="noteMaxLength(mode)"
        :class="FORM_INPUT_CLASS"
        placeholder="例如：可電話訂位"
      />
    </div>

    <div class="space-y-2">
      <Label :for="`${idPrefix}-image-url`" :class="FORM_LABEL_CLASS">
        {{ mode === 'create' ? '圖片網址（選填）' : '圖片網址' }}
      </Label>
      <Input
        :id="`${idPrefix}-image-url`"
        v-model="form.imageUrl"
        :maxlength="imageUrlMaxLength(mode)"
        :class="FORM_INPUT_CLASS"
        placeholder="https://example.com/restaurant.jpg"
      />
    </div>

    <div v-if="mode === 'edit'" class="space-y-2">
      <Label :for="`${idPrefix}-last-selected-at`" :class="FORM_LABEL_CLASS">
        最後被選取時間
      </Label>
      <Input
        :id="`${idPrefix}-last-selected-at`"
        v-model="form.lastSelectedAt"
        type="datetime-local"
        step="1"
        :class="FORM_INPUT_CLASS"
      />
    </div>
  </FormAlertDialog>
</template>
