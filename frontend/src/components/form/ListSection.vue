<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  title?: string
  summary?: string
  sectionClass?: HTMLAttributes['class']
}>()
</script>

<template>
  <section :class="cn('space-y-4 rounded-lg border border-border bg-card/70 p-4', props.sectionClass)">
    <div
      v-if="title || summary || $slots.header"
      class="flex flex-wrap items-center justify-between gap-2"
    >
      <slot name="header">
        <h2 v-if="title" class="text-lg font-bold text-card-foreground">{{ title }}</h2>
        <p v-if="summary" class="text-sm text-muted-foreground">{{ summary }}</p>
      </slot>
    </div>
    <slot />
  </section>
</template>
