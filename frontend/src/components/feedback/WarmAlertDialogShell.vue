<script setup lang="ts">
import { AlertDialogContent } from '@/components/ui/alert-dialog'
import { cn } from '@/lib/utils'

const ALERT_BG_IMAGE = `${import.meta.env.BASE_URL}images/Alert.png`
</script>

<template>
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
        <slot />
      </div>
    </div>
  </AlertDialogContent>
</template>
