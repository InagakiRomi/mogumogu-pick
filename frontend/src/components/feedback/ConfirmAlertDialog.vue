<script setup lang="ts">
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import WarmAlertDialogShell from '@/components/feedback/WarmAlertDialogShell.vue'

const open = defineModel<boolean>('open', { required: true })

withDefaults(
  defineProps<{
    title: string
    confirmLabel?: string
    cancelLabel?: string
    loading?: boolean
    loadingLabel?: string
  }>(),
  {
    confirmLabel: '確認',
    cancelLabel: '取消',
    loading: false,
    loadingLabel: '處理中...',
  },
)

const emit = defineEmits<{
  confirm: []
}>()

const confirmButtonClass =
  'h-10 min-w-[96px] rounded-lg border border-[rgba(176,68,68,0.38)] bg-linear-to-br from-[#d78867] to-[#c96d57] font-semibold text-primary-foreground shadow-[0_8px_18px_rgba(138,73,52,0.2)] hover:from-[#de8f6c] hover:to-[#d3735d]'

const cancelButtonClass =
  'h-10 min-w-[96px] rounded-lg border border-[rgba(146,80,58,0.32)] bg-linear-to-br from-[rgba(255,252,248,0.98)] to-[rgba(255,236,220,0.95)] font-semibold text-[#5c4033] shadow-[0_2px_8px_rgba(138,73,52,0.1)] hover:border-[rgba(146,80,58,0.45)] hover:from-[rgba(255,245,235,1)] hover:to-[rgba(255,228,210,0.98)] hover:text-[#4a2c2a]'
</script>

<template>
  <AlertDialog :open="open" @update:open="open = $event">
    <WarmAlertDialogShell>
      <template #title>
        <AlertDialogTitle class="w-full text-center text-2xl font-bold text-[#5e3a28]">
          {{ title }}
        </AlertDialogTitle>
      </template>
      <AlertDialogDescription
        class="w-full text-pretty wrap-break-word text-center text-xl font-semibold tracking-wide text-[#5e3a28]/90"
      >
        <slot />
      </AlertDialogDescription>
      <template #actions>
        <div class="flex flex-wrap items-center justify-center gap-3">
          <AlertDialogCancel :disabled="loading" :class="cancelButtonClass">
            {{ cancelLabel }}
          </AlertDialogCancel>
          <AlertDialogAction
            :disabled="loading"
            :class="confirmButtonClass"
            @click="emit('confirm')"
          >
            {{ loading ? loadingLabel : confirmLabel }}
          </AlertDialogAction>
        </div>
      </template>
    </WarmAlertDialogShell>
  </AlertDialog>
</template>
