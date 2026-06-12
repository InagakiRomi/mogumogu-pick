<script setup lang="ts">
import WarmButton from '@/components/warm/WarmButton.vue'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

const FORM_DIALOG_FOOTER_BUTTON_CLASS = 'h-11 min-w-[120px] flex-1 sm:flex-none'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    submitLabel?: string
    cancelLabel?: string
    loadingLabel?: string
    loading?: boolean
    canSubmit?: boolean
  }>(),
  {
    submitLabel: '確認',
    cancelLabel: '取消',
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
</script>

<template>
  <AlertDialog :open="open" @update:open="emit('update:open', $event)">
    <AlertDialogContent
      class="max-w-[min(92vw,768px)]! data-[size=default]:max-w-[min(92vw,768px)]! data-[size=default]:sm:max-w-[min(92vw,768px)]! border-border bg-card text-card-foreground"
    >
      <AlertDialogHeader>
        <AlertDialogTitle>{{ title }}</AlertDialogTitle>
      </AlertDialogHeader>

      <form class="space-y-4" @submit.prevent="emit('submit')">
        <slot />

        <AlertDialogFooter class="mt-1 sm:gap-3">
          <WarmButton
            type="button"
            variant="outline-standard"
            :class="FORM_DIALOG_FOOTER_BUTTON_CLASS"
            :disabled="loading"
            @click="emit('cancel')"
          >
            {{ cancelLabel }}
          </WarmButton>
          <WarmButton
            type="submit"
            :class="FORM_DIALOG_FOOTER_BUTTON_CLASS"
            :disabled="!canSubmit || loading"
          >
            {{ loading ? loadingLabel : submitLabel }}
          </WarmButton>
        </AlertDialogFooter>
      </form>
    </AlertDialogContent>
  </AlertDialog>
</template>
