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
      class="form-dialog w-[min(92vw,48rem)] max-w-[min(92vw,48rem)] border-border bg-card text-card-foreground"
    >
      <AlertDialogHeader>
        <AlertDialogTitle>{{ title }}</AlertDialogTitle>
      </AlertDialogHeader>

      <form class="grid gap-4" @submit.prevent="emit('submit')">
        <div class="form-dialog__fields grid gap-4">
          <slot />
        </div>

        <AlertDialogFooter class="mt-1 gap-3">
          <PrimaryButton
            type="button"
            variant="outline"
            class="h-11 min-w-30 flex-1 sm:flex-none"
            :disabled="loading"
            @click="handleCancel"
          >
            取消
          </PrimaryButton>

          <PrimaryButton
            type="submit"
            class="h-11 min-w-30 flex-1 sm:flex-none"
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
.form-dialog__fields :deep(div) {
  display: grid;
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
