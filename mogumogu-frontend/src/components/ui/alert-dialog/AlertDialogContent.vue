<script setup lang="ts">
import type { AlertDialogContentEmits, AlertDialogContentProps } from "reka-ui"
import type { HTMLAttributes } from "vue"
import { reactiveOmit, useScrollLock } from "@vueuse/core"
import {
  AlertDialogContent,
  AlertDialogPortal,
  injectDialogRootContext,
  useForwardPropsEmits,
} from "reka-ui"
import { watch } from "vue"
import { cn } from "@/lib/utils"

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<AlertDialogContentProps & {
    class?: HTMLAttributes["class"]
    size?: "default" | "sm"
  }>(),
  {
    size: "default",
    disableOutsidePointerEvents: false,
  },
)
const emits = defineEmits<AlertDialogContentEmits>()

const delegatedProps = reactiveOmit(props, "class", "size")

const forwarded = useForwardPropsEmits(delegatedProps, emits)

const rootContext = injectDialogRootContext()
const isBodyLocked = useScrollLock(document.body)

watch(
  () => rootContext.open.value,
  (isOpen) => {
    isBodyLocked.value = isOpen
  },
  { immediate: true },
)
</script>

<template>
  <AlertDialogPortal>
    <div
      v-if="rootContext.open.value"
      data-slot="alert-dialog-overlay"
      class="fixed inset-0 z-9998 bg-black/10 supports-backdrop-filter:backdrop-blur-xs"
      aria-hidden="true"
    />
    <AlertDialogContent
      data-slot="alert-dialog-content"
      :data-size="size"
      v-bind="{ ...$attrs, ...forwarded }"
      :class="
        cn(
          'data-open:animate-in data-closed:animate-out data-closed:fade-out-0 data-open:fade-in-0 data-closed:zoom-out-95 data-open:zoom-in-95 bg-popover text-popover-foreground ring-foreground/10 gap-4 rounded-xl p-4 ring-1 duration-100 data-[size=default]:max-w-xs data-[size=sm]:max-w-xs data-[size=default]:sm:max-w-sm group/alert-dialog-content fixed top-1/2 left-1/2 z-9999 grid w-full -translate-x-1/2 -translate-y-1/2 outline-none',
          props.class,
        )
      "
    >
      <slot />
    </AlertDialogContent>
  </AlertDialogPortal>
</template>
