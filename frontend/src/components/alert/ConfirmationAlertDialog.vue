<script setup lang="ts">
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

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
    <AlertDialogContent class="confirmation-alert-dialog">
      <AlertDialogHeader>
        <AlertDialogTitle>{{ title }}</AlertDialogTitle>
        <AlertDialogDescription v-if="description" class="confirmation-alert-dialog__description">
          {{ description }}
        </AlertDialogDescription>
      </AlertDialogHeader>

      <AlertDialogFooter class="confirmation-alert-dialog__footer">
        <PrimaryButton
          type="button"
          variant="outline"
          class="confirmation-alert-dialog__button"
          :disabled="loading"
          @click="handleCancel"
        >
          {{ cancelLabel }}
        </PrimaryButton>
        <PrimaryButton
          type="button"
          class="confirmation-alert-dialog__button"
          :disabled="!canConfirm || loading"
          @click="emit('confirm')"
        >
          {{ loading ? loadingLabel : confirmLabel }}
        </PrimaryButton>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>

<style scoped>
.confirmation-alert-dialog {
  width: min(92vw, 48rem);
  max-width: min(92vw, 48rem) !important;
  border-color: var(--border);
  background: var(--card);
  color: var(--card-foreground);
}

.confirmation-alert-dialog__description {
  text-align: center;
  text-wrap: pretty;
  overflow-wrap: anywhere;
}

.confirmation-alert-dialog__footer {
  margin-top: 0.25rem;
}

.confirmation-alert-dialog__button {
  min-width: 7.5rem;
  height: 2.75rem;
  flex: 1 1 0%;
}

@media (min-width: 640px) {
  .confirmation-alert-dialog__footer {
    gap: 0.75rem;
  }

  .confirmation-alert-dialog__button {
    flex: none;
  }
}
</style>
