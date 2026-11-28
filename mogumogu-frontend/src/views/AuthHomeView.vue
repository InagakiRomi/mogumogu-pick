<script setup lang="ts">
import { Card } from '@/components/ui/card'
</script>

<template>
  <main class="auth-page">
    <Card class="auth-card"> </Card>
  </main>
</template>

<style scoped>
.auth-page {
  flex: 1;
  display: grid;
  place-items: center;
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 720px;
  padding: 32px;
}
</style>
