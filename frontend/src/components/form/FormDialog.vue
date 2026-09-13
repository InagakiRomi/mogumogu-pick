<script setup lang="ts">
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    submitLabel?: string
    loadingLabel?: string
    loading?: boolean
    canSubmit?: boolean
  }>(),
  {
    submitLabel: '確認',
    loadingLabel: '處理中...',
    loading: false,
    canSubmit: true,
  },
)

const emit = defineEmits<{
  'update:open': [boolean]
  submit: []
  cancel: []
}>()

function handleCancel() {
  emit('update:open', false)
  emit('cancel')
}
</script>

<template>
  <AlertDialog :open="open" @update:open="emit('update:open', $event)">
    <AlertDialogContent
      class="form-dialog max-h-[min(90dvh,44rem)] w-[min(94vw,48rem)] max-w-[min(94vw,48rem)] overflow-y-auto border-border bg-card text-card-foreground"
    >
      <AlertDialogHeader>
        <AlertDialogTitle>{{ title }}</AlertDialogTitle>
      </AlertDialogHeader>

      <form class="grid gap-4" @submit.prevent="emit('submit')">
        <div class="form-dialog__fields grid min-w-0 gap-4">
          <slot />
        </div>

        <AlertDialogFooter class="mt-1 flex-col-reverse gap-3 sm:flex-row">
          <PrimaryButton
            type="button"
            variant="outline"
            class="h-11 w-full min-w-0 sm:w-auto sm:min-w-30 sm:flex-none"
            :disabled="loading"
            @click="handleCancel"
          >
            取消
          </PrimaryButton>

          <PrimaryButton
            type="submit"
            class="h-11 w-full min-w-0 sm:w-auto sm:min-w-30 sm:flex-none"
            :disabled="!canSubmit || loading"
          >
            {{ loading ? loadingLabel : submitLabel }}
          </PrimaryButton>
        </AlertDialogFooter>
      </form>
    </AlertDialogContent>
  </AlertDialog>
</template>

<style scoped>
.form-dialog__fields > :deep(*) {
  display: grid;
  min-width: 0;
  gap: 0.5rem;
}

.form-dialog__fields :deep(label) {
  font-weight: 700;
  color: var(--muted-foreground);
}

.form-dialog__fields :deep([data-slot='input']) {
  height: 2.5rem;
  padding-inline: 0.625rem;
  border-color: var(--border);
  border-radius: var(--radius-md);
  background: color-mix(in oklab, var(--muted) 90%, transparent);
  color: var(--popover-foreground);
}

:global(body:has([data-slot='alert-dialog-content'].form-dialog) [data-slot='select-content']) {
  z-index: 10000;
}
</style>
