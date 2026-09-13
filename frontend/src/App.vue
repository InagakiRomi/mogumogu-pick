<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AlertFeedback from '@/components/alert/AlertFeedback.vue'
import AppNavbar from '@/layouts/AppNavbar.vue'
import { authToken } from '@/lib/authToken'

const route = useRoute()

const showNavbar = computed(
  () => !!authToken.value && !route.meta.hideNavbar,
)
</script>

<template>
  <div class="flex h-dvh flex-col overflow-hidden">
    <AppNavbar v-if="showNavbar" />
    <div class="app-page min-h-0 flex-1 overflow-x-hidden overflow-y-auto [&>*]:min-h-full">
      <RouterView />
    </div>
    <AlertFeedback />
  </div>
</template>
