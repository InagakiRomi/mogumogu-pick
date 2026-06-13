<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { homeBgBackgroundStyle } from '@/lib/utils'

const route = useRoute()

const pageTitle = computed(() => {
  const title = route.meta.pageTitle
  return typeof title === 'string' ? title : '頁面建置中'
})
</script>

<template>
  <main
    class="min-h-[calc(100vh-88px)] bg-cover bg-center bg-no-repeat px-4 py-10"
    :style="homeBgBackgroundStyle"
  >
    <div class="mx-auto max-w-xl rounded-xl border border-border bg-card/90 px-6 py-10 text-center shadow-sm">
      <h1 class="text-2xl font-bold text-card-foreground">{{ pageTitle }}</h1>
      <p class="mt-3 text-muted-foreground">此功能頁面尚在開發中。</p>
    </div>
  </main>
</template>
