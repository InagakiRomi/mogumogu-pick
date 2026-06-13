<script setup lang="ts">
import { AlertDialogContent } from '@/components/ui/alert-dialog'
import { cn, publicAsset } from '@/lib/utils'

const alertBgImage = publicAsset('images/Alert.png')
</script>

<template>
  <AlertDialogContent
    :class="
      cn(
        'flex! flex-col! w-[min(92vw,640px)]! min-w-[min(92vw,640px)]! max-w-[min(92vw,640px)]! gap-0 border-0 bg-transparent p-0 shadow-none ring-0',
        'aspect-2/1 overflow-hidden bg-size-[100%_100%] bg-center bg-no-repeat',
      )
    "
    :style="{ backgroundImage: `url('${alertBgImage}')` }"
  >
    <div class="flex min-h-0 flex-1 flex-col px-12 text-center sm:px-16">
      <header class="flex shrink-0 items-end justify-center pb-2 pt-16">
        <slot name="title" />
      </header>

      <div class="content-scroll h-0 min-h-0 flex-1 overscroll-contain">
        <div class="flex min-h-full items-center justify-center py-1">
          <div class="w-full">
            <slot />
          </div>
        </div>
      </div>

      <footer class="flex shrink-0 items-start justify-center pb-11 pt-2">
        <slot name="actions" />
      </footer>
    </div>
  </AlertDialogContent>
</template>

<style scoped>
.content-scroll {
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(94, 58, 40, 0.45) rgba(94, 58, 40, 0.12);
}

.content-scroll::-webkit-scrollbar {
  width: 8px;
}

.content-scroll::-webkit-scrollbar-track {
  border-radius: 9999px;
  background: rgba(94, 58, 40, 0.1);
}

.content-scroll::-webkit-scrollbar-thumb {
  border-radius: 9999px;
  background: rgba(94, 58, 40, 0.4);
}

.content-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(94, 58, 40, 0.55);
}
</style>
