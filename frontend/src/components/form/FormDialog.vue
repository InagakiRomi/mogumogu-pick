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
      class="form-dialog flex flex-col gap-0 max-h-[min(90dvh,44rem)] w-[min(94vw,48rem)] max-w-[min(94vw,48rem)] data-[size=default]:max-w-[min(94vw,48rem)] data-[size=sm]:max-w-[min(94vw,48rem)] data-[size=default]:sm:max-w-[min(94vw,48rem)] overflow-hidden border-border bg-card p-0 text-card-foreground"
    >
      <AlertDialogHeader
        class="w-full shrink-0 grid-rows-[auto] rounded-t-xl border-b border-border bg-secondary p-4"
      >
        <AlertDialogTitle class="text-xl font-bold">{{ title }}</AlertDialogTitle>
      </AlertDialogHeader>

      <form class="flex min-h-0 flex-1 flex-col overflow-hidden" @submit.prevent="emit('submit')">
        <div class="form-dialog__fields grid min-h-0 min-w-0 flex-1 content-start gap-4 overflow-y-auto px-4 py-4">
          <slot />
        </div>

        <AlertDialogFooter class="mx-0 mb-0 shrink-0 flex-col-reverse gap-3 sm:flex-row">
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
.form-dialog__fields {
  scrollbar-gutter: stable;
}

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
  width: var(--reka-select-trigger-width);
  max-width: var(--reka-select-trigger-width);
}

:global(
  body:has([data-slot='alert-dialog-content'].form-dialog)
    [data-slot='select-content']
    [data-position='popper']
) {
  height: auto;
  max-height: min(16rem, var(--reka-select-content-available-height, 16rem));
}
</style>
