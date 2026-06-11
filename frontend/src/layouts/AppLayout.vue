<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import { authToken } from '@/lib/authToken'

const route = useRoute()

const showHeader = computed(() => {
  if (!authToken.value) {
    return false
  }

  return route.name !== 'home' && route.name !== 'no-group'
})
</script>

<template>
  <div class="min-h-screen">
    <AppHeader v-if="showHeader" />
    <RouterView />
  </div>
</template>
