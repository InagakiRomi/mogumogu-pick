<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { SelectTrigger } from '@/components/ui/select'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <SelectTrigger
    v-bind="$attrs"
    :class="
      cn(
        'h-auto min-h-[42px] w-full whitespace-normal rounded-md border-border bg-muted/90 px-2.5 py-0 text-base text-popover-foreground shadow-[inset_0_1px_2px_rgba(121,73,52,0.12)] transition-colors',
        'placeholder:text-[rgba(118,78,60,0.72)]',
        'focus-visible:border-[rgba(168,98,68,0.75)] focus-visible:bg-[rgba(255,239,227,0.95)] focus-visible:shadow-[0_0_0_3px_rgba(238,175,143,0.26),inset_0_1px_3px_rgba(121,73,52,0.14)]',
        'data-[size=default]:h-auto',
        props.class,
      )
    "
  >
    <slot />
  </SelectTrigger>
</template>
