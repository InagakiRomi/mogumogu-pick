<script setup lang="ts">
import { computed } from 'vue'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogDescription,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import WarmAlertDialogShell from '@/components/feedback/WarmAlertDialogShell.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'

const { open, message, type, onOpenChange } = useFeedbackDialog()

const title = computed(() => (type.value === 'error' ? '提示' : '完成'))

const confirmButtonClass =
  'h-10 min-w-[96px] rounded-lg border border-[rgba(176,68,68,0.38)] bg-linear-to-br from-[#d78867] to-[#c96d57] font-semibold text-primary-foreground shadow-[0_8px_18px_rgba(138,73,52,0.2)] hover:from-[#de8f6c] hover:to-[#d3735d]'
</script>

<template>
  <AlertDialog :open="open" @update:open="onOpenChange">
    <WarmAlertDialogShell>
      <AlertDialogTitle class="w-full text-center text-2xl font-bold text-[#5e3a28]">
        {{ title }}
      </AlertDialogTitle>
      <AlertDialogDescription
        class="w-full text-center text-xl font-semibold tracking-wide text-[#5e3a28]/90"
      >
        {{ message }}
      </AlertDialogDescription>
      <AlertDialogAction :class="confirmButtonClass" @click="onOpenChange(false)">
        確定
      </AlertDialogAction>
    </WarmAlertDialogShell>
  </AlertDialog>
</template>
