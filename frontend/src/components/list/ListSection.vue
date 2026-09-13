<script setup lang="ts">
defineProps<{
  title?: string
  summary?: string
}>()
</script>

<template>
  <section class="min-w-0 space-y-4 overflow-hidden rounded-lg border border-border bg-card/70 p-3 sm:p-4">
    <div
      v-if="title || summary"
      class="flex flex-col gap-1 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between sm:gap-2"
    >
      <h2 v-if="title" class="text-lg font-bold text-card-foreground">{{ title }}</h2>
      <p v-if="summary" class="text-muted-foreground">{{ summary }}</p>
    </div>
    <slot />
  </section>
</template>
