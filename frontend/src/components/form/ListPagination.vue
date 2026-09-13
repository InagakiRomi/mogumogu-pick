<script setup lang="ts">
import PrimaryButton from '@/components/common/PrimaryButton.vue'

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
      <PrimaryButton
        variant="outline"
        :disabled="!hasPrevPage || isLoading"
        @click="emit('prev')"
      >
        上一頁
      </PrimaryButton>
      <span class="text-sm font-medium text-card-foreground">
        第 {{ page }} / {{ totalPages }} 頁
      </span>
      <PrimaryButton
        variant="outline"
        :disabled="!hasNextPage || isLoading"
        @click="emit('next')"
      >
        下一頁
      </PrimaryButton>
    </div>
  </div>
</template>
