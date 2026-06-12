<script setup lang="ts">
import WarmButton from '@/components/warm/WarmButton.vue'

defineProps<{
  pagingText: string
  page: number
  totalPages: number
  hasPrevPage: boolean
  hasNextPage: boolean
  isLoading?: boolean
}>()

const emit = defineEmits<{
  prev: []
  next: []
}>()
</script>

<template>
  <div class="flex flex-col items-center gap-3">
    <p class="text-sm text-muted-foreground">{{ pagingText }}</p>
    <div class="flex items-center justify-center gap-2">
      <WarmButton
        variant="outline-standard"
        :disabled="!hasPrevPage || isLoading"
        @click="emit('prev')"
      >
        上一頁
      </WarmButton>
      <span class="text-sm font-medium text-card-foreground">
        第 {{ page }} / {{ totalPages }} 頁
      </span>
      <WarmButton
        variant="outline-standard"
        :disabled="!hasNextPage || isLoading"
        @click="emit('next')"
      >
        下一頁
      </WarmButton>
    </div>
  </div>
</template>
