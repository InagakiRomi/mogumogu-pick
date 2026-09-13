<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import FeedbackAlertDialog from '@/components/feedback/FeedbackAlertDialog.vue'
import AppNavbar from '@/layouts/AppNavbar.vue'
import { authToken } from '@/lib/authToken'

const route = useRoute()

const showNavbar = computed(
  () => !!authToken.value && !route.meta.hideNavbar,
)
</script>

<template>
  <AppNavbar v-if="showNavbar" />
  <RouterView />
  <FeedbackAlertDialog />
</template>
