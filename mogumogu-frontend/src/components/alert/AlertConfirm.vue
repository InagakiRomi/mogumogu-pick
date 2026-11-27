<script setup lang="ts">
import PrimaryAlert from '@/components/common/PrimaryAlert.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import { AlertDialog, AlertDialogDescription, AlertDialogTitle } from '@/components/ui/alert-dialog'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    confirmLabel?: string
    cancelLabel?: string
    loadingLabel?: string
    loading?: boolean
    canConfirm?: boolean
  }>(),
  {
    confirmLabel: '確認',
    cancelLabel: '取消',
    loadingLabel: '處理中...',
    loading: false,
    canConfirm: true,
  },
)

const emit = defineEmits<{
  'update:open': [boolean]
  confirm: []
  cancel: []
}>()

function handleCancel() {
  emit('update:open', false)
  emit('cancel')
}
</script>

<template>
  <AlertDialog :open="open" @update:open="emit('update:open', $event)">
    <PrimaryAlert>
      <template #title>
        <AlertDialogTitle class="w-full text-center text-2xl font-bold text-popover-foreground">
          {{ title }}
        </AlertDialogTitle>
      </template>
      <AlertDialogDescription
        v-if="description"
        class="w-full text-pretty wrap-break-word text-center text-xl font-semibold tracking-wide text-popover-foreground/90"
      >
        {{ description }}
      </AlertDialogDescription>
      <template #actions>
        <div class="flex flex-wrap items-center justify-center gap-3">
          <PrimaryButton
            type="button"
            variant="outline"
            class="min-w-24"
            :disabled="loading"
            @click="handleCancel"
          >
            {{ cancelLabel }}
          </PrimaryButton>
          <PrimaryButton
            type="button"
            class="min-w-24"
            :disabled="!canConfirm || loading"
            @click="emit('confirm')"
          >
            {{ loading ? loadingLabel : confirmLabel }}
          </PrimaryButton>
        </div>
      </template>
    </PrimaryAlert>
  </AlertDialog>
</template>
