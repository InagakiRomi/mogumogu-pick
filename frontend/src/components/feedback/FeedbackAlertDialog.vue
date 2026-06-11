<script setup lang="ts">
import { computed } from 'vue'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { cn } from '@/lib/utils'

const ALERT_BG_IMAGE = '/images/Alert.png'

const { open, message, type, onOpenChange } = useFeedbackDialog()

const title = computed(() => (type.value === 'error' ? '提示' : '完成'))
</script>

<template>
  <AlertDialog :open="open" @update:open="onOpenChange">
    <AlertDialogContent
      :class="
        cn(
          'grid w-[min(92vw,640px)]! max-w-[min(92vw,640px)]! gap-0 border-0 bg-transparent p-0 shadow-none ring-0',
          'aspect-2/1 bg-size-[100%_100%] bg-center bg-no-repeat',
        )
      "
      :style="{ backgroundImage: `url('${ALERT_BG_IMAGE}')` }"
    >
      <div class="flex h-full items-center justify-center px-16">
        <div class="flex w-full translate-y-4 flex-col items-center gap-10 text-center">
          <AlertDialogTitle class="w-full text-center text-2xl font-bold text-[#5e3a28]">
            {{ title }}
          </AlertDialogTitle>
          <AlertDialogDescription
            class="w-full text-center text-xl font-semibold tracking-wide text-[#5e3a28]/90"
          >
            {{ message }}
          </AlertDialogDescription>
          <AlertDialogAction
            @click="onOpenChange(false)"
            class="h-10 min-w-[96px] rounded-lg border border-[rgba(176,68,68,0.38)] bg-linear-to-br from-[#d78867] to-[#c96d57] font-semibold text-primary-foreground shadow-[0_8px_18px_rgba(138,73,52,0.2)] hover:from-[#de8f6c] hover:to-[#d3735d]"
          >
            確定
          </AlertDialogAction>
        </div>
      </div>
    </AlertDialogContent>
  </AlertDialog>
</template>
