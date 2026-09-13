<script setup lang="ts">
defineProps<{
  title?: string
  summary?: string
}>()
</script>

<template>
  <section class="space-y-4 rounded-lg border border-border bg-card/70 p-4">
    <div v-if="title || summary" class="flex flex-wrap items-center justify-between gap-2">
      <h2 v-if="title" class="text-lg font-bold text-card-foreground">{{ title }}</h2>
      <p v-if="summary" class="text-muted-foreground">{{ summary }}</p>
    </div>
    <slot />
  </section>
</template>
