<script setup lang="ts">
import type { HTMLAttributes } from "vue"
import { cn } from "@/lib/utils"

const props = defineProps<{
  class?: HTMLAttributes["class"]
}>()
</script>

<template>
  <tr
    data-slot="table-row"
    :class="cn('border-b transition-colors hover:bg-[rgba(255,185,135,0.72)] data-[state=selected]:bg-[rgba(255,200,160,0.85)] has-aria-expanded:bg-[rgba(255,200,160,0.85)]', props.class)"
  >
    <slot />
  </tr>
</template>
